/**
 * TradeBall backend API client.
 * Configure base URL via window.TRADEBALL_API_BASE or localStorage key tradeball.apiBase.
 * Default: http://localhost:8080/api/v1  (Android emulator: http://10.0.2.2:8080/api/v1)
 *
 * The backend is authoritative for persisted data and trade evaluation.
 */
(function (global) {
  const DEFAULT_BASE = "http://localhost:8080/api/v1";
  const TOKEN_KEY = "tradeball.accessToken";
  const USER_KEY = "tradeball.user";
  const BASE_KEY = "tradeball.apiBase";

  function defaultBase() {
    try {
      if (global.Capacitor?.getPlatform?.() === "android") {
        return "http://10.0.2.2:8080/api/v1";
      }
    } catch (_) { /* ignore */ }
    return DEFAULT_BASE;
  }

  function baseUrl() {
    const configured = global.TRADEBALL_API_BASE || global.localStorage?.getItem(BASE_KEY);
    return (configured || defaultBase()).replace(/\/$/, "");
  }

  function authHeader() {
    const token = global.localStorage?.getItem(TOKEN_KEY);
    return token ? { Authorization: "Bearer " + token } : {};
  }

  function isAuthPath(path) {
    return path === "/auth/login" || path === "/auth/register";
  }

  function notifyUnauthorized() {
    if (typeof global.onTradeBallUnauthorized === "function") {
      global.onTradeBallUnauthorized();
    }
  }

  async function parseBody(res) {
    if (res.status === 204) return null;
    const text = await res.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch (_) { return { message: text }; }
  }

  function errorFrom(res, body) {
    const message = (body && (body.message || body.error)) || ("HTTP " + res.status);
    const err = new Error(message);
    err.status = res.status;
    err.code = body?.error || null;
    err.body = body;
    return err;
  }

  async function request(path, options = {}) {
    const controller = new AbortController();
    const timeout = global.setTimeout(() => controller.abort(), options.timeoutMs || 15000);
    const headers = {
      Accept: "application/json",
      ...authHeader(),
      ...(options.headers || {}),
    };
    if (options.body && !headers["Content-Type"]) {
      headers["Content-Type"] = "application/json";
    }
    let res;
    try {
      res = await fetch(baseUrl() + path, {
        ...options,
        signal: controller.signal,
        headers,
      });
    } catch (cause) {
      const err = new Error(cause?.name === "AbortError" ? "Request timed out" : "Network request failed");
      err.code = cause?.name === "AbortError" ? "TIMEOUT" : "NETWORK_ERROR";
      throw err;
    } finally {
      global.clearTimeout(timeout);
    }

    const body = await parseBody(res);
    if (!res.ok) {
      if (res.status === 401 && !isAuthPath(path)) {
        TradeBallApi.setToken(null);
        TradeBallApi.setUser(null);
        notifyUnauthorized();
      }
      throw errorFrom(res, body);
    }
    return body;
  }

  const TradeBallApi = {
    baseUrl,
    getToken() {
      return global.localStorage?.getItem(TOKEN_KEY) || null;
    },
    setToken(token) {
      if (token) global.localStorage?.setItem(TOKEN_KEY, token);
      else global.localStorage?.removeItem(TOKEN_KEY);
    },
    getUser() {
      try {
        const raw = global.localStorage?.getItem(USER_KEY);
        return raw ? JSON.parse(raw) : null;
      } catch (_) {
        return null;
      }
    },
    setUser(user) {
      if (user) global.localStorage?.setItem(USER_KEY, JSON.stringify(user));
      else global.localStorage?.removeItem(USER_KEY);
    },
    applyAuth(auth) {
      TradeBallApi.setToken(auth?.accessToken || null);
      TradeBallApi.setUser(auth?.user || null);
      return auth;
    },
    register(email, password, displayName) {
      return request("/auth/register", {
        method: "POST",
        body: JSON.stringify({ email, password, displayName }),
      }).then(TradeBallApi.applyAuth);
    },
    login(email, password) {
      return request("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      }).then(TradeBallApi.applyAuth);
    },
    me() { return request("/auth/me"); },
    listPlayers(page = 0, size = 20) {
      return request(`/players?page=${page}&size=${size}&sort=lastName,asc`);
    },
    searchPlayers(q, page = 0, size = 20) {
      return request(`/players/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`);
    },
    getPlayer(id) { return request(`/players/${id}`); },
    getPlayerStats(id, season) {
      const q = season ? `?season=${season}` : "";
      return request(`/players/${id}/stats${q}`);
    },
    getFantasyValue(id) { return request(`/players/${id}/fantasy-value`); },
    createRoster(name) {
      return request("/rosters", { method: "POST", body: JSON.stringify({ name }) });
    },
    listRosters() { return request("/rosters"); },
    getRoster(id) { return request(`/rosters/${id}`); },
    updateRoster(id, name) {
      return request(`/rosters/${id}`, { method: "PUT", body: JSON.stringify({ name }) });
    },
    deleteRoster(id) { return request(`/rosters/${id}`, { method: "DELETE" }); },
    addRosterPlayer(rosterId, playerId) {
      return request(`/rosters/${rosterId}/players/${playerId}`, { method: "POST" });
    },
    removeRosterPlayer(rosterId, playerId) {
      return request(`/rosters/${rosterId}/players/${playerId}`, { method: "DELETE" });
    },
    evaluateTrade(incomingPlayerIds, outgoingPlayerIds, rosterId) {
      const payload = { incomingPlayerIds, outgoingPlayerIds };
      if (rosterId != null) payload.rosterId = rosterId;
      return request("/trades/evaluate", {
        method: "POST",
        body: JSON.stringify(payload),
      });
    },
    tradeHistory(page = 0, size = 20) {
      return request(`/trades?page=${page}&size=${size}`);
    },
    getTrade(id) { return request(`/trades/${id}`); },
  };

  global.TradeBallApi = TradeBallApi;
})(typeof window !== "undefined" ? window : globalThis);
