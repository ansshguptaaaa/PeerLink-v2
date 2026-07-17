'use client';

import { useState } from 'react';
import { FiLogOut, FiChevronDown, FiUser } from 'react-icons/fi';
import { motion, AnimatePresence } from 'framer-motion';

interface NavbarProps {
  userEmail: string;
  onLogout: () => void;
}

export default function Navbar({ userEmail, onLogout }: NavbarProps) {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const userInitial = userEmail ? userEmail.charAt(0).toUpperCase() : 'U';

  return (
    <header className="h-16 border-b border-slate-900 bg-slate-950/40 backdrop-blur-md sticky top-0 z-40 flex items-center justify-between px-8">
      {/* Title / Section Name */}
      <div>
        <h2 className="text-sm font-semibold text-slate-400">
          Workspace / <span className="text-white">Console</span>
        </h2>
      </div>

      {/* Profile & Controls */}
      <div className="flex items-center space-x-4">
        {/* User Info & Dropdown */}
        <div className="relative">
          <button
            onClick={() => setDropdownOpen(!dropdownOpen)}
            className="flex items-center space-x-2.5 bg-slate-900/60 hover:bg-slate-900 border border-slate-800/80 hover:border-slate-700 px-3.5 py-1.5 rounded-full transition-all duration-200 focus:outline-none"
          >
            <div className="h-6 w-6 rounded-full bg-gradient-to-tr from-blue-500 to-indigo-500 flex items-center justify-center text-xs font-bold text-white uppercase shadow-sm">
              {userInitial}
            </div>
            <span className="text-sm font-medium text-slate-300 max-w-[150px] truncate">
              {userEmail}
            </span>
            <FiChevronDown className={`w-3.5 h-3.5 text-slate-400 transition-transform duration-200 ${dropdownOpen ? 'rotate-180' : ''}`} />
          </button>

          {/* Dropdown Menu */}
          <AnimatePresence>
            {dropdownOpen && (
              <>
                {/* Backdrop to close */}
                <div className="fixed inset-0 z-10" onClick={() => setDropdownOpen(false)} />
                
                <motion.div
                  initial={{ opacity: 0, y: 10, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 10, scale: 0.95 }}
                  transition={{ duration: 0.15 }}
                  className="absolute right-0 mt-2 w-52 bg-slate-900/90 backdrop-blur-xl border border-slate-800/80 rounded-xl shadow-2xl p-1.5 z-20"
                >
                  <div className="px-3 py-2 border-b border-slate-800/50 mb-1">
                    <p className="text-[10px] text-slate-500 uppercase tracking-wider font-bold">Logged In As</p>
                    <p className="text-xs font-semibold text-slate-200 truncate mt-0.5">{userEmail}</p>
                  </div>

                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      onLogout();
                    }}
                    className="w-full flex items-center space-x-2 px-3 py-2 rounded-lg text-sm text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-colors focus:outline-none text-left"
                  >
                    <FiLogOut className="w-4 h-4" />
                    <span>Sign Out</span>
                  </button>
                </motion.div>
              </>
            )}
          </AnimatePresence>
        </div>
      </div>
    </header>
  );
}
