import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api, message } from './api';
import { PlayerSearch } from './PlayerSearch';
import { TradeResult } from './TradeResult';
import { selectionError, tradeError } from './selection';
import type { ApiFailure, Evaluation, Player, Roster, User } from './types';

const searchBackend = async (query: string) => (await api.searchPlayers(query, 0, 20)).content;

function SignIn({ expired, onSignIn }: { expired: boolean; onSignIn: (user: User) => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setBusy(true);
    try { onSignIn((await api.login(email.trim(), password)).user); }
    catch (reason) { setError((reason as ApiFailure).status === 401 ? 'Email or password is incorrect.' : message(reason)); }
    finally { setBusy(false); }
  }
  return <section className="auth card"><p className="eyebrow">YOUR NEXT MOVE</p><h1>Make the trade.<br />Know the impact.</h1>
    <p>Compare players across nine fantasy categories using your TradeBall roster.</p>
    {expired && <p role="alert">Your session expired. Sign in again to continue. Your previous selections have been cleared.</p>}
    <form onSubmit={submit}><label>Email<input type="email" autoComplete="username" value={email} onChange={e => setEmail(e.target.value)} required disabled={busy} /></label>
      <label>Password<input type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} required disabled={busy} /></label>
      {error && <p role="alert">{error}</p>}<button className="primary" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      {busy && <p role="status">Connecting to your account…</p>}
    </form><p className="hint">Use your existing TradeBall account. New accounts can be created in the original app.</p>
  </section>;
}

function Selected({ players, side, disabled, remove }: { players: Player[]; side: string; disabled: boolean; remove: (id: number) => void }) {
  return players.length ? <ul className="selected">{players.map(player => <li key={player.id}>
    <span><strong>{player.fullName}</strong><small>{player.team} · {player.position}</small></span>
    <button type="button" disabled={disabled} aria-label={`Remove ${player.fullName} from ${side}`} onClick={() => remove(player.id)}>Remove</button>
  </li>)}</ul> : <p className="selection-empty">No {side} players selected.</p>;
}

