// requires pdf-lib and qrcode
import { PDFDocument, StandardFonts, rgb, PDFFont, PageSizes, PDFPage } from 'pdf-lib';
import QRCode from 'qrcode';

type SealInput = {
  title: string;
  messagesHtml?: string;              // optional rendered chat excerpt
  evidence: Array<{name: string; sha512: string}>;
  utcTimestamp: string;               // ISO string
  localTimestamp: string;             // User's local time string with timezone
  appVersion: string;
  logoDataUrl?: string;               // data URL for VO logo if available
};

const drawPageFurniture = (
    page: PDFPage,
    font: PDFFont,
    partialHash: string | null
) => {
    const { width, height } = page.getSize();
    const footerText = `™ Patent Pending Verum Omnis`;
    const hashText = partialHash ? ` | Evidence Hash: ${partialHash}...` : '';
    
    page.drawText(`${footerText}${hashText}`, {
        x: 50,
        y: 40,
        size: 8,
        font,
        color: rgb(0.5, 0.5, 0.5)
    });
};

export async function makeSealedPdf(input: SealInput): Promise<Uint8Array> {
  const pdf = await PDFDocument.create();
  const font = await pdf.embedFont(StandardFonts.Helvetica);
  const pageCount = pdf.getPageCount(); // will be updated later
  const primaryHash = input.evidence[0]?.sha512 || null;
  const partialHash = primaryHash ? primaryHash.substring(0, 16) : null;


  // Build QR payload
  const qrPayload = {
    sha512: input.evidence.map(e => e.sha512),
    filenames: input.evidence.map(e => e.name),
    utcTimestamp: input.utcTimestamp,
    appVersion: input.appVersion,
    pageCount: pageCount, // will be updated
  };
  const qrDataUrl = await QRCode.toDataURL(JSON.stringify(qrPayload));

  // Page 1: Title + summary
  const page = pdf.addPage(PageSizes.A4);
  const { width, height } = page.getSize();
  
  drawPageFurniture(page, font, partialHash);

  // Main Heading
  page.drawText('Verum Omnis V5', { x: 50, y: height - 80, size: 24, font });

  // Title
  page.drawText(input.title || 'Sealed Forensic Report', { x: 50, y: height - 120, size: 18, font, color: rgb(0.2, 0.2, 0.2) });

  // Evidence list
  page.drawText('Evidence & Hashes:', { x: 50, y: height - 165, size: 12, font });
  let y = height - 185;
  input.evidence.forEach(ev => {
    if (y < 100) return; // Avoid writing off the page
    page.drawText(`• ${ev.name}`, { x: 60, y, size: 10, font });
    y -= 12;
    page.drawText(`  SHA-512: ${ev.sha512}`, { x: 60, y, size: 9, font });
    y -= 14;
  });

  // Page 2 onwards: Chat/body (optional)
  if (input.messagesHtml) {
    let currentPage = pdf.addPage(PageSizes.A4);
    drawPageFurniture(currentPage, font, partialHash);

    currentPage.drawText('Conversation Excerpt:', { x: 50, y: height - 80, size: 12, font });
    
    const x = 50;
    const fontSize = 10;
    const lineHeight = 14;
    const bottomMargin = 80;
    const topMargin = 80;
    const maxWidth = width - 2 * x;
    let currentY = height - 100;
    
    const paragraphs = input.messagesHtml.split('\n');

    for (const paragraph of paragraphs) {
        if (paragraph.trim() === '') {
            currentY -= lineHeight;
            if (currentY < bottomMargin) {
                currentPage = pdf.addPage(PageSizes.A4);
                drawPageFurniture(currentPage, font, partialHash);
                currentY = height - topMargin;
            }
            continue;
        }

        const words = paragraph.split(' ');
        let line = '';

        for (const word of words) {
            const testLine = line.length === 0 ? word : `${line} ${word}`;
            const testWidth = font.widthOfTextAtSize(testLine, fontSize);
            
            if (testWidth > maxWidth) {
                if (currentY < bottomMargin) {
                    currentPage = pdf.addPage(PageSizes.A4);
                    drawPageFurniture(currentPage, font, partialHash);
                    currentY = height - topMargin;
                }
                currentPage.drawText(line, { x, y: currentY, size: fontSize, font });
                currentY -= lineHeight;
                line = word;
            } else {
                line = testLine;
            }
        }
        
        if (line.length > 0) {
            if (currentY < bottomMargin) {
                currentPage = pdf.addPage(PageSizes.A4);
                drawPageFurniture(currentPage, font, partialHash);
                currentY = height - topMargin;
            }
            currentPage.drawText(line, { x, y: currentY, size: fontSize, font });
            currentY -= lineHeight;
        }
    }
  }


  // Final sealing page
  const final = pdf.addPage(PageSizes.A4);
  drawPageFurniture(final, font, partialHash);
  
  final.drawText('Sealing Metadata', { x: 50, y: height - 80, size: 14, font });

  // QR bottom-right
  const qrPng = await pdf.embedPng(qrDataUrl);
  const qrw = 120;
  final.drawImage(qrPng, { x: width - 50 - qrw, y: 50, width: qrw, height: qrw });

  // Metadata text
  const finalPageCount = pdf.getPageCount();
  qrPayload.pageCount = finalPageCount; // Update final page count in payload for QR

  const meta = [
    `Local Timestamp: ${input.localTimestamp}`,
    `UTC Timestamp: ${input.utcTimestamp}`,
    `Version: ${input.appVersion}`,
    `Files: ${input.evidence.length}`,
    `Page Count: ${finalPageCount}`,
  ];
  let my = height - 110;
  meta.forEach(m => { final.drawText(m, { x: 50, y: my, size: 11, font }); my -= 14; });

  return await pdf.save();
}