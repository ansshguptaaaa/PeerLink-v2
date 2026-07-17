'use client';

import { motion } from 'framer-motion';
import { FiShield, FiZap, FiDatabase } from 'react-icons/fi';

interface HeroBannerProps {
  userEmail: string;
}

export default function HeroBanner({ userEmail }: HeroBannerProps) {
  // Extract username from email
  const username = userEmail ? userEmail.split('@')[0] : 'User';
  // Capitalize first letter
  const capitalizedUsername = username.charAt(0).toUpperCase() + username.slice(1);

  const badges = [
    { label: 'Secure', value: 'JWT Protected', icon: FiShield, color: 'text-emerald-400', bg: 'bg-emerald-500/10 border-emerald-500/20' },
    { label: 'Fast', value: 'High Performance', icon: FiZap, color: 'text-amber-400', bg: 'bg-amber-500/10 border-amber-500/20' },
    { label: 'Reliable', value: 'PostgreSQL', icon: FiDatabase, color: 'text-blue-400', bg: 'bg-blue-500/10 border-blue-500/20' },
  ];

  return (
    <div className="relative overflow-hidden bg-gradient-to-r from-slate-900/60 to-indigo-950/20 border border-slate-800/80 rounded-3xl p-8 shadow-2xl flex flex-col md:flex-row justify-between items-center mb-8">
      {/* Background radial glows */}
      <div className="absolute top-0 right-0 w-80 h-80 rounded-full bg-blue-500/5 blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-60 h-60 rounded-full bg-purple-500/5 blur-3xl pointer-events-none" />

      {/* Text & Content */}
      <div className="flex-1 space-y-6 z-10">
        <div>
          <motion.h1
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="text-3xl font-extrabold text-white tracking-tight"
          >
            Welcome back, <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-indigo-400 to-purple-400">{capitalizedUsername}</span>! 👋
          </motion.h1>
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.15 }}
            className="text-slate-400 text-sm mt-2 max-w-lg leading-relaxed"
          >
            Share files securely and privately with your peers.
          </motion.p>
        </div>

        {/* Feature Badges */}
        <div className="flex flex-wrap gap-4 pt-2">
          {badges.map((badge, index) => {
            const Icon = badge.icon;
            return (
              <motion.div
                key={badge.label}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, delay: 0.2 + index * 0.1 }}
                className={`flex items-center space-x-2.5 px-4 py-2 border rounded-xl ${badge.bg} backdrop-blur-sm`}
              >
                <Icon className={`w-4 h-4 ${badge.color}`} />
                <div className="text-left">
                  <p className="text-[10px] uppercase font-bold text-slate-500 leading-none">{badge.label}</p>
                  <p className="text-xs font-semibold text-slate-200 mt-1">{badge.value}</p>
                </div>
              </motion.div>
            );
          })}
        </div>
      </div>

      {/* Futuristic Illustration on Right */}
      <div className="w-full md:w-56 h-40 md:h-auto flex items-center justify-center relative mt-6 md:mt-0">
        <div className="absolute w-36 h-36 rounded-full bg-blue-500/10 blur-xl animate-pulse" />
        <div className="relative w-28 h-28 flex items-center justify-center">
          {/* Glowing circles */}
          <motion.div 
            animate={{ rotate: 360 }}
            transition={{ duration: 15, repeat: Infinity, ease: "linear" }}
            className="absolute inset-0 rounded-full border border-dashed border-blue-500/30 flex items-center justify-center"
          >
            <div className="w-2 h-2 rounded-full bg-blue-400 absolute top-0 left-1/2 -translate-x-1/2" />
          </motion.div>
          
          <motion.div 
            animate={{ rotate: -360 }}
            transition={{ duration: 25, repeat: Infinity, ease: "linear" }}
            className="absolute inset-2 rounded-full border border-dashed border-purple-500/20 flex items-center justify-center"
          >
            <div className="w-1.5 h-1.5 rounded-full bg-purple-400 absolute bottom-0 left-1/2 -translate-x-1/2" />
          </motion.div>

          {/* Central orb */}
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-blue-600 to-purple-600 flex items-center justify-center shadow-xl shadow-blue-500/20 border border-white/10 relative overflow-hidden group">
            <div className="absolute inset-0 bg-gradient-to-tr from-transparent to-white/20 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
            <svg className="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
            </svg>
          </div>
        </div>
      </div>
    </div>
  );
}
