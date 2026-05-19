import React, { useState, useEffect } from 'react';
import { generateFranchiseInsight } from '../utils/ai';
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

const DetailsPanel = ({ franchise, onClose, averages, currentUser }) => {
  if (!franchise) {
    return (
      <div className="details-panel" style={{ transform: 'translateX(100%)' }}>
      </div>
    );
  }

  const { industryAverages, regionAverages } = averages;

  const [aiInsight, setAiInsight] = useState(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [aiError, setAiError] = useState(null);

  useEffect(() => {
    // Reset AI insight when a different franchise is selected
    setAiInsight(null);
    setAiError(null);
  }, [franchise.id]);

  const handleGenerateInsight = async () => {
    setIsGenerating(true);
    setAiError(null);
    try {
      const insight = await generateFranchiseInsight(franchise, averages);
      setAiInsight(insight);
    } catch (err) {
      setAiError(err.message);
    } finally {
      setIsGenerating(false);
    }
  };

  const labels = franchise.monthlySales.map(s => s.month);
  const salesData = franchise.monthlySales.map(s => s.sales);
  
  // Get comparison data
  const indAvg = industryAverages[franchise.industry]?.monthlySales.map(s => s.sales) || [];
  const regAvg = regionAverages[franchise.region]?.monthlySales.map(s => s.sales) || [];

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
        label: `${franchise.industry} 평균`,
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

  // Generate mock AI insight based on data
  const latestSales = salesData[salesData.length - 1];
  const prevSales = salesData[salesData.length - 2];
  const isUp = latestSales > prevSales;
  const growthRate = (((latestSales - prevSales) / prevSales) * 100).toFixed(1);

  return (
    <div className="details-panel open">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 className="section-title" style={{ margin: 0 }}>{franchise.name} 상세 정보</h2>
        <button onClick={onClose} style={{ cursor: 'pointer', border: 'none', background: 'none', fontSize: '1.2rem' }}>✕</button>
      </div>

      <div className="card">
        <div className="metric-label">최근 월 매출 ({labels[labels.length - 1]})</div>
        <div className="metric-value">{(latestSales / 10000).toLocaleString()}만원</div>
        <div className={`metric-label ${isUp ? 'trend-up' : 'trend-down'}`}>
          전월 대비 {isUp ? '▲' : '▼'} {Math.abs(growthRate)}%
        </div>
      </div>

      <div className="card">
        <h3 className="section-title">월별 매출 추이</h3>
        <Line options={options} data={data} />
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
          <h3 className="section-title" style={{ margin: 0 }}>✨ AI 컨설팅 리포트</h3>
          {(!aiInsight && currentUser?.permissions?.canUseAI !== false) && (
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
              {isGenerating ? '분석 중... ⏳' : 'Gemini 분석하기'}
            </button>
          )}
        </div>
        
        <div className="ai-insight" style={{ 
          minHeight: '100px', whiteSpace: 'pre-line', lineHeight: '1.6', 
          fontSize: '0.95rem', color: '#374151' 
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
            <div>{aiInsight}</div>
          ) : currentUser?.permissions?.canUseAI === false ? (
            <div style={{ color: '#ef4444', textAlign: 'center', padding: '30px 10px', fontWeight: 'bold' }}>
              ⚠️ AI 컨설팅 기능 사용 권한이 없습니다.<br/>관리자에게 문의해주세요.
            </div>
          ) : (
            <div style={{ color: '#9ca3af', textAlign: 'center', padding: '30px 10px' }}>
              우측 상단의 버튼을 눌러<br/>매장 맞춤형 AI 비즈니스 인사이트를 생성해보세요.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DetailsPanel;
