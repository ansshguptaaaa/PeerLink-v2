/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  async rewrites() {
    return [
      {
        source: '/api/upload',
        destination: 'https://peerlink-backend-k7va.onrender.com/upload',
      },
      {
        source: '/api/download/:port',
        destination: 'https://peerlink-backend-k7va.onrender.com/download/:port',
      },
    ];
  },
}

module.exports = nextConfig