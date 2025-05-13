'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useState } from 'react';

const Navbar = () => {
  const pathname = usePathname();
  const [isOpen, setIsOpen] = useState(true);

  return (
    <>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={`fixed top-4 ${isOpen ? 'left-56' : 'left-4'} z-20 p-2 rounded-lg bg-gray-800 text-white hover:bg-gray-700 transition-all duration-300`}
      >
        {isOpen ? (
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
          </svg>
        ) : (
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 5l7 7-7 7M5 5l7 7-7 7" />
          </svg>
        )}
      </button>

      <div className={`fixed top-0 left-0 h-full bg-gray-900 text-white transition-all duration-300 z-10 ${
        isOpen ? 'w-64' : 'w-0'
      } overflow-hidden`}>
        <div className="flex flex-col h-full p-4">
          <div className="mb-8">
            <Link href="/" className="text-2xl font-bold text-white hover:text-gray-300">
              News Guardian
            </Link>
          </div>

          <nav className="flex-1">
            <div className="space-y-2">
              <Link
                href="/"
                className={`block px-4 py-2 rounded-lg transition-colors ${
                  pathname === '/'
                    ? 'bg-gray-700 text-white'
                    : 'text-gray-300 hover:bg-gray-800'
                }`}
              >
                Ana Sayfa
              </Link>
              <Link
                href="/history"
                className={`block px-4 py-2 rounded-lg transition-colors ${
                  pathname === '/history'
                    ? 'bg-gray-700 text-white'
                    : 'text-gray-300 hover:bg-gray-800'
                }`}
              >
                Arama Geçmişi
              </Link>
            </div>
          </nav>

          <div className="border-t border-gray-700 pt-4 space-y-2">
            <Link
              href="/login"
              className={`block px-4 py-2 rounded-lg transition-colors ${
                pathname === '/login'
                  ? 'bg-gray-700 text-white'
                  : 'text-gray-300 hover:bg-gray-800'
              }`}
            >
              Giriş Yap
            </Link>
            <Link
              href="/signup"
              className={`block px-4 py-2 rounded-lg transition-colors ${
                pathname === '/signup'
                  ? 'bg-gray-700 text-white'
                  : 'text-gray-300 hover:bg-gray-800'
              }`}
            >
              Kayıt Ol
            </Link>
          </div>
        </div>
      </div>
    </>
  );
};

export default Navbar;