import axios from 'axios';

// Create a custom axios instance with base URL configuration
const api = axios.create({
  baseURL: '', // Uses Next.js proxy rewrites
});

// Request interceptor to automatically attach JWT Access Token
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle token expiration (401 Unauthorized) and execute token rotation/refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Guard to check for 401 Unauthorized and prevent infinite retries
    if (
      error.response &&
      error.response.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes(`${process.env.NEXT_PUBLIC_API_URL}/refresh`) &&
      !originalRequest.url?.includes(`${process.env.NEXT_PUBLIC_API_URL}/login`)
    ) {
      originalRequest._retry = true;

      try {
        if (typeof window !== 'undefined') {
          const refreshToken = localStorage.getItem('refreshToken');
          if (refreshToken) {
            // Call refresh endpoint to rotate tokens
            const res = await axios.post(`${process.env.NEXT_PUBLIC_API_URL}/refresh`, { refreshToken });
            if (res.status === 200) {
              const { accessToken, refreshToken: newRefreshToken } = res.data;
              localStorage.setItem('accessToken', accessToken);
              localStorage.setItem('refreshToken', newRefreshToken);

              // Re-attempt original request with updated token
              originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
              return api(originalRequest);
            }
          }
        }
      } catch (refreshError) {
        console.error('Session expired, logging out:', refreshError);
        // Clear auth tokens and redirect user to login
        if (typeof window !== 'undefined') {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('userEmail');
          localStorage.removeItem('userRole');
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
