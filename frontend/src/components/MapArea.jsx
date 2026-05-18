import React, { useEffect, useRef } from 'react';

const MapArea = ({ franchises, onMarkerClick, selectedFranchiseId }) => {
  const mapContainer = useRef(null);
  const mapInstance = useRef(null);
  const markersRef = useRef([]);

  useEffect(() => {
    // Check if kakao API is loaded and services library is available
    if (!window.kakao || !window.kakao.maps || !window.kakao.maps.services) {
      console.warn("Kakao Maps API or services library not loaded.");
      return;
    }

    // Initialize Map if not already initialized
    if (!mapInstance.current) {
      window.kakao.maps.load(() => {
        const options = {
          center: new window.kakao.maps.LatLng(37.5665, 126.9780), // Default center (Seoul City Hall)
          level: 7
        };
        mapInstance.current = new window.kakao.maps.Map(mapContainer.current, options);
      });
    }

    // Wait for map to be initialized
    if (!mapInstance.current) return;

    // Clear existing markers
    markersRef.current.forEach(marker => marker.setMap(null));
    markersRef.current = [];

    const bounds = new window.kakao.maps.LatLngBounds();
    const geocoder = new window.kakao.maps.services.Geocoder();
    let validMarkersCount = 0;

    // Geocode each franchise address and create a marker
    franchises.forEach(franchise => {
      if (!franchise.address) return;

      geocoder.addressSearch(franchise.address, (result, status) => {
        if (status === window.kakao.maps.services.Status.OK) {
          const coords = new window.kakao.maps.LatLng(result[0].y, result[0].x);
          
          const marker = new window.kakao.maps.Marker({
            map: mapInstance.current,
            position: coords,
            title: franchise.name
          });

          // Add click event
          window.kakao.maps.event.addListener(marker, 'click', () => {
            onMarkerClick(franchise.id);
          });

          markersRef.current.push(marker);
          bounds.extend(coords);
          validMarkersCount++;

          // If all valid markers are processed, fit bounds
          // Note: addressSearch is async, so we extend bounds as callbacks complete.
          // In a production app, Promise.all would be better for bulk geocoding.
          mapInstance.current.setBounds(bounds);
        }
      });
    });

  }, [franchises, onMarkerClick]);

  return (
    <div className="map-container">
      <div ref={mapContainer} className="map-element">
        {(!window.kakao || !window.kakao.maps) && (
          <div style={{ padding: '2rem', textAlign: 'center', color: '#666' }}>
            지도 영역 (Kakao Maps API 키가 필요합니다)
            <br />
            임시로 마커 클릭을 시뮬레이션 하려면 필터에서 검색된 가맹점 수를 확인하세요.
          </div>
        )}
      </div>
    </div>
  );
};

export default MapArea;
