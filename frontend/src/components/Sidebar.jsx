import React from 'react';

const Sidebar = ({ 
  selectedRegion, 
  setSelectedRegion, 
  selectedIndustry, 
  setSelectedIndustry,
  franchisesCount,
  franchises,
  selectedFranchiseId,
  setSelectedFranchiseId
}) => {
  return (
    <aside className="sidebar">
      <div>
        <h2 className="section-title">필터 및 조건</h2>
        
        <div className="filter-group">
          <label className="filter-label">지역</label>
          <select 
            value={selectedRegion} 
            onChange={(e) => setSelectedRegion(e.target.value)}
          >
            <option value="전체">전체 지역</option>
            <option value="서울 강남구">서울 강남구</option>
            <option value="서울 마포구">서울 마포구</option>
            <option value="서울 영등포구">서울 영등포구</option>
            <option value="서울 성동구">서울 성동구</option>
          </select>
        </div>

        <div className="filter-group" style={{ marginTop: '16px' }}>
          <label className="filter-label">업종</label>
          <select 
            value={selectedIndustry} 
            onChange={(e) => setSelectedIndustry(e.target.value)}
          >
            <option value="전체">전체 업종</option>
            <option value="카페">카페</option>
            <option value="음식점">음식점</option>
          </select>
        </div>
      </div>

      <div className="card" style={{ marginTop: 'auto' }}>
        <div className="metric-label">검색된 가맹점 수</div>
        <div className="metric-value">{franchisesCount}개</div>
      </div>

      {/* 가맹점 리스트 */}
      <div style={{ marginTop: '24px' }}>
        <h3 className="section-title">가맹점 목록</h3>
        <ul style={{ listStyle: 'none', padding: 0, maxHeight: '300px', overflowY: 'auto' }}>
          {franchises.map(fr => (
            <li key={fr.id}
                style={{ padding: '8px 4px', cursor: 'pointer', borderBottom: '1px solid var(--border-color)' }}
                onClick={() => setSelectedFranchiseId(fr.id)}>
              <span style={{ fontWeight: selectedFranchiseId === fr.id ? '600' : '400' }}>{fr.name}</span>
              <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>{fr.region} / {fr.industry}</div>
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
};

export default Sidebar;
