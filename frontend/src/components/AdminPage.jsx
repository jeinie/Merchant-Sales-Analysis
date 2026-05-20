import React from 'react';
import { api } from '../utils/api';

const AdminPage = ({ usersData, franchises, onRefresh, onClose }) => {

  const handleFranchiseChange = async (franchiseId, e) => {
    const newUserId = e.target.value;
    try {
      await api.assignManager(franchiseId, newUserId);
      if (onRefresh) onRefresh();
    } catch (err) {
      alert(err.message || '담당자 할당에 실패했습니다.');
    }
  };

  const handlePermissionToggle = async (userId, permissionKey) => {
    if (permissionKey !== 'canUseAI') return;
    const user = usersData.find(u => u.id === userId);
    if (!user) return;

    const currentVal = !!user.permissions?.canUseAI;
    try {
      await api.toggleAi(userId, !currentVal);
      if (onRefresh) onRefresh();
    } catch (err) {
      alert(err.message || 'AI 권한 변경에 실패했습니다.');
    }
  };

  const salesUsers = usersData.filter(u => u.role === 'SALES');

  // Helper to find who currently manages a franchise
  const getManagerForFranchise = (franchiseId) => {
    const manager = salesUsers.find(u => u.assignedFranchiseIds?.includes(franchiseId));
    return manager ? manager.id : '';
  };

  return (
    <div style={{ padding: '20px', backgroundColor: '#f9fafb', minHeight: '100vh', fontFamily: "'Inter', sans-serif" }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 style={{ color: '#111827', margin: 0 }}>⚙️ 시스템 관리자 페이지</h2>
        <button 
          onClick={onClose}
          style={{ 
            padding: '8px 16px', backgroundColor: '#e5e7eb', color: '#374151',
            border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold'
          }}
        >
          ← 대시보드로 돌아가기
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
        
        {/* Franchise Assignment Panel */}
        <div style={{ backgroundColor: 'white', padding: '20px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <h3 style={{ marginTop: 0, marginBottom: '15px', color: '#1f2937', borderBottom: '2px solid #f3f4f6', paddingBottom: '10px' }}>
            가맹점 별 담당자 배정
          </h3>
          <p style={{ fontSize: '0.85rem', color: '#6b7280', marginBottom: '15px' }}>
            각 가맹점을 관리할 영업사원을 지정하세요. 변경 사항은 즉시 적용됩니다.
          </p>
          
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ backgroundColor: '#f9fafb', color: '#4b5563', fontSize: '0.9rem' }}>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>가맹점명</th>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>지역/업종</th>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>담당 영업사원</th>
              </tr>
            </thead>
            <tbody>
              {franchises.map(franchise => (
                <tr key={franchise.id}>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6', fontWeight: '500' }}>
                    {franchise.name}
                  </td>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6', color: '#6b7280', fontSize: '0.85rem' }}>
                    {franchise.region} / {franchise.industry}
                  </td>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6' }}>
                    <select 
                      value={getManagerForFranchise(franchise.id)}
                      onChange={(e) => handleFranchiseChange(franchise.id, e)}
                      style={{ 
                        padding: '6px', borderRadius: '4px', border: '1px solid #d1d5db',
                        width: '100%', outline: 'none'
                      }}
                    >
                      <option value="">-- 배정 안 됨 --</option>
                      {salesUsers.map(user => (
                        <option key={user.id} value={user.id}>{user.name}</option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* User Permissions Panel */}
        <div style={{ backgroundColor: 'white', padding: '20px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <h3 style={{ marginTop: 0, marginBottom: '15px', color: '#1f2937', borderBottom: '2px solid #f3f4f6', paddingBottom: '10px' }}>
            영업사원 별 메뉴 권한 설정
          </h3>
          <p style={{ fontSize: '0.85rem', color: '#6b7280', marginBottom: '15px' }}>
            특정 사원에게 AI 분석 기능 사용 권한을 부여하거나 회수할 수 있습니다.
          </p>

          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ backgroundColor: '#f9fafb', color: '#4b5563', fontSize: '0.9rem' }}>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>영업사원</th>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb', textAlign: 'center' }}>AI 분석 권한 (Gemini)</th>
              </tr>
            </thead>
            <tbody>
              {salesUsers.map(user => (
                <tr key={user.id}>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6', fontWeight: '500' }}>
                    {user.name} ({user.id})
                  </td>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6', textAlign: 'center' }}>
                    <button
                      onClick={() => handlePermissionToggle(user.id, 'canUseAI')}
                      style={{
                        padding: '6px 12px',
                        backgroundColor: user.permissions?.canUseAI ? '#10b981' : '#ef4444',
                        color: 'white', border: 'none', borderRadius: '20px',
                        cursor: 'pointer', fontWeight: 'bold', fontSize: '0.8rem',
                        width: '80px'
                      }}
                    >
                      {user.permissions?.canUseAI ? 'ON' : 'OFF'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

      </div>
    </div>
  );
};

export default AdminPage;
