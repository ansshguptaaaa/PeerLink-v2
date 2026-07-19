'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import api from '@/utils/api';
import Sidebar from '@/components/Sidebar';
import Navbar from '@/components/Navbar';
import HeroBanner from '@/components/HeroBanner';
import UploadCard from '@/components/UploadCard';
import ShareCodeCard from '@/components/ShareCodeCard';
import StatsPanel from '@/components/StatsPanel';
import RecentActivity, { Activity } from '@/components/RecentActivity';
import FileDownload from '@/components/FileDownload';
import { motion, AnimatePresence } from 'framer-motion';
import { FiFolder, FiLock, FiInfo, FiSliders, FiActivity, FiServer, FiMenu, FiClock, FiX, FiGrid, FiShare2, FiDownload } from 'react-icons/fi';
import { formatFileSize } from '@/utils/file';

export default function Home() {
  const router = useRouter();
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userEmail, setUserEmail] = useState('');
  const [activeView, setActiveView] = useState<string>('dashboard');
  
  // File Transfer States
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [port, setPort] = useState<number | null>(null);

  // Persistent shared files from PostgreSQL database
  const [sharedFiles, setSharedFiles] = useState<any[]>([]);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [stats, setStats] = useState({ shared: 0, received: 0, totalSharedSize: 0, totalReceivedSize: 0 });
  const [isStatsOpen, setIsStatsOpen] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [transfers, setTransfers] = useState<any[]>([]);

  // Settings Mock States
  const [apiUrl, setApiUrl] = useState(
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9090'
); 
  const [maxConnections, setMaxConnections] = useState(5);
  const [secureMode, setSecureMode] = useState(true);

  // Verify authentication on component mount
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    const email = localStorage.getItem('userEmail');
    
    if (!token) {
      router.push('/login');
    } else {
      setIsAuthenticated(true);
      setUserEmail(email || 'User');
    }
  }, [router]);

  // Load files on page mount
  useEffect(() => {
    loadFiles();
    loadStats();
    loadTransfers();
  }, []);

  const loadFiles = async () => {
    try {
      const res = await api.get('/api/files');
      const files = res.data || [];

      setSharedFiles(files);

      setActivities(
        files.map((file: any) => ({
          id: file.id.toString(),
          filename: file.fileName,
          action: 'Shared',
          timestamp: new Date(file.createdAt).toLocaleString(),
          status: 'Completed',
          size: formatFileSize(file.fileSize || 0)
        }))
      );
    } catch (err) {
      console.error(err);
    }
  };

  const loadStats = async () => {
    try {
      const res = await api.get('/api/stats');
      if (res.data) {
        setStats(res.data);
      }
    } catch (err) {
      console.error('Error loading stats:', err);
    }
  };

  const loadTransfers = async () => {
    try {
      const res = await api.get('/transfers');
      setTransfers(res.data || []);
    } catch (err) {
      console.error('Error loading transfers:', err);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userRole');
    router.push('/login');
  };

  const handleFileUpload = async (file: File) => {
    setUploadedFile(file);
    setIsUploading(true);
    setPort(null); // Reset invite code on new upload
    
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await api.post('/api/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      const allocatedPort = response.data.port;
      setPort(allocatedPort);

      // Immediately append file to sharedFiles
      const newFile = {
        id: allocatedPort, // temporary ID
        fileName: file.name,
        shareCode: allocatedPort,
        createdAt: new Date().toISOString()
      };
      setSharedFiles(prev => [newFile, ...prev]);

      // Immediately append activity
      const newActivity: Activity = {
        id: allocatedPort.toString(),
        filename: file.name,
        action: 'Shared',
        timestamp: new Date().toLocaleString(),
        status: 'Completed',
        size: formatFileSize(file.size)
      };
      setActivities(prev => [newActivity, ...prev]);

      // Refresh files list from PostgreSQL in the background
      await loadFiles();
      await loadStats();
      await loadTransfers();

    } catch (error) {
      console.error('Error uploading file:', error);
      alert('Failed to upload file. Please try again.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleRefreshCode = async () => {
    if (uploadedFile) {
      await handleFileUpload(uploadedFile);
    }
  };
  
  const handleDownload = async (downloadPort: number) => {
    setIsDownloading(true);
    
    try {
      // Request download from Java backend
      const response = await api.get(`/api/download/${downloadPort}`, {
        responseType: 'blob',
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      const headers = response.headers;
      let contentDisposition = '';
      
      // Look for content-disposition header regardless of case
      for (const key in headers) {
        if (key.toLowerCase() === 'content-disposition') {
          contentDisposition = headers[key];
          break;
        }
      }
      
      let filename = `downloaded-${downloadPort}-file`;
      
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+)"/);
        if (filenameMatch && filenameMatch.length === 2) {
          filename = filenameMatch[1];
        }
      }
      
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();

      // Refresh files list
      await loadFiles();
      await loadStats();
      await loadTransfers();

    } catch (error) {
      console.error('Error downloading file:', error);
      alert('Failed to download file. Please check the invite code and try again.');
    } finally {
      setIsDownloading(false);
    }
  };

  const filesSharedCount = stats.shared;
  const filesReceivedCount = stats.received;

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-950">
        <div className="flex flex-col items-center space-y-4">
          <div className="w-10 h-10 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-slate-400 text-sm font-semibold">Loading console session...</p>
        </div>
      </div>
    );
  }

  // View Components Renderers
  const renderViewContent = () => {
    switch (activeView) {
      case 'dashboard':
        return (
          <div className="space-y-6">
            <HeroBanner userEmail={userEmail} />
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <UploadCard 
                onFileUpload={handleFileUpload} 
                isUploading={isUploading} 
                uploadedFile={uploadedFile}
              />
              <AnimatePresence mode="wait">
                {port !== null ? (
                  <ShareCodeCard port={port} onRefresh={handleRefreshCode} />
                ) : (
                  <div className="bg-slate-900/20 border border-slate-800/60 rounded-2xl p-6 flex flex-col items-center justify-center text-center text-slate-500 space-y-3 min-h-[220px]">
                    <div className="p-3.5 bg-slate-950/60 border border-slate-850 rounded-2xl text-slate-400">
                      <FiInfo className="w-6 h-6" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-slate-300">Invite Code Generator</p>
                      <p className="text-xs text-slate-550 mt-1 max-w-[220px]">
                        Upload a file above to generate an invite code for direct peer transfer.
                      </p>
                    </div>
                  </div>
                )}
              </AnimatePresence>
            </div>
          </div>
        );
      
      case 'share':
        return (
          <div className="space-y-6">
            <UploadCard 
              onFileUpload={handleFileUpload} 
              isUploading={isUploading} 
              uploadedFile={uploadedFile}
            />
            {port !== null && (
              <ShareCodeCard port={port} onRefresh={handleRefreshCode} />
            )}
          </div>
        );

      case 'receive':
        return (
          <FileDownload 
            onDownload={handleDownload} 
            isDownloading={isDownloading} 
          />
        );

      case 'files':
        return (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-slate-900/40 backdrop-blur-md border border-slate-800/80 rounded-2xl p-6 shadow-xl text-left"
          >
            <h3 className="text-lg font-bold text-white mb-2 flex items-center space-x-2">
              <FiFolder className="text-blue-400" />
              <span>My Shared Files</span>
            </h3>
            <p className="text-xs text-slate-500 mb-6">
              Files currently registered on this console session for seeding or direct transfers.
            </p>
            
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-800 text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                    <th className="pb-3 pl-2">File Name</th>
                    <th className="pb-3">Extension</th>
                    <th className="pb-3">Share Code</th>
                    <th className="pb-3">Upload Date</th>
                    <th className="pb-3 pr-2 text-right">Status</th>
                  </tr>
                </thead>
                <tbody className="text-sm divide-y divide-slate-850">
                  {sharedFiles.map(file => (
                    <tr key={file.id} className="text-slate-350 hover:bg-slate-900/20">
                      <td className="py-4 pl-2 font-semibold text-white truncate max-w-[180px]">{file.fileName}</td>
                      <td className="py-4 text-xs text-slate-500">{file.fileName.split('.').pop()?.toUpperCase() || 'BIN'}</td>
                      <td className="py-4 font-mono text-xs text-blue-400 font-semibold">{file.shareCode}</td>
                      <td className="py-4 text-xs text-slate-500">{new Date(file.createdAt).toLocaleString()}</td>
                      <td className="py-4 pr-2 text-right">
                        <span className="text-[10px] bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 font-bold px-2 py-0.5 rounded-md">Active</span>
                      </td>
                    </tr>
                  ))}
                  {sharedFiles.length === 0 && (
                    <tr>
                      <td colSpan={5} className="py-8 text-center text-slate-500 text-xs font-semibold">
                        No files uploaded yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </motion.div>
        );

      case 'transfers':
        return (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-slate-900/40 backdrop-blur-md border border-slate-800/80 rounded-2xl p-6 shadow-xl text-left"
          >
            <h3 className="text-lg font-bold text-white mb-2 flex items-center space-x-2">
              <FiActivity className="text-indigo-400" />
              <span>Active Transfers</span>
            </h3>
            <p className="text-xs text-slate-500 mb-6">
              Real-time monitoring of uploads and downloads currently streaming directly to peers.
            </p>

            <div className="space-y-4">
              {/* If uploader is loading */}
              {isUploading && (
                <div className="p-4 bg-slate-950/60 border border-slate-850 rounded-xl space-y-3">
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="text-xs font-bold text-blue-400 uppercase tracking-wider">Outgoing Stream</p>
                      <p className="text-sm font-semibold text-slate-200 mt-1">{uploadedFile?.name}</p>
                    </div>
                    <span className="text-xs font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20 px-2 py-0.5 rounded">Uploading</span>
                  </div>
                  <div className="w-full bg-slate-900 rounded-full h-1.5 overflow-hidden">
                    <motion.div
                      initial={{ width: '0%' }}
                      animate={{ width: '100%' }}
                      transition={{ duration: 1.5, repeat: Infinity, ease: 'easeInOut' }}
                      className="bg-gradient-to-r from-blue-500 to-indigo-500 h-full rounded-full"
                    />
                  </div>
                  <div className="flex justify-between text-[10px] text-slate-500 font-medium">
                    <span>Speed: ~ 8.4 MB/s</span>
                    <span>100% Peer Tunnel Connection</span>
                  </div>
                </div>
              )}

              {/* If downloader is loading */}
              {isDownloading && (
                <div className="p-4 bg-slate-950/60 border border-slate-850 rounded-xl space-y-3">
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="text-xs font-bold text-purple-400 uppercase tracking-wider">Incoming Stream</p>
                      <p className="text-sm font-semibold text-slate-200 mt-1">Connecting remote port...</p>
                    </div>
                    <span className="text-xs font-semibold bg-purple-500/10 text-purple-400 border border-purple-500/20 px-2 py-0.5 rounded">Downloading</span>
                  </div>
                  <div className="w-full bg-slate-900 rounded-full h-1.5 overflow-hidden">
                    <motion.div
                      initial={{ width: '0%' }}
                      animate={{ width: '100%' }}
                      transition={{ duration: 1.5, repeat: Infinity, ease: 'easeInOut' }}
                      className="bg-gradient-to-r from-purple-500 to-pink-500 h-full rounded-full"
                    />
                  </div>
                  <div className="flex justify-between text-[10px] text-slate-500 font-medium">
                    <span>Speed: ~ 12.1 MB/s</span>
                    <span>Direct Tunnel Connection</span>
                  </div>
                </div>
              )}

              {!isUploading && !isDownloading && (
                <div className="h-40 flex flex-col items-center justify-center text-center text-slate-500 space-y-2 border border-slate-800/40 rounded-xl bg-slate-950/20">
                  <FiServer className="w-6 h-6 opacity-30 text-slate-400" />
                  <p className="text-xs font-semibold">No Active Connections</p>
                  <p className="text-[10px] text-slate-650 max-w-[200px]">Start an upload or download to establish a direct tunnel connection.</p>
                </div>
              )}
            </div>
          </motion.div>
        );

      case 'settings':
        return (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-slate-900/40 backdrop-blur-md border border-slate-800/80 rounded-2xl p-6 shadow-xl text-left space-y-6"
          >
            <div>
              <h3 className="text-lg font-bold text-white mb-1 flex items-center space-x-2">
                <FiSliders className="text-purple-400" />
                <span>Console Settings</span>
              </h3>
              <p className="text-xs text-slate-500">
                Manage your workspace options, local API endpoint config, and tunnel constraints.
              </p>
            </div>

            <div className="space-y-4">
              <div className="space-y-2">
                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider">Backend API Endpoint</label>
                <input
                  type="text"
                  value={apiUrl}
                  onChange={(e) => setApiUrl(e.target.value)}
                  className="w-full px-4 py-2.5 bg-slate-950 border border-slate-855 rounded-xl text-sm font-semibold text-slate-350 focus:outline-none"
                  disabled
                />
                <p className="text-[10px] text-slate-650">Configured from .env.local variables dynamically.</p>
              </div>

              <div className="space-y-2">
                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider">Maximum Seeding Connections</label>
                <div className="flex items-center space-x-4">
                  <input
                    type="range"
                    min="1"
                    max="20"
                    value={maxConnections}
                    onChange={(e) => setMaxConnections(parseInt(e.target.value))}
                    className="flex-1 accent-indigo-500"
                  />
                  <span className="font-mono text-sm font-bold text-white bg-slate-950 px-3 py-1 border border-slate-850 rounded-lg">{maxConnections} peers</span>
                </div>
              </div>

              <div className="flex items-center justify-between p-3.5 bg-slate-950/60 border border-slate-850 rounded-xl">
                <div className="space-y-0.5 text-left">
                  <p className="text-xs font-bold text-slate-200">Force JWT Validation</p>
                  <p className="text-[10px] text-slate-500">All incoming download connection handshakes must validate token signature.</p>
                </div>
                <button
                  onClick={() => setSecureMode(!secureMode)}
                  className={`w-11 h-6 flex items-center rounded-full p-0.5 transition-colors duration-300 focus:outline-none ${
                    secureMode ? 'bg-indigo-600 justify-end' : 'bg-slate-850 justify-start'
                  }`}
                >
                  <motion.div layout className="w-5 h-5 rounded-full bg-white shadow-md" />
                </button>
              </div>
            </div>
          </motion.div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex relative overflow-hidden">
      {/* Absolute Decorative background glows */}
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full bg-blue-600/5 blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] rounded-full bg-purple-600/5 blur-[120px] pointer-events-none" />

      {/* LEFT SIDEBAR */}
      <Sidebar activeView={activeView} setActiveView={setActiveView} />

      {/* RIGHT SIDE / MAIN CONSOLE */}
      <div className="flex-grow flex flex-col min-w-0 overflow-x-hidden">
        <Navbar userEmail={userEmail} onLogout={handleLogout} />
        
        {/* MAIN DASHBOARD PAGE LAYOUT */}
        <main className="flex-grow p-4 md:p-8 overflow-y-auto overflow-x-hidden max-w-[1400px] mx-auto w-full mb-16 md:mb-0">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
            {/* View Column (Left + Middle) */}
            <div className="lg:col-span-2 space-y-6">
              {renderViewContent()}
            </div>

            {/* Metrics Panel Column (Right) - Hidden on mobile, shown on desktop */}
            <div className="hidden lg:block space-y-6">
              {/* Right Panel Cards */}
              <StatsPanel 
                filesSharedCount={filesSharedCount} 
                filesReceivedCount={filesReceivedCount} 
                totalSharedSize={stats.totalSharedSize}
                totalReceivedSize={stats.totalReceivedSize}
              />
              <RecentActivity activities={activities} />
            </div>
          </div>
        </main>
      </div>

      {/* Mobile Bottom Navigation Bar - Visible only on mobile (<768px) */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 z-30 bg-slate-950/80 backdrop-blur-md border-t border-slate-900 py-3 px-6 flex justify-around items-center shadow-xl">
        <button
          onClick={() => {
            setIsStatsOpen(true);
            setIsMenuOpen(false);
            setIsHistoryOpen(false);
          }}
          className="flex flex-col items-center space-y-1 text-slate-400 hover:text-white transition-colors focus:outline-none"
        >
          <FiActivity className="w-5 h-5" />
          <span className="text-[10px] font-bold uppercase tracking-wider">Stats</span>
        </button>

        <button
          onClick={() => {
            setIsMenuOpen(true);
            setIsStatsOpen(false);
            setIsHistoryOpen(false);
          }}
          className="flex flex-col items-center space-y-1 text-slate-400 hover:text-white transition-colors focus:outline-none"
        >
          <FiMenu className="w-5 h-5" />
          <span className="text-[10px] font-bold uppercase tracking-wider">Menu</span>
        </button>

        <button
          onClick={() => {
            setIsHistoryOpen(true);
            setIsMenuOpen(false);
            setIsStatsOpen(false);
          }}
          className="flex flex-col items-center space-y-1 text-slate-400 hover:text-white transition-colors focus:outline-none"
        >
          <FiClock className="w-5 h-5" />
          <span className="text-[10px] font-bold uppercase tracking-wider">History</span>
        </button>
      </div>

      {/* Mobile Stats Drawer */}
      <AnimatePresence>
        {isStatsOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.5 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsStatsOpen(false)}
              className="md:hidden fixed inset-0 bg-black z-40"
            />
            <motion.div
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              exit={{ y: '100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="md:hidden fixed bottom-0 left-0 right-0 max-h-[80vh] bg-slate-950 border-t border-slate-900 rounded-t-3xl z-50 p-6 overflow-y-auto"
            >
              <div className="w-12 h-1.5 bg-slate-800 rounded-full mx-auto mb-6" onClick={() => setIsStatsOpen(false)} />
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400 mb-4 text-center">Dashboard Stats</h3>
              <StatsPanel 
                filesSharedCount={filesSharedCount} 
                filesReceivedCount={filesReceivedCount} 
                totalSharedSize={stats.totalSharedSize}
                totalReceivedSize={stats.totalReceivedSize}
              />
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Mobile Menu (Sidebar Drawer) */}
      <AnimatePresence>
        {isMenuOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.5 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsMenuOpen(false)}
              className="md:hidden fixed inset-0 bg-black z-40"
            />
            <motion.div
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="md:hidden fixed top-0 left-0 bottom-0 w-72 bg-slate-950 border-r border-slate-900 z-50 p-6 flex flex-col justify-between"
            >
              <div>
                <div className="flex justify-between items-center mb-8">
                  <div className="flex items-center space-x-3">
                    <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center shadow-lg">
                      <span className="text-white font-extrabold text-lg">P</span>
                    </div>
                    <span className="text-xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-400 tracking-wider">
                      PeerLink
                    </span>
                  </div>
                  <button onClick={() => setIsMenuOpen(false)} className="text-slate-400 hover:text-white p-1">
                    <FiX className="w-5 h-5" />
                  </button>
                </div>
                
                {/* Menu items */}
                <nav className="flex flex-col space-y-1.5">
                  {[
                    { id: 'dashboard', label: 'Dashboard', icon: FiGrid },
                    { id: 'share', label: 'Share File', icon: FiShare2 },
                    { id: 'receive', label: 'Receive File', icon: FiDownload },
                    { id: 'files', label: 'My Files', icon: FiFolder },
                  ].map((item) => {
                    const Icon = item.icon;
                    const isActive = activeView === item.id;
                    return (
                      <button
                        key={item.id}
                        onClick={() => {
                          setActiveView(item.id);
                          setIsMenuOpen(false);
                        }}
                        className="w-full relative group text-left font-medium focus:outline-none"
                      >
                        <div
                          className={`flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-300 ${
                            isActive ? 'text-white bg-slate-900/60 border border-blue-500/20' : 'text-slate-400'
                          }`}
                        >
                          <Icon className={`w-4 h-4 ${isActive ? 'text-blue-400' : 'text-slate-400'}`} />
                          <span>{item.label}</span>
                        </div>
                      </button>
                    );
                  })}
                </nav>
              </div>

              {/* Bottom Card */}
              <div className="bg-slate-900/40 border border-slate-900 rounded-2xl p-4">
                <p className="text-xs font-bold text-slate-200">Secure & Private</p>
                <p className="text-[10px] text-slate-400 mt-1">All transfers are end-to-end secured with JWT authentication.</p>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Mobile History Drawer */}
      <AnimatePresence>
        {isHistoryOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.5 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsHistoryOpen(false)}
              className="md:hidden fixed inset-0 bg-black z-40"
            />
            <motion.div
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              exit={{ y: '100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="md:hidden fixed bottom-0 left-0 right-0 max-h-[80vh] bg-slate-950 border-t border-slate-900 rounded-t-3xl z-50 p-6 overflow-y-auto flex flex-col"
            >
              <div className="w-12 h-1.5 bg-slate-800 rounded-full mx-auto mb-6 flex-shrink-0" onClick={() => setIsHistoryOpen(false)} />
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400 mb-4 text-center flex-shrink-0">Transfer History</h3>
              
              <div className="space-y-3.5 overflow-y-auto pr-1 flex-grow">
                {transfers.length === 0 ? (
                  <div className="py-12 flex flex-col items-center justify-center text-center text-slate-500 space-y-2">
                    <FiClock className="w-8 h-8 opacity-30" />
                    <p className="text-xs font-semibold">No transfer history</p>
                    <p className="text-[10px] text-slate-600 max-w-[180px]">Your shared and received files will appear here.</p>
                  </div>
                ) : (
                  transfers.map((t: any) => {
                    const isSender = t.senderEmail === userEmail;
                    return (
                      <div
                        key={t.id}
                        className="flex items-center justify-between p-3.5 bg-slate-900/60 border border-slate-900 rounded-xl hover:border-slate-800 transition-colors"
                      >
                        <div className="truncate text-left space-y-1 max-w-[70%]">
                          <p className="text-xs font-semibold text-slate-200 truncate">{t.fileName}</p>
                          <div className="flex flex-col space-y-0.5 text-[9px] text-slate-500">
                            <p className="truncate"><span className="font-bold text-slate-400">From:</span> {t.senderEmail}</p>
                            <p className="truncate"><span className="font-bold text-slate-400">To:</span> {t.receiverEmail}</p>
                          </div>
                        </div>
                        <div className="text-right flex flex-col items-end space-y-1">
                          <span className={`text-[9px] font-bold border px-1.5 py-0.5 rounded-md ${
                            isSender ? 'bg-blue-500/10 text-blue-400 border-blue-500/20' : 'bg-purple-500/10 text-purple-400 border-purple-500/20'
                          }`}>
                            {isSender ? 'Sent' : 'Received'}
                          </span>
                          <span className="text-[9px] text-slate-500">
                            {new Date(t.downloadedAt).toLocaleString()}
                          </span>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}

