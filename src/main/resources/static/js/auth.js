/**
 * JWT Authentication Helper
 * This script provides functions to work with JWT tokens for authentication
 */

// Initialize authentication when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    console.log('Auth module initialized');
    // Check if we're on a protected page
    if (isAuthenticationRequired()) {
        validateAuthentication();
    }
});

/**
 * Determine if current page requires authentication
 */
function isAuthenticationRequired() {
    // List of paths that require authentication
    const protectedPaths = ['/dashboard'];
    const currentPath = window.location.pathname;
    
    return protectedPaths.some(path => currentPath.startsWith(path));
}

/**
 * Validate user authentication
 */
function validateAuthentication() {
    const token = getToken();
    
    if (!token) {
        console.warn('No JWT token found, redirecting to login');
        window.location.href = '/login';
        return;
    }
    
    // Optional: Validate token on server or check expiration
    // For a simple check, we could verify if the token can be decoded
    try {
        const payload = parseJwt(token);
        const currentTime = Math.floor(Date.now() / 1000);
        
        if (payload.exp && payload.exp < currentTime) {
            console.warn('JWT token expired');
            removeToken();
            window.location.href = '/login';
            return;
        }
        
        console.log('User authenticated:', payload.sub);
    } catch (e) {
        console.error('Invalid token:', e);
        removeToken();
        window.location.href = '/login';
    }
}

/**
 * Get JWT token from storage
 */
function getToken() {
    return localStorage.getItem('token') || localStorage.getItem('jwtToken');
}

/**
 * Save JWT token to storage
 */
function setToken(token) {
    localStorage.setItem('jwtToken', token);
}

/**
 * Remove JWT token from storage
 */
function removeToken() {
    localStorage.removeItem('jwtToken');
}

/**
 * Parse JWT token to get payload
 */
function parseJwt(token) {
    try {
        // Add a basic check if the token looks like a JWT
        if (!token || typeof token !== 'string' || token.split('.').length !== 3) {
            console.error('Invalid token format:', token);
            return null;
        }
        // Get the payload part of the JWT (second part)
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error('Error parsing JWT token:', e);
        return null;
    }
}

/**
 * Perform authenticated fetch request with JWT token
 */
function fetchWithAuth(url, options = {}) {
    const token = getToken();
    options.headers = options.headers || {};
    
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
    
    return fetch(url, options)
        .then(response => {
            // Handle unauthorized errors (e.g., expired token)
            if (response.status === 401) {
                removeToken();
                window.location.href = '/login';
                throw new Error('Authentication failed');
            }
            return response;
        });
}

/**
 * Logout user by removing token and redirecting
 */
function logout() {
    // First remove the token from localStorage
    removeToken();
    
    // Clear any session cookies via POST to logout endpoint
    fetch('/logout', {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
            'X-CSRF-TOKEN': getCsrfToken()
        }
    }).then(() => {
        console.log('Logout successful');
        // Navigate to home page or login page
        window.location.href = '/';
    }).catch(error => {
        console.error('Logout error:', error);
        // Even if the server logout fails, still redirect
        window.location.href = '/';
    });
}

/**
 * Get CSRF token from meta tag
 */
function getCsrfToken() {
    const metaTag = document.querySelector('meta[name="_csrf"]');
    return metaTag ? metaTag.getAttribute('content') : '';
}

// Export functions for global use
window.auth = {
    getToken,
    setToken,
    removeToken,
    fetchWithAuth,
    logout
}; 