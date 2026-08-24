/**
 * The shared plumbing every api/ module sits on. Nothing feature-specific here.
 *
 * Backend counterpart: this is the JdbcTemplate/EntityManager of the frontend -
 * the one place that knows about HTTP, the auth header, the response envelope and
 * the token. A feature module below it only knows URLs.
 *
 *   api/http.js         <- you are here: transport
 *   api/planApi.js      <- one module per backend controller
 *   features/x/useX.js  <- orchestration (the facade layer)
 *   pages/XPage.jsx     <- the screen
 */

export const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";
const TOKEN_KEY = "prehire.token";

/**
 * The app has been renamed twice (MockMentor -> AbhiMentor -> PreHire), so a
 * token may sit under an older key too.
 */
const LEGACY_TOKEN_KEYS = ["abhimentor.token", "mockmentor.token"];

/**
 * Anything still holding a token in localStorage is cleared on load.
 *
 * The token used to live there, which meant a session survived closing the tab
 * and even quitting the browser. It lives in sessionStorage now, and this purge
 * is what makes the change take effect for people who were already logged in -
 * without it their old persisted token would keep working indefinitely and the
 * new rule would apply to nobody but new users.
 *
 * It also sweeps the legacy keys. Carrying those forward used to be worth doing
 * so a rename didn't log everyone out; under a policy where closing the tab logs
 * you out anyway, there is nothing left to preserve.
 */
function purgePersistedTokens() {
  try {
    for (const key of [TOKEN_KEY, ...LEGACY_TOKEN_KEYS]) {
      localStorage.removeItem(key);
    }
  } catch {
    /* storage blocked - nothing to purge */
  }
}
purgePersistedTokens();

/**
 * The token lives in sessionStorage, so closing the tab ends the session.
 *
 * sessionStorage is scoped to one tab and wiped when that tab closes. Which
 * gives exactly the behaviour we want, and two consequences worth knowing:
 *
 *  - **Refreshing keeps you logged in.** sessionStorage survives F5 and
 *    in-page navigation - it is the tab closing that clears it, not the page
 *    reloading. Anything else would make the app unusable.
 *  - **A second tab is a second login.** Nothing is shared between tabs, so
 *    opening the app in a new tab asks for credentials again. That follows from
 *    the same rule rather than being a separate decision.
 *
 * Every call is wrapped: storage access itself throws in some private-browsing
 * modes, and a login screen that white-screens instead of loading is worse than
 * one that cannot remember you. A failed read just means "not logged in".
 *
 * Note this is still JS-readable, so it is still exposed to XSS. An httpOnly
 * cookie is the stronger answer; sessionStorage fixes session lifetime, not
 * script access.
 */
