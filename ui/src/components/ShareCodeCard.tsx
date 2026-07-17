'use client';

import { useState } from 'react';
import { FiCopy, FiCheck, FiRefreshCw, FiClock } from 'react-icons/fi';
import { motion } from 'framer-motion';

interface ShareCodeCardProps {
  port: number | null;
  onRefresh?: () => void;
}

export default function ShareCodeCard({ port, onRefresh }: ShareCodeCardProps) {
  const [copied, setCopied] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  if (port === null) return null;

  const copyToClipboard = () => {
    navigator.clipboard.writeText(port.toString());
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleRefresh = async () => {
    if (!onRefresh) return;
    setRefreshing(true);
    try {
      await onRefresh();
    } finally {
      setTimeout(() => setRefreshing(false), 800);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.4 }}
      className="bg-gradient-to-br from-slate-900/60 to-indigo-950/20 border border-slate-800/80 rounded-2xl p-6 shadow-2xl relative overflow-hidden"
    >
      <div className="absolute top-0 right-0 w-32 h-32 rounded-full bg-emerald-500/5 blur-xl pointer-events-none" />

      <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400 mb-2">
        Share this code with your peer
      </h3>
      <p className="text-xs text-slate-500 mb-6">
        Your peer can enter this code in their dashboard to download the file directly from your computer.
      </p>

      {/* Code Container */}
      <div className="flex items-center space-x-2">
        <div className="flex-1 bg-slate-950/60 border border-slate-850 px-5 py-4 rounded-xl font-mono text-2xl font-bold text-center tracking-widest text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-400 border border-slate-800/50 shadow-inner">
          {port}
        </div>
        
        {/* Action Buttons */}
        <div className="flex space-x-1.5">
          {/* Copy Button */}
          <button
            onClick={copyToClipboard}
            className="p-4 bg-slate-900 hover:bg-slate-850 border border-slate-850 hover:border-slate-700 text-slate-300 hover:text-white rounded-xl transition-all duration-200 active:scale-95 flex items-center justify-center"
            title="Copy Invite Code"
          >
            {copied ? <FiCheck className="w-5 h-5 text-emerald-400" /> : <FiCopy className="w-5 h-5" />}
          </button>

          {/* Refresh Button */}
          {onRefresh && (
            <button
              onClick={handleRefresh}
              className="p-4 bg-slate-900 hover:bg-slate-850 border border-slate-850 hover:border-slate-700 text-slate-300 hover:text-white rounded-xl transition-all duration-200 active:scale-95 flex items-center justify-center"
              title="Generate New Code"
              disabled={refreshing}
            >
              <FiRefreshCw className={`w-5 h-5 ${refreshing ? 'animate-spin text-blue-400' : ''}`} />
            </button>
          )}
        </div>
      </div>

      {/* Expiry / Info text */}
      <div className="mt-5 flex items-center space-x-2 text-xs text-slate-500 font-medium">
        <FiClock className="w-3.5 h-3.5 text-slate-650" />
        <span>Expires once you refresh, close the page, or sign out.</span>
      </div>
    </motion.div>
  );
}
