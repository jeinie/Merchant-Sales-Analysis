import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';

const KAKAO_MAP_SDK_ID = 'kakao-map-sdk';
const KAKAO_MAP_API_KEY = import.meta.env.VITE_KAKAO_MAP_API_KEY;

const TREND_COLORS = {
  up: '#16a34a',
  down: '#dc2626',
  flat: '#2563eb',
};

const loadKakaoMaps = () => new Promise((resolve, reject) => {
  const resolveAfterLoad = () => {
    window.kakao.maps.load(() => {
      resolve(window.kakao);
    });
  };

  if (window.kakao?.maps) {
    resolveAfterLoad();
    return;
  }

  if (!KAKAO_MAP_API_KEY) {
    reject(new Error('Kakao Maps API 키가 설정되지 않았습니다.'));
    return;
  }

  const existingScript = document.getElementById(KAKAO_MAP_SDK_ID);

  const handleLoad = () => {
    if (!window.kakao?.maps) {
      reject(new Error('Kakao Maps SDK를 불러오지 못했습니다.'));
      return;
    }

    resolveAfterLoad();
  };

  const handleError = () => {
    reject(new Error('Kakao Maps SDK 요청이 실패했습니다. 네트워크 연결, 브라우저 차단 설정, JavaScript 키와 SDK 도메인 설정을 확인해주세요.'));
  };

  if (existingScript) {
    existingScript.addEventListener('load', handleLoad, { once: true });
    existingScript.addEventListener('error', handleError, { once: true });
    return;
  }

  const script = document.createElement('script');
  script.id = KAKAO_MAP_SDK_ID;
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(KAKAO_MAP_API_KEY)}&libraries=clusterer&autoload=false`;
  script.async = true;
  script.onload = handleLoad;
  script.onerror = handleError;
  document.head.appendChild(script);
});

const toNumber = (value) => {
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
};

const readFranchiseCoordinates = (franchise) => {
  const latitude = toNumber(franchise.latitude);
  const longitude = toNumber(franchise.longitude);

  if (latitude === null || longitude === null) {
    return null;
  }

  return { latitude, longitude };
};

const getLatestSalesStats = (franchise) => {
  const monthlySales = franchise.monthlySales || [];
  const latest = monthlySales[monthlySales.length - 1];
  const previous = monthlySales[monthlySales.length - 2];
  const latestSales = Number(latest?.sales || 0);
  const previousSales = Number(previous?.sales || 0);
  const growthRate = previousSales > 0 ? ((latestSales - previousSales) / previousSales) * 100 : 0;
  const trend = growthRate >= 5 ? 'up' : growthRate <= -5 ? 'down' : 'flat';

  return {
    latestSales,
    previousSales,
    growthRate,
    trend,
    latestMonth: latest?.month || '',
  };
};

const getSalesRange = (franchises) => {
  const values = franchises
    .map((franchise) => getLatestSalesStats(franchise).latestSales)
    .filter((value) => value > 0);

  if (values.length === 0) {
    return { min: 0, max: 0 };
  }

  return {
    min: Math.min(...values),
    max: Math.max(...values),
  };
};

const getMarkerSize = (latestSales, salesRange) => {
  if (!latestSales || salesRange.max <= salesRange.min) {
    return 40;
  }

  const ratio = (latestSales - salesRange.min) / (salesRange.max - salesRange.min);
  return Math.round(34 + ratio * 20);
};

const formatSales = (sales) => `${Math.round(sales / 10000).toLocaleString()}만원`;

const formatGrowthRate = (growthRate) => {
  const sign = growthRate > 0 ? '+' : '';
  return `${sign}${growthRate.toFixed(1)}%`;
};

const escapeHtml = (value) => String(value ?? '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#039;');

const markerImageCache = new Map();

const createMarkerImage = (kakao, { color, size, selected }) => {
  const dimension = selected ? size + 12 : size;
  const center = dimension / 2;
  const radius = selected ? (size / 2) - 3 : (size / 2) - 4;
  const strokeWidth = selected ? 5 : 3;
  const cacheKey = `${color}-${dimension}-${selected}`;

  if (markerImageCache.has(cacheKey)) {
    return markerImageCache.get(cacheKey);
  }

  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${dimension}" height="${dimension}" viewBox="0 0 ${dimension} ${dimension}">
      <defs>
        <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
          <feDropShadow dx="0" dy="3" stdDeviation="2.5" flood-color="#0f172a" flood-opacity="0.28"/>
        </filter>
      </defs>
      <circle cx="${center}" cy="${center}" r="${radius}" fill="${color}" stroke="${selected ? '#f59e0b' : '#ffffff'}" stroke-width="${strokeWidth}" filter="url(#shadow)"/>
      <circle cx="${center}" cy="${center}" r="${Math.max(4, radius * 0.26)}" fill="#ffffff" fill-opacity="0.92"/>
    </svg>`;

  const markerImage = new kakao.maps.MarkerImage(
    `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`,
    new kakao.maps.Size(dimension, dimension),
    { offset: new kakao.maps.Point(center, center) }
  );

  markerImageCache.set(cacheKey, markerImage);
  return markerImage;
};

