import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { authApi } from "../../api/authApi";

/**
 * Wrong portal is reported exactly like a wrong password.
 *
 * Saying "that's a mentor account" would confirm the email exists AND reveal
 * what role it has - free reconnaissance for anyone probing addresses. The
 * generic wording keeps the two cases indistinguishable; the trailing hint is
 * true for everyone and gives nothing away.
 *
 * This string must match the server's bad-credentials message EXACTLY. If the
 * wrong-portal case said anything extra - even a helpful hint - the longer
 * message would itself tell you the password was right, leaking precisely what
 * the check was meant to hide. The hint lives in static text under the form
 * instead, where it is shown to everyone and reveals nothing.
 */
const GENERIC_LOGIN_ERROR = "Invalid email or password";

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
    authApi
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
      /**
       * `expectedRole` is the portal the user picked. The check happens before
       * acceptAuth, so on a mismatch the token is never stored and the wrong
       * dashboard never renders for even a frame.
       */
      login: (payload, expectedRole) =>
        authApi.login(payload).then((response) => {
          if (expectedRole && response.user.role !== expectedRole) {
            throw new Error(GENERIC_LOGIN_ERROR);
          }
          return acceptAuth(response);
        }),
      signupStudent: (payload) => authApi.signupStudent(payload).then(acceptAuth),
      signupMentor: (payload) => authApi.signupMentor(payload).then(acceptAuth),
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
