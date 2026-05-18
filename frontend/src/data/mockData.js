// frontend/src/data/mockData.js

export const franchises = [
  {
    id: "F001",
    name: "강남역 1호점",
    industry: "카페",
    region: "서울 강남구",
    address: "서울 강남구 강남대로 396", // 강남역 인근 도로명 주소
    monthlySales: [
      { month: "2023-01", sales: 15000000, txCount: 1500, avgTicket: 10000 },
      { month: "2023-02", sales: 16500000, txCount: 1600, avgTicket: 10312 },
      { month: "2023-03", sales: 18000000, txCount: 1800, avgTicket: 10000 }
    ]
  },
  {
    id: "F002",
    name: "홍대입구점",
    industry: "음식점",
    region: "서울 마포구",
    address: "서울 마포구 양화로 160", // 홍대입구역 인근 도로명 주소
    monthlySales: [
      { month: "2023-01", sales: 25000000, txCount: 800, avgTicket: 31250 },
      { month: "2023-02", sales: 23000000, txCount: 750, avgTicket: 30666 },
      { month: "2023-03", sales: 28000000, txCount: 900, avgTicket: 31111 }
    ]
  },
  {
    id: "F003",
    name: "여의도 금융타운점",
    industry: "카페",
    region: "서울 영등포구",
    address: "서울 영등포구 여의대로 108", // 파크원(여의도) 도로명 주소
    monthlySales: [
      { month: "2023-01", sales: 22000000, txCount: 3000, avgTicket: 7333 },
      { month: "2023-02", sales: 20000000, txCount: 2800, avgTicket: 7142 },
      { month: "2023-03", sales: 25000000, txCount: 3200, avgTicket: 7812 }
    ]
  },
  {
    id: "F004",
    name: "성수 카페거리점",
    industry: "카페",
    region: "서울 성동구",
    address: "서울 성동구 연무장길 14", // 성수동 인근 주소
    monthlySales: [
      { month: "2023-01", sales: 18000000, txCount: 1200, avgTicket: 15000 },
      { month: "2023-02", sales: 20000000, txCount: 1300, avgTicket: 15384 },
      { month: "2023-03", sales: 21000000, txCount: 1400, avgTicket: 15000 }
    ]
  }
];

export const industryAverages = {
  "카페": {
    monthlySales: [
      { month: "2023-01", sales: 16000000 },
      { month: "2023-02", sales: 17000000 },
      { month: "2023-03", sales: 18500000 }
    ]
  },
  "음식점": {
    monthlySales: [
      { month: "2023-01", sales: 24000000 },
      { month: "2023-02", sales: 23500000 },
      { month: "2023-03", sales: 26000000 }
    ]
  }
};

export const regionAverages = {
  "서울 강남구": {
    monthlySales: [
      { month: "2023-01", sales: 20000000 },
      { month: "2023-02", sales: 21000000 },
      { month: "2023-03", sales: 22000000 }
    ]
  },
  "서울 마포구": {
    monthlySales: [
      { month: "2023-01", sales: 18000000 },
      { month: "2023-02", sales: 17500000 },
      { month: "2023-03", sales: 19000000 }
    ]
  },
  "서울 영등포구": {
    monthlySales: [
      { month: "2023-01", sales: 21000000 },
      { month: "2023-02", sales: 19000000 },
      { month: "2023-03", sales: 23000000 }
    ]
  },
  "서울 성동구": {
    monthlySales: [
      { month: "2023-01", sales: 16000000 },
      { month: "2023-02", sales: 18000000 },
      { month: "2023-03", sales: 19500000 }
    ]
  }
};
