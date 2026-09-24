import { createContext, useContext, useState } from 'react';
import { tokenStorage } from '../api/axiosClient';
import { authApi } from '../api/employeeApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => tokenStorage.getUser());

  const login = async (username, password) => {
    const data = await authApi.login(username, password);
    const loggedIn = { username: data.username, role: data.role };
    tokenStorage.save(data.token, loggedIn);
    setUser(loggedIn);
  };

  const logout = () => {
    tokenStorage.clear();
    setUser(null);
  };

  const value = {
    user,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ADMIN',
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
