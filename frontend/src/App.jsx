import React, { useState, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import MapArea from './components/MapArea';
import DetailsPanel from './components/DetailsPanel';
import Login from './components/Login';
import AdminPage from './components/AdminPage';
import { api } from './utils/api';

const RISK_LABELS = {
  CHECK_REQUIRED: '점검 필요',
  CAUTION: '주의',
  NORMAL: '정상 관찰',
};

function App() {
  // Authentication State
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem('currentUser');
    return saved ? JSON.parse(saved) : null;
  });

  const [usersData, setUsersData] = useState([]);
  const [franchises, setFranchises] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [averages, setAverages] = useState({ 
    industryAverages: {}, 
    regionAverages: {} 
  });
  
  const [currentView, setCurrentView] = useState('dashboard'); // 'dashboard' | 'admin'
  const [selectedRegion, setSelectedRegion] = useState('전체');
  const [selectedIndustry, setSelectedIndustry] = useState('전체');
  const [selectedFranchiseId, setSelectedFranchiseId] = useState(null);

  const handleLogout = () => {
    api.logout();
    setCurrentUser(null);
    setUsersData([]);
    setSelectedFranchiseId(null);
    setCurrentView('dashboard');
    setFranchises([]);
    setAlerts([]);
  };

  // Load all required data from the authenticated backend API.
  const loadData = async () => {
    if (!currentUser || !api.hasAuth()) {
      handleLogout();
      return;
    }

    try {
      const franchiseList = await api.getFranchises();
      setFranchises(franchiseList);

      const alertList = await api.getAlerts();
      setAlerts(alertList);

      const averageData = await api.getAverages();
      setAverages(averageData);

      const usersList = await api.getUsers();
      setUsersData(usersList);
      
      const updatedProfile = usersList.find(u => u.id === currentUser.id);
      if (updatedProfile) {
        setCurrentUser(updatedProfile);
        localStorage.setItem('currentUser', JSON.stringify(updatedProfile));
      }
    } catch (err) {
      console.error('데이터 로드 실패:', err);
      if (err.status === 401) {
        handleLogout();
      }
    }
  };

  // Trigger loading when user session is active.
  useEffect(() => {
    loadData();
  }, [currentUser?.id]);

  // Load public users for the login test account guide.
  useEffect(() => {
    if (!currentUser) {
      api.getTestUsers().then(setUsersData).catch(console.error);
    }
  }, [currentUser]);

  const handleLogin = (user) => {
    localStorage.setItem('currentUser', JSON.stringify(user));
    setCurrentUser(user);
  };

  // Filter franchises in frontend for region & industry selections
  const filteredFranchises = franchises.filter(f => {
    const regionMatch = selectedRegion === '전체' || f.region === selectedRegion;
    const industryMatch = selectedIndustry === '전체' || f.industry === selectedIndustry;
    return regionMatch && industryMatch;
  });

  const [showAlerts, setShowAlerts] = React.useState(false);
  const filteredFranchiseIds = new Set(filteredFranchises.map(franchise => franchise.id));
  const visibleAlerts = alerts.filter(alert => filteredFranchiseIds.has(alert.franchiseId));
  const checkRequiredCount = visibleAlerts.filter(alert => alert.riskLevel === 'CHECK_REQUIRED').length;
  const cautionCount = visibleAlerts.filter(alert => alert.riskLevel === 'CAUTION').length;

  const selectedFranchise = franchises.find(f => f.id === selectedFranchiseId);

  if (!currentUser) {
    return <Login usersData={usersData} onLogin={handleLogin} />;
  }

  if (currentView === 'admin' && currentUser.role === 'ADMIN') {
    return (
      <AdminPage 
        usersData={usersData} 
        franchises={franchises} 
        onRefresh={loadData} 
        onClose={() => setCurrentView('dashboard')} 
      />
    );
  }

  return (
    <div className="dashboard-container">
      {/* Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', padding: '16px' }}>
        <div className="card">
          <div className="metric-label">총 매출 (만원)</div>
          <div className="metric-value">{(() => {
            const total = filteredFranchises.reduce((sum, fr) => {
              const latest = fr.monthlySales?.[fr.monthlySales.length - 1]?.sales || 0;
              return sum + latest;
            }, 0);
            return (total / 10000).toLocaleString();
          })()}</div>
        </div>
        <div className="card">
          <div className="metric-label">전월 대비 성장률</div>
          <div className="metric-value">{(() => {
            const rates = filteredFranchises.map(fr => {
              const ms = fr.monthlySales;
              if (ms && ms.length >= 2) {
                const latest = ms[ms.length - 1].sales;
                const prev = ms[ms.length - 2].sales;
                return ((latest - prev) / prev) * 100;
              }
              return 0;
            });
            const avg = rates.reduce((a, b) => a + b, 0) / (rates.length || 1);
            return avg.toFixed(1) + '%';
          })()}</div>
        </div>
        <div className="card">
          <div className="metric-label">담당 가맹점 수</div>
          <div className="metric-value">{filteredFranchises.length}개</div>
        </div>
        <div className="card">
          <div className="metric-label">평균 객단가 (만원)</div>
          <div className="metric-value">{(() => {
            const tickets = filteredFranchises.reduce((sum, fr) => {
              const latest = fr.monthlySales?.[fr.monthlySales.length - 1]?.avgTicket || 0;
              return sum + latest;
            }, 0);
            const avg = tickets / (filteredFranchises.length || 1);
            return (avg / 10000).toFixed(2);
          })()}</div>
        </div>
        <div className="card">
          <div className="metric-label">운영 알림</div>
          <div className="metric-value">{visibleAlerts.length}건</div>
          <div className="metric-label">점검 {checkRequiredCount} · 주의 {cautionCount}</div>
        </div>
      </div>
       <header className="header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: '20px' }}>
         <div style={{ fontWeight: 'bold' }}>가맹점 매출 시각화 및 AI 분석 플랫폼</div>
         <div style={{ display: 'flex', alignItems: 'center', gap: '15px', fontSize: '0.9rem' }}>
           <div style={{ position: 'relative' }}>
             <span style={{ cursor: 'pointer' }} onClick={() => setShowAlerts(!showAlerts)}>🔔</span>
             {visibleAlerts.length > 0 && (
               <span style={{ position: 'absolute', top: '-4px', right: '-8px', backgroundColor: '#ef4444', color: 'white', borderRadius: '50%', padding: '2px 5px', fontSize: '0.7rem' }}>{visibleAlerts.length}</span>
             )}
             {showAlerts && (
               <div className="alert-popover">
                 <div className="alert-popover-title">운영 알림</div>
                 {visibleAlerts.length === 0 ? (
                   <div className="alert-empty">현재 필터에서 확인할 알림이 없습니다.</div>
                 ) : (
                   <ul className="alert-list">
                   {visibleAlerts.map((alert) => (
                     <li key={alert.franchiseId} onClick={() => { setSelectedFranchiseId(alert.franchiseId); setShowAlerts(false); }}>
                       <div className="alert-row-header">
                         <span className={`risk-badge ${alert.riskLevel?.toLowerCase()}`}>{RISK_LABELS[alert.riskLevel] || alert.riskLevel}</span>
                         <span className="alert-score">{alert.priorityScore}점</span>
                       </div>
                       <strong>{alert.franchiseName}</strong>
                       <div className="alert-summary">{alert.summary}</div>
                       <div className="alert-meta">
                         {alert.latestMonth} · 전월 대비 {alert.salesGrowthRate > 0 ? '+' : ''}{alert.salesGrowthRate.toFixed(1)}%
                       </div>
                     </li>
                   ))}
                   </ul>
                 )}
               </div>
             )}
           </div>
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
        franchises={filteredFranchises}
        selectedFranchiseId={selectedFranchiseId}
        setSelectedFranchiseId={setSelectedFranchiseId}
        riskLabels={RISK_LABELS}
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
