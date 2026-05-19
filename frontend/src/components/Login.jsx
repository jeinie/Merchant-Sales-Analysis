import React, { useState } from 'react';
import { franchises } from '../data/mockData';

const Login = ({ usersData, onLogin }) => {
  const [id, setId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleLogin = (e) => {
    e.preventDefault();

    // Find user in usersData
    const user = usersData.find(u => u.id === id && u.password === password);

    if (user) {
      // Don't store plain password in state/localStorage in real apps!
      const userProfile = { ...user };
      delete userProfile.password;
      onLogin(userProfile);
    } else {
      setError('아이디 또는 비밀번호가 올바르지 않습니다.');
    }
  };

  const getFranchiseDescription = (user) => {
    if (user.role === 'ADMIN') return '전체 가맹점 접근 가능';
    if (!user.assignedFranchiseIds || user.assignedFranchiseIds.length === 0) return '담당 가맹점 없음';
    
    const names = user.assignedFranchiseIds.map(id => {
      const f = franchises.find(item => item.id === id);
      return f ? f.region : '';
    }).filter(Boolean);
    
    return `${names.join(', ')} 담당`;
  };

  return (
    <div style={{
      display: 'flex', justifyContent: 'center', alignItems: 'center',
      height: '100vh', backgroundColor: '#f3f4f6', fontFamily: "'Inter', sans-serif"
    }}>
      <div style={{
        backgroundColor: 'white', padding: '40px', borderRadius: '12px',
        boxShadow: '0 10px 25px rgba(0,0,0,0.05)', width: '100%', maxWidth: '400px'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '30px' }}>
          <h1 style={{ color: '#111827', fontSize: '1.5rem', marginBottom: '10px' }}>
            가맹점 통합 관리 시스템
          </h1>
          <p style={{ color: '#6b7280', fontSize: '0.9rem' }}>
            계정 정보를 입력하여 로그인하세요
          </p>
        </div>

        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.875rem', color: '#374151', fontWeight: '500' }}>
              아이디
            </label>
            <input
              type="text"
              value={id}
              onChange={(e) => setId(e.target.value)}
              placeholder="admin 또는 sales_user"
              style={{
                width: '100%', padding: '10px 12px', border: '1px solid #d1d5db',
                borderRadius: '6px', outline: 'none', fontSize: '1rem',
                boxSizing: 'border-box'
              }}
              required
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.875rem', color: '#374151', fontWeight: '500' }}>
              비밀번호
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="1234"
              style={{
                width: '100%', padding: '10px 12px', border: '1px solid #d1d5db',
                borderRadius: '6px', outline: 'none', fontSize: '1rem',
                boxSizing: 'border-box'
              }}
              required
            />
          </div>

          {error && <div style={{ color: '#ef4444', fontSize: '0.875rem', textAlign: 'center' }}>{error}</div>}

          <button
            type="submit"
            style={{
              backgroundColor: '#3b82f6', color: 'white', padding: '12px',
              border: 'none', borderRadius: '6px', fontSize: '1rem', fontWeight: '600',
              cursor: 'pointer', marginTop: '10px', transition: 'background-color 0.2s'
            }}
            onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#2563eb'}
            onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#3b82f6'}
          >
            로그인
          </button>
        </form>

        <div style={{ marginTop: '30px', backgroundColor: '#f8fafc', padding: '15px', borderRadius: '8px', fontSize: '0.8rem', color: '#64748b', lineHeight: '1.8' }}>
          <strong>테스트 계정 안내 (비밀번호는 모두 1234)</strong><br />
          {usersData.map(u => (
            <div key={u.id} style={{ marginTop: '6px' }}>
              - {u.name}: <code>{u.id}</code> <span style={{ fontSize: '0.75rem', color: '#9ca3af', marginLeft: '4px' }}>({getFranchiseDescription(u)})</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Login;
