'use client';

import { motion } from 'framer-motion';
import { FiArrowUpRight, FiArrowDownLeft, FiHardDrive, FiActivity } from 'react-icons/fi';
import { Activity } from './RecentActivity';
import { formatFileSize } from '@/utils/file';

interface StatsPanelProps {
  filesSharedCount: number;
  filesReceivedCount: number;
  totalSharedSize?: number;
  totalReceivedSize?: number;
  activities?: Activity[];
}

export default function StatsPanel({
  filesSharedCount = 0,
  filesReceivedCount = 0,
  totalSharedSize = 0,
  totalReceivedSize = 0,
}: StatsPanelProps) {

  const totalSharedSizeStr = formatFileSize(totalSharedSize);
  const totalReceivedSizeStr = formatFileSize(totalReceivedSize);

  const stats = [
    {
      label: 'Files Shared',
      value: filesSharedCount,
      icon: FiArrowUpRight,
      color: 'text-blue-400',
      bg: 'from-blue-500/10 to-indigo-500/5 border-blue-500/10',
    },
    {
      label: 'Files Received',
      value: filesReceivedCount,
      icon: FiArrowDownLeft,
      color: 'text-purple-400',
      bg: 'from-purple-500/10 to-pink-500/5 border-purple-500/10',
    },
    {
      label: 'Total Shared',
      value: totalSharedSizeStr,
      icon: FiHardDrive,
      color: 'text-emerald-400',
      bg: 'from-emerald-500/10 to-teal-500/5 border-emerald-500/10',
    },
    {
      label: 'Total Received',
      value: totalReceivedSizeStr,
      icon: FiActivity,
      color: 'text-cyan-400',
      bg: 'from-cyan-500/10 to-blue-500/5 border-cyan-500/10',
    },
  ];

  return (
    <div className="grid grid-cols-2 gap-4">
      {stats.map((stat, index) => {
        const Icon = stat.icon;
        return (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: index * 0.08 }}
            className={`bg-gradient-to-br ${stat.bg} border rounded-2xl p-4 shadow-lg text-left relative overflow-hidden group hover:border-white/10 transition-all duration-300`}
          >
            {/* Soft decorative background glow */}
            <div className="absolute -right-4 -bottom-4 w-16 h-16 rounded-full bg-current opacity-5 blur-xl pointer-events-none group-hover:scale-150 transition-transform duration-500" />

            <div className="flex justify-between items-start">
              <span className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                {stat.label}
              </span>
              <Icon className={`w-4 h-4 ${stat.color}`} />
            </div>
            
            <div className="mt-2.5">
              <span className="text-xl font-bold text-white tracking-tight">
                {stat.value}
              </span>
            </div>
          </motion.div>
        );
      })}
    </div>
  );
}
