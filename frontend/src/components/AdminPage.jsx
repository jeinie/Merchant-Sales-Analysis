import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Ban, CheckCircle2, MapPin, Plus, Save, Search, X } from 'lucide-react';
import { api } from '../utils/api';

const KAKAO_MAP_SDK_ID = 'kakao-map-sdk';
const KAKAO_MAP_API_KEY = import.meta.env.VITE_KAKAO_MAP_API_KEY;

const emptyForm = {
  name: '',
  industry: '',
  region: '',
  address: '',
  managerId: '',
};

const defaultIndustryOptions = ['카페', '음식점'];
const defaultRegionOptions = ['서울 강남구', '서울 마포구', '서울 영등포구', '서울 성동구'];

const loadKakaoMaps = () => new Promise((resolve, reject) => {
  const resolveAfterLoad = () => {
    window.kakao.maps.load(() => {
      if (!window.kakao?.maps?.services) {
        reject(new Error('Kakao Maps 주소 검색 라이브러리를 불러오지 못했습니다.'));
        return;
      }
      resolve(window.kakao);
    });
  };

  if (window.kakao?.maps?.services) {
    resolveAfterLoad();
    return;
  }

  if (!KAKAO_MAP_API_KEY) {
    reject(new Error('Kakao Maps API 키가 설정되지 않았습니다.'));
    return;
  }

  const existingScript = document.getElementById(KAKAO_MAP_SDK_ID);
  if (existingScript) {
    existingScript.addEventListener('load', resolveAfterLoad, { once: true });
    existingScript.addEventListener('error', () => reject(new Error('Kakao Maps SDK 요청이 실패했습니다.')), { once: true });
    return;
  }

  const script = document.createElement('script');
  script.id = KAKAO_MAP_SDK_ID;
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(KAKAO_MAP_API_KEY)}&libraries=services,clusterer&autoload=false`;
  script.async = true;
  script.onload = resolveAfterLoad;
  script.onerror = () => reject(new Error('Kakao Maps SDK 요청이 실패했습니다.'));
  document.head.appendChild(script);
});

const statusLabel = {
  VERIFIED: '확인 완료',
  GEOCODED: '자동 좌표',
  UNVERIFIED: '좌표 미확인',
  FAILED: '주소 확인 실패',
  MANUAL: '수동 보정',
};

const panelStyle = {
  backgroundColor: 'white',
  padding: '20px',
  borderRadius: '8px',
  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
};

const AdminPage = ({ usersData, franchises, onRefresh, onClose }) => {
  const [activeTab, setActiveTab] = useState('franchises');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [coordinates, setCoordinates] = useState(null);
  const [locationStatus, setLocationStatus] = useState('UNVERIFIED');
  const [formError, setFormError] = useState('');
  const [isGeocoding, setIsGeocoding] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [closingFranchiseId, setClosingFranchiseId] = useState('');
  const [placeCandidates, setPlaceCandidates] = useState([]);
  const [locationLookupSource, setLocationLookupSource] = useState('');
  const previewMapRef = useRef(null);
  const mapContainerRef = useRef(null);
  const markerRef = useRef(null);

  const salesUsers = usersData.filter(u => u.role === 'SALES');
  const unresolvedCount = franchises.filter(franchise => !franchise.latitude || !franchise.longitude).length;
  const hasCoordinates = (franchise) => franchise.latitude != null && franchise.longitude != null;
  const getLocationStatusLabel = (franchise) => {
    if (hasCoordinates(franchise) && franchise.locationStatus === 'UNVERIFIED') {
      return statusLabel.GEOCODED;
    }
    return statusLabel[franchise.locationStatus] || (hasCoordinates(franchise) ? statusLabel.GEOCODED : statusLabel.UNVERIFIED);
  };
  const industryOptions = useMemo(() => {
    const values = new Set(defaultIndustryOptions);
    franchises.forEach(franchise => {
      if (franchise.industry) values.add(franchise.industry);
    });
    return Array.from(values).sort((left, right) => left.localeCompare(right, 'ko'));
  }, [franchises]);
  const regionOptions = useMemo(() => {
    const values = new Set(defaultRegionOptions);
    franchises.forEach(franchise => {
      if (franchise.region) values.add(franchise.region);
    });
    return Array.from(values).sort((left, right) => left.localeCompare(right, 'ko'));
  }, [franchises]);

  const managerByFranchiseId = useMemo(() => {
    const result = new Map();
    salesUsers.forEach(user => {
      user.assignedFranchiseIds?.forEach(franchiseId => {
        result.set(franchiseId, user.id);
      });
    });
    return result;
  }, [salesUsers]);

  useEffect(() => {
    if (!isCreateOpen || !coordinates || !mapContainerRef.current || !window.kakao?.maps) return;

    const kakao = window.kakao;
    const position = new kakao.maps.LatLng(coordinates.latitude, coordinates.longitude);

    if (!previewMapRef.current) {
      previewMapRef.current = new kakao.maps.Map(mapContainerRef.current, {
        center: position,
        level: 4,
      });
    } else {
      previewMapRef.current.setCenter(position);
    }

    if (markerRef.current) {
      markerRef.current.setMap(null);
    }
    markerRef.current = new kakao.maps.Marker({ position });
    markerRef.current.setMap(previewMapRef.current);

    const syncPreviewMapLayout = () => {
      if (!previewMapRef.current) return;
      previewMapRef.current.relayout();
      previewMapRef.current.setCenter(position);
    };

    requestAnimationFrame(syncPreviewMapLayout);
    const timerId = window.setTimeout(syncPreviewMapLayout, 120);
    return () => window.clearTimeout(timerId);
  }, [coordinates, isCreateOpen]);

  const closeCreateModal = () => {
    setIsCreateOpen(false);
    setForm(emptyForm);
    setCoordinates(null);
    setLocationStatus('UNVERIFIED');
    setPlaceCandidates([]);
    setLocationLookupSource('');
    setFormError('');
    previewMapRef.current = null;
    markerRef.current = null;
  };

  const updateForm = (key, value) => {
    setForm(prev => ({ ...prev, [key]: value }));
    if (key === 'address' || key === 'name') {
      setCoordinates(null);
      setLocationStatus('UNVERIFIED');
      setPlaceCandidates([]);
      setLocationLookupSource('');
    }
  };

  const applyLocationCandidate = (candidate, source = 'KAKAO_PLACE_KEYWORD') => {
    const resolvedAddress = candidate.road_address_name || candidate.address_name || form.address;
    setForm(prev => ({
      ...prev,
      address: resolvedAddress || prev.address,
      name: prev.name || candidate.place_name || '',
    }));
    setCoordinates({
      latitude: Number(candidate.y),
      longitude: Number(candidate.x),
    });
    setLocationStatus('VERIFIED');
    setLocationLookupSource(source);
    setFormError('');
  };

  const searchPlaceByKeyword = (kakao, keyword) => new Promise((resolve) => {
    const places = new kakao.maps.services.Places();
    places.keywordSearch(keyword, (result, status) => {
      if (status !== kakao.maps.services.Status.OK || !result?.length) {
        resolve([]);
        return;
      }
      resolve(result.slice(0, 5));
    });
  });

  const searchAddress = (kakao, address) => new Promise((resolve) => {
    const geocoder = new kakao.maps.services.Geocoder();
    geocoder.addressSearch(address, (result, status) => {
      if (status !== kakao.maps.services.Status.OK || !result?.[0]) {
        resolve(null);
        return;
      }
      resolve(result[0]);
    });
  });

  const handleGeocode = async () => {
    const address = form.address.trim();
    const keyword = [form.name.trim(), address].filter(Boolean).join(' ');

    if (!address && !form.name.trim()) {
      setFormError('가맹점명 또는 주소를 먼저 입력해주세요.');
      return;
    }

    setIsGeocoding(true);
    setFormError('');
    setPlaceCandidates([]);
    try {
      const kakao = await loadKakaoMaps();

      if (address) {
        const addressResult = await searchAddress(kakao, address);
        if (addressResult) {
          setCoordinates({
            latitude: Number(addressResult.y),
            longitude: Number(addressResult.x),
          });
          setLocationLookupSource('KAKAO_ADDRESS_GEOCODER');
          setLocationStatus('VERIFIED');
          setIsGeocoding(false);
          return;
        }
      }

      const places = await searchPlaceByKeyword(kakao, keyword || form.name.trim());
      setIsGeocoding(false);
      if (!places.length) {
        setCoordinates(null);
        setLocationStatus('FAILED');
        setFormError('가맹점명 또는 주소로 위치를 찾지 못했습니다. 검색어를 더 구체적으로 입력해주세요.');
        return;
      }

      setPlaceCandidates(places);
      applyLocationCandidate(places[0]);
      if (places.length > 1) {
        setFormError('검색 결과가 여러 개입니다. 아래 목록에서 실제 가맹점을 선택해주세요.');
      }
    } catch (err) {
      setIsGeocoding(false);
      setFormError(err.message);
    }
  };

  const handleSelectPlaceCandidate = (candidate) => {
    applyLocationCandidate(candidate);
  };

  const handleCreateFranchise = async (event) => {
    event.preventDefault();
    setFormError('');

    if (!form.name.trim() || !form.industry.trim() || !form.region.trim() || !form.address.trim()) {
      setFormError('가맹점명, 업종, 지역, 주소는 필수입니다.');
      return;
    }

    setIsSaving(true);
    try {
      await api.createFranchise({
        ...form,
        latitude: coordinates?.latitude ?? null,
        longitude: coordinates?.longitude ?? null,
        locationStatus: coordinates ? locationStatus : 'UNVERIFIED',
        geocodeSource: coordinates ? locationLookupSource || 'KAKAO_FRONTEND_LOOKUP' : '',
        locationNote: coordinates ? '관리자가 등록 화면에서 카카오 위치 검색으로 좌표를 확인했습니다.' : '등록 시 좌표를 확인하지 않았습니다.',
      });
      if (onRefresh) await onRefresh();
      closeCreateModal();
    } catch (err) {
      setFormError(err.message || '가맹점 등록에 실패했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleCloseFranchise = async (franchise) => {
    const confirmed = window.confirm(`${franchise.name} 가맹점을 폐점 처리할까요?\n목록과 지도에서는 숨겨지지만 매출 데이터와 AI 분석 이력은 보존됩니다.`);
    if (!confirmed) return;

    setClosingFranchiseId(franchise.id);
    try {
      await api.closeFranchise(franchise.id, '관리자 화면에서 폐점 처리했습니다.');
      if (onRefresh) await onRefresh();
    } catch (err) {
      alert(err.message || '가맹점 폐점 처리에 실패했습니다.');
    } finally {
      setClosingFranchiseId('');
    }
  };

  const handleFranchiseChange = async (franchiseId, e) => {
    const newUserId = e.target.value;
    try {
      await api.assignManager(franchiseId, newUserId);
      if (onRefresh) onRefresh();
    } catch (err) {
      alert(err.message || '담당자 할당에 실패했습니다.');
    }
  };

  const handlePermissionToggle = async (userId) => {
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

  return (
    <div style={{ height: '100vh', overflowY: 'auto', overflowX: 'hidden', padding: '20px 20px 48px', backgroundColor: '#f9fafb', fontFamily: "'Inter', sans-serif" }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', gap: '16px' }}>
        <div>
          <div style={{ color: '#64748b', fontSize: '0.8rem', fontWeight: 800 }}>관리자 설정</div>
          <h2 style={{ color: '#111827', margin: '4px 0 0' }}>가맹점 운영 관리</h2>
        </div>
        <button
          onClick={onClose}
          style={{
            padding: '8px 16px',
            backgroundColor: '#e5e7eb',
            color: '#374151',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontWeight: 'bold',
          }}
        >
          대시보드로 돌아가기
        </button>
      </div>

      <div style={{ display: 'flex', gap: '8px', marginBottom: '16px', flexWrap: 'wrap' }}>
        {[
          ['franchises', '가맹점 관리'],
          ['assignments', '담당자 배정'],
          ['permissions', '권한 관리'],
        ].map(([key, label]) => (
          <button
            key={key}
            type="button"
            onClick={() => setActiveTab(key)}
            style={{
              padding: '9px 14px',
              border: '1px solid #dbe3ef',
              borderRadius: '999px',
              backgroundColor: activeTab === key ? '#2563eb' : '#ffffff',
              color: activeTab === key ? '#ffffff' : '#475569',
              cursor: 'pointer',
              fontWeight: 800,
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {activeTab === 'franchises' && (
        <div style={panelStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', alignItems: 'center', marginBottom: '16px' }}>
            <div>
              <h3 style={{ margin: 0, color: '#1f2937' }}>가맹점 관리</h3>
              <p style={{ margin: '6px 0 0', color: '#64748b', fontSize: '0.86rem' }}>
                신규 가맹점을 등록하고 지도 표시용 좌표 확인 상태를 관리합니다. 좌표 미확인 {unresolvedCount}개
              </p>
            </div>
            <button
              type="button"
              onClick={() => setIsCreateOpen(true)}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '9px 12px',
                border: 'none',
                borderRadius: '8px',
                backgroundColor: '#2563eb',
                color: '#ffffff',
                cursor: 'pointer',
                fontWeight: 800,
              }}
            >
              <Plus size={16} />
              신규 등록
            </button>
          </div>

          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ backgroundColor: '#f9fafb', color: '#4b5563', fontSize: '0.9rem' }}>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>가맹점</th>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>지역/업종</th>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>위치 상태</th>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>담당자</th>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb', textAlign: 'right' }}>관리</th>
              </tr>
            </thead>
            <tbody>
              {franchises.map(franchise => (
                <tr key={franchise.id}>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6', fontWeight: 700 }}>
                    {franchise.name}
                    <div style={{ marginTop: '4px', color: '#64748b', fontSize: '0.78rem', fontWeight: 500 }}>{franchise.address}</div>
                  </td>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6', color: '#6b7280', fontSize: '0.85rem' }}>
                    {franchise.region} / {franchise.industry}
                  </td>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6' }}>
                    <span style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '5px',
                      padding: '4px 8px',
                      borderRadius: '999px',
                      backgroundColor: hasCoordinates(franchise) ? '#ecfdf5' : '#fff7ed',
                      color: hasCoordinates(franchise) ? '#047857' : '#c2410c',
                      fontSize: '0.75rem',
                      fontWeight: 800,
                    }}>
                      <MapPin size={13} />
                      {getLocationStatusLabel(franchise)}
                    </span>
                  </td>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6', color: '#475569', fontSize: '0.85rem' }}>
                    {salesUsers.find(user => user.id === managerByFranchiseId.get(franchise.id))?.name || '미배정'}
                  </td>
                  <td style={{ padding: '12px 10px', borderBottom: '1px solid #f3f4f6', textAlign: 'right' }}>
                    <button
                      type="button"
                      onClick={() => handleCloseFranchise(franchise)}
                      disabled={closingFranchiseId === franchise.id}
                      title="폐점 처리"
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        width: '34px',
                        height: '34px',
                        border: '1px solid #fecaca',
                        borderRadius: '8px',
                        backgroundColor: closingFranchiseId === franchise.id ? '#fee2e2' : '#fff1f2',
                        color: '#dc2626',
                        cursor: closingFranchiseId === franchise.id ? 'wait' : 'pointer',
                      }}
                    >
                      <Ban size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'assignments' && (
        <div style={panelStyle}>
          <h3 style={{ marginTop: 0, marginBottom: '15px', color: '#1f2937', borderBottom: '2px solid #f3f4f6', paddingBottom: '10px' }}>
            가맹점별 담당자 배정
          </h3>
          <p style={{ fontSize: '0.85rem', color: '#6b7280', marginBottom: '15px' }}>
            각 가맹점을 관리할 영업사원을 지정합니다. 변경 사항은 즉시 적용됩니다.
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
                      value={managerByFranchiseId.get(franchise.id) || ''}
                      onChange={(e) => handleFranchiseChange(franchise.id, e)}
                      style={{
                        padding: '6px',
                        borderRadius: '4px',
                        border: '1px solid #d1d5db',
                        width: '100%',
                        outline: 'none',
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
      )}

      {activeTab === 'permissions' && (
        <div style={panelStyle}>
          <h3 style={{ marginTop: 0, marginBottom: '15px', color: '#1f2937', borderBottom: '2px solid #f3f4f6', paddingBottom: '10px' }}>
            영업사원별 권한 설정
          </h3>
          <p style={{ fontSize: '0.85rem', color: '#6b7280', marginBottom: '15px' }}>
            특정 사원에게 AI 분석 기능 사용 권한을 부여하거나 회수할 수 있습니다.
          </p>

          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ backgroundColor: '#f9fafb', color: '#4b5563', fontSize: '0.9rem' }}>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>영업사원</th>
                <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb', textAlign: 'center' }}>AI 분석 권한</th>
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
                      onClick={() => handlePermissionToggle(user.id)}
                      style={{
                        padding: '6px 12px',
                        backgroundColor: user.permissions?.canUseAI ? '#10b981' : '#ef4444',
                        color: 'white',
                        border: 'none',
                        borderRadius: '20px',
                        cursor: 'pointer',
                        fontWeight: 'bold',
                        fontSize: '0.8rem',
                        width: '80px',
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
      )}

      {isCreateOpen && (
        <div
          onClick={closeCreateModal}
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 60,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '24px',
            background: 'rgba(15, 23, 42, 0.48)',
          }}
        >
          <form
            onSubmit={handleCreateFranchise}
            onClick={(event) => event.stopPropagation()}
            style={{
              width: 'min(920px, 100%)',
              maxHeight: 'calc(100vh - 48px)',
              overflowY: 'auto',
              background: '#ffffff',
              borderRadius: '8px',
              border: '1px solid #e2e8f0',
              boxShadow: '0 20px 40px rgba(15, 23, 42, 0.24)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '16px', padding: '18px 20px', borderBottom: '1px solid #e2e8f0' }}>
              <div>
                <div style={{ color: '#64748b', fontSize: '0.78rem', fontWeight: 800 }}>신규 가맹점</div>
                <h3 style={{ margin: '4px 0 0', color: '#0f172a' }}>가맹점 등록</h3>
              </div>
              <button type="button" onClick={closeCreateModal} aria-label="등록 닫기" className="icon-button">
                <X size={18} />
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) minmax(300px, 0.9fr)', gap: '20px', padding: '20px' }}>
              <div style={{ display: 'grid', gap: '14px' }}>
                <label className="filter-group">
                  <span className="filter-label">가맹점명</span>
                  <input value={form.name} onChange={(event) => updateForm('name', event.target.value)} placeholder="예: 강남 신규점" />
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  <label className="filter-group">
                    <span className="filter-label">업종</span>
                    <select value={form.industry} onChange={(event) => updateForm('industry', event.target.value)}>
                      <option value="">업종 선택</option>
                      {industryOptions.map(industry => (
                        <option key={industry} value={industry}>{industry}</option>
                      ))}
                    </select>
                  </label>
                  <label className="filter-group">
                    <span className="filter-label">지역</span>
                    <select value={form.region} onChange={(event) => updateForm('region', event.target.value)}>
                      <option value="">지역 선택</option>
                      {regionOptions.map(region => (
                        <option key={region} value={region}>{region}</option>
                      ))}
                    </select>
                  </label>
                </div>
                <label className="filter-group">
                  <span className="filter-label">주소 또는 가맹점명 검색</span>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <input
                      value={form.address}
                      onChange={(event) => updateForm('address', event.target.value)}
                      placeholder="예: 서울 강남구 테헤란로 123 또는 스타벅스 강남역점"
                      style={{ flex: 1 }}
                    />
                    <button
                      type="button"
                      onClick={handleGeocode}
                      disabled={isGeocoding}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px',
                        border: '1px solid #bfdbfe',
                        borderRadius: '8px',
                        background: '#eff6ff',
                        color: '#2563eb',
                        cursor: isGeocoding ? 'wait' : 'pointer',
                        fontWeight: 800,
                        padding: '0 12px',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      <Search size={16} />
                      {isGeocoding ? '검색 중' : '위치 검색'}
                    </button>
                  </div>
                </label>
                {placeCandidates.length > 0 && (
                  <div style={{ border: '1px solid #e2e8f0', borderRadius: '8px', overflow: 'hidden', background: '#ffffff' }}>
                    {placeCandidates.map((candidate) => {
                      const candidateAddress = candidate.road_address_name || candidate.address_name || '주소 정보 없음';
                      const isSelected = coordinates
                        && Number(candidate.y) === coordinates.latitude
                        && Number(candidate.x) === coordinates.longitude;
                      return (
                        <button
                          key={candidate.id || `${candidate.place_name}-${candidate.x}-${candidate.y}`}
                          type="button"
                          onClick={() => handleSelectPlaceCandidate(candidate)}
                          style={{
                            width: '100%',
                            border: 'none',
                            borderBottom: '1px solid #f1f5f9',
                            background: isSelected ? '#eff6ff' : '#ffffff',
                            color: '#0f172a',
                            cursor: 'pointer',
                            padding: '10px 12px',
                            textAlign: 'left',
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
                            <strong style={{ fontSize: '0.88rem' }}>{candidate.place_name}</strong>
                            {isSelected && <span style={{ color: '#2563eb', fontSize: '0.72rem', fontWeight: 900 }}>선택됨</span>}
                          </div>
                          <div style={{ marginTop: '4px', color: '#64748b', fontSize: '0.78rem', lineHeight: 1.4 }}>
                            {candidateAddress}
                          </div>
                        </button>
                      );
                    })}
                  </div>
                )}
                <label className="filter-group">
                  <span className="filter-label">담당 영업사원</span>
                  <select value={form.managerId} onChange={(event) => updateForm('managerId', event.target.value)}>
                    <option value="">미배정</option>
                    {salesUsers.map(user => (
                      <option key={user.id} value={user.id}>{user.name}</option>
                    ))}
                  </select>
                </label>
                {formError && (
                  <div style={{ padding: '10px 12px', borderRadius: '8px', background: '#fef2f2', color: '#b91c1c', fontSize: '0.85rem', fontWeight: 700 }}>
                    {formError}
                  </div>
                )}
              </div>

              <div style={{ display: 'grid', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: coordinates ? '#047857' : '#c2410c', fontSize: '0.86rem', fontWeight: 800 }}>
                  {coordinates ? <CheckCircle2 size={16} /> : <MapPin size={16} />}
                  {coordinates ? '주소 좌표 확인 완료' : '주소 확인 전'}
                </div>
                <div style={{ position: 'relative' }}>
                  <div
                    ref={mapContainerRef}
                    style={{
                      width: '100%',
                      height: '300px',
                      minHeight: '260px',
                      border: '1px solid #e2e8f0',
                      borderRadius: '8px',
                      background: '#f8fafc',
                      overflow: 'hidden',
                    }}
                  />
                  {!coordinates && (
                    <div
                      style={{
                        position: 'absolute',
                        inset: 0,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#64748b',
                        fontSize: '0.86rem',
                        textAlign: 'center',
                        pointerEvents: 'none',
                      }}
                    >
                      주소 확인 후 지도 미리보기가 표시됩니다.
                    </div>
                  )}
                </div>
                {coordinates && (
                  <div style={{ color: '#64748b', fontSize: '0.78rem', lineHeight: 1.5 }}>
                    위도 {coordinates.latitude.toFixed(7)} / 경도 {coordinates.longitude.toFixed(7)}
                  </div>
                )}
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', padding: '16px 20px', borderTop: '1px solid #e2e8f0' }}>
              <button type="button" onClick={closeCreateModal} style={{ padding: '9px 14px', border: '1px solid #cbd5e1', borderRadius: '8px', background: '#ffffff', cursor: 'pointer', fontWeight: 800 }}>
                취소
              </button>
              <button
                type="submit"
                disabled={isSaving}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '9px 14px',
                  border: 'none',
                  borderRadius: '8px',
                  background: '#2563eb',
                  color: '#ffffff',
                  cursor: isSaving ? 'wait' : 'pointer',
                  fontWeight: 800,
                }}
              >
                <Save size={16} />
                {isSaving ? '저장 중' : '등록'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

export default AdminPage;
