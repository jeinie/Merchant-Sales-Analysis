import React, { useState, useEffect } from 'react';
import { api } from '../utils/api';
import { History, Maximize2, Save, X } from 'lucide-react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Line } from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

const renderInlineMarkdown = (text) => {
  return text.split(/(\*\*.+?\*\*)/g).map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }

    return part;
  });
};

const AiInsightMarkdown = ({ content }) => {
  const lines = content
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean);

  const elements = [];
  let listItems = [];

  const flushList = () => {
    if (listItems.length === 0) return;

    elements.push(
      <ul className="ai-insight-list" key={`list-${elements.length}`}>
        {listItems}
      </ul>
    );
    listItems = [];
  };

  lines.forEach((line, index) => {
    if (/^\*\*.+\*\*$/.test(line)) {
      flushList();
      elements.push(
        <h4 className="ai-insight-heading" key={`heading-${index}`}>
          {line.slice(2, -2)}
        </h4>
      );
      return;
    }

    if (/^#{1,3}\s+/.test(line)) {
      flushList();
      elements.push(
        <h4 className="ai-insight-heading" key={`heading-${index}`}>
          {line.replace(/^#{1,3}\s+/, '')}
        </h4>
      );
      return;
    }

    if (/^[-*]\s+/.test(line)) {
      const item = line.replace(/^[-*]\s+/, '');
      const markdownLabelMatch = item.match(/^\*\*([^*]+?)\*\*\s*[:：]?\s*(.*)$/);
      const plainLabelMatch = item.match(/^([^:：]+[:：])\s*(.*)$/);

      listItems.push(
        <li key={`item-${index}`}>
          {markdownLabelMatch ? (
            <>
              <strong>{markdownLabelMatch[1]}:</strong>
              {markdownLabelMatch[2] && <span> {renderInlineMarkdown(markdownLabelMatch[2])}</span>}
            </>
          ) : plainLabelMatch ? (
            <>
              <strong>{plainLabelMatch[1]}</strong>
              {plainLabelMatch[2] && <span> {renderInlineMarkdown(plainLabelMatch[2])}</span>}
            </>
          ) : (
            renderInlineMarkdown(item)
          )}
        </li>
      );
      return;
    }

    flushList();
    elements.push(
      <p className="ai-insight-paragraph" key={`paragraph-${index}`}>
        {renderInlineMarkdown(line)}
      </p>
    );
  });

  flushList();

  return <div className="ai-insight-markdown">{elements}</div>;
};

const DetailsPanel = ({ merchant, onClose, averages, currentUser }) => {
  const [aiInsight, setAiInsight] = useState(null);
  const [aiInsightMeta, setAiInsightMeta] = useState(null);
  const [aiInsightHistories, setAiInsightHistories] = useState([]);
  const [activeInsightTab, setActiveInsightTab] = useState('current');
  const [selectedHistoryId, setSelectedHistoryId] = useState(null);
  const [insightNote, setInsightNote] = useState('');
  const [isSavingNote, setIsSavingNote] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [aiError, setAiError] = useState(null);
  const [isInsightModalOpen, setIsInsightModalOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setAiInsight(null);
    setAiInsightMeta(null);
    setAiInsightHistories([]);
    setActiveInsightTab('current');
    setSelectedHistoryId(null);
    setInsightNote('');
    setAiError(null);
    setIsInsightModalOpen(false);

    if (!merchant) {
      return () => {
        cancelled = true;
      };
    }

    Promise.all([
      api.getLatestAiInsight(merchant.id),
      api.getAiInsights(merchant.id),
    ])
      .then(([latest, histories]) => {
        if (cancelled) return;
        setAiInsightHistories(histories || []);
        if (!latest) return;
        setAiInsight(latest.content);
        setAiInsightMeta(latest);
        setInsightNote(latest.note || '');
      })
      .catch((err) => {
        if (!cancelled && err.status !== 404) {
          console.error('AI 인사이트 이력 조회 실패:', err);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [merchant?.id]);

  if (!merchant) {
    return (
      <div className="details-panel" style={{ transform: 'translateX(100%)' }}>
      </div>
    );
  }

  const { industryAverages, regionAverages } = averages;
  const riskLabel = {
    CHECK_REQUIRED: '점검 필요',
    CAUTION: '주의',
    NORMAL: '정상 관찰',
  }[merchant.riskLevel] || merchant.riskLevel || '정상 관찰';

  const handleGenerateInsight = async () => {
    setIsGenerating(true);
    setAiError(null);
    try {
      const saved = await api.generateAiInsight(merchant.id);
      setAiInsight(saved.content);
      setAiInsightMeta(saved);
      setInsightNote(saved.note || '');
      setSelectedHistoryId(saved.id);
      setActiveInsightTab('current');
      setAiInsightHistories(await api.getAiInsights(merchant.id));
    } catch (err) {
      setAiError(err.message);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleOpenHistory = async () => {
    setActiveInsightTab('history');
    setIsInsightModalOpen(true);
    try {
      setAiInsightHistories(await api.getAiInsights(merchant.id));
    } catch (err) {
      console.error('AI 인사이트 이력 조회 실패:', err);
    }
  };

  const handleSelectHistory = (history) => {
    setSelectedHistoryId(history.id);
    setAiInsight(history.content);
    setAiInsightMeta(history);
    setInsightNote(history.note || '');
    setActiveInsightTab('current');
  };

  const handleSaveNote = async () => {
    if (!aiInsightMeta?.id) return;

    setIsSavingNote(true);
    try {
      const updated = await api.updateAiInsightNote(merchant.id, aiInsightMeta.id, insightNote);
      setAiInsightMeta(updated);
      setAiInsightHistories((histories) => histories.map(history => (
        history.id === updated.id ? updated : history
      )));
    } catch (err) {
      setAiError(err.message);
    } finally {
      setIsSavingNote(false);
    }
  };

  const labels = merchant.monthlySales.map(s => s.month);
  const salesData = merchant.monthlySales.map(s => s.sales);
  
  // Get comparison data
  const indAvg = industryAverages[merchant.industry]?.monthlySales.map(s => s.sales) || [];
  const regAvg = regionAverages[merchant.region]?.monthlySales.map(s => s.sales) || [];

  const data = {
    labels,
    datasets: [
      {
        label: '해당 가맹점 매출',
        data: salesData,
        borderColor: 'rgb(37, 99, 235)',
        backgroundColor: 'rgba(37, 99, 235, 0.5)',
        tension: 0.3,
      },
      {
        label: `${merchant.industry} 평균`,
        data: indAvg,
        borderColor: 'rgb(156, 163, 175)',
        backgroundColor: 'rgba(156, 163, 175, 0.5)',
        borderDash: [5, 5],
        tension: 0.3,
      }
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: false,
      },
    },
    scales: {
      y: {
        ticks: {
          callback: function(value) {
            return (value / 10000) + '만'; // Format to ten-thousands
          }
        }
      }
    }
  };

  // Summarize the latest sales movement for the operations insight panel.
  const latestSales = salesData[salesData.length - 1];
  const prevSales = salesData[salesData.length - 2];
  const isUp = latestSales > prevSales;
  const growthRate = (((latestSales - prevSales) / prevSales) * 100).toFixed(1);

  return (
    <>
    <div className="details-panel open">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 className="section-title" style={{ margin: 0 }}>{merchant.name} 상세 정보</h2>
        <button onClick={onClose} style={{ cursor: 'pointer', border: 'none', background: 'none', fontSize: '1.2rem' }}>✕</button>
      </div>

      <div className="card">
        <div className="metric-label">최근 월 매출 ({labels[labels.length - 1]})</div>
        <div className="metric-value">{(latestSales / 10000).toLocaleString()}만원</div>
        <div className={`metric-label ${isUp ? 'trend-up' : 'trend-down'}`}>
          전월 대비 {isUp ? '▲' : '▼'} {Math.abs(growthRate)}%
        </div>
      </div>

      <div className={`card risk-card ${merchant.riskLevel?.toLowerCase() || 'normal'}`}>
        <div className="risk-card-header">
          <span className={`risk-badge ${merchant.riskLevel?.toLowerCase() || 'normal'}`}>{riskLabel}</span>
          <span className="alert-score">{merchant.priorityScore || 0}점</span>
        </div>
        <h3 className="section-title">점검 사유</h3>
        <p className="risk-summary-text">{merchant.riskSummary || '현재 기준에서는 정상 관찰 대상입니다.'}</p>
        {merchant.alertReasons?.length > 0 && (
          <ul className="risk-reason-list">
            {merchant.alertReasons.map((reason, index) => (
              <li key={index}>{reason}</li>
            ))}
          </ul>
        )}
        {merchant.alertTags?.length > 0 && (
          <div className="risk-tag-list">
            {merchant.alertTags.map((tag) => (
              <span key={tag}>{tag}</span>
            ))}
          </div>
        )}
      </div>

      <div className="card">
        <h3 className="section-title">월별 매출 추이</h3>
        <Line options={options} data={data} />
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
          <div>
            <h3 className="section-title" style={{ margin: 0 }}>✨ AI 운영 인사이트</h3>
            {aiInsightMeta && (
              <div className="ai-insight-meta">
                저장됨 · {aiInsightMeta.salesMonth} · {aiInsightMeta.createdByName || aiInsightMeta.createdBy}
              </div>
            )}
          </div>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
            {aiInsight && (
              <button
                type="button"
                onClick={() => setIsInsightModalOpen(true)}
                className="icon-button"
                aria-label="AI 인사이트 크게 보기"
                title="AI 인사이트 크게 보기"
              >
                <Maximize2 size={16} strokeWidth={2.2} />
              </button>
            )}
            {aiInsightHistories.length > 0 && (
              <button
                type="button"
                onClick={handleOpenHistory}
                className="icon-button"
                aria-label="AI 인사이트 이력 보기"
                title="AI 인사이트 이력 보기"
              >
                <History size={16} strokeWidth={2.2} />
              </button>
            )}
            {currentUser?.permissions?.canUseAI !== false && (
              <button 
                onClick={handleGenerateInsight} 
                disabled={isGenerating}
                style={{
                  backgroundColor: '#8b5cf6', color: 'white', border: 'none', 
                  padding: '8px 16px', borderRadius: '8px', cursor: isGenerating ? 'wait' : 'pointer',
                  fontWeight: 'bold', fontSize: '0.9rem', transition: 'background-color 0.2s',
                  boxShadow: '0 2px 4px rgba(139, 92, 246, 0.3)'
                }}
              >
                {isGenerating ? '분석 중... ⏳' : aiInsight ? '새 분석 생성' : 'Gemini 분석하기'}
              </button>
            )}
          </div>
        </div>
        
        <div className="ai-insight" style={{ 
          minHeight: '100px', lineHeight: '1.6', fontSize: '0.95rem', color: '#374151' 
        }}>
          {isGenerating ? (
            <div style={{ textAlign: 'center', color: '#6b7280', padding: '30px 10px' }}>
              <strong>Gemini 1.5 Flash</strong> 모델이<br/>가맹점 매출 데이터를 분석하고 있습니다...
            </div>
          ) : aiError ? (
            <div style={{ color: '#ef4444', backgroundColor: '#fef2f2', padding: '15px', borderRadius: '8px' }}>
              {aiError}
            </div>
          ) : aiInsight ? (
            <AiInsightMarkdown content={aiInsight} />
          ) : currentUser?.permissions?.canUseAI === false ? (
            <div style={{ color: '#ef4444', textAlign: 'center', padding: '30px 10px', fontWeight: 'bold' }}>
              ⚠️ AI 운영 인사이트 기능 사용 권한이 없습니다.<br/>관리자에게 문의해주세요.
            </div>
          ) : (
            <div style={{ color: '#9ca3af', textAlign: 'center', padding: '30px 10px' }}>
              우측 상단의 버튼을 눌러<br/>본사/영업 담당자용 운영 인사이트를 생성해보세요.
            </div>
          )}
        </div>
      </div>
    </div>

    {isInsightModalOpen && aiInsight && (
      <div className="insight-modal-backdrop" onClick={() => setIsInsightModalOpen(false)}>
        <section
          className="insight-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="ai-insight-modal-title"
          onClick={(event) => event.stopPropagation()}
        >
          <div className="insight-modal-header">
            <div>
              <div className="metric-label">AI 운영 인사이트</div>
              <h2 id="ai-insight-modal-title">{merchant.name}</h2>
            </div>
            <button
              type="button"
              className="modal-close-button"
              onClick={() => setIsInsightModalOpen(false)}
              aria-label="AI 인사이트 크게 보기 닫기"
            >
              <X size={18} strokeWidth={2.2} />
            </button>
          </div>
          <div className="insight-modal-tabs" role="tablist" aria-label="AI 인사이트 보기">
            <button
              type="button"
              className={activeInsightTab === 'current' ? 'active' : ''}
              onClick={() => setActiveInsightTab('current')}
            >
              현재 분석
            </button>
            <button
              type="button"
              className={activeInsightTab === 'history' ? 'active' : ''}
              onClick={() => setActiveInsightTab('history')}
            >
              이력 {aiInsightHistories.length}
            </button>
          </div>
          <div className="insight-modal-body">
            {activeInsightTab === 'current' ? (
              <>
                <AiInsightMarkdown content={aiInsight} />
                {aiInsightMeta && (
                  <div className="insight-note-box">
                    <label htmlFor="ai-insight-note">담당자 메모</label>
                    <textarea
                      id="ai-insight-note"
                      value={insightNote}
                      onChange={(event) => setInsightNote(event.target.value)}
                      placeholder="확인한 내용이나 후속 조치 메모를 남겨주세요."
                    />
                    <button type="button" onClick={handleSaveNote} disabled={isSavingNote}>
                      <Save size={15} strokeWidth={2.2} />
                      {isSavingNote ? '저장 중...' : '메모 저장'}
                    </button>
                  </div>
                )}
              </>
            ) : (
              <div className="insight-history-list">
                {aiInsightHistories.map((history) => (
                  <button
                    type="button"
                    key={history.id}
                    className={selectedHistoryId === history.id ? 'active' : ''}
                    onClick={() => handleSelectHistory(history)}
                  >
                    <div className="insight-history-row">
                      <strong>{history.salesMonth}</strong>
                      <span>{history.createdByName || history.createdBy}</span>
                    </div>
                    <p>{history.summary}</p>
                    {history.note && <em>메모 있음</em>}
                  </button>
                ))}
              </div>
            )}
          </div>
        </section>
      </div>
    )}
    </>
  );
};

export default DetailsPanel;
