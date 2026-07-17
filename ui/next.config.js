/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,

  async rewrites() {
    const backend =
      process.env.NODE_ENV === 'development'
        ? 'http://localhost:9090'
        : 'https://peerlink-backend-k7va.onrender.com';

    return [
      {
        source: '/api/upload',
        destination: `${backend}/upload`,
      },
      {
        source: '/api/files',
        destination: `${backend}/files`,
      },
      {
        source: '/api/transfers',
        destination: `${backend}/transfers`,
      },
      {
        source: '/api/stats',
        destination: `${backend}/stats`,
      },
      {
        source: '/api/download/:port',
        destination: `${backend}/download/:port`,
      },
      {
        source: '/api/signup',
        destination: `${backend}/signup`,
      },
      {
        source: '/api/login',
        destination: `${backend}/login`,
      },
      {
        source: '/api/refresh',
        destination: `${backend}/refresh`,
      },
      {
        source: '/api/send-otp',
        destination: `${backend}/send-otp`,
      },
      {
        source: '/api/verify-otp',
        destination: `${backend}/verify-otp`,
      },
    ];
  },
};

module.exports = nextConfig;