'use client';

import { motion } from 'framer-motion';
import { FiGrid, FiShare2, FiDownload, FiFolder, FiRefreshCw, FiSettings, FiShield } from 'react-icons/fi';

interface SidebarProps {
  activeView: string;
  setActiveView: (view: string) => void;
}

export default function Sidebar({ activeView, setActiveView }: SidebarProps) {
  const menuItems = [
    { id: 'dashboard', label: 'Dashboard', icon: FiGrid },
    { id: 'share', label: 'Share File', icon: FiShare2 },
    { id: 'receive', label: 'Receive File', icon: FiDownload },
    { id: 'files', label: 'My Files', icon: FiFolder },
  ];

  return (
    <aside className="hidden md:flex w-64 border-r border-slate-800 bg-slate-950/70 backdrop-blur-xl flex-col justify-between p-6 h-screen sticky top-0">
      {/* Brand & Logo */}
      <div className="flex flex-col space-y-8">
        <div className="flex items-center space-x-3">
          <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
            <span className="text-white font-extrabold text-lg">P</span>
          </div>
          <span className="text-xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-400 tracking-wider">
            PeerLink
          </span>
        </div>

        {/* Navigation Items */}
        <nav className="flex flex-col space-y-1.5">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeView === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveView(item.id)}
                className="w-full relative group"
              >
                <div
                  className={`flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-300 ${
                    isActive
                      ? 'text-white'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/40'
                  }`}
                >
                  <Icon className={`w-4 h-4 transition-colors ${isActive ? 'text-blue-400' : 'text-slate-400 group-hover:text-slate-300'}`} />
                  <span>{item.label}</span>
                </div>
                {isActive && (
                  <motion.div
                    layoutId="active-nav-glow"
                    className="absolute inset-0 bg-gradient-to-r from-blue-500/10 to-indigo-500/10 rounded-xl border border-blue-500/20 -z-10 pointer-events-none"
                    transition={{ type: 'spring', stiffness: 380, damping: 30 }}
                  />
                )}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Bottom Security Card */}
      <div className="relative overflow-hidden bg-gradient-to-b from-slate-900/60 to-slate-950/80 border border-slate-800/80 rounded-2xl p-4 shadow-xl shadow-blue-950/5">
        <div className="absolute top-0 right-0 w-24 h-24 rounded-full bg-blue-500/5 blur-xl pointer-events-none" />
        <div className="flex items-start space-x-3">
          <div className="p-2 bg-blue-950/50 border border-blue-800/30 rounded-lg text-blue-400">
            <FiShield className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-slate-200">Secure & Private</h4>
            <p className="text-[10px] text-slate-400 mt-1 leading-relaxed">
              All transfers are end-to-end secured with JWT authentication.
            </p>
          </div>
        </div>
      </div>
    </aside>
  );
}
