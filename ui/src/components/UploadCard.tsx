'use client';

import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { FiUploadCloud, FiFile, FiCheckCircle } from 'react-icons/fi';
import { motion, AnimatePresence } from 'framer-motion';
import { formatFileSize } from '@/utils/file';

interface UploadCardProps {
  onFileUpload: (file: File) => void;
  isUploading: boolean;
  uploadedFile: File | null;
}

export default function UploadCard({ onFileUpload, isUploading, uploadedFile }: UploadCardProps) {
  const [dragActive, setDragActive] = useState(false);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      onFileUpload(acceptedFiles[0]);
    }
  }, [onFileUpload]);

  const { getRootProps, getInputProps } = useDropzone({
    onDrop,
    multiple: false,
    onDragEnter: () => setDragActive(true),
    onDragLeave: () => setDragActive(false),
    onDropAccepted: () => setDragActive(false),
    onDropRejected: () => setDragActive(false),
    disabled: isUploading,
  });

  return (
    <div className="bg-slate-900/40 backdrop-blur-md border border-slate-800/80 rounded-2xl p-6 shadow-xl relative overflow-hidden">
      <div className="absolute top-0 right-0 w-32 h-32 rounded-full bg-blue-500/5 blur-xl pointer-events-none" />
      
      <h3 className="text-lg font-bold text-white mb-4 flex items-center space-x-2">
        <span className="h-2 w-2 rounded-full bg-blue-500 animate-pulse" />
        <span>Share a File</span>
      </h3>

      <div
        {...getRootProps()}
        className={`w-full p-8 border-2 border-dashed rounded-xl text-center cursor-pointer transition-all duration-300 relative group overflow-hidden ${
          dragActive
            ? 'border-blue-500 bg-blue-500/5 shadow-inner shadow-blue-500/5'
            : 'border-slate-800 hover:border-slate-700 bg-slate-950/40 hover:bg-slate-950/60'
        } ${isUploading ? 'opacity-50 pointer-events-none' : ''}`}
      >
        <input {...getInputProps()} />

        {/* Decorative inner glow */}
        <div className="absolute -inset-px bg-gradient-to-r from-blue-500/0 via-blue-500/5 to-purple-500/0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none" />

        <div className="flex flex-col items-center justify-center space-y-4 relative z-10">
          <motion.div
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            className={`p-4 rounded-2xl border transition-all duration-300 ${
              dragActive 
                ? 'bg-blue-950 border-blue-500/50 text-blue-400' 
                : 'bg-slate-900 border-slate-800 text-slate-400 group-hover:border-slate-700 group-hover:text-slate-300'
            }`}
          >
            <FiUploadCloud className="w-8 h-8" />
          </motion.div>
          
          <div className="space-y-1">
            <p className="text-sm font-semibold text-slate-200">
              Drag & drop a file here, or <span className="text-blue-400 hover:underline">browse</span>
            </p>
            <p className="text-xs text-slate-500">
              Files are directly streamed to peers securely
            </p>
          </div>
        </div>
      </div>

      {/* Selected file and progress section */}
      <AnimatePresence>
        {(uploadedFile || isUploading) && (
          <motion.div
            initial={{ opacity: 0, height: 0, marginTop: 0 }}
            animate={{ opacity: 1, height: 'auto', marginTop: 16 }}
            exit={{ opacity: 0, height: 0, marginTop: 0 }}
            className="overflow-hidden"
          >
            <div className="p-4 bg-slate-950/60 border border-slate-900 rounded-xl space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3 truncate">
                  <div className="p-2 bg-blue-950/50 border border-blue-800/30 rounded-lg text-blue-400">
                    <FiFile className="w-4 h-4" />
                  </div>
                  <div className="truncate text-left">
                    <p className="text-xs text-slate-500 uppercase font-bold tracking-wider leading-none">Selected File</p>
                    <p className="text-sm font-semibold text-slate-200 truncate mt-1.5">{uploadedFile?.name || 'Uploading...'}</p>
                  </div>
                </div>
                {uploadedFile && (
                  <span className="text-[10px] font-bold bg-slate-900 border border-slate-800 px-2.5 py-1 rounded-lg text-slate-400 whitespace-nowrap ml-4">
                    {formatFileSize(uploadedFile.size)}
                  </span>
                )}
              </div>

              {/* Progress Bar / Upload State */}
              {isUploading ? (
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-xs font-semibold text-slate-400">
                    <span className="flex items-center space-x-2">
                      <svg className="animate-spin -ml-1 mr-2 h-3 w-3 text-blue-500" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                      </svg>
                      Uploading file...
                    </span>
                    <span className="animate-pulse">Active Stream</span>
                  </div>
                  <div className="w-full bg-slate-900 rounded-full h-1.5 overflow-hidden">
                    <motion.div
                      initial={{ width: '0%' }}
                      animate={{ width: '100%' }}
                      transition={{ duration: 1.5, repeat: Infinity, ease: 'easeInOut' }}
                      className="bg-gradient-to-r from-blue-500 to-indigo-500 h-full rounded-full"
                    />
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-1.5 text-xs text-emerald-400 font-semibold bg-emerald-500/5 border border-emerald-500/10 px-2.5 py-1 rounded-lg w-max">
                  <FiCheckCircle className="w-3.5 h-3.5" />
                  <span>Metadata registered successfully</span>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
