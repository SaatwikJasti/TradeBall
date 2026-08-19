/**
 * TradeBall UI. Backend APIs are the source of truth for auth, players,
 * roster ownership, fantasy values, trade evaluation, and history.
 */
(function (global) {
  const CATS = ["pts", "reb", "ast", "stl", "blk", "threepm", "fgp", "ftp", "tov"];
  const CATEGORY_MAP = {
    PTS: "pts", REB: "reb", AST: "ast", STL: "stl", BLK: "blk",
    THREE_PM: "threepm", FG_PCT: "fgp", FT_PCT: "ftp", TO: "tov"
  };
  const CAT_LABELS = {
    pts: "PTS", reb: "REB", ast: "AST", stl: "STL", blk: "BLK",
    threepm: "3PM", fgp: "FG%", ftp: "FT%", tov: "TOV"
  };

  let NBA_PLAYERS = [];
  let playerById = new Map();
  let myRoster = [];
  let currentRosterId = null;
  let currentRosterName = "My Roster";
  let currentUser = null;
  let tradeLeft = [];
  let tradeRight = [];
  let modalSide = "left";
  let modalPosFilter = "ALL";
  let authMode = "login";
  let dataSource = "TradeBall backend";
  let searchTimer = null;
  let modalSearchTimer = null;

  function $(id) { return document.getElementById(id); }

  function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, (ch) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    }[ch]));
  }

  function initials(name) {
    return (name || "").split(" ").map((w) => w[0]).filter(Boolean).join("").slice(0, 2).toUpperCase();
  }

  function sum(arr, key) {
    return arr.reduce((total, item) => total + (item[key] || 0), 0);
  }
  function avg(arr, key) {
    return arr.length ? sum(arr, key) / arr.length : 0;
  }
  function combinedFs(players) {
    return sum(players, "_fscore");
  }
  function platformFsLabel(players) {
    if (!players.length) return "";
    if (players.length === 1) return `Fantasy Score ${players[0]._fscore ?? "—"}`;
    return `Combined FS ${combinedFs(players).toFixed(0)} · ${players.length} players`;
  }

  function fmt(n) { return (n > 0 ? "+" : "") + Number(n || 0).toFixed(1); }
  function cls(n) { return n > 0.05 ? "pos" : n < -0.05 ? "neg" : "neu"; }

  function cleanPos(pos) {
    if (!pos) return "G";
    const p = pos.trim().split("-")[0].split("/")[0].toUpperCase();
    return ["PG", "SG", "SF", "PF", "C"].includes(p) ? p : p;
  }

  function formatVerdict(verdict) {
    if (!verdict) return "";
    return String(verdict).toLowerCase().replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
  }

  function statLabel(value, digits = 1) {
    return Number.isFinite(value) ? Number(value).toFixed(digits) : "—";
  }

  function showAuthError(message) {
    const el = $("auth-error");
    if (!el) return;
    if (!message) {
      el.style.display = "none";
      el.textContent = "";
      return;
    }
    el.style.display = "block";
    el.textContent = message;
  }

  function setBusy(button, busy, idleLabel) {
    if (!button) return;
    button.disabled = !!busy;
    if (busy) button.textContent = "Working…";
    else if (idleLabel) button.textContent = idleLabel;
  }

  function mapPlayer(player, stats, fantasy) {
    return {
      id: player.id,
      name: player.fullName || [player.firstName, player.lastName].filter(Boolean).join(" "),
      team: player.team || "—",
      pos: cleanPos(player.position),
      age: player.age ?? null,
      gp: stats?.gamesPlayed ?? 0,
      pts: stats?.points ?? 0,
      reb: stats?.rebounds ?? 0,
      ast: stats?.assists ?? 0,
      stl: stats?.steals ?? 0,
      blk: stats?.blocks ?? 0,
      threepm: stats?.threePointers ?? 0,
      fgp: stats?.fieldGoalPercentage ?? 0,
      ftp: stats?.freeThrowPercentage ?? 0,
      tov: stats?.turnovers ?? 0,
      _fscore_raw: fantasy?.fantasyScore,
      _fscore: fantasy?.normalizedScore,
      _loaded: !!(stats && fantasy)
    };
  }

  function rememberPlayer(mapped) {
    const existing = playerById.get(mapped.id);
    const merged = existing ? { ...existing, ...mapped } : mapped;
    playerById.set(merged.id, merged);
    const idx = NBA_PLAYERS.findIndex((p) => p.id === merged.id);
    if (idx >= 0) NBA_PLAYERS[idx] = merged;
    else NBA_PLAYERS.push(merged);
    return merged;
  }

  async function mapPool(items, limit, fn) {
    const out = new Array(items.length);
    let i = 0;
    async function worker() {
      while (i < items.length) {
        const idx = i++;
        out[idx] = await fn(items[idx]);
      }
    }
    const workers = Array.from({ length: Math.min(limit, items.length) || 0 }, worker);
    await Promise.all(workers);
    return out;
  }

  async function hydratePlayer(player) {
    const cached = playerById.get(player.id);
    if (cached?._loaded) return cached;
    const [stats, fantasy] = await Promise.all([
      TradeBallApi.getPlayerStats(player.id),
      TradeBallApi.getFantasyValue(player.id)
    ]);
    return rememberPlayer(mapPlayer(player, stats, fantasy));
  }

  async function fetchAllPlayers() {
    const first = await TradeBallApi.listPlayers(0, 50);
    let content = first.content || [];
    const totalPages = first.totalPages || 1;
    for (let page = 1; page < totalPages; page++) {
      const next = await TradeBallApi.listPlayers(page, 50);
      content = content.concat(next.content || []);
    }
    return content;
  }

  function updateDataBanner(state, message) {
    const banner = $("data-banner");
    const text = $("data-banner-text");
    if (!banner || !text) return;
    banner.className = "data-banner" + (state === "ready" ? "" : " " + state);
    text.textContent = message;
  }

  function updateProfileStats() {
    const user = currentUser || TradeBallApi.getUser();
    $("prof-source").textContent = dataSource || "—";
    $("prof-players").textContent = NBA_PLAYERS.length || "—";
    $("prof-user").textContent = user?.email || "—";
    $("prof-name").textContent = user?.displayName || "—";
    $("profile-league-sub").textContent = currentRosterName || "—";
    $("roster-league-name").textContent = currentRosterName || "—";
    const el = $("prof-roster-summary");
    if (!myRoster.length) {
      el.innerHTML = '<p style="font-size:13px;color:var(--c-muted);">Add players to your roster to see summary.</p>';
      return;
    }
    const avgFs = (myRoster.reduce((s, p) => s + (p._fscore || 0), 0) / myRoster.length).toFixed(1);
    const avgAge = (myRoster.reduce((s, p) => s + (p.age || 0), 0) / myRoster.length).toFixed(1);
    const avgPts = (myRoster.reduce((s, p) => s + (p.pts || 0), 0) / myRoster.length).toFixed(1);
    el.innerHTML = `
      <div class="profile-stat-row"><span class="profile-stat-label">Players</span><span class="profile-stat-value">${myRoster.length}</span></div>
      <div class="profile-stat-row"><span class="profile-stat-label">Avg fantasy score</span><span class="profile-stat-value">${avgFs}/100</span></div>
      <div class="profile-stat-row"><span class="profile-stat-label">Avg age</span><span class="profile-stat-value">${avgAge} yrs</span></div>
      <div class="profile-stat-row"><span class="profile-stat-label">Avg PPG</span><span class="profile-stat-value">${avgPts}</span></div>
    `;
  }

  async function loadPlayers() {
    updateDataBanner("loading", "Loading NBA players from backend…");
    try {
      const players = await fetchAllPlayers();
      if (!players.length) {
        updateDataBanner("error", "No players synchronized yet");
        NBA_PLAYERS = [];
        playerById = new Map();
        updateProfileStats();
        return;
      }
      players.forEach((player) => rememberPlayer(mapPlayer(player)));
      updateDataBanner("loading", `Loading stats for ${players.length} players…`);
      await mapPool(players, 6, async (player) => {
        try { await hydratePlayer(player); }
        catch (_) { /* keep the name-only record */ }
      });
      dataSource = "TradeBall backend";
      updateDataBanner("ready", `${dataSource} · ${NBA_PLAYERS.length} players`);
      updateProfileStats();
    } catch (err) {
      NBA_PLAYERS = [];
      playerById = new Map();
      updateDataBanner("error", err.message || "Could not load players");
      updateProfileStats();
    }
  }

  async function applyRoster(roster) {
    currentRosterId = roster.id;
    currentRosterName = roster.name;
    const hydrated = [];
    for (const player of roster.players || []) {
      try { hydrated.push(await hydratePlayer(player)); }
      catch (_) { hydrated.push(rememberPlayer(mapPlayer(player))); }
    }
    myRoster = hydrated;
    tradeLeft = tradeLeft.filter((p) => myRoster.some((r) => r.id === p.id));
    renderRoster();
    updateLeftPlatform();
    checkAnalyze();
    updateProfileStats();
  }

  async function ensureRoster(preferredName) {
    const list = await TradeBallApi.listRosters();
    if (list.length) {
      await applyRoster(list[0]);
      if (preferredName && preferredName !== list[0].name) {
        try {
          const updated = await TradeBallApi.updateRoster(list[0].id, preferredName);
          await applyRoster(updated);
        } catch (_) { /* keep existing name */ }
      }
      return;
    }
    const created = await TradeBallApi.createRoster(preferredName || "My Roster");
    await applyRoster(created);
  }

  function setAuthMode(mode) {
    authMode = mode;
    $("mode-login").classList.toggle("active", mode === "login");
    $("mode-register").classList.toggle("active", mode === "register");
    $("display-name-field").style.display = mode === "register" ? "block" : "none";
    $("auth-submit").textContent = mode === "register" ? "Create account" : "Sign in";
    $("password-hint").style.display = mode === "register" ? "block" : "none";
    showAuthError("");
  }

  function showLogin() {
    $("screen-main").classList.remove("active");
    $("screen-login").classList.add("active");
  }

  function showMain() {
    $("screen-login").classList.remove("active");
    $("screen-main").classList.add("active");
  }

  async function enterApp(user, rosterName) {
    currentUser = user;
    TradeBallApi.setUser(user);
    showMain();
    showAuthError("");
    updateProfileStats();
    await Promise.all([
      loadPlayers(),
      ensureRoster(rosterName || currentRosterName || "My Roster").catch((err) => {
        updateDataBanner("error", err.message || "Could not load roster");
      })
    ]);
    loadHistory().catch(() => {});
  }

  async function submitAuth() {
    const email = $("login-email").value.trim();
    const password = $("login-pass").value;
    const displayName = $("login-name").value.trim();
    const rosterName = $("login-league").value.trim();
    const btn = $("auth-submit");
    showAuthError("");
    if (!email || !password) {
      showAuthError("Email and password are required.");
      return;
    }
    if (authMode === "register") {
      if (displayName.length < 2) {
        showAuthError("Display name must be at least 2 characters.");
        return;
      }
      if (password.length < 8) {
        showAuthError("Password must be at least 8 characters.");
        return;
      }
    }
    setBusy(btn, true);
    try {
      const auth = authMode === "register"
        ? await TradeBallApi.register(email, password, displayName)
        : await TradeBallApi.login(email, password);
      await enterApp(auth.user, rosterName);
    } catch (err) {
      if (err.status === 401) showAuthError("Invalid email or password.");
      else if (err.status === 409) showAuthError(err.message || "Email already registered.");
      else if (err.status === 400) showAuthError(err.message || "Check the form and try again.");
      else showAuthError(err.message || "Could not reach the TradeBall API.");
    } finally {
      setBusy(btn, false, authMode === "register" ? "Create account" : "Sign in");
    }
  }

  function resetClientState() {
    myRoster = [];
    tradeLeft = [];
    tradeRight = [];
    currentRosterId = null;
    NBA_PLAYERS = [];
    playerById = new Map();
    $("score-section").style.display = "none";
    $("roster-search").value = "";
    $("roster-suggestions").style.display = "none";
    updateLeftPlatform();
    updateRightPlatform();
    checkAnalyze();
    renderRoster();
    $("history-list").innerHTML = `<div class="empty-state"><i class="ti ti-history"></i><p>Sign in to view trade history</p></div>`;
  }

  function doLogout(message) {
    TradeBallApi.setToken(null);
    TradeBallApi.setUser(null);
    currentUser = null;
    resetClientState();
    showLogin();
    if (message) showAuthError(message);
    switchTab("team");
  }

  global.onTradeBallUnauthorized = function () {
    if ($("screen-login").classList.contains("active")) return;
    doLogout("Session expired or token is invalid. Please sign in again.");
  };

  function switchTab(tab) {
    ["team", "trade", "history", "profile"].forEach((t) => {
      const panel = $("tab-" + t);
      const btn = $("tab-btn-" + t);
      if (panel) panel.style.display = t === tab ? "flex" : "none";
      if (btn) btn.classList.toggle("active", t === tab);
    });
    if (tab === "profile") updateProfileStats();
    if (tab === "history") loadHistory().catch(() => {});
  }

  function renderSuggestions(results, emptyText) {
    const box = $("roster-suggestions");
    if (!results.length) {
      box.innerHTML = `<div class="suggestion-item" style="cursor:default;color:var(--c-muted);">${esc(emptyText)}</div>`;
      box.style.display = "block";
      return;
    }
    box.innerHTML = results.map((p) => `
      <div class="suggestion-item" onclick="addToRoster(${p.id})">
        <div class="player-avatar">${esc(initials(p.name))}</div>
        <div class="player-info">
          <div class="player-name">${esc(p.name)}</div>
          <div class="player-meta">${esc(p.team)} · ${esc(p.pos)} · ${statLabel(p.pts)} PPG · FS ${p._fscore ?? "—"}</div>
        </div>
        <i class="ti ti-plus" style="color:var(--c-muted);font-size:17px;"></i>
      </div>`).join("");
    box.style.display = "block";
  }

  async function rosterSearch(q) {
    const box = $("roster-suggestions");
    clearTimeout(searchTimer);
    if (!q.trim()) { box.style.display = "none"; return; }
    searchTimer = setTimeout(async () => {
      try {
        const page = await TradeBallApi.searchPlayers(q.trim(), 0, 8);
        const mapped = [];
        for (const player of page.content || []) {
          if (myRoster.some((r) => r.id === player.id)) continue;
          mapped.push(playerById.get(player.id) || rememberPlayer(mapPlayer(player)));
        }
        renderSuggestions(mapped, "No matching players");
      } catch (err) {
        const local = NBA_PLAYERS.filter((p) =>
          p.name.toLowerCase().includes(q.toLowerCase()) && !myRoster.some((r) => r.id === p.id)
        ).slice(0, 8);
        if (local.length) renderSuggestions(local, "No matching players");
        else renderSuggestions([], err.message || "Search failed");
      }
    }, 220);
  }

  async function addToRoster(id) {
    if (!currentRosterId) {
      updateDataBanner("error", "Roster is not ready yet");
      return;
    }
    $("roster-suggestions").style.display = "none";
    $("roster-search").value = "";
    try {
      const updated = await TradeBallApi.addRosterPlayer(currentRosterId, id);
      await applyRoster(updated);
    } catch (err) {
      updateDataBanner("error", err.message || "Could not add player");
    }
  }

  async function removeFromRoster(id) {
    if (!currentRosterId) return;
    try {
      const updated = await TradeBallApi.removeRosterPlayer(currentRosterId, id);
      tradeLeft = tradeLeft.filter((p) => p.id !== id);
      await applyRoster(updated);
    } catch (err) {
      updateDataBanner("error", err.message || "Could not remove player");
    }
  }

  function renderRoster() {
    const el = $("roster-list");
    if (!myRoster.length) {
      el.innerHTML = `<div class="empty-state"><i class="ti ti-users"></i><p>Search for players above<br>to build your roster</p></div>`;
      return;
    }
    el.innerHTML = myRoster.map((p) => {
      const sel = !!tradeLeft.find((x) => x.id === p.id);
      return `<div class="roster-card">
        <div class="pos-badge">${esc(p.pos)}</div>
        <div class="player-avatar">${esc(initials(p.name))}</div>
        <div class="roster-stats">
          <div class="roster-name">${esc(p.name)}</div>
          <div class="roster-meta">${esc(p.team)} · ${statLabel(p.pts)}pts ${statLabel(p.reb)}reb ${statLabel(p.ast)}ast · FS ${p._fscore ?? "—"}</div>
        </div>
        <button class="select-btn ${sel ? "selected" : ""}" onclick="toggleTradeLeft(${p.id})">
          ${sel ? "✓ Trade" : "Trade"}
        </button>
        <button class="rm-btn" onclick="removeFromRoster(${p.id})" aria-label="Remove ${esc(p.name)}">
          <i class="ti ti-x"></i>
        </button>
      </div>`;
    }).join("");
  }

  function toggleTradeLeft(id) {
    const p = myRoster.find((x) => x.id === id);
    if (!p) return;
    if (tradeLeft.find((x) => x.id === id)) tradeLeft = tradeLeft.filter((x) => x.id !== id);
    else tradeLeft.push(p);
    renderRoster();
    updateLeftPlatform();
    checkAnalyze();
  }

  function updateLeftPlatform() {
    const show = tradeLeft.length > 0;
    $("left-empty").style.display = show ? "none" : "flex";
    $("left-filled").style.display = show ? "flex" : "none";
    $("left-platform").classList.toggle("filled", show);
    if (!show) return;
    const p = tradeLeft[0];
    $("left-initials").textContent = initials(p.name);
    $("left-name").textContent = tradeLeft.length === 1 ? p.name : `${tradeLeft.length} players`;
    $("left-pos").textContent = tradeLeft.length === 1 ? `${p.team} · ${p.pos}` : "";
    $("left-fscore").textContent = platformFsLabel(tradeLeft);
    $("left-tags").innerHTML = tradeLeft.length > 1
      ? tradeLeft.map((x) => `<span class="player-tag">${esc(x.name.split(" ").pop())}</span>`).join("")
      : "";
  }

  function updateRightPlatform() {
    const show = tradeRight.length > 0;
    $("right-empty").style.display = show ? "none" : "flex";
    $("right-filled").style.display = show ? "flex" : "none";
    $("right-platform").classList.toggle("filled", show);
    if (!show) return;
    const p = tradeRight[0];
    $("right-initials").textContent = initials(p.name);
    $("right-name").textContent = tradeRight.length === 1 ? p.name : `${tradeRight.length} players`;
    $("right-pos").textContent = tradeRight.length === 1 ? `${p.team} · ${p.pos}` : "";
    $("right-fscore").textContent = platformFsLabel(tradeRight);
    $("right-tags").innerHTML = tradeRight.length > 1
      ? tradeRight.map((x) => `<span class="player-tag">${esc(x.name.split(" ").pop())}</span>`).join("")
      : "";
  }

  function checkAnalyze() {
    $("analyze-btn").disabled = !(tradeLeft.length && tradeRight.length);
  }

  function openModal(side) {
    modalSide = side;
    $("modal-title").textContent = side === "left" ? "Select players to trade away" : "Select players to receive";
    $("modal-input").value = "";
    modalPosFilter = "ALL";
    document.querySelectorAll("#modal .chip").forEach((c) => c.classList.remove("active"));
    document.querySelector("#modal .chip")?.classList.add("active");
    renderModalList("");
    $("modal").style.display = "flex";
    setTimeout(() => $("modal-input").focus(), 250);
  }

  function closeModalOutside(e) { if (e.target === $("modal")) closeModal(); }
  function closeModal() { $("modal").style.display = "none"; checkAnalyze(); }

  function filterPos(el, pos) {
    modalPosFilter = pos;
    document.querySelectorAll("#modal .chip").forEach((c) => c.classList.remove("active"));
    el.classList.add("active");
    renderModalList($("modal-input").value);
  }

  function modalSearch(q) {
    clearTimeout(modalSearchTimer);
    if (modalSide === "right" && q.trim()) {
      modalSearchTimer = setTimeout(() => renderModalList(q), 180);
    } else {
      renderModalList(q);
    }
  }

  async function renderModalList(q) {
    const selected = modalSide === "left" ? tradeLeft : tradeRight;
    const el = $("modal-list");
    let players;
    if (modalSide === "left") {
      players = myRoster.filter((p) => {
        const matchQ = !q.trim() || p.name.toLowerCase().includes(q.toLowerCase()) || (p.team || "").toLowerCase().includes(q.toLowerCase());
        const matchPos = modalPosFilter === "ALL" || p.pos === modalPosFilter;
        return matchQ && matchPos;
      });
    } else if (q.trim()) {
      try {
        const page = await TradeBallApi.searchPlayers(q.trim(), 0, 30);
        players = (page.content || []).map((player) => playerById.get(player.id) || rememberPlayer(mapPlayer(player)));
      } catch (_) {
        players = NBA_PLAYERS.filter((p) => p.name.toLowerCase().includes(q.toLowerCase()) || (p.team || "").toLowerCase().includes(q.toLowerCase()));
      }
      players = players.filter((p) => modalPosFilter === "ALL" || p.pos === modalPosFilter);
    } else {
      players = NBA_PLAYERS.filter((p) => modalPosFilter === "ALL" || p.pos === modalPosFilter);
    }

    if (!players.length) {
      el.innerHTML = `<div class="empty-state"><i class="ti ti-search-off"></i><p>${modalSide === "left" ? "Add players to your roster first" : "No players found"}</p></div>`;
      return;
    }
    el.innerHTML = players.map((p) => {
      const isSel = !!selected.find((x) => x.id === p.id);
      return `<div class="suggestion-item" onclick="toggleModalPlayer(${p.id})"
        style="${isSel ? "background:rgba(29,158,117,0.08);" : ""}">
        <div class="player-avatar ${isSel ? "green-bg" : ""}">${esc(initials(p.name))}</div>
        <div class="player-info">
          <div class="player-name">${esc(p.name)}</div>
          <div class="player-meta">${esc(p.team)} · ${esc(p.pos)} · ${statLabel(p.pts)}pts ${statLabel(p.reb)}reb ${statLabel(p.ast)}ast · FS ${p._fscore ?? "—"}</div>
        </div>
        ${isSel
          ? '<i class="ti ti-check" style="color:#1D9E75;font-size:20px;"></i>'
          : '<i class="ti ti-plus"  style="color:var(--c-muted);font-size:17px;"></i>'}
      </div>`;
    }).join("");
  }

  function toggleModalPlayer(id) {
    const p = playerById.get(id) || NBA_PLAYERS.find((x) => x.id === id) || myRoster.find((x) => x.id === id);
    if (!p) return;
    if (modalSide === "left") {
      if (tradeLeft.find((x) => x.id === id)) tradeLeft = tradeLeft.filter((x) => x.id !== id);
      else tradeLeft.push(p);
      updateLeftPlatform();
      renderRoster();
    } else {
      if (tradeRight.find((x) => x.id === id)) tradeRight = tradeRight.filter((x) => x.id !== id);
      else tradeRight.push(p);
      updateRightPlatform();
    }
    renderModalList($("modal-input").value);
  }

  function suggestTrade() {
    if (!tradeLeft.length) {
      alert("Select a player to trade away from your Roster tab first.");
      return;
    }
    const exclude = new Set([...tradeLeft.map((p) => p.id), ...myRoster.map((p) => p.id)]);
    const outgoingFs = combinedFs(tradeLeft);
    const candidates = NBA_PLAYERS
      .filter((p) => !exclude.has(p.id) && p._fscore != null)
      .sort((a, b) => Math.abs((a._fscore || 0) - outgoingFs) - Math.abs((b._fscore || 0) - outgoingFs))
      .slice(0, 1);
    if (!candidates.length) {
      alert("No backend-valued trade targets are available yet.");
      return;
    }
    tradeRight = candidates;
    updateRightPlatform();
    checkAnalyze();
    switchTab("trade");
  }

  function toTradeRenderModel(result) {
    const byCategory = Object.fromEntries((result.categoryAnalysis || []).map((c) => [c.category, c]));
    const zBreakdown = {};
    Object.entries(CATEGORY_MAP).forEach(([api, ui]) => {
      const c = byCategory[api] || {};
      zBreakdown[ui] = {
        givZ: 0,
        getZ: c.zScoreDelta || 0,
        delta: c.zScoreDelta || 0,
        incomingValue: c.incomingValue,
        outgoingValue: c.outgoingValue
      };
    });
    const stat = (api) => byCategory[api]?.delta || 0;
    const signals = result.signals || [];
    return {
      score: result.score,
      fscoreDelta: (result.incomingFantasyScore || 0) - (result.outgoingFantasyScore || 0),
      ptsDelta: stat("PTS"), rebDelta: stat("REB"), astDelta: stat("AST"), stlDelta: stat("STL"),
      blkDelta: stat("BLK"), tpDelta: stat("THREE_PM"), fgpDelta: stat("FG_PCT"),
      ftpDelta: stat("FT_PCT"), tovDelta: stat("TO"), zBreakdown,
      buyLow: signals.includes("BUY_LOW"), sellHigh: signals.includes("SELL_HIGH")
    };
  }

  function showResult(r, ai) {
    $("score-loading").style.display = "none";
    $("score-result").style.display = "block";

    const score = r.score;
    const numEl = $("score-num");
    numEl.textContent = score;
    numEl.className = "score-value " + (score >= 65 ? "good" : score >= 45 ? "neutral" : "bad");

    const bar = $("score-bar");
    bar.style.width = score + "%";
    bar.style.background = score >= 65 ? "#1D9E75" : score >= 45 ? "#BA7517" : "#E24B4A";

    $("score-verdict").textContent = ai.verdict || "";
    $("score-reason").textContent = ai.reason || "";

    const flags = $("flags-row");
    let flagsHTML = "";
    if (score >= 65) flagsHTML += `<span class="flag-pill good">Good trade</span>`;
    else if (score < 45) flagsHTML += `<span class="flag-pill bad">Poor trade</span>`;
    if (ai.buyLowOpportunity || r.buyLow) flagsHTML += `<span class="flag-pill buy">Buy low</span>`;
    if (ai.sellHighOpportunity || r.sellHigh) flagsHTML += `<span class="flag-pill sell">Sell high</span>`;
    (ai.strengths || []).slice(0, 2).forEach((s) => { flagsHTML += `<span class="flag-pill buy">${esc(s)}</span>`; });
    (ai.weaknesses || []).slice(0, 2).forEach((s) => { flagsHTML += `<span class="flag-pill bad">${esc(s)}</span>`; });
    flags.innerHTML = flagsHTML;

    $("z-cat-grid").innerHTML = CATS.map((cat) => {
      const z = r.zBreakdown[cat] || {};
      const givStat = z.outgoingValue ?? avg(tradeLeft, cat);
      const getStat = z.incomingValue ?? avg(tradeRight, cat);
      const toWidth = (v) => Math.round(Math.min(100, Math.max(0, (((v || 0) + 3) / 6) * 100)));
      const gW = toWidth(z.givZ);
      const eW = toWidth(z.getZ);
      const isBetter = cat === "tov" ? (z.getZ || 0) < (z.givZ || 0) : (z.delta || 0) > 0;
      return `<div class="z-cat-card">
        <div class="z-cat-label">${CAT_LABELS[cat]}</div>
        <div class="z-cat-bar-track" style="height:5px;">
          <div class="z-bar-giv" style="width:${gW}%;"></div>
          <div class="z-bar-get" style="width:${eW}%;opacity:0.8;background:${isBetter ? "#1D9E75" : "#E24B4A"};"></div>
        </div>
        <div class="z-cat-values">
          <span class="z-val-giv" style="color:var(--c-muted);">Out ${statLabel(givStat)}</span>
          <span class="z-val-get" style="color:${isBetter ? "#1D9E75" : "#E24B4A"};">In ${statLabel(getStat)}</span>
        </div>
      </div>`;
    }).join("");

    const rows = [
      { label: "Combined 9-cat production", val: r.fscoreDelta, suffix: " pts", flip: false },
      { label: "Points per game", val: r.ptsDelta, suffix: " PPG", flip: false },
      { label: "Rebounds per game", val: r.rebDelta, suffix: " RPG", flip: false },
      { label: "Assists per game", val: r.astDelta, suffix: " APG", flip: false },
      { label: "Steals per game", val: r.stlDelta, suffix: " SPG", flip: false },
      { label: "Blocks per game", val: r.blkDelta, suffix: " BPG", flip: false },
      { label: "3-Pointers per game", val: r.tpDelta, suffix: " 3PM", flip: false },
      { label: "FG%", val: r.fgpDelta, suffix: "%", flip: false },
      { label: "FT%", val: r.ftpDelta, suffix: "%", flip: false },
      { label: "Turnovers (− = better)", val: r.tovDelta, suffix: " TOV", flip: true }
    ];
    $("heuristics-table").innerHTML = rows.map((row) => {
      const c = row.flip ? cls(-row.val) : cls(row.val);
      return `<div class="heuristic-row">
        <span class="heuristic-label">${row.label}</span>
        <span class="heuristic-delta ${c}">${fmt(row.val)}${row.suffix}</span>
      </div>`;
    }).join("");

    $("score-section").scrollIntoView({ behavior: "smooth", block: "start" });
  }

  async function analyzeTrade() {
    $("score-section").style.display = "block";
    $("score-loading").style.display = "block";
    $("score-result").style.display = "none";
    try {
      const result = await TradeBallApi.evaluateTrade(
        tradeRight.map((player) => player.id),
        tradeLeft.map((player) => player.id),
        currentRosterId
      );
      const signals = result.signals || [];
      showResult(toTradeRenderModel(result), {
        verdict: formatVerdict(result.verdict),
        reason: result.explanation,
        buyLowOpportunity: signals.includes("BUY_LOW"),
        sellHighOpportunity: signals.includes("SELL_HIGH"),
        strengths: result.strengths || [],
        weaknesses: result.weaknesses || []
      });
      loadHistory().catch(() => {});
    } catch (e) {
      $("score-loading").style.display = "none";
      $("score-result").style.display = "block";
      $("score-num").textContent = "—";
      $("score-num").className = "score-value";
      $("score-bar").style.width = "0%";
      $("flags-row").innerHTML = "";
      $("z-cat-grid").innerHTML = "";
      $("heuristics-table").innerHTML = "";
      if (e.status === 401) $("score-verdict").textContent = "Sign in required";
      else if (e.status === 403) $("score-verdict").textContent = "Not allowed";
      else if (e.status === 400) $("score-verdict").textContent = "Invalid trade";
      else $("score-verdict").textContent = "Trade analysis unavailable";
      $("score-reason").textContent = e.message || "The backend could not evaluate this trade.";
    }
  }

  async function loadHistory() {
    const el = $("history-list");
    el.innerHTML = `<div class="loading-dots"><span></span><span></span><span></span></div>`;
    try {
      const page = await TradeBallApi.tradeHistory(0, 20);
      const items = page.content || [];
      if (!items.length) {
        el.innerHTML = `<div class="empty-state"><i class="ti ti-history"></i><p>No evaluated trades yet.<br>Analyze a trade to see it here.</p></div>`;
        return;
      }
      el.innerHTML = items.map((item) => {
        const score = item.score ?? "—";
        const tone = (item.score ?? 0) >= 65 ? "good" : (item.score ?? 0) >= 45 ? "neutral" : "bad";
        return `<div class="history-card" onclick="openHistoryTrade(${item.tradeId})">
          <div class="history-top">
            <div class="score-value ${tone}" style="font-size:22px;">${esc(score)}</div>
            <div>
              <div class="score-verdict">${esc(formatVerdict(item.verdict))}</div>
              <div class="player-meta">Trade #${esc(item.tradeId)} · ${esc(item.modelVersion || "backend")}</div>
            </div>
          </div>
          <p class="score-reason" style="margin:0;">${esc(item.explanation || "")}</p>
        </div>`;
      }).join("");
    } catch (err) {
      el.innerHTML = `<div class="empty-state"><i class="ti ti-alert-circle"></i><p>${esc(err.message || "Could not load trade history")}</p></div>`;
    }
  }

  async function openHistoryTrade(id) {
    switchTab("trade");
    $("score-section").style.display = "block";
    $("score-loading").style.display = "block";
    $("score-result").style.display = "none";
    try {
      const result = await TradeBallApi.getTrade(id);
      const signals = result.signals || [];
      showResult(toTradeRenderModel(result), {
        verdict: formatVerdict(result.verdict),
        reason: result.explanation,
        buyLowOpportunity: signals.includes("BUY_LOW"),
        sellHighOpportunity: signals.includes("SELL_HIGH"),
        strengths: result.strengths || [],
        weaknesses: result.weaknesses || []
      });
    } catch (e) {
      $("score-loading").style.display = "none";
      $("score-result").style.display = "block";
      $("score-verdict").textContent = e.status === 403 ? "Not allowed" : "Could not load trade";
      $("score-reason").textContent = e.message;
    }
  }

  async function restoreSession() {
    const token = TradeBallApi.getToken();
    if (!token) return;
    try {
      const user = await TradeBallApi.me();
      TradeBallApi.setUser(user);
      await enterApp(user, $("login-league").value.trim());
    } catch (_) {
      TradeBallApi.setToken(null);
      TradeBallApi.setUser(null);
    }
  }

  document.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && $("screen-login").classList.contains("active")) {
      const tag = (event.target && event.target.tagName) || "";
      if (tag === "INPUT") submitAuth();
    }
  });

  document.addEventListener("DOMContentLoaded", restoreSession);

  Object.assign(global, {
    setAuthMode, submitAuth, doLogout, switchTab, rosterSearch,
    addToRoster, removeFromRoster, toggleTradeLeft, openModal,
    closeModalOutside, closeModal, filterPos, modalSearch,
    toggleModalPlayer, suggestTrade, analyzeTrade, openHistoryTrade
  });
})(window);