const createInfoWindowContent = (franchise, stats) => {
  const growthClass = stats.trend === 'down' ? 'down' : stats.trend === 'up' ? 'up' : 'flat';

  return `
    <div class="map-info-window">
      <div class="map-info-title">${escapeHtml(franchise.name)}</div>
      <div class="map-info-meta">${escapeHtml(franchise.region)} · ${escapeHtml(franchise.industry)}</div>
      <div class="map-info-sales">${formatSales(stats.latestSales)}</div>
      <div class="map-info-growth ${growthClass}">전월 대비 ${formatGrowthRate(stats.growthRate)}</div>
      <div class="map-info-address">${escapeHtml(franchise.address)}</div>
    </div>
  `;
};

const getClusterStyles = () => [
  {
    width: '44px',
    height: '44px',
    background: 'rgba(37, 99, 235, 0.88)',
    border: '3px solid #ffffff',
    borderRadius: '50%',
    color: '#ffffff',
    textAlign: 'center',
    fontWeight: '700',
    lineHeight: '38px',
    boxShadow: '0 8px 20px rgba(15, 23, 42, 0.22)',
  },
  {
    width: '54px',
    height: '54px',
    background: 'rgba(22, 163, 74, 0.9)',
    border: '3px solid #ffffff',
    borderRadius: '50%',
    color: '#ffffff',
    textAlign: 'center',
    fontWeight: '700',
    lineHeight: '48px',
    boxShadow: '0 8px 20px rgba(15, 23, 42, 0.24)',
  },
  {
    width: '64px',
    height: '64px',
    background: 'rgba(220, 38, 38, 0.9)',
    border: '3px solid #ffffff',
    borderRadius: '50%',
    color: '#ffffff',
    textAlign: 'center',
    fontWeight: '700',
    lineHeight: '58px',
    boxShadow: '0 8px 20px rgba(15, 23, 42, 0.26)',
  },
];

