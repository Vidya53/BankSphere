// Token storage with explicit "Remember me" support.
//
// When the user ticks **Remember me** at login → tokens go to localStorage,
// so they survive a browser restart. Otherwise → tokens go to sessionStorage,
// which is wiped the moment the browser/tab closes. This is the standard
// real-bank behaviour: a shared computer doesn't keep your session alive.
//
// The choice itself is persisted in localStorage under `bs.rememberMe` so the
// page-load bootstrap knows which store to read from. `clear()` wipes both
// stores defensively in case anything is stale.

const ACCESS_KEY   = 'bs.accessToken';
const REFRESH_KEY  = 'bs.refreshToken';
const USER_KEY     = 'bs.user';
const REMEMBER_KEY = 'bs.rememberMe';     // 'true' → localStorage; otherwise sessionStorage

const isRemembered = () => localStorage.getItem(REMEMBER_KEY) === 'true';
const activeStore  = () => (isRemembered() ? localStorage : sessionStorage);
const otherStore   = () => (isRemembered() ? sessionStorage : localStorage);

const wipeKeys = (store) => {
  store.removeItem(ACCESS_KEY);
  store.removeItem(REFRESH_KEY);
  store.removeItem(USER_KEY);
};

export const tokenStore = {
  /**
   * Call this BEFORE setAccessToken/setRefreshToken/setUser during login.
   * Records the preference, then clears any stale tokens from the *other*
   * store so we never keep two copies floating around.
   */
  setRememberMe: (remember) => {
    if (remember) localStorage.setItem(REMEMBER_KEY, 'true');
    else          localStorage.removeItem(REMEMBER_KEY);
    wipeKeys(otherStore());
  },

  isRemembered,

  getAccessToken:  () => activeStore().getItem(ACCESS_KEY),
  setAccessToken:  (t) => activeStore().setItem(ACCESS_KEY, t),
  getRefreshToken: () => activeStore().getItem(REFRESH_KEY),
  setRefreshToken: (t) => activeStore().setItem(REFRESH_KEY, t),

  getUser: () => {
    try { return JSON.parse(activeStore().getItem(USER_KEY) || 'null'); }
    catch { return null; }
  },
  setUser: (u) => activeStore().setItem(USER_KEY, JSON.stringify(u)),

  clear: () => {
    wipeKeys(localStorage);
    wipeKeys(sessionStorage);
    localStorage.removeItem(REMEMBER_KEY);
  },
};
