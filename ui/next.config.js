/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  async rewrites() {
    return [
      {
        source: '/api/upload',
        destination: 'http://backend:9090/upload',
      },
      {
        source: '/api/download/:port',
        destination: 'http://backend:9090/download/:port',
      },
    ];
  },
}

module.exports = nextConfig