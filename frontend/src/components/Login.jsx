import React, { useState } from 'react';
import { api } from '../utils/api';

const Login = ({ usersData, onLogin }) => {
  const [id, setId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const userProfile = await api.login(id, password);
      onLogin(userProfile);
    } catch (err) {
      setError(err.message || '아이디 또는 비밀번호가 올바르지 않습니다.');
    }
  };

  const getMerchantDescription = (user) => {
    if (user.role === 'ADMIN') return '전체 가맹점 접근 가능';
    if (!user.assignedMerchantIds || user.assignedMerchantIds.length === 0) return '담당 가맹점 없음';

    return `${user.assignedMerchantIds.length}개 가맹점 담당`;
  };
  const displayUsers = usersData || [];

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
            가맹점 매출 분석 플랫폼
          </h1>
          <p style={{ color: '#6b7280', fontSize: '0.9rem' }}>
            매장별 매출 현황과 AI 인사이트를 확인하세요
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
          {displayUsers.length > 0 ? (
            displayUsers.map(u => (
              <div key={u.id} style={{ marginTop: '6px' }}>
                - {u.name}: <code>{u.id}</code> <span style={{ fontSize: '0.75rem', color: '#9ca3af', marginLeft: '4px' }}>({getMerchantDescription(u)})</span>
              </div>
            ))
          ) : (
            <div style={{ marginTop: '6px' }}>계정 정보를 불러오는 중입니다.</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Login;
