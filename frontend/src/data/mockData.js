// frontend/src/data/mockData.js

export const franchises = [
  {
    id: "F001",
    name: "강남역 1호점",
    industry: "카페",
    region: "서울 강남구",
    address: "서울 강남구 강남대로 396", // 강남역 인근 도로명 주소
    latitude: 37.4979,
    longitude: 127.0276,
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
    latitude: 37.5572,
    longitude: 126.9245,
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
    latitude: 37.5259,
    longitude: 126.9286,
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
    latitude: 37.5438,
    longitude: 127.0565,
    monthlySales: [
      { month: "2023-01", sales: 18000000, txCount: 1200, avgTicket: 15000 },
      { month: "2023-02", sales: 20000000, txCount: 1300, avgTicket: 15384 },
      { month: "2023-03", sales: 20400000, txCount: 1320, avgTicket: 15454 }
    ]
  },
  {
    id: "F005",
    name: "테헤란 점심특화점",
    industry: "음식점",
    region: "서울 강남구",
    address: "서울 강남구 테헤란로 152",
    latitude: 37.5013,
    longitude: 127.0396,
    monthlySales: [
      { month: "2023-01", sales: 17000000, txCount: 850, avgTicket: 20000 },
      { month: "2023-02", sales: 19000000, txCount: 930, avgTicket: 20430 },
      { month: "2023-03", sales: 16000000, txCount: 780, avgTicket: 20512 }
    ]
  },
  {
    id: "F006",
    name: "합정 브런치점",
    industry: "음식점",
    region: "서울 마포구",
    address: "서울 마포구 독막로 10",
    latitude: 37.5496,
    longitude: 126.9139,
    monthlySales: [
      { month: "2023-01", sales: 26000000, txCount: 1040, avgTicket: 25000 },
      { month: "2023-02", sales: 26500000, txCount: 1060, avgTicket: 25000 },
      { month: "2023-03", sales: 27000000, txCount: 1080, avgTicket: 25000 }
    ]
  },
  {
    id: "F007",
    name: "샛강 테이크아웃점",
    industry: "카페",
    region: "서울 영등포구",
    address: "서울 영등포구 의사당대로 83",
    latitude: 37.5184,
    longitude: 126.9317,
    monthlySales: [
      { month: "2023-01", sales: 19000000, txCount: 2600, avgTicket: 7307 },
      { month: "2023-02", sales: 18000000, txCount: 2500, avgTicket: 7200 },
      { month: "2023-03", sales: 15000000, txCount: 2100, avgTicket: 7142 }
    ]
  },
  {
    id: "F008",
    name: "성수 로스터리점",
    industry: "카페",
    region: "서울 성동구",
    address: "서울 성동구 성수이로 87",
    latitude: 37.5446,
    longitude: 127.0557,
    monthlySales: [
      { month: "2023-01", sales: 14000000, txCount: 950, avgTicket: 14736 },
      { month: "2023-02", sales: 15500000, txCount: 1050, avgTicket: 14761 },
      { month: "2023-03", sales: 19000000, txCount: 1250, avgTicket: 15200 }
    ]
  },
  {
    id: "F009",
    name: "상암 오피스푸드점",
    industry: "음식점",
    region: "서울 마포구",
    address: "서울 마포구 월드컵북로 396",
    latitude: 37.5796,
    longitude: 126.8896,
    monthlySales: [
      { month: "2023-01", sales: 30000000, txCount: 1200, avgTicket: 25000 },
      { month: "2023-02", sales: 30600000, txCount: 1220, avgTicket: 25081 },
      { month: "2023-03", sales: 31200000, txCount: 1240, avgTicket: 25161 }
    ]
  },
  {
    id: "F010",
    name: "선릉 라운지카페",
    industry: "카페",
    region: "서울 강남구",
    address: "서울 강남구 선릉로 428",
    latitude: 37.5045,
    longitude: 127.0490,
    monthlySales: [
      { month: "2023-01", sales: 28000000, txCount: 1900, avgTicket: 14736 },
      { month: "2023-02", sales: 27000000, txCount: 1850, avgTicket: 14594 },
      { month: "2023-03", sales: 22000000, txCount: 1500, avgTicket: 14666 }
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
