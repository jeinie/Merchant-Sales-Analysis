import {
  users as defaultUsers,
  franchises as defaultFranchises,
  industryAverages,
  regionAverages,
} from '../data/mockData';

const USERS_STORAGE_KEY = 'usersData';

const clone = (value) => JSON.parse(JSON.stringify(value));

const normalizeUsers = (storedUsers) => {
  const storedById = new Map((storedUsers || []).map((user) => [user.id, user]));

  return defaultUsers.map((defaultUser) => {
    const storedUser = storedById.get(defaultUser.id) || {};
    return {
      ...defaultUser,
      ...storedUser,
      id: defaultUser.id,
      password: defaultUser.password,
      name: defaultUser.name,
      role: defaultUser.role,
      assignedFranchiseIds: storedUser.assignedFranchiseIds ?? defaultUser.assignedFranchiseIds,
      permissions: {
        ...defaultUser.permissions,
        ...storedUser.permissions,
      },
    };
  });
};

const loadUsers = () => {
  const saved = localStorage.getItem(USERS_STORAGE_KEY);
  if (!saved) return clone(defaultUsers);

  try {
    return normalizeUsers(JSON.parse(saved));
  } catch (err) {
    console.warn('저장된 mock 사용자 데이터를 읽지 못해 기본 데이터로 초기화합니다.');
    return clone(defaultUsers);
  }
};

let mockUsers = loadUsers();
let mockFranchises = clone(defaultFranchises);

const persistUsers = () => {
  localStorage.setItem(USERS_STORAGE_KEY, JSON.stringify(mockUsers));
};

const toPublicUser = ({ password, ...user }) => clone(user);

const syncSavedCurrentUser = (userId) => {
  const currentRaw = localStorage.getItem('currentUser');
  if (!currentRaw) return;

  try {
    const currentUser = JSON.parse(currentRaw);
    if (currentUser.id !== userId) return;

    const updated = mockUsers.find((user) => user.id === userId);
    if (updated) {
      localStorage.setItem('currentUser', JSON.stringify(toPublicUser(updated)));
    }
  } catch (err) {
    localStorage.removeItem('currentUser');
  }
};

export const api = {
  async getUsers() {
    return mockUsers.map(toPublicUser);
  },

  async login(id, password) {
    const user = mockUsers.find((item) => item.id === id && item.password === password);
    if (!user) {
      throw new Error('아이디 또는 비밀번호가 올바르지 않습니다.');
    }

    return toPublicUser(user);
  },

  async getFranchises(userId, role) {
    if (!userId || role === 'ADMIN') {
      return clone(mockFranchises);
    }

    const user = mockUsers.find((item) => item.id === userId);
    const assignedIds = user?.assignedFranchiseIds || [];
    return clone(mockFranchises.filter((franchise) => assignedIds.includes(franchise.id)));
  },

  async getAverages() {
    return clone({ industryAverages, regionAverages });
  },

  async getAdminUsers() {
    return mockUsers.filter((user) => user.role === 'SALES').map(toPublicUser);
  },

  async assignManager(franchiseId, managerId) {
    const franchiseExists = mockFranchises.some((franchise) => franchise.id === franchiseId);
    if (!franchiseExists) {
      throw new Error('존재하지 않는 가맹점입니다.');
    }

    if (managerId && !mockUsers.some((user) => user.id === managerId && user.role === 'SALES')) {
      throw new Error('존재하지 않는 영업사원입니다.');
    }

    mockUsers = mockUsers.map((user) => {
      if (user.role !== 'SALES') return user;

      let assignedFranchiseIds = user.assignedFranchiseIds
        ? [...user.assignedFranchiseIds]
        : [];

      assignedFranchiseIds = assignedFranchiseIds.filter((id) => id !== franchiseId);
      if (user.id === managerId) {
        assignedFranchiseIds.push(franchiseId);
      }

      return { ...user, assignedFranchiseIds };
    });

    persistUsers();
    mockUsers.forEach((user) => syncSavedCurrentUser(user.id));
    return { success: true };
  },

  async toggleAi(userId, canUseAI) {
    const userExists = mockUsers.some((user) => user.id === userId);
    if (!userExists) {
      throw new Error('존재하지 않는 사용자입니다.');
    }

    mockUsers = mockUsers.map((user) => {
      if (user.id !== userId) return user;
      return {
        ...user,
        permissions: {
          ...user.permissions,
          canUseAI,
        },
      };
    });

    persistUsers();
    syncSavedCurrentUser(userId);
    return { success: true };
  },
};
