const API_BASE_URL = 'http://localhost:8080/api';

export const fetchFranchises = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/franchises`);
    if (!response.ok) throw new Error('Failed to fetch franchises');
    return await response.json();
  } catch (error) {
    console.error('API Error:', error);
    return [];
  }
};

export const fetchAverages = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/averages`);
    if (!response.ok) throw new Error('Failed to fetch averages');
    return await response.json();
  } catch (error) {
    console.error('API Error:', error);
    return { industryAverages: {}, regionAverages: {} };
  }
};
