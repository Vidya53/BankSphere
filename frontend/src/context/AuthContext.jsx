import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/auth';
import { tokenStore } from '../api/tokenStore';
import { decodeJwt } from '../utils/jwt';

const AuthContext = createContext(null);

const buildUserFromTokens = (accessToken, fallback) => {
  const claims = decodeJwt(accessToken) || {};
  return {
    userId:     claims.sub || fallback?.userId,
    email:      claims.email || fallback?.email,
    fullName:   claims.fullName || fallback?.fullName,
    role:       claims.role || fallback?.role,
    branchCode: claims.branchCode || fallback?.branchCode,
    exp:        claims.exp,
  };
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => tokenStore.getUser());
  const [bootstrapping, setBootstrapping] = useState(true);

  useEffect(() => {
    const accessToken = tokenStore.getAccessToken();
    if (accessToken && !user) {
      setUser(buildUserFromTokens(accessToken, tokenStore.getUser()));
    }
    setBootstrapping(false);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const login = async ({ email, password, remember = false }) => {
    const payload = await authApi.login({ email, password });
    const accessToken  = payload?.accessToken;
    const refreshToken = payload?.refreshToken;

    if (!accessToken) throw new Error('Login response did not include an access token.');

    // Record the "Remember me" choice FIRST so the storage helpers route
    // to localStorage (remembered) or sessionStorage (this tab only) when
    // we persist the tokens below.
    tokenStore.setRememberMe(remember);

    tokenStore.setAccessToken(accessToken);
    if (refreshToken) tokenStore.setRefreshToken(refreshToken);

    const nextUser = buildUserFromTokens(accessToken, payload);
    tokenStore.setUser(nextUser);
    setUser(nextUser);
    return nextUser;
  };

  const signup = async (payload) => authApi.signup(payload);

  const logout = async () => {
    const refreshToken = tokenStore.getRefreshToken();
    await authApi.logout(refreshToken);
    tokenStore.clear();
    setUser(null);
  };

  const value = useMemo(() => ({
    user,
    isAuthenticated: !!user,
    bootstrapping,
    login,
    signup,
    logout,
  }), [user, bootstrapping]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside an AuthProvider.');
  return ctx;
}
