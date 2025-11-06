import React, { memo } from 'react';
import { UploadedFile } from '../types';
import { FileIcon, LoadingSpinner, TrashIcon } from './Icons';

interface FilePanelProps {
  files: UploadedFile[];
  onClearCase: () => void;
  isOpen: boolean;
  onClose: () => void;
}

const FilePanel: React.FC<FilePanelProps> = ({ 
  files, 
  onClearCase, 
  isOpen, 
  onClose 
}) => {
  const firewallFiles = files.filter(f => f.source === 'firewall');
  const userFiles = files.filter(f => f.source === 'user');

  return (
    <>
      {/* Backdrop for mobile */}
      <div 
        className={`fixed inset-0 bg-black bg-opacity-50 z-30 md:hidden transition-opacity ${isOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}
        onClick={onClose}
        aria-hidden="true"
      />
      <div 
        className={`
          w-80 bg-gray-50 dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 flex flex-col h-full shrink-0
          fixed md:relative top-0 left-0 z-40 md:z-auto
          transition-transform duration-300 ease-in-out
          ${isOpen ? 'translate-x-0' : '-translate-x-full'} md:translate-x-0
        `}
      >
        <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
          <h2 className="font-bold text-lg text-gray-800 dark:text-white">Case Data</h2>
          <div className="flex items-center space-x-2">
            <button onClick={onClearCase} className="text-gray-500 dark:text-gray-400 hover:text-red-500" aria-label="Clear case data">
              <TrashIcon className="h-5 w-5" />
            </button>
            <button onClick={onClose} className="text-gray-500 dark:text-gray-400 md:hidden" aria-label="Close file panel">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
          </div>
        </div>

        <div className="flex-grow p-4 overflow-y-auto space-y-6">
          
          <div>
            <h3 className="font-semibold text-gray-700 dark:text-gray-300 mb-2">Case Narrative</h3>
            <div className="flex items-center space-x-2 text-sm text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700/50 p-2 rounded-md">
                <FileIcon className="h-5 w-5 text-blue-500" />
                <span className="truncate flex-grow font-medium">narrative.txt</span>
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 italic">
              A live summary of events and communications for fast AI context. Stored on-device.
            </p>
          </div>

          <div>
            <h3 className="font-semibold text-gray-700 dark:text-gray-300 mb-2">On-Device Firewall</h3>
            <div className="text-sm bg-white dark:bg-gray-700/50 p-3 rounded-md space-y-2 max-h-48 overflow-y-auto">
              {firewallFiles.length === 0 ? (
                <p className="text-gray-500 dark:text-gray-400">No events captured by firewall.</p>
              ) : (
                firewallFiles.map(file => (
                  <div key={file.id}>
                    <p className="font-medium text-gray-800 dark:text-gray-200 truncate flex items-center">
                      <FileIcon className="h-4 w-4 mr-2 flex-shrink-0" />
                      {file.name}
                    </p>
                    {file.sha512 ? (
                        <p className="text-xs text-gray-500 dark:text-gray-400 font-mono break-all ml-6" title={file.sha512}>
                            SHA-512: {file.sha512.substring(0,32)}...
                        </p>
                    ) : (
                        <p className="text-xs text-gray-500 dark:text-gray-400 font-mono break-all ml-6">
                            <LoadingSpinner className="h-3 w-3 inline-block mr-1" />
                            Hashing...
                        </p>
                    )}
                  </div>
                ))
              )}
            </div>
             <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 italic">
              Evidence automatically captured by the local firewall APK.
            </p>
          </div>
          
          <div>
            <h3 className="font-semibold text-gray-700 dark:text-gray-300 mb-2">Evidence Locker</h3>
            <div className="text-sm bg-white dark:bg-gray-700/50 p-3 rounded-md space-y-2 max-h-48 overflow-y-auto">
              {userFiles.length === 0 ? (
                <p className="text-gray-500 dark:text-gray-400">No evidence files manually uploaded.</p>
              ) : (
                userFiles.map(file => (
                  <div key={file.id}>
                    <p className="font-medium text-gray-800 dark:text-gray-200 truncate flex items-center">
                      <FileIcon className="h-4 w-4 mr-2 flex-shrink-0" />
                      {file.name}
                    </p>
                    {file.sha512 ? (
                        <p className="text-xs text-gray-500 dark:text-gray-400 font-mono break-all ml-6" title={file.sha512}>
                            SHA-512: {file.sha512.substring(0,32)}...
                        </p>
                    ) : (
                        <p className="text-xs text-gray-500 dark:text-gray-400 font-mono break-all ml-6">
                            <LoadingSpinner className="h-3 w-3 inline-block mr-1" />
                            Hashing...
                        </p>
                    )}
                  </div>
                ))
              )}
            </div>
             <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 italic">
              Client-side hashed evidence files for analysis.
            </p>
          </div>
          
        </div>
      </div>
    </>
  );
};

export default memo(FilePanel);