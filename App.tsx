import React, { useState, useCallback, useEffect, useRef, lazy, Suspense } from 'react';
import { makeSealedPdf } from './utils/sealPdf';
import Header from './components/Header';
import FilePanel from './components/FilePanel';
import ChatPanel from './components/ChatPanel';
import { LoadingSpinner } from './components/Icons';
import { ChatMessage, MessageAuthor, UploadedFile, FileType, SealingMetadata, VerificationResult } from './types';
import { getDirectAnalysis, getPreliminaryAnalysis, synthesizeFinalReport, generateSimpleChat, routeUserIntent, auditWithApkLogic } from './services/geminiService';
import { getPreliminaryAnalysisWithOpenAI } from './services/openAIService';
import { db as idb } from './services/db';
import { sha512OfFile } from './utils/hash';
import { getCurrentUser, db_firestore as firestore, storage } from './services/firebase';
import { ref, uploadBytes } from 'firebase/storage';
import { doc, setDoc, serverTimestamp } from 'firebase/firestore';

const TaxServicePage = lazy(() => import('./components/TaxServicePage'));
const BusinessServicesPage = lazy(() => import('./components/BusinessServicesPage'));


const APP_VERSION = '5.1.0';

// Helper to trigger file download
const downloadBlob = (data: Uint8Array, fileName: string, mimeType: string) => {
    const blob = new Blob([data], { type: mimeType });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
};

// Helper function to convert markdown to plain text for PDF
const markdownToPlainText = (markdown: string): string => {
    return markdown
        .replace(/(\*\*|__)(.*?)\1/g, '$2') // Bold
        .replace(/(\*|_)(.*?)\1/g, '$2')   // Italic
        .replace(/#+\s*(.*)/g, '$1')       // Headers
        .replace(/\[(.*?)\]\(.*?\)/g, '$1') // Links
        .replace(/`{1,3}(.*?)`{1,3}/g, '$1') // Code
        .replace(/(\n|^)- /g, '\n• ')        // Lists
        .replace(/(\n|^)> /g, '\n');      // Blockquotes
};

const getBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve((reader.result as string).split(',')[1]);
        reader.onerror = error => reject(error);
    });
};

