import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api, tokenStore } from "../api/client";

/**
 * Holds "who is logged in" for the whole app.
 *
 * React Context is the standard way to share state that many components need
 * without passing props down through every level.
 */
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // "loading" matters: on a refresh we have a token but don't yet know if it is
  // still valid. Without this the app would flash the login screen every time.
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!tokenStore.get()) {
      setLoading(false);
      return;
    }
    // Ask the server whether the saved token is still good.
    api
      .me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  function acceptAuth(response) {
    tokenStore.set(response.token);
    setUser(response.user);
    return response.user;
  }

  const value = useMemo(
    () => ({
      user,
      loading,
      login: (payload) => api.login(payload).then(acceptAuth),
      signupStudent: (payload) => api.signupStudent(payload).then(acceptAuth),
      logout: () => {
        // Logging out is purely a client-side act with JWTs: the server keeps
        // no session, so we just throw the token away.
        tokenStore.clear();
        setUser(null);
      },
    }),
    [user, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside <AuthProvider>");
  }
  return context;
}