export const tokenStore = {
  get: () => {
    try {
      return sessionStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set: (token) => {
    try {
      sessionStorage.setItem(TOKEN_KEY, token);
    } catch {
      /* storage blocked - the session lasts until this page unloads */
    }
  },
  clear: () => {
    try {
      sessionStorage.removeItem(TOKEN_KEY);
    } catch {
      /* nothing to clear */
    }
  },
};

/** Carries the backend's error message (and field errors) up to the component. */
export class ApiError extends Error {
  constructor(body, status) {
    super(body?.message ?? "Something went wrong. Is the backend running?");
    this.status = status;
    this.fieldErrors = body?.fieldErrors ?? {};
  }
}

/**
 * The backend wraps every response in one envelope:
 *
 *   { success, status, message, data, fieldErrors?, path?, timestamp }
 *
 * Unwrapping it happens HERE and nowhere else. That is the whole deal with an
 * envelope: the tax is one function, and nothing above this line - not a single
 * component - knows the envelope exists. `planApi.all()` still resolves to an
 * array of plans, exactly as it did before.
 *
 * `success` is the field to branch on, not the HTTP status. They agree today,
 * and if a proxy ever rewrites a status the body still carries the truth.
 */
function unwrap(body, status) {
  // A 204 has no body at all, and a non-JSON error (a proxy's HTML 502) parses
  // to null - both mean "nothing to unwrap".
  if (body === null || body === undefined) return null;

  // Envelopes always carry a boolean `success`. Anything else is a bare payload
  // from an endpoint that predates the envelope, or from something that isn't
  // our backend - pass it through rather than turning it into null.
  if (typeof body.success !== "boolean") return body;

  if (!body.success) throw new ApiError(body, status ?? body.status);
  return body.data ?? null;
}

/**
 * Like {@link request}, but returns the whole envelope: `{ data, message, status }`.
 *
 * Some endpoints put the important part in `message` rather than `data` - the
 * project-access grant returns "Add @user to owner/repo: <link>", and a mentor
 * declaring six hours gets told which two were skipped and why. `request()`
 * hands back only `data`, so those messages were being dropped on the floor.
 *
 * Use this wherever the server has something to say; `request()` everywhere else.
 */
export async function requestEnvelope(path, options = {}) {
  const envelope = await rawRequest(path, options);
  if (envelope === null || typeof envelope.success !== "boolean") {
    // Not an enveloped endpoint - hand back something with the same shape so
    // callers do not need to branch.
    return { data: envelope, message: null, status: 200 };
  }
  return { data: envelope.data ?? null, message: envelope.message ?? null,
           status: envelope.status };
}

async function rawRequest(path, options = {}) {
  const token = tokenStore.get();
  const isFormData = options.body instanceof FormData;

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      // Let the browser set Content-Type for multipart - it has to append the
      // boundary, and setting it ourselves silently breaks the upload.
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      // This header is the whole authentication mechanism on the client side.
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  const body = response.status === 204 ? null : await response.json().catch(() => null);

  if (!response.ok) {
    // An expired or tampered token gives 401 - drop it so the app shows login.
    if (response.status === 401 && token) {
      tokenStore.clear();
    }
    throw new ApiError(body, response.status);
  }
  // The envelope, intact. request() and requestEnvelope() decide what to keep.
  if (body !== null && typeof body?.success === "boolean" && !body.success) {
    throw new ApiError(body, response.status);
  }
  return body;
}

/**
 * The everyday call: resolves to the payload, throws an ApiError on failure.
 *
 * Nothing above this line knows the envelope exists - `planApi.all()` gives you
 * an array of plans.
 */
export async function request(path, options = {}) {
  return unwrap(await rawRequest(path, options), null);
}

/**
 * The screenshot endpoint needs the Authorization header, so it can't just go
 * in an <img src>. Fetch it as a blob and hand back an object URL.
 *
 * On failure the backend sends the same JSON error body as every other endpoint,
 * so read it rather than discarding it - passing null here meant a 403 ("not
 * yours to view") and a 404 ("no longer available") both surfaced as ApiError's
 * generic fallback, which blames the backend for being down.
 */
export async function fetchScreenshotUrl(paymentId) {
  const response = await fetch(`${BASE_URL}/payments/${paymentId}/screenshot`, {
    headers: { Authorization: `Bearer ${tokenStore.get()}` },
  });

  if (!response.ok) {
    // Same as request(): a 401 means the token is dead, so drop it.
    if (response.status === 401) tokenStore.clear();
    // The success path is image bytes, but a failure is still the JSON envelope.
    const body = await response.json().catch(() => null);
    throw new ApiError(body, response.status);
  }
  return URL.createObjectURL(await response.blob());
}

/**
 * Study material files need the Authorization header too, so the same trick as
 * the screenshot: fetch as a blob, hand back an object URL. The caller is
 * responsible for revoking it.
 */
export async function fetchMaterialBlobUrl(materialId) {
  const response = await fetch(`${BASE_URL}/materials/${materialId}/file`, {
    headers: { Authorization: `Bearer ${tokenStore.get()}` },
  });

  if (!response.ok) {
    if (response.status === 401) tokenStore.clear();
    const body = await response.json().catch(() => null);
    throw new ApiError(body, response.status);
  }
  return URL.createObjectURL(await response.blob());
}

/** The project-access payment screenshot. Same shape, different endpoint. */
export async function fetchProjectScreenshotUrl(accessId) {
  const response = await fetch(`${BASE_URL}/projects/access/${accessId}/screenshot`, {
    headers: { Authorization: `Bearer ${tokenStore.get()}` },
  });

  if (!response.ok) {
    if (response.status === 401) tokenStore.clear();
    const body = await response.json().catch(() => null);
    throw new ApiError(body, response.status);
  }
  return URL.createObjectURL(await response.blob());
}

/**
 * The plan payment screenshot, for the admin review card.
 *
 * Same shape as fetchScreenshotUrl but a different endpoint - plan purchases
 * are their own entity, so their screenshots are too.
 */
export async function fetchPlanScreenshotUrl(enrollmentId) {
  const response = await fetch(`${BASE_URL}/plans/enrollments/${enrollmentId}/screenshot`, {
    headers: { Authorization: `Bearer ${tokenStore.get()}` },
  });

  if (!response.ok) {
    if (response.status === 401) tokenStore.clear();
    const body = await response.json().catch(() => null);
    throw new ApiError(body, response.status);
  }
  return URL.createObjectURL(await response.blob());
}
