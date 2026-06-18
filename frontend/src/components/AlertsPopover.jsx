import React, { useMemo, useState } from 'react';
import { AlertTriangle, Bell, CheckCircle2, ChevronRight, X } from 'lucide-react';

const FILTERS = [
  { key: 'ALL', label: '전체' },
  { key: 'CHECK_REQUIRED', label: '점검 필요' },
  { key: 'CAUTION', label: '주의' },
];

const AlertsPopover = ({
  alerts,
  isOpen,
  onToggle,
  onClose,
  onSelectAlert,
  riskLabels,
}) => {
  const [activeFilter, setActiveFilter] = useState('ALL');

  const counts = useMemo(() => ({
    all: alerts.length,
    checkRequired: alerts.filter(alert => alert.riskLevel === 'CHECK_REQUIRED').length,
    caution: alerts.filter(alert => alert.riskLevel === 'CAUTION').length,
  }), [alerts]);

  const filteredAlerts = useMemo(() => {
    if (activeFilter === 'ALL') {
      return alerts;
    }

    return alerts.filter(alert => alert.riskLevel === activeFilter);
  }, [activeFilter, alerts]);

  const handleSelect = (alert) => {
    onSelectAlert(alert);
    onClose();
  };

  return (
    <div className="alerts-menu">
      <button
        type="button"
        className={`alerts-trigger ${isOpen ? 'open' : ''}`}
        onClick={onToggle}
        aria-haspopup="dialog"
        aria-expanded={isOpen}
      >
        <Bell size={17} strokeWidth={2.3} />
        <span>알림</span>
        {alerts.length > 0 && <strong>{alerts.length}</strong>}
      </button>

      {isOpen && (
        <section className="alert-popover" role="dialog" aria-label="운영 알림 목록">
          <div className="alert-popover-header">
            <div>
              <div className="alert-popover-eyebrow">운영 알림</div>
              <h3>확인이 필요한 가맹점</h3>
            </div>
            <button type="button" className="alert-close-button" onClick={onClose} aria-label="알림 목록 닫기">
              <X size={16} strokeWidth={2.2} />
            </button>
          </div>

          <div className="alert-summary-grid">
            <div>
              <span>전체</span>
              <strong>{counts.all}</strong>
            </div>
            <div className="check-required">
              <span>점검</span>
              <strong>{counts.checkRequired}</strong>
            </div>
            <div className="caution">
              <span>주의</span>
              <strong>{counts.caution}</strong>
            </div>
          </div>

          <div className="alert-filter-tabs" role="tablist" aria-label="알림 필터">
            {FILTERS.map(filter => (
              <button
                key={filter.key}
                type="button"
                className={activeFilter === filter.key ? 'active' : ''}
                onClick={() => setActiveFilter(filter.key)}
              >
                {filter.label}
              </button>
            ))}
          </div>

          {filteredAlerts.length === 0 ? (
            <div className="alert-empty">
              <CheckCircle2 size={24} strokeWidth={2.1} />
              <strong>현재 조건에서 확인할 알림이 없습니다.</strong>
              <span>지역/업종 필터를 바꾸면 다른 알림을 볼 수 있습니다.</span>
            </div>
          ) : (
            <ul className="alert-list">
              {filteredAlerts.map((alert) => (
                <li key={alert.merchantId}>
                  <button type="button" onClick={() => handleSelect(alert)}>
                    <div className="alert-row-header">
                      <span className={`risk-badge ${alert.riskLevel?.toLowerCase()}`}>
                        {riskLabels[alert.riskLevel] || alert.riskLevel}
                      </span>
                      <span className="alert-score">{alert.priorityScore}점</span>
                    </div>

                    <div className="alert-item-title">
                      <AlertTriangle size={16} strokeWidth={2.2} />
                      <strong>{alert.merchantName}</strong>
                      <ChevronRight size={15} strokeWidth={2.2} />
                    </div>

                    <div className="alert-summary">{alert.summary}</div>
                    {alert.reasons?.[0] && (
                      <div className="alert-reason">{alert.reasons[0]}</div>
                    )}

                    <div className="alert-meta">
                      <span>{alert.region} · {alert.industry}</span>
                      <span>{alert.latestMonth} · 전월 대비 {alert.salesGrowthRate > 0 ? '+' : ''}{alert.salesGrowthRate.toFixed(1)}%</span>
                    </div>

                    {alert.tags?.length > 0 && (
                      <div className="alert-tag-row">
                        {alert.tags.slice(0, 3).map(tag => (
                          <span key={tag}>{tag}</span>
                        ))}
                      </div>
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
};

export default AlertsPopover;
