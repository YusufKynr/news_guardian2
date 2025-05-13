import { Inter } from 'next/font/google';
import './globals.css';
import Navbar from './components/Navbar';
import ThemeToggle from './components/ThemeToggle';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';

const inter = Inter({ subsets: ['latin'] });

export const metadata = {
  title: 'News Guardian',
  description: 'Haber doğrulama platformu',
};

export default function RootLayout({ children }) {
  return (
    <html lang="tr" className="light">
      <body className={`${inter.className} bg-white dark:bg-gray-900 text-gray-900 dark:text-white`}>
        <ThemeProvider>
          <AuthProvider>
            <ThemeToggle />
            <div className="flex min-h-screen">
              <div className="relative">
                <Navbar />
              </div>
              <main className="flex-1 transition-all duration-300">
                <div className="mx-auto w-full max-w-4xl px-8 py-8">
                  {children}
                </div>
              </main>
            </div>
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
} 