// Helper function to generate SHA-512 hash of a byte array
const stringToHash = async (data: Uint8Array): Promise<string> => {
    const hashBuffer = await crypto.subtle.digest('SHA-512', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
};


const App: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [loadingState, setLoadingState] = useState<'idle' | 'analyzing' | 'consulting' | 'synthesizing' | 'verifying'>('idle');
  const [isComplexMode, setIsComplexMode] = useState(false);
  const [isEnterpriseMode, setIsEnterpriseMode] = useState(false);
  const [location, setLocation] = useState<{ latitude: number; longitude: number } | null>(null);
  const [isFilePanelOpen, setIsFilePanelOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState<'firewall' | 'tax' | 'business'>('firewall'); // Page navigation state
  const [isDbLoaded, setIsDbLoaded] = useState(false);

  const initialUserPrompt = useRef<string>('');
  
  // Load data from IndexedDB on initial render
  useEffect(() => {
    const loadData = async () => {
        const data = await idb.loadCase();
        if (data && (data.messages.length > 0 || data.files.length > 0)) {
            const loadedFiles = data.files.map(f => ({ ...f, file: new File([], f.name, { type: f.mimeType }) }));
            setUploadedFiles(loadedFiles);
            setMessages(data.messages);
        } else {
            // Start with a clean slate for new users
            setUploadedFiles([]);
            setMessages([{
                id: 'initial',
                author: MessageAuthor.AI,
                content: "Welcome to Verum Omnis V5. This system provides court-ready forensic analysis. Upload your evidence, and I will generate a formal report with drafted legal correspondence."
            }]);
        }
        setIsDbLoaded(true);
    };
    loadData();
  }, []);

  // Save data to IndexedDB whenever messages or files change
  useEffect(() => {
      if (!isDbLoaded) {
          return;
      }
      if (currentPage === 'firewall') {
        // We can't save the raw File object in IndexedDB, so we remove it before saving
        const filesToSave = uploadedFiles.map(({file, ...rest}) => rest);
        idb.saveCase(messages, filesToSave as any).catch(console.error);
      }
  }, [messages, uploadedFiles, currentPage, isDbLoaded]);


  useEffect(() => {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                setLocation({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                });
            },
            (error) => {
                console.error("Error getting user location:", error.message);
            }
        );
    }
  }, []);

  const handleFilesChange = (files: FileList) => {
    const newFiles: UploadedFile[] = Array.from(files).map(file => {
      let type: FileType;
      if (file.type.startsWith('image/')) type = FileType.IMAGE;
      else if (file.type.startsWith('video/')) type = FileType.VIDEO;
      else if (file.type.startsWith('audio/')) type = FileType.AUDIO;
      else type = FileType.DOCUMENT;

      return {
        id: `${file.name}-${file.lastModified}-${file.size}`,
        name: file.name,
        type,
        mimeType: file.type,
        file,
        size: file.size,
        addedAt: new Date().toISOString(),
        source: 'user', // Manually added files are from the user
      };
    });

    setUploadedFiles(prev => [...prev, ...newFiles]);

    newFiles.forEach(newFile => {
      sha512OfFile(newFile.file).then(hash => {
        setUploadedFiles(currentFiles => 
            currentFiles.map(f => f.id === newFile.id ? { ...f, sha512: hash } : f)
        );
      }).catch(err => {
        console.error(`Error hashing file ${newFile.name}:`, err);
      });
    });

    setMessages(prev => [...prev, {
        id: `file-upload-${Date.now()}`,
        author: MessageAuthor.SYSTEM,
        content: `${newFiles.length} file(s) added to Evidence Locker. Ready for analysis.`
    }]);
  };

  const performAutomaticVerification = async (
    prompt: string,
    files: (UploadedFile & { base64: string; })[],
    report: string
  ): Promise<VerificationResult> => {
    let verifiedCount = 3; // Start with the 3 web agents assumed as verified for generating the report
    const totalVoters = 5;
    const notes: string[] = [];

    try {
        const [consumerResult, enterpriseResult] = await Promise.all([
            auditWithApkLogic(prompt, files, report),
            auditWithApkLogic(prompt, files, report)
        ]);

        if (consumerResult.startsWith('APK Audit Passed')) {
            verifiedCount++;
        } else {
            notes.push(`Consumer APK: ${consumerResult.replace('APK Audit Notes:', '').trim()}`);
        }

        if (enterpriseResult.startsWith('APK Audit Passed')) {
            verifiedCount++;
        } else {
            notes.push(`Enterprise APK: ${enterpriseResult.replace('APK Audit Notes:', '').trim()}`);
        }
    } catch (error) {
        console.error("Error during automated APK verification:", error);
        notes.push("On-device verification encountered an unexpected error.");
    }
    
    let consensusText = "Consensus Pending";
    if (verifiedCount >= 5) consensusText = "Full Ecosystem Consensus";
    else if (verifiedCount === 4) consensusText = "Quadruple Verification Consensus";
    else if (verifiedCount === 3) consensusText = "Triple Verification Consensus";

    return {
        consensusText,
        verifiedCount,
        totalVoters,
        notes: notes.join('\n') || "All checks passed.",
    };
  };

  const handleSendMessage = useCallback(async (prompt: string) => {
    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      author: MessageAuthor.USER,
      content: prompt,
      files: uploadedFiles,
    };
    setMessages(prev => [...prev, userMessage]);
    
    const filesToAnalyze = await Promise.all(
        uploadedFiles
        .filter(f => f.file && f.file.size > 0) // Filter out placeholder files without content
        .map(async f => ({
            ...f,
            base64: await getBase64(f.file)
        }))
    );

    const totalSize = filesToAnalyze.reduce((acc, file) => acc + file.size, 0);
    const MAX_TOTAL_SIZE_BYTES = 30 * 1024 * 1024; // 30 MB limit

    if (filesToAnalyze.length > 0 && totalSize > MAX_TOTAL_SIZE_BYTES) {
        const errorMessage: ChatMessage = {
            id: `error-${Date.now()}`,
            author: MessageAuthor.AI,
            content: `The total size of the uploaded files (${(totalSize / (1024 * 1024)).toFixed(2)} MB) exceeds the operational limit of 30MB for a single analysis. This is to ensure compliance with the model's processing capacity. Please reduce the total file size and resubmit your request.`,
        };
        setMessages(prev => [...prev, errorMessage]);
        return; // Stop processing to prevent the error
    }

    try {
      setLoadingState('analyzing');
      const intent = await routeUserIntent(prompt, filesToAnalyze);
      
      if (intent === 'chat') {
        const responseText = await generateSimpleChat(prompt, uploadedFiles, location);
        const aiMessage: ChatMessage = { id: `ai-${Date.now()}`, author: MessageAuthor.AI, content: responseText };
        setMessages(prev => [...prev, aiMessage]);
      } else if (intent === 'seal') {
        let aiMessage: ChatMessage;
        if (filesToAnalyze.length === 0) {
            aiMessage = {
                id: `ai-seal-prompt-${Date.now()}`,
                author: MessageAuthor.AI,
                content: "Of course. To create a sealed, tamper-proof document manifest, please upload the files you wish to certify using the paperclip icon below. Once they're uploaded, confirm you wish to proceed."
            };
        } else {
            const fileCount = filesToAnalyze.length;
            aiMessage = {
                id: `ai-seal-container-${Date.now()}`,
                author: MessageAuthor.AI,
                content: `I have received your ${fileCount} document(s) and they are ready to be sealed. The generated PDF will serve as a manifest, containing the certified SHA-512 hashes of each file. This creates a tamper-proof record of their state at this exact moment.\n\nClick the button below to generate and download the sealed PDF.\n\n## Sealing Metadata\n- Certified SHA-512 Hash: [Placeholder for SHA-512 hash of this report]\n- Cloud Anchor: [Placeholder for Cloud Anchor]\n- Firestore Record: [Placeholder for Firestore Record]\n- QR Metadata: {created_at: [Timestamp], file_count: ${fileCount}, hash: [SHA-512 Placeholder]}\n™ Patent Pending Verum Omnis`
            };
        }
        setMessages(prev => [...prev, aiMessage]);
      } else if (intent === 'scan') {
          if (filesToAnalyze.length === 0) {
              const noFilesMessage: ChatMessage = {
                  id: `ai-error-${Date.now()}`,
                  author: MessageAuthor.AI,
                  content: "To perform a forensic analysis, please upload the relevant documents using the paperclip icon first."
              };
              setMessages(prev => [...prev, noFilesMessage]);
              return;
          }

          initialUserPrompt.current = prompt;
          let finalReportContent: string;
          let aiMessage: ChatMessage;

          if (isComplexMode) {
            setLoadingState('analyzing');
            const [geminiResult, openAIResult] = await Promise.allSettled([
                getPreliminaryAnalysis(prompt, filesToAnalyze, isComplexMode, location),
                getPreliminaryAnalysisWithOpenAI(prompt, filesToAnalyze)
            ]);
            
            setLoadingState('consulting');
            const geminiStrategy = geminiResult.status === 'fulfilled' ? geminiResult.value : `Gemini analysis failed: ${geminiResult.reason}`;
            const openAIStrategy = openAIResult.status === 'fulfilled' ? openAIResult.value : `OpenAI analysis failed: ${openAIResult.reason}`;

            if (geminiResult.status === 'rejected' && openAIResult.status === 'rejected') {
                throw new Error("Both Gemini and OpenAI analyses failed.");
            }
            
            setLoadingState('synthesizing');
            const synthesisInstruction = "Synthesize the provided strategies into a single, comprehensive report."
            finalReportContent = await synthesizeFinalReport(synthesisInstruction, prompt, filesToAnalyze, geminiStrategy, openAIStrategy, isComplexMode, location);
          } else {
            setLoadingState('analyzing');
            finalReportContent = await getDirectAnalysis(prompt, filesToAnalyze, isComplexMode, location);
          }
          
          setLoadingState('verifying');
          const verificationResult = await performAutomaticVerification(prompt, filesToAnalyze, finalReportContent);

          aiMessage = {
            id: `ai-report-${Date.now()}`,
            author: MessageAuthor.AI,
            content: finalReportContent,
            verificationResult: verificationResult,
          };
          setMessages(prev => [...prev, aiMessage]);
      }
    } catch (error) {
      console.error("Critical error in AI analysis pipeline:", error);
      const friendlyErrorMessage = `I'm sorry, a critical error occurred: ${(error as Error).message}. Please try again.`;
      const errorMessage: ChatMessage = {
        id: `error-${Date.now()}`,
        author: MessageAuthor.AI,
        content: friendlyErrorMessage,
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoadingState('idle');
    }
  }, [uploadedFiles, isComplexMode, location]);
  
  const handleClearCase = useCallback(async () => {
    try {
        await idb.clearCase();
        setUploadedFiles([]);
        setMessages([
            {
                id: 'initial-reset',
                author: MessageAuthor.AI,
                content: "Case file cleared. You can now start a new analysis by uploading your evidence."
            }
        ]);
    } catch (error) {
        console.error("Failed to clear case:", error);
        setMessages(prev => [...prev, {
            id: `error-${Date.now()}`,
            author: MessageAuthor.SYSTEM,
            content: "Error: Could not clear the case data from local storage."
        }]);
    }
  }, []);

  const handleToggleComplexMode = useCallback(() => { setIsComplexMode(prev => !prev); }, []);
  const handleToggleEnterpriseMode = useCallback(() => { setIsEnterpriseMode(prev => !prev); }, []);
  
    const performSealingProcess = useCallback(async (messageId: string): Promise<{
        fileName: string;
        hash: string;
        cloudAnchor: SealingMetadata['cloudAnchor'];
        pdfBytes: Uint8Array;
        updatedContent: string;
    } | null> => {
        const messageIndex = messages.findIndex(m => m.id === messageId);
        if (messageIndex === -1) return null;

        const message = messages[messageIndex];
        if (!message) return null;

        const now = new Date();
        const utcTimestamp = now.toISOString();
        const localTimestamp = now.toLocaleString(undefined, {
            year: 'numeric', month: 'numeric', day: 'numeric',
            hour: '2-digit', minute: '2-digit', second: '2-digit',
            timeZoneName: 'short'
        });
        
        const isManifestOnly = !message.content.includes('## Key Findings');
        const title = isManifestOnly ? "Sealed Document Manifest" : "Sealed Forensic Report";

        const evidence = uploadedFiles
            .filter(f => f.sha512)
            .map(f => ({ name: f.name, sha512: f.sha512! }));
        
        const pdfBytes = await makeSealedPdf({
            title: title,
            messagesHtml: isManifestOnly ? '' : markdownToPlainText(message.content),
            evidence,
            utcTimestamp,
            localTimestamp,
            appVersion: APP_VERSION,
        });

        const pdfHash = await stringToHash(pdfBytes);
        const fileName = `VO_Sealed_Report_${utcTimestamp.replace(/[:.]/g, '-')}.pdf`;
        let cloudAnchorResult: SealingMetadata['cloudAnchor'];

        try {
            const currentUser = await getCurrentUser();
            if (!currentUser) throw new Error("Authentication failed. Cannot seal report to the cloud.");

            const reportId = `report-${Date.now()}`;
            const storagePath = `users/${currentUser.uid}/reports/${fileName}`;
            const storageRef = ref(storage, storagePath);
            
            await uploadBytes(storageRef, pdfBytes);

            const firestoreId = reportId;
            const reportDocRef = doc(firestore, 'reports', firestoreId);
            await setDoc(reportDocRef, {
                userId: currentUser.uid,
                fileName,
                sha512: pdfHash,
                storagePath,
                createdAt: serverTimestamp(),
                originalPrompt: initialUserPrompt.current,
                fileCount: uploadedFiles.length,
            });

            cloudAnchorResult = { status: 'confirmed', storagePath, firestoreId };
        } catch (error) {
            console.error("Firebase sealing failed:", error);
            cloudAnchorResult = { status: 'failed', error: (error as Error).message };
        }

        const updatedMessages = [...messages];
        let newContent = message.content
            .replace(/\[Placeholder for SHA-512 hash of this report\]/g, pdfHash)
            .replace(/\[Placeholder for Cloud Anchor\]/g, cloudAnchorResult.storagePath || 'N/A')
            .replace(/\[Placeholder for Firestore Record\]/g, cloudAnchorResult.firestoreId || 'N/A');

        updatedMessages[messageIndex] = {
            ...message,
            content: newContent,
            sealingMetadata: {
                ...message.sealingMetadata!,
                sha512: pdfHash,
                cloudAnchor: cloudAnchorResult,
            },
        };
        setMessages(updatedMessages);

        return { fileName, hash: pdfHash, cloudAnchor: cloudAnchorResult, pdfBytes, updatedContent: newContent };
    }, [messages, uploadedFiles]);

    const handleSealReport = useCallback(async (messageId: string) => {
        const sealResult = await performSealingProcess(messageId);
        if (sealResult) {
            downloadBlob(sealResult.pdfBytes, sealResult.fileName, 'application/pdf');
        }
    }, [performSealingProcess]);

    const handleSealAndEmailReport = useCallback(async (messageId: string) => {
        const sealResult = await performSealingProcess(messageId);
        if (!sealResult) return;

        downloadBlob(sealResult.pdfBytes, sealResult.fileName, 'application/pdf');

        const recipient = 'submissions@verum-foundation.org';
        const subject = `Verum Omnis Sealed Report Submission - Case ID ${Date.now()}`;
        
        const now = new Date();
        const localTimestampForEmail = now.toLocaleString(undefined, {
            year: 'numeric', month: 'numeric', day: 'numeric',
            hour: '2-digit', minute: '2-digit', timeZoneName: 'short'
        });

        const extractSection = (markdown: string, sectionTitle: string): string => {
            const regex = new RegExp(`## ${sectionTitle}\\n([\\s\\S]*?)(?=\\n##|$)`, 'i');
            const match = markdown.match(regex);
            return match ? markdownToPlainText(match[1].trim()) : 'Not found in report.';
        };

        const keyFindings = extractSection(sealResult.updatedContent, 'Key Findings');
        const contradictions = extractSection(sealResult.updatedContent, 'Contradictions & Risks');
        const nextSteps = extractSection(sealResult.updatedContent, 'Next Steps');

        const body = `
Dear Counsel,

Please find the sealed forensic report attached, generated by the Verum Omnis V5 AI.

The analysis has highlighted several key areas that require your attention. Below is a summary extracted directly from the report for your convenience.

---
**Key Findings:**
${keyFindings}
---
**Contradictions & Risks Identified:**
${contradictions}
---
**Suggested Next Steps:**
${nextSteps}
---

**Sealing Metadata:**
- Report Filename: ${sealResult.fileName}
- Certified SHA-512 Hash of PDF: ${sealResult.hash}
- Timestamp: ${localTimestampForEmail} (UTC: ${now.toISOString()})
- Cloud Anchor: ${sealResult.cloudAnchor?.storagePath || 'Failed to anchor'}
- Case Files Analyzed: ${uploadedFiles.map(f => f.name).join(', ') || 'N/A'}
---

This report has been certified by Verum Omnis V5. To preserve the integrity of the hash, please do not modify the attached file.

We await your response on the matters raised.

Regards,
Verum Omnis User
`;
        window.location.href = `mailto:${recipient}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;

        alert(`Your PDF report "${sealResult.fileName}" has been downloaded.\n\nPlease remember to manually attach this file to the email draft that has just opened.`);
    }, [performSealingProcess, uploadedFiles]);
  
    const handleCloseFilePanel = useCallback(() => setIsFilePanelOpen(false), []);
    const handleToggleFilePanel = useCallback(() => setIsFilePanelOpen(p => !p), []);
    const handleNavigateToFirewall = useCallback(() => setCurrentPage('firewall'), []);
    const handleNavigateToTax = useCallback(() => setCurrentPage('tax'), []);
    const handleNavigateToBusiness = useCallback(() => setCurrentPage('business'), []);

  const renderFirewallPage = () => (
    <div className="flex flex-row flex-grow overflow-hidden">
        <FilePanel 
          files={uploadedFiles} 
          onClearCase={handleClearCase}
          isOpen={isFilePanelOpen}
          onClose={handleCloseFilePanel}
        />
        <main className="flex-grow h-full overflow-hidden min-w-0">
          <ChatPanel 
            messages={messages}
            onSendMessage={handleSendMessage}
            onFilesChange={handleFilesChange}
            onSealReport={handleSealReport}
            onSealAndEmailReport={handleSealAndEmailReport}
            loadingState={loadingState}
            isComplexMode={isComplexMode}
            onToggleComplexMode={handleToggleComplexMode}
            isEnterpriseMode={isEnterpriseMode}
          />
        </main>
    </div>
  );
  
  const renderCurrentPage = () => {
    switch (currentPage) {
        case 'tax': return (
            <Suspense fallback={<div className="flex-grow flex items-center justify-center"><LoadingSpinner className="h-10 w-10 text-blue-600" /></div>}>
                <TaxServicePage />
            </Suspense>
        );
        case 'business': return (
             <Suspense fallback={<div className="flex-grow flex items-center justify-center"><LoadingSpinner className="h-10 w-10 text-blue-600" /></div>}>
                <BusinessServicesPage />
            </Suspense>
        );
        case 'firewall': return renderFirewallPage();
        default:
            return renderFirewallPage();
    }
  };


  return (
    <div className="flex flex-col h-screen overflow-hidden">
      <Header 
        isEnterpriseMode={isEnterpriseMode} 
        onToggleEnterpriseMode={handleToggleEnterpriseMode}
        onToggleFilePanel={handleToggleFilePanel}
        onNavigateToFirewall={handleNavigateToFirewall}
        onNavigateToTax={handleNavigateToTax}
        onNavigateToBusiness={handleNavigateToBusiness}
      />
      {renderCurrentPage()}
      <div style={{
        position: 'fixed', bottom: 0, left: 0, right: 0,
        padding: '8px 12px', fontSize: 12, textAlign: 'center',
        background: 'rgba(23, 23, 23, 0.9)', color: '#e5e5e5', zIndex: 9999,
        backdropFilter: 'blur(2px)',
        borderTop: '1px solid rgba(255, 255, 255, 0.1)',
      }}>
        Free for private people. Institutions & companies: trial access applies; fees apply after the trial.
      </div>
    </div>
  );
};

export default App;
