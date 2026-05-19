import React, { useState, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import MapArea from './components/MapArea';
import DetailsPanel from './components/DetailsPanel';
import Login from './components/Login';
import AdminPage from './components/AdminPage';
import { franchises as mockFranchises, industryAverages as mockIndustryAverages, regionAverages as mockRegionAverages, users as mockUsers } from './data/mockData';

function App() {
  // Authentication State
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem('currentUser');
    return saved ? JSON.parse(saved) : null;
  });

  const [usersData, setUsersData] = useState(() => {
    const saved = localStorage.getItem('usersData');
    if (!saved) return mockUsers;
    
    // 로컬 스토리지 데이터가 있더라도, mockData.js에서 수정된 이름(name)은 자동으로 동기화되도록 병합합니다.
    try {
      const parsed = JSON.parse(saved);
      return parsed.map(u => {
        const original = mockUsers.find(mu => mu.id === u.id);
        return original ? { ...u, name: original.name } : u;
      });
    } catch (e) {
      return mockUsers;
    }
  });

  // Whenever usersData changes, save to localStorage
  useEffect(() => {
    localStorage.setItem('usersData', JSON.stringify(usersData));
    
    // If current user's permissions or assigned franchises changed, update currentUser
    if (currentUser) {
      const updatedUser = usersData.find(u => u.id === currentUser.id);
      if (updatedUser) {
        setCurrentUser(updatedUser);
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
      }
    }
  }, [usersData]);

  const [currentView, setCurrentView] = useState('dashboard'); // 'dashboard' | 'admin'
  
  const [franchises, setFranchises] = useState(() => {
    const saved = localStorage.getItem('currentUser');
    const user = saved ? JSON.parse(saved) : null;
    if (!user) return [];
    if (user.role === 'SALES' && user.assignedFranchiseIds) {
      return mockFranchises.filter(f => user.assignedFranchiseIds.includes(f.id));
    }
    return mockFranchises;
  });
  
  const [averages, setAverages] = useState({ 
    industryAverages: mockIndustryAverages, 
    regionAverages: mockRegionAverages 
  });
  
  const [selectedRegion, setSelectedRegion] = useState('전체');
  const [selectedIndustry, setSelectedIndustry] = useState('전체');
  const [selectedFranchiseId, setSelectedFranchiseId] = useState(null);
  const handleLogin = (user) => {
    localStorage.setItem('currentUser', JSON.stringify(user));
    setCurrentUser(user);
    
    // 즉시 가맹점 리스트 업데이트
    if (user.role === 'SALES' && user.assignedFranchiseIds) {
      setFranchises(mockFranchises.filter(f => user.assignedFranchiseIds.includes(f.id)));
    } else {
      setFranchises(mockFranchises);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('currentUser');
    setCurrentUser(null);
    setSelectedFranchiseId(null);
    setCurrentView('dashboard');
    setFranchises([]); // 로그아웃 시 리스트 비우기
  };

  // Filter franchises
  const filteredFranchises = franchises.filter(f => {
    const regionMatch = selectedRegion === '전체' || f.region === selectedRegion;
    const industryMatch = selectedIndustry === '전체' || f.industry === selectedIndustry;
    return regionMatch && industryMatch;
  });

  const selectedFranchise = franchises.find(f => f.id === selectedFranchiseId);

  if (!currentUser) {
    return <Login usersData={usersData} onLogin={handleLogin} />;
  }

  if (currentView === 'admin' && currentUser.role === 'ADMIN') {
    return <AdminPage usersData={usersData} setUsersData={setUsersData} onClose={() => setCurrentView('dashboard')} />;
  }

  return (
    <div className="dashboard-container">
      <header className="header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: '20px' }}>
        <div style={{ fontWeight: 'bold' }}>가맹점 매출 시각화 및 AI 분석 플랫폼</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px', fontSize: '0.9rem' }}>
          {currentUser.role === 'ADMIN' && (
            <button 
              onClick={() => setCurrentView('admin')}
              style={{
                backgroundColor: '#10b981', color: 'white', border: 'none',
                padding: '6px 12px', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold'
              }}
            >
              ⚙️ 관리자 설정
            </button>
          )}
          <span style={{ backgroundColor: '#f3f4f6', color: '#374151', padding: '6px 12px', borderRadius: '6px', fontWeight: '500', border: '1px solid #e5e7eb' }}>
            👤 {currentUser.name} ({currentUser.role})
          </span>
          <button 
            onClick={handleLogout}
            style={{ 
              backgroundColor: '#ef4444', color: 'white', border: 'none', 
              padding: '6px 14px', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold',
              boxShadow: '0 1px 2px rgba(0,0,0,0.1)'
            }}
          >
            로그아웃
          </button>
        </div>
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
          currentUser={currentUser}
        />
      </main>
    </div>
  );
}

export default App;
