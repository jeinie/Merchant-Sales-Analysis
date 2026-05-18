import React, { useState, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import MapArea from './components/MapArea';
import DetailsPanel from './components/DetailsPanel';
import { franchises as mockFranchises, industryAverages as mockIndustryAverages, regionAverages as mockRegionAverages } from './data/mockData';

function App() {
  const [franchises, setFranchises] = useState([]);
  const [averages, setAverages] = useState({ industryAverages: {}, regionAverages: {} });
  
  const [selectedRegion, setSelectedRegion] = useState('전체');
  const [selectedIndustry, setSelectedIndustry] = useState('전체');
  const [selectedFranchiseId, setSelectedFranchiseId] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setFranchises(mockFranchises);
    setAverages({
      industryAverages: mockIndustryAverages,
      regionAverages: mockRegionAverages
    });
    setLoading(false);
  }, []);

  // Filter franchises
  const filteredFranchises = franchises.filter(f => {
    const regionMatch = selectedRegion === '전체' || f.region === selectedRegion;
    const industryMatch = selectedIndustry === '전체' || f.industry === selectedIndustry;
    return regionMatch && industryMatch;
  });

  const selectedFranchise = franchises.find(f => f.id === selectedFranchiseId);

  if (loading) {
    return <div className="app-container">데이터를 불러오는 중입니다... (오라클 DB 연결 확인 필요)</div>;
  }

  return (
    <div className="dashboard-container">
      <header className="header">
        가맹점 매출 시각화 및 AI 분석 플랫폼
      </header>
      
      <main className="main-content">
        <Sidebar 
          selectedRegion={selectedRegion}
          setSelectedRegion={setSelectedRegion}
          selectedIndustry={selectedIndustry}
          setSelectedIndustry={setSelectedIndustry}
          franchisesCount={filteredFranchises.length}
        />
        
        <MapArea 
          franchises={filteredFranchises} 
          onMarkerClick={setSelectedFranchiseId} 
          selectedFranchiseId={selectedFranchiseId}
        />
        
        <DetailsPanel 
          franchise={selectedFranchise} 
          onClose={() => setSelectedFranchiseId(null)}
          averages={averages}
        />
      </main>
    </div>
  );
}

export default App;
