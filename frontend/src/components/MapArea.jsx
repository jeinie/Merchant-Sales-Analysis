import React, { useEffect, useRef, useState } from 'react';

const KAKAO_MAP_SDK_ID = 'kakao-map-sdk';
const KAKAO_MAP_API_KEY = import.meta.env.VITE_KAKAO_MAP_API_KEY;

const loadKakaoMaps = () => new Promise((resolve, reject) => {
  if (window.kakao?.maps?.services) {
    window.kakao.maps.load(() => resolve(window.kakao));
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

    window.kakao.maps.load(() => resolve(window.kakao));
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
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(KAKAO_MAP_API_KEY)}&libraries=services&autoload=false`;
  script.async = true;
  script.onload = handleLoad;
  script.onerror = handleError;
  document.head.appendChild(script);
});

const MapArea = ({ franchises, onMarkerClick }) => {
  const mapContainer = useRef(null);
  const mapInstance = useRef(null);
  const markersRef = useRef([]);
  const [isMapReady, setIsMapReady] = useState(false);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    let cancelled = false;

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
    if (!isMapReady || !mapInstance.current || !window.kakao?.maps?.services) return;

    markersRef.current.forEach((marker) => marker.setMap(null));
    markersRef.current = [];

    const bounds = new window.kakao.maps.LatLngBounds();
    const geocoder = new window.kakao.maps.services.Geocoder();

    franchises.forEach((franchise) => {
      if (!franchise.address) return;

      geocoder.addressSearch(franchise.address, (result, status) => {
        if (status !== window.kakao.maps.services.Status.OK || !mapInstance.current) return;

        const coords = new window.kakao.maps.LatLng(result[0].y, result[0].x);
        const marker = new window.kakao.maps.Marker({
          map: mapInstance.current,
          position: coords,
          title: franchise.name,
        });

        window.kakao.maps.event.addListener(marker, 'click', () => {
          onMarkerClick(franchise.id);
        });

        markersRef.current.push(marker);
        bounds.extend(coords);
        mapInstance.current.setBounds(bounds);
      });
    });
  }, [franchises, isMapReady, onMarkerClick]);

  return (
    <div className="map-container">
      <div ref={mapContainer} className="map-element">
        {(loadError || !isMapReady) && (
          <div style={{ padding: '2rem', textAlign: 'center', color: '#666' }}>
            {loadError ? (
              <>
                지도 로드 실패: {loadError}
                <br />
                Kakao Developers의 JavaScript SDK 도메인에 현재 주소를 등록했는지 확인해주세요.
              </>
            ) : (
              '지도 로딩 중...'
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default MapArea;
