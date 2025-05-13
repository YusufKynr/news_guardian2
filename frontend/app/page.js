'use client';

import { useState } from 'react';

export default function Home() {
  const [newsInput, setNewsInput] = useState('');
  const [newsImage, setNewsImage] = useState(null);
  const [similarNews, setSimilarNews] = useState([]);
  const [analysisResults, setAnalysisResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [step, setStep] = useState('input');
  const [activeTab, setActiveTab] = useState('text'); // 'text' veya 'image'
  const [dragActive, setDragActive] = useState(false);
  const [selectedNewsForComparison, setSelectedNewsForComparison] = useState(null);

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    const file = e.dataTransfer.files[0];
    if (file) {
      await handleImageUpload(file);
    }
  };

  const handleImageUpload = async (file) => {
    if (!file) return;

    setLoading(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('http://localhost:8090/img/convert', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error('Görsel işlenirken bir hata oluştu');
      }

      const data = await response.json();
      setNewsInput(data.text);
      setActiveTab('text');
    } catch (err) {
      setError('Görsel yüklenirken bir hata oluştu: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!newsInput.trim()) return;

    setLoading(true);
    setError(null);
    setSimilarNews([]);

    try {
      const response = await fetch(`http://localhost:8080/api/similar?query=${encodeURIComponent(newsInput)}`);
      
      if (!response.ok) {
        throw new Error('Benzer haberler aranırken bir hata oluştu');
      }

      const data = await response.json();
      // Benzerlik oranına göre büyükten küçüğe sırala
      const sortedNews = data.sort((a, b) => b.similarity - a.similarity);
      setSimilarNews(sortedNews);
      setStep('search');
    } catch (err) {
      setError('Arama sırasında bir hata oluştu: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleAnalyze = async () => {
    if (similarNews.length === 0) return;

    setLoading(true);
    setError(null);

    try {
      const analysisPromises = similarNews.map(news => 
        fetch('http://localhost:8000/analyze', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            input_news: newsInput,
            comparison_news: news.summary || ''
          }),
        }).then(res => res.json())
      );

      const results = await Promise.all(analysisPromises);
      
      // En yüksek benzerlik skoruna sahip sonucu al
      const bestMatch = results.reduce((prev, current) => 
        (current.similarity_score > prev.similarity_score) ? current : prev
      );

      setAnalysisResults({
        ...bestMatch,
        similarNews: similarNews
      });
      setStep('analysis');
    } catch (err) {
      setError('Analiz sırasında bir hata oluştu: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCompare = async (news) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('http://localhost:8000/analyze', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          input_news: newsInput,
          comparison_news: news.summary
        })
      });

      if (!response.ok) {
        throw new Error('Karşılaştırma yapılırken bir hata oluştu');
      }

      const data = await response.json();
      setAnalysisResults(data);
      setSelectedNewsForComparison(news);
      setStep('analysis');
    } catch (err) {
      setError('Karşılaştırma sırasında bir hata oluştu: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-8">
      <header className="text-center mb-12">
        <div className="inline-block mb-6">
          <div className="flex items-center justify-center space-x-2">
            <svg
              className="w-10 h-10 text-blue-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"
              />
            </svg>
            <span className="text-3xl font-bold bg-gradient-to-r from-blue-600 to-blue-800 bg-clip-text text-transparent">
              News Guardian
            </span>
          </div>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">
            Makine Öğrenmesi Destekli Haber Doğrulama Platformu
          </p>
        </div>
        <h1 className="text-4xl font-bold mb-2">Haber Doğrulama Asistanı</h1>
        <p className="text-gray-600 dark:text-gray-400 text-lg">
          Haberleri analiz ederek doğruluğunu kontrol edin
        </p>
      </header>

      {step === 'input' && (
        <div className="max-w-2xl mx-auto">
          <div className="mb-6">
            <div className="flex space-x-1 bg-gray-800/20 p-1 rounded-lg">
              <button
                className={`flex-1 py-2.5 px-5 rounded-md font-medium text-sm transition-all duration-200 ${
                  activeTab === 'text'
                    ? 'bg-blue-600 text-white shadow-lg'
                    : 'text-gray-400 hover:text-gray-200'
                }`}
                onClick={() => setActiveTab('text')}
              >
                <div className="flex items-center justify-center space-x-2">
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                    />
                  </svg>
                  <span>Metin Girişi</span>
                </div>
              </button>
              <button
                className={`flex-1 py-2.5 px-5 rounded-md font-medium text-sm transition-all duration-200 ${
                  activeTab === 'image'
                    ? 'bg-blue-600 text-white shadow-lg'
                    : 'text-gray-400 hover:text-gray-200'
                }`}
                onClick={() => setActiveTab('image')}
              >
                <div className="flex items-center justify-center space-x-2">
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                    />
                  </svg>
                  <span>Görsel Yükle</span>
                </div>
              </button>
            </div>
          </div>

          <form onSubmit={handleSearch} className="space-y-6">
            {activeTab === 'text' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Haber Metni
                </label>
                <textarea
                  value={newsInput}
                  onChange={(e) => setNewsInput(e.target.value)}
                  placeholder="Doğrulamak istediğiniz haberi buraya yapıştırın..."
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[200px] dark:bg-gray-800 dark:border-gray-700 dark:text-white"
                />
              </div>
            )}

            {activeTab === 'image' && (
              <div
                className={`relative border-2 border-dashed rounded-lg p-8 text-center ${
                  dragActive
                    ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                    : 'border-gray-300 dark:border-gray-700'
                }`}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
              >
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => handleImageUpload(e.target.files[0])}
                  className="hidden"
                  id="file-upload"
                />
                <label
                  htmlFor="file-upload"
                  className="cursor-pointer flex flex-col items-center"
                >
                  <svg
                    className="w-12 h-12 text-gray-400 dark:text-gray-600 mb-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                    />
                  </svg>
                  <p className="text-gray-600 dark:text-gray-400 mb-2">
                    Görsel dosyasını sürükleyip bırakın veya
                  </p>
                  <span className="text-blue-600 hover:text-blue-700 font-medium">
                    Dosya Seçin
                  </span>
                  <p className="text-sm text-gray-500 dark:text-gray-500 mt-2">
                    PNG, JPG, GIF dosyaları desteklenir
                  </p>
                </label>
              </div>
            )}

            <button
              type="submit"
              disabled={loading || !newsInput.trim()}
              className="w-full py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
            >
              {loading ? 'İşleniyor...' : 'Analizi Başlat'}
            </button>
          </form>
        </div>
      )}

      {step === 'search' && similarNews.length > 0 && (
        <div className="max-w-2xl mx-auto">
          <h2 className="text-2xl font-semibold mb-4">Bulunan Benzer Haberler</h2>
          <div className="space-y-4 mb-6">
            {similarNews.map((news, index) => (
              <div key={index} className="border rounded-lg p-4 relative">
                <div className={`absolute top-2 right-2 px-3 py-1 rounded-full text-sm font-medium ${
                  news.similarity >= 0.7 ? 'bg-green-100 text-green-800 dark:bg-green-900/50 dark:text-green-200' :
                  news.similarity >= 0.5 ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/50 dark:text-yellow-200' :
                  'bg-red-100 text-red-800 dark:bg-red-900/50 dark:text-red-200'
                }`}>
                  {(news.similarity * 100).toFixed(1)}% Benzerlik
                </div>
                
                <h3 className="font-medium mb-2 pr-32">{news.title}</h3>
                <p className="text-gray-600 dark:text-gray-400 text-sm mb-2">{news.summary}</p>
                <div className="flex items-center space-x-4">
                  <a
                    href={news.link}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 text-sm inline-flex items-center"
                  >
                    Habere Git
                    <svg className="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                    </svg>
                  </a>
                  <button
                    onClick={() => handleCompare(news)}
                    disabled={loading}
                    className="text-purple-600 hover:text-purple-800 dark:text-purple-400 dark:hover:text-purple-300 text-sm inline-flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Detaylı Karşılaştır
                    <svg className="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                  </button>
                </div>
              </div>
            ))}
          </div>
          <button
            onClick={handleAnalyze}
            disabled={loading}
            className="w-full py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200 dark:bg-green-500 dark:hover:bg-green-600"
          >
            {loading ? 'Analiz Ediliyor...' : 'Haberleri Analiz Et'}
          </button>
        </div>
      )}

      {step === 'analysis' && analysisResults && (
        <div className="max-w-2xl mx-auto">
          <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-lg">
            <div className="flex justify-between items-start mb-6">
              <h2 className="text-2xl font-semibold dark:text-white">Karşılaştırma Sonuçları</h2>
              <button
                onClick={() => setStep('search')}
                className="text-gray-600 hover:text-gray-800 dark:text-gray-400 dark:hover:text-gray-200"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            <div className="mb-8">
              <div className="text-lg font-medium mb-4 p-4 bg-blue-50 dark:bg-blue-900/50 text-blue-800 dark:text-blue-200 rounded-lg">
                {analysisResults.fact_check_summary}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
              <div className="space-y-2">
                <h3 className="text-lg font-medium dark:text-white">Girilen Haber</h3>
                <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
                  <p className="text-gray-800 dark:text-gray-200">{newsInput}</p>
                </div>
              </div>
              
              <div className="space-y-2">
                <h3 className="text-lg font-medium dark:text-white">Karşılaştırılan Haber</h3>
                <div className="p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
                  <p className="text-gray-800 dark:text-gray-200">{selectedNewsForComparison?.summary}</p>
                  <a
                    href={selectedNewsForComparison?.link}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 text-sm inline-flex items-center mt-2"
                  >
                    Kaynağa Git
                    <svg className="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                    </svg>
                  </a>
                </div>
              </div>
            </div>

            {analysisResults.discrepancies.length > 0 && (
              <div className="mb-8">
                <h3 className="text-lg font-medium mb-4 dark:text-white">Tespit Edilen Farklılıklar</h3>
                <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg p-4">
                  <ul className="list-disc list-inside space-y-2">
                    {analysisResults.discrepancies.map((discrepancy, index) => (
                      <li key={index} className="text-yellow-800 dark:text-yellow-200">{discrepancy}</li>
                    ))}
                  </ul>
                </div>
              </div>
            )}

            <div className="mb-8">
              <h3 className="text-lg font-medium mb-4 dark:text-white">Benzerlik Skoru</h3>
              <div className="flex items-center gap-4">
                <div className="text-3xl font-bold text-blue-600 dark:text-blue-400">
                  {(analysisResults.similarity_score * 100).toFixed(1)}%
                </div>
                <div className={`px-3 py-1 rounded-full text-sm ${
                  analysisResults.similarity_score >= 0.8
                    ? 'bg-green-100 text-green-800 dark:bg-green-900/50 dark:text-green-200'
                    : analysisResults.similarity_score >= 0.5
                    ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/50 dark:text-yellow-200'
                    : 'bg-red-100 text-red-800 dark:bg-red-900/50 dark:text-red-200'
                }`}>
                  {analysisResults.similarity_score >= 0.8
                    ? 'Yüksek Güvenilirlik'
                    : analysisResults.similarity_score >= 0.5
                    ? 'Orta Güvenilirlik'
                    : 'Düşük Güvenilirlik'}
                </div>
              </div>
            </div>

            <button
              onClick={() => setStep('search')}
              className="w-full py-3 bg-gray-600 text-white rounded-lg hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-500 dark:bg-gray-700 dark:hover:bg-gray-600"
            >
              Diğer Haberlere Dön
            </button>
          </div>
        </div>
      )}

      {error && (
        <div className="max-w-2xl mx-auto mt-4 p-4 bg-red-100 text-red-700 rounded-lg">
          {error}
        </div>
      )}
    </div>
  );
}