function TradeWorkflow() {
  const [rosters, setRosters] = useState<Roster[]>([]);
  const [rosterId, setRosterId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [reload, setReload] = useState(0);
  const [name, setName] = useState('My Roster');
  const [outgoing, setOutgoing] = useState<Player[]>([]);
  const [incoming, setIncoming] = useState<Player[]>([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);
  const [evaluating, setEvaluating] = useState(false);
  const [result, setResult] = useState<Evaluation | null>(null);
  const roster = rosters.find(item => item.id === rosterId) || null;
  const busy = saving || evaluating;
  useEffect(() => {
    let active = true;
    setLoading(true); setLoadError('');
    api.listRosters().then(list => { if (active) { setRosters(list); setRosterId(list[0]?.id ?? null); } })
      .catch(reason => { if (active) setLoadError(message(reason)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [reload]);
  const searchRoster = useCallback(async (query: string) => (roster?.players || []).filter(player =>
    `${player.fullName} ${player.team || ''} ${player.position || ''}`.toLowerCase().includes(query.toLowerCase())), [roster]);
  function changed() { setResult(null); setError(''); setNotice(''); }
  async function createRoster(event: FormEvent) {
    event.preventDefault(); setSaving(true); setError('');
    try { const created = await api.createRoster(name.trim()); setRosters([created]); setRosterId(created.id); }
    catch (reason) { setError(message(reason)); }
    finally { setSaving(false); }
  }
  async function addToRoster(player: Player) {
    if (!roster || busy) return;
    setSaving(true); changed();
    try {
      const updated = await api.addRosterPlayer(roster.id, player.id);
      setRosters(items => items.map(item => item.id === updated.id ? updated : item));
      setNotice(`${player.fullName} added to ${updated.name}.`);
    } catch (reason) { setError(message(reason)); }
    finally { setSaving(false); }
  }
  function select(player: Player, side: 'outgoing' | 'incoming') {
    if (busy) return;
    const own = side === 'outgoing' ? outgoing : incoming;
    const other = side === 'outgoing' ? incoming : outgoing;
    const invalid = selectionError(player, own, other);
    if (invalid) { setError(invalid); return; }
    if (side === 'outgoing' && !roster?.players.some(p => p.id === player.id)) { setError('Outgoing players must belong to your selected roster.'); return; }
    changed();
    const update = side === 'outgoing' ? setOutgoing : setIncoming;
    update(previous => previous.some(p => p.id === player.id) ? previous : [...previous, player]);
  }
  function remove(id: number, side: 'outgoing' | 'incoming') {
    changed(); (side === 'outgoing' ? setOutgoing : setIncoming)(players => players.filter(player => player.id !== id));
  }
  async function evaluate(event: FormEvent) {
    event.preventDefault(); if (busy) return;
    const invalid = tradeError(outgoing, incoming, roster);
    if (invalid) { setError(invalid); return; }
    setEvaluating(true); changed();
    try { setResult(await api.evaluateTrade(incoming.map(p => p.id), outgoing.map(p => p.id), roster!.id)); }
    catch (reason) { setError(message(reason)); }
    finally { setEvaluating(false); }
  }
  if (loading) return <p role="status" className="card">Loading your rosters…</p>;
  if (loadError) return <div role="alert" className="card"><p>{loadError}</p><button onClick={() => setReload(n => n + 1)}>Retry rosters</button></div>;
  return <>
    <section className="intro"><p className="eyebrow">TRADE DESK / NINE CATEGORIES</p><h1>A clearer view of<br />your next trade.</h1><p>Select the players you give up and receive. Compare the impact before you decide.</p></section>
    <section className="card roster" aria-labelledby="roster-title"><h2 id="roster-title">Your roster</h2>
      {!rosters.length ? <><p>No rosters yet. Create one, then add the players you own.</p><form onSubmit={createRoster} className="inline-form">
        <label>Roster name<input value={name} onChange={e => setName(e.target.value)} required maxLength={100} disabled={busy} /></label>
        <button disabled={busy || !name.trim()}>{saving ? 'Creating…' : 'Create roster'}</button></form></>
        : <><label>Selected roster<select value={rosterId ?? ''} disabled={busy} onChange={e => {
          setRosterId(Number(e.target.value)); setOutgoing([]); setIncoming([]); changed();
        }}>{rosters.map(item => <option key={item.id} value={item.id}>{item.name} · {item.players.length} {item.players.length === 1 ? 'player' : 'players'}</option>)}</select></label>
          <button type="button" disabled={busy} onClick={() => { setOutgoing([]); setIncoming([]); changed(); setReload(n => n + 1); }}>Refresh rosters</button>
          <details open={!roster?.players.length}><summary>Add players you own</summary>
            <PlayerSearch label="Search players to add to roster" find={searchBackend} selected={roster?.players || []}
              onSelect={addToRoster} disabled={busy} action="roster" /></details>
          {!roster?.players.length && <p className="hint">Your roster is empty. Add at least one player before selecting an outgoing trade.</p>}</>}
    </section>
    {notice && <p role="status" className="notice">{notice}</p>}
    {error && <p role="alert" className="error">{error}</p>}
    <form onSubmit={evaluate} aria-label="Trade comparison">
      <div className="trade-grid"><section className="card" aria-labelledby="outgoing-title"><p className="eyebrow">01 / YOU GIVE</p><h2 id="outgoing-title">Outgoing players <span className="count">{outgoing.length}</span></h2>
        <Selected players={outgoing} side="outgoing" disabled={busy} remove={id => remove(id, 'outgoing')} />
        {roster && <PlayerSearch key={`out-${roster.id}`} label="Search your roster" find={searchRoster} minimum={0} selected={outgoing} opposite={incoming}
          disabled={busy} onSelect={player => select(player, 'outgoing')} />}
      </section><section className="card" aria-labelledby="incoming-title"><p className="eyebrow">02 / YOU RECEIVE</p><h2 id="incoming-title">Incoming players <span className="count">{incoming.length}</span></h2>
        <Selected players={incoming} side="incoming" disabled={busy} remove={id => remove(id, 'incoming')} />
        <PlayerSearch key={`in-${rosterId}`} label="Search incoming players" find={searchBackend} selected={incoming} opposite={outgoing}
          disabled={busy || !roster} onSelect={player => select(player, 'incoming')} />
      </section></div>
      <div className="submit-row"><p>Outgoing players must belong to your selected roster.<br /><span className="hint">No player can appear on both sides.</span></p>
        <button className="primary" disabled={busy || !roster}>{evaluating ? 'Evaluating…' : 'Evaluate trade'}</button></div>
    </form>
    {evaluating && <p role="status" className="card">Evaluating your trade with TradeBall…</p>}
    {result ? <TradeResult result={result} /> : !evaluating && <section className="result-placeholder"><h2>Your comparison will appear here.</h2><p>Select both sides, then evaluate to see the score and category-level impact.</p></section>}
  </>;
}

export function App() {
  const [user, setUser] = useState<User | null>(null);
  const [checking, setChecking] = useState(!!api.getToken());
  const [expired, setExpired] = useState(false);
  const [sessionError, setSessionError] = useState('');
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const previous = window.onTradeBallUnauthorized;
    window.onTradeBallUnauthorized = () => { setUser(null); setExpired(true); setChecking(false); };
    return () => { window.onTradeBallUnauthorized = previous; };
  }, []);
  useEffect(() => {
    let active = true;
    if (!api.getToken()) { setChecking(false); return; }
    setChecking(true); setSessionError('');
    api.me().then(current => { if (active) setUser(current); })
      .catch(reason => { if (active && (reason as ApiFailure).status !== 401) setSessionError(message(reason)); })
      .finally(() => { if (active) setChecking(false); });
    return () => { active = false; };
  }, [retry]);
  const original = import.meta.env.DEV ? 'http://localhost:5500/' : '../index.html';
  return <div className="shell"><header><a className="brand" href={original}><span className="brand-mark">TR</span>TradeRank</a>
    <nav aria-label="Account"><a href={original}>Original app</a>{user && <><span>{user.displayName}</span><button onClick={() => {
      api.setToken(null); api.setUser(null); setUser(null); setExpired(false);
    }}>Sign out</button></>}</nav></header>
    <main>{checking ? <p role="status" className="card">Checking your session…</p>
      : sessionError ? <section className="card" role="alert"><p>{sessionError}</p><button onClick={() => setRetry(n => n + 1)}>Retry session</button></section>
      : user ? <TradeWorkflow /> : <SignIn expired={expired} onSignIn={current => { setUser(current); setExpired(false); }} />}</main>
    <footer>TradeRank · Fantasy basketball trade comparison <span>Valuations provided by TradeBall</span></footer>
  </div>;
}
