import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import App from './App.jsx';
import { AuthProvider } from './context/AuthContext.jsx';
import { ThemeProvider } from './context/ThemeContext.jsx';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <App />
          <Toaster
            position="top-right"
            toastOptions={{
              duration: 4000,
              className: 'bs-toast',
              style: {
                borderRadius: '12px',
                padding: '12px 16px',
                fontSize: '14px',
                fontWeight: 500,
              },
              success: { iconTheme: { primary: '#0a7b3f', secondary: '#fff' } },
              error:   { iconTheme: { primary: '#c52828', secondary: '#fff' } },
            }}
          />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </React.StrictMode>
);
