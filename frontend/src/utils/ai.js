import { GoogleGenerativeAI } from "@google/generative-ai";

// Initialize the Gemini API client
// Note: Vite uses import.meta.env for environment variables
const apiKey = import.meta.env.VITE_GEMINI_API_KEY;
const genAI = apiKey ? new GoogleGenerativeAI(apiKey) : null;

/**
 * Generates an AI insight report for a given franchise.
 * 
 * @param {Object} franchise The franchise data
 * @param {Object} averages Region and Industry averages for comparison
 * @returns {Promise<string>} The markdown-formatted response from Gemini
 */
export const generateFranchiseInsight = async (franchise, averages) => {
  if (!genAI) {
    throw new Error("Gemini API 키가 설정되지 않았습니다. .env 파일을 확인해주세요.");
  }

  // Use the Flash model for fast text generation
  const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

  // Find the latest month's sales data
  const latestSales = franchise.monthlySales[franchise.monthlySales.length - 1];
  const previousSales = franchise.monthlySales[franchise.monthlySales.length - 2];
  const salesGrowthRate = previousSales?.sales
    ? ((latestSales.sales - previousSales.sales) / previousSales.sales) * 100
    : 0;
  const txCountGrowthRate = previousSales?.txCount
    ? ((latestSales.txCount - previousSales.txCount) / previousSales.txCount) * 100
    : 0;
  const avgTicketGrowthRate = previousSales?.avgTicket
    ? ((latestSales.avgTicket - previousSales.avgTicket) / previousSales.avgTicket) * 100
    : 0;
  const monthlySalesSummary = franchise.monthlySales
    .map((item) => (
      `- ${item.month}: 매출 ${item.sales.toLocaleString()}원, 거래 ${item.txCount.toLocaleString()}건, 객단가 ${item.avgTicket.toLocaleString()}원`
    ))
    .join('\n');

  // Calculate total averages (simplified)
  const industryAvgData = averages.industryAverages[franchise.industry]?.monthlySales || [];
  const latestIndustryAvg = industryAvgData[industryAvgData.length - 1]?.sales || 0;

  const regionAvgData = averages.regionAverages[franchise.region]?.monthlySales || [];
  const latestRegionAvg = regionAvgData[regionAvgData.length - 1]?.sales || 0;
  const industryGapRate = latestIndustryAvg
    ? ((latestSales.sales - latestIndustryAvg) / latestIndustryAvg) * 100
    : 0;
  const regionGapRate = latestRegionAvg
    ? ((latestSales.sales - latestRegionAvg) / latestRegionAvg) * 100
    : 0;
  const salesStatus = salesGrowthRate >= 5 ? '상승' : salesGrowthRate <= -5 ? '하락' : '보합';

  // Construct the prompt for headquarters and sales-operations users.
  const prompt = `
당신은 "브랜드 본사와 영업 담당자를 위한 지도 기반 가맹점/매장 매출 모니터링 및 운영 인사이트 플랫폼"의 데이터 분석가입니다.
이 서비스의 목적은 점주 컨설팅이 아니라, 본사 관리자와 담당 영업사원이 여러 가맹점/매장 중 어느 곳을 먼저 확인해야 하는지, 어떤 지표를 계속 모니터링해야 하는지 판단하도록 돕는 것입니다.

아래 데이터를 바탕으로 특정 가맹점의 매출 상태를 진단하고, 지도/대시보드에서 관리 우선순위를 판단할 수 있는 운영 인사이트를 작성해주세요.
점주에게 직접 조언하는 표현은 피하고, 반드시 "본사", "관리자", "담당자", "영업 담당자" 관점으로 작성해주세요.
데이터로 확인되지 않는 외부 요인(날씨, 경쟁점, 이벤트 등)은 단정하지 말고 "가능성" 또는 "확인 필요"로 표현해주세요.

[가맹점 정보]
- 매장명: ${franchise.name}
- 업종: ${franchise.industry}
- 지역: ${franchise.region}
- 지도상 위치 식별 정보: ${franchise.address || franchise.region}
- 최근 월 매출: ${latestSales.sales.toLocaleString()}원
- 최근 월 결제 건수: ${latestSales.txCount.toLocaleString()}건
- 평균 객단가: ${latestSales.avgTicket.toLocaleString()}원
- 전월 대비 매출 변화율: ${salesGrowthRate.toFixed(1)}%
- 전월 대비 거래 건수 변화율: ${txCountGrowthRate.toFixed(1)}%
- 전월 대비 객단가 변화율: ${avgTicketGrowthRate.toFixed(1)}%
- 현재 매출 상태 분류: ${salesStatus}

[월별 이력]
${monthlySalesSummary}

[비교 지표]
- 동일 업종(${franchise.industry}) 평균 월 매출: ${latestIndustryAvg.toLocaleString()}원
- 동일 지역(${franchise.region}) 평균 월 매출: ${latestRegionAvg.toLocaleString()}원
- 동일 업종 평균 대비 차이: ${industryGapRate.toFixed(1)}%
- 동일 지역 평균 대비 차이: ${regionGapRate.toFixed(1)}%

요청사항:
1. 지도 기반 모니터링 관점에서 현재 상태를 "상승", "보합", "하락" 중 하나로 판단하고 핵심 근거를 요약
2. 동일 업종 평균 및 동일 지역 평균 대비 이 가맹점의 상대적 위치를 해석
3. 매출 변화의 원인 후보를 "거래 건수 요인", "객단가 요인", "비교 평균 대비 요인"으로 나누어 설명
4. 최근 흐름에서 이상 징후가 있는지 판단하고, 있다면 어떤 지표 때문에 주의가 필요한지 설명
5. 다음 모니터링에서 담당자가 확인해야 할 핵심 지표 2가지를 제시
6. 관리 우선순위를 "정상 관찰", "주의", "점검 필요" 중 하나로 제시
7. 지도/대시보드에서 사용할 수 있는 운영 태그 2~3개를 제시

출력 형식:
**AI 운영 인사이트**
- 상태 요약:
- 비교 위치:
- 원인 후보:
- 이상 징후:
- 모니터링 포인트:
- 관리 우선순위:
- 운영 태그:

전체 답변은 500~700자 내외로 작성하고, 추상적인 조언보다 지표 기반 판단을 우선해주세요.
  `;

  try {
    const result = await model.generateContent(prompt);
    const response = await result.response;
    return response.text();
  } catch (error) {
    console.error("AI Insight Generation Error:", error);

    // If it's a 404 model not found error, fetch the available models to diagnose
    if (error.message && error.message.includes('404')) {
      try {
        const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`);
        const data = await res.json();
        const availableModels = data.models
          ?.filter(m => m.supportedGenerationMethods?.includes('generateContent'))
          .map(m => m.name.replace('models/', ''))
          .join(', ');
        throw new Error(`API 권한 문제: 모델을 찾을 수 없습니다.\n현재 계정(키)에서 사용 가능한 모델 리스트:\n[${availableModels || '없음(API 권한 부족)'}]`);
      } catch (e) {
        throw new Error("API 오류: " + error.message);
      }
    }

    throw new Error("API 오류: " + (error.message || "알 수 없는 오류가 발생했습니다."));
  }
};
