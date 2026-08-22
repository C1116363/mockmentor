import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api, tokenStore } from "../api/client";

const PORTAL_NAME = {
  STUDENT: "Take interviews",
  MENTOR: "Give interviews",
  ADMIN: "Admin",
};

/**
 * Thrown when someone logs in from the wrong portal - e.g. mentor credentials
 * entered under the Admin tab.
 *
 * This is a usability guard, not a security control. The account owner could
 * always log in from the correct tab; what it prevents is the confusing
 * situation where you pick "Admin", it accepts your details, and you land on a
 * mentor dashboard wondering what happened.
 */
class WrongPortalError extends Error {
  constructor(actualRole, expectedRole) {
    super(
      `That's a ${actualRole.toLowerCase()} account, not ${
        expectedRole === "ADMIN" ? "an admin" : "a " + expectedRole.toLowerCase()
      } account. Choose "${PORTAL_NAME[actualRole]}" and log in again.`
    );
    this.actualRole = actualRole;
  }
}

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
      /**
       * `expectedRole` is the portal the user picked. The check happens before
       * acceptAuth, so on a mismatch the token is never stored and the wrong
       * dashboard never renders for even a frame.
       */
      login: (payload, expectedRole) =>
        api.login(payload).then((response) => {
          if (expectedRole && response.user.role !== expectedRole) {
            throw new WrongPortalError(response.user.role, expectedRole);
          }
          return acceptAuth(response);
        }),
      signupStudent: (payload) => api.signupStudent(payload).then(acceptAuth),
      signupMentor: (payload) => api.signupMentor(payload).then(acceptAuth),
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
