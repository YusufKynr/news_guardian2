"use client";
import Navbar from "@/components/Navbar";
import { useState } from 'react';

export default function Home() {
  const [activeTab, setActiveTab] = useState('text');
  const [inputValue, setInputValue] = useState('');
  const [results, setResults] = useState(null);

  const handleSearch = async (inputValue) => {
    try {
      const nerResponse = await fetch(`http://127.0.0.1:8000/ner/extract?text=${encodeURIComponent(inputValue)}`);
      const nerData = await nerResponse.json();
  
      // NER'den gelen önemli kelimeleri al
      const keywords = nerData.entities.map(entity => entity.word).join(" ");
  
      // Google Custom Search API'yi çağır
      const searchResponse = await fetch(`http://localhost:8080/api/news/similar?query=${encodeURIComponent(keywords)}`);
      const searchData = await searchResponse.json();
  
      setResults(searchData);
    } catch (error) {
      console.error("Error:", error);
    }
  };
  
  


  return (
    <div className="min-h-screen flex flex-col text-center bg-gradient-to-b from-gray-900 to-black text-white">
      <Navbar />
      
      <main className="flex-grow py-16"> 
        <section className="text-center mb-16">
          <h1 className="text-6xl font-extrabold text-white mb-4">
            News Guardian
          </h1> 
          <p className="text-lg text-gray-300">
            Haberin doğruluğunu kontrol etmek için buraya tıklayın.
          </p>
          
        </section>

        <section className="mb-16">
          <div className="flex justify-center gap-4 mb-4">
            <div className="inline-flex rounded-lg bg-gray-800 p-1 shadow-md">
              <button 
                className={`px-5 py-2 rounded-lg text-sm font-medium transition-all ${activeTab === 'text' ? 'bg-gray-700 text-white shadow-lg' : 'text-gray-400 hover:text-white'}`} 
                onClick={() => setActiveTab('text')}
              >
                Metin
              </button>
              <button 
                className={`px-5 py-2 rounded-lg text-sm font-medium transition-all ${activeTab === 'image' ? 'bg-gray-700 text-white shadow-lg' : 'text-gray-400 hover:text-white'}`} 
                onClick={() => setActiveTab('image')}
              >
                Görsel
              </button>
            </div>
          </div>
          <div className="flex justify-center gap-4">
            {activeTab === 'text' ? (
              <textarea
                placeholder="Haber başlığı veya metnini girin"
                className="w-full max-w-md px-4 py-3 border border-gray-600 bg-gray-800 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder-gray-400 text-white"
                rows="1"
                onChange={(e) => setInputValue(e.target.value)}
                value={inputValue}
                onInput={(e) => {
                  e.target.style.height = 'auto';
                  e.target.style.height = e.target.scrollHeight + 'px';
                }}
              />
            ) : (
              <input
                type="file"
                accept="image/*"
                className="w-full max-w-md px-4 py-3 border border-gray-600 bg-gray-800 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder-gray-400 text-white"
              />
            )}
            <button className="px-6 py-3 bg-gray-700 text-white rounded-lg border border-gray-600 hover:bg-gray-600 hover:text-white transition-colors"
            onClick={() => handleSearch(inputValue)}>
              Ara
     
            </button>
          </div>
        </section>

        <section className="grid grid-cols-1 md:grid-cols-3 gap-10 max-w-5xl mx-auto">
          <div className="bg-gray-800 p-4 rounded-lg shadow-lg hover:scale-105 transition-transform">
            <h2 className="text-xl font-semibold mb-2">Hızlı Analiz</h2>
            <p className="text-gray-300">Saniyeler içinde sonuç alın.</p>
          </div>
          <div className="bg-gray-800 p-4 rounded-lg shadow-lg hover:scale-105 transition-transform">
            <h2 className="text-xl font-semibold mb-2">Güvenilir Sonuçlar</h2>
            <p className="text-gray-300">%95 doğruluk oranı.</p>
          </div>
          <div className="bg-gray-800 p-4 rounded-lg shadow-lg hover:scale-105 transition-transform">
            <h2 className="text-xl font-semibold mb-2">Detaylı Rapor</h2>
            <p className="text-gray-300">Kapsamlı analiz raporu.</p>
          </div>
        </section>

        <section className="mt-16">
          <h2 className="text-3xl font-semibold mb-4">Sonuçlar</h2>

          {results && results.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-10 max-w-5xl mx-auto">
                          {results && results.map((news, index) => (
                <div key={index} className="bg-gray-800 p-4 rounded-lg shadow-lg">
                  <h3 className="text-xl font-semibold mb-2">{news.title}</h3>
                  <p className="text-gray-300">{news.snippet}</p>
                  <a href={news.link} target="_blank" className="text-blue-400">Haberi Oku</a>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-300">Sonuç bulunamadı.</p>
          )}
            
        </section>
      </main>

      <footer className="bg-gray-900 py-3 text-center text-sm text-gray-500">
        <p>© 2025 News Guardian. Tüm hakları saklıdır.</p>
      </footer>
    </div>
  );
}
