'use client';

import { motion } from 'framer-motion';
import { FiArrowUpRight, FiArrowDownLeft, FiClock, FiFileText } from 'react-icons/fi';

export interface Activity {
  id: string;
  filename: string;
  action: 'Shared' | 'Received';
  timestamp: string;
  status: 'Completed' | 'Pending' | 'Failed';
  size?: string;
}

interface RecentActivityProps {
  activities: Activity[];
}

export default function RecentActivity({ activities }: RecentActivityProps) {
  return (
    <div className="bg-slate-900/40 backdrop-blur-md border border-slate-800/80 rounded-2xl p-6 shadow-xl text-left relative overflow-hidden flex-grow flex flex-col min-h-[300px]">
      <div className="absolute top-0 right-0 w-32 h-32 rounded-full bg-purple-500/5 blur-xl pointer-events-none" />

      <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center justify-between">
        <span>Recent Activity</span>
        <span className="text-[10px] bg-slate-900 border border-slate-850 px-2 py-0.5 rounded text-slate-500 font-semibold normal-case">
          Real-time
        </span>
      </h3>

      <div className="space-y-3.5 overflow-y-auto max-h-[350px] pr-1 flex-grow">
        {activities.length === 0 ? (
          <div className="h-48 flex flex-col items-center justify-center text-center text-slate-500 space-y-2">
            <FiFileText className="w-8 h-8 opacity-30" />
            <p className="text-xs font-semibold">No recent activity</p>
            <p className="text-[10px] text-slate-655 max-w-[180px]">Your file transfers and downloads will appear here.</p>
          </div>
        ) : (
          activities.map((activity, index) => {
            const isShared = activity.action === 'Shared';
            const statusColors = {
              Completed: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
              Pending: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
              Failed: 'bg-red-500/10 text-red-400 border-red-500/20',
            };

            return (
              <motion.div
                key={activity.id}
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.3, delay: index * 0.05 }}
                className="flex items-center justify-between p-3 bg-slate-950/40 border border-slate-900 rounded-xl hover:border-slate-800 transition-colors duration-200"
              >
                {/* Left Side: Icon & Filename & Size */}
                <div className="flex items-center space-x-3 truncate mr-3">
                  <div className={`p-2 rounded-lg border ${
                    isShared 
                      ? 'bg-blue-950/40 border-blue-900/30 text-blue-400' 
                      : 'bg-purple-950/40 border-purple-900/30 text-purple-400'
                  }`}>
                    {isShared ? <FiArrowUpRight className="w-4 h-4" /> : <FiArrowDownLeft className="w-4 h-4" />}
                  </div>
                  <div className="truncate text-left">
                    <p className="text-xs font-semibold text-slate-200 truncate max-w-[160px] md:max-w-[200px]">
                      {activity.filename}
                    </p>
                    <div className="flex items-center space-x-2 mt-1">
                      <span className="text-[9px] text-slate-500 flex items-center">
                        <FiClock className="w-2.5 h-2.5 mr-1" />
                        {activity.timestamp}
                      </span>
                      {activity.size && (
                        <span className="text-[9px] text-slate-500">
                          • {activity.size}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Right Side: Status Badge */}
                <div>
                  <span className={`text-[9px] font-bold border px-2 py-0.5 rounded-md ${statusColors[activity.status] || statusColors.Completed}`}>
                    {activity.status}
                  </span>
                </div>
              </motion.div>
            );
          })
        )}
      </div>
    </div>
  );
}