const MapArea = ({ franchises, onMarkerClick, selectedFranchiseId }) => {
  const mapContainer = useRef(null);
  const mapInstance = useRef(null);
  const markersRef = useRef([]);
  const markerRecordsRef = useRef(new Map());
  const clustererRef = useRef(null);
  const selectedFranchiseIdRef = useRef(selectedFranchiseId);

  const [isMapReady, setIsMapReady] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [markerSummary, setMarkerSummary] = useState({ total: 0, unresolved: 0 });
  const [markersVersion, setMarkersVersion] = useState(0);

  const salesRange = useMemo(() => getSalesRange(franchises), [franchises]);

  const clearMapObjects = useCallback(() => {
    markerRecordsRef.current.forEach(({ infoWindow }) => infoWindow.close());

    if (clustererRef.current) {
      clustererRef.current.clear();
      clustererRef.current = null;
    }

    markersRef.current.forEach((marker) => marker.setMap(null));
    markersRef.current = [];
    markerRecordsRef.current = new Map();
  }, []);

  const applySelectedMarkerStyle = useCallback((selectedId) => {
    if (!isMapReady || !mapInstance.current || !window.kakao?.maps) return;

    const kakao = window.kakao;
    let selectedRecord = null;

    markerRecordsRef.current.forEach((record, franchiseId) => {
      const selected = franchiseId === selectedId;
      record.marker.setImage(createMarkerImage(kakao, {
        color: record.color,
        size: record.baseSize,
        selected,
      }));
      record.marker.setZIndex(selected ? 1000 : record.zIndex);

      if (selected) {
        selectedRecord = record;
      } else {
        record.infoWindow.close();
      }
    });

    if (selectedRecord) {
      selectedRecord.infoWindow.open(mapInstance.current, selectedRecord.marker);
      mapInstance.current.panTo(selectedRecord.position);
    }
  }, [isMapReady]);

  useEffect(() => {
    let cancelled = false;
    setLoadError('');

    loadKakaoMaps()
      .then((kakao) => {
        if (cancelled || !mapContainer.current) return;

        if (!mapInstance.current) {
          mapInstance.current = new kakao.maps.Map(mapContainer.current, {
            center: new kakao.maps.LatLng(37.5665, 126.9780),
            level: 7,
          });
        }

        setIsMapReady(true);
      })
      .catch((err) => {
        if (!cancelled) {
          setLoadError(err.message);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!isMapReady || !mapInstance.current || !window.kakao?.maps) return;

    let cancelled = false;
    const kakao = window.kakao;
    const map = mapInstance.current;

    clearMapObjects();
    setMarkerSummary({ total: franchises.length, unresolved: 0 });

    if (franchises.length === 0) {
      setMarkersVersion((version) => version + 1);
      return;
    }

    const resolved = franchises.map((franchise) => ({
      franchise,
      coordinates: readFranchiseCoordinates(franchise),
    }));

    const bounds = new kakao.maps.LatLngBounds();
    const markerRecords = new Map();
    const markers = [];
    let unresolved = 0;

    resolved.forEach(({ franchise, coordinates }) => {
      if (!coordinates) {
        unresolved += 1;
        return;
      }

      const stats = getLatestSalesStats(franchise);
      const color = TREND_COLORS[stats.trend];
      const baseSize = getMarkerSize(stats.latestSales, salesRange);
      const position = new kakao.maps.LatLng(coordinates.latitude, coordinates.longitude);
      const marker = new kakao.maps.Marker({
        position,
        title: franchise.name,
        image: createMarkerImage(kakao, {
          color,
          size: baseSize,
          selected: false,
        }),
      });
      const infoWindow = new kakao.maps.InfoWindow({
        content: createInfoWindowContent(franchise, stats),
        zIndex: 20,
      });

      kakao.maps.event.addListener(marker, 'click', () => {
        infoWindow.open(map, marker);
        onMarkerClick(franchise.id);
      });
      kakao.maps.event.addListener(marker, 'mouseover', () => {
        infoWindow.open(map, marker);
      });
      kakao.maps.event.addListener(marker, 'mouseout', () => {
        if (selectedFranchiseIdRef.current !== franchise.id) {
          infoWindow.close();
        }
      });

      markerRecords.set(franchise.id, {
        marker,
        infoWindow,
        position,
        color,
        baseSize,
        zIndex: baseSize,
      });
      markers.push(marker);
      bounds.extend(position);
    });

    if (cancelled) return;

    markerRecordsRef.current = markerRecords;
    markersRef.current = markers;

    if (kakao.maps.MarkerClusterer) {
      clustererRef.current = new kakao.maps.MarkerClusterer({
        map,
        markers,
        averageCenter: true,
        minLevel: 6,
        calculator: [5, 10, 20],
        styles: getClusterStyles(),
      });
    } else {
      markers.forEach((marker) => marker.setMap(map));
    }

    if (markers.length > 1) {
      map.setBounds(bounds);
    } else if (markers.length === 1) {
      map.setCenter(markers[0].getPosition());
      map.setLevel(5);
    }

    setMarkerSummary({ total: franchises.length, unresolved });
    setMarkersVersion((version) => version + 1);
    applySelectedMarkerStyle(selectedFranchiseIdRef.current);

    return () => {
      cancelled = true;
    };
  }, [applySelectedMarkerStyle, clearMapObjects, franchises, isMapReady, onMarkerClick, salesRange]);

  useEffect(() => {
    selectedFranchiseIdRef.current = selectedFranchiseId;
    applySelectedMarkerStyle(selectedFranchiseId);
  }, [applySelectedMarkerStyle, markersVersion, selectedFranchiseId]);

  useEffect(() => () => clearMapObjects(), [clearMapObjects]);

  return (
    <div className="map-container">
      <div ref={mapContainer} className="map-element" />

      {(loadError || !isMapReady) && (
        <div className="map-message">
          {loadError ? (
            <>
              지도 로드 실패: {loadError}
              <br />
              Kakao Developers의 JavaScript SDK 도메인 설정을 확인해주세요.
            </>
          ) : (
            '지도 로딩 중...'
          )}
        </div>
      )}

      {isMapReady && !loadError && (
        <>
          <div className="map-legend" aria-label="지도 마커 범례">
            <div className="map-legend-title">매출 마커</div>
            <div className="map-legend-row">
              <span className="map-legend-dot up" />
              <span>상승</span>
            </div>
            <div className="map-legend-row">
              <span className="map-legend-dot flat" />
              <span>보합</span>
            </div>
            <div className="map-legend-row">
              <span className="map-legend-dot down" />
              <span>하락</span>
            </div>
            <div className="map-legend-scale">크기: 최근 월 매출</div>
          </div>

          <div className="map-summary-pill">
            표시 {markerSummary.total - markerSummary.unresolved}/{markerSummary.total}
            {markerSummary.unresolved > 0 && ` · 좌표 미확인 ${markerSummary.unresolved}`}
          </div>
        </>
      )}
    </div>
  );
};

export default MapArea;
