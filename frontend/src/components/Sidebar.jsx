import React from 'react';

const Sidebar = ({ 
  selectedRegion, 
  setSelectedRegion, 
  selectedIndustry, 
  setSelectedIndustry,
  franchisesCount 
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
            <option value="강남구">강남구</option>
            <option value="마포구">마포구</option>
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
    </aside>
  );
};

export default Sidebar;
