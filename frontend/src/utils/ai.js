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

  // Calculate total averages (simplified)
  const industryAvgData = averages.industryAverages[franchise.industry]?.monthlySales || [];
  const latestIndustryAvg = industryAvgData[industryAvgData.length - 1]?.sales || 0;

  const regionAvgData = averages.regionAverages[franchise.region]?.monthlySales || [];
  const latestRegionAvg = regionAvgData[regionAvgData.length - 1]?.sales || 0;

  // Construct the prompt
  const prompt = `
당신은 전문적인 프랜차이즈 경영 컨설턴트입니다. 
아래 제공된 가맹점의 데이터를 분석하여 점주에게 실질적이고 도움이 되는 비즈니스 인사이트를 제공해주세요.

[가맹점 정보]
- 매장명: ${franchise.name}
- 업종: ${franchise.industry}
- 지역: ${franchise.region}
- 최근 월 매출: ${latestSales.sales.toLocaleString()}원
- 최근 월 결제 건수: ${latestSales.txCount.toLocaleString()}건
- 평균 객단가: ${latestSales.avgTicket.toLocaleString()}원

[비교 지표]
- 동일 업종(${franchise.industry}) 평균 월 매출: ${latestIndustryAvg.toLocaleString()}원
- 동일 지역(${franchise.region}) 평균 월 매출: ${latestRegionAvg.toLocaleString()}원

요청사항:
1. 매장의 현재 상태 요약 (평균 대비 강점/약점 분석)
2. 가장 시급하게 개선해야 할 점
3. 매출 증대를 위한 구체적이고 현실적인 아이디어 2가지
결과는 마크다운 포맷으로 가독성 좋게, 부드러운 전문가 어투로 작성해주세요. 300~400자 내외로 핵심만 요약해주세요.
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
