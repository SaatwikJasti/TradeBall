import { useEffect, useState } from 'react';
import { message } from './api';
import { selectionError } from './selection';
import type { Player } from './types';

interface Props {
  label: string; find: (query: string) => Promise<Player[]>;
  selected: Player[]; opposite?: Player[]; disabled?: boolean; minimum?: number;
  onSelect: (player: Player) => void; action?: 'select' | 'roster';
}
export function PlayerSearch({ label, find, selected, opposite = [], disabled = false,
  minimum = 2, onSelect, action = 'select' }: Props) {
  const [query, setQuery] = useState('');
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    setPlayers([]); setError('');
    if (query.trim().length < minimum) { setLoading(false); return; }
    setLoading(true);
    const timer = setTimeout(() => {
      find(query.trim()).then(result => { if (active) setPlayers(result); })
        .catch(reason => { if (active) setError(message(reason)); })
        .finally(() => { if (active) setLoading(false); });
    }, minimum === 0 ? 0 : 250);
    // Old responses must not replace results for a more recent query.
    return () => { active = false; clearTimeout(timer); };
  }, [query, find, minimum, retry]);
  return <div className="search">
    <label>{label}<input type="search" value={query} disabled={disabled}
      onChange={event => setQuery(event.target.value)} placeholder={minimum ? 'Name, team, or position' : 'Filter roster players'} /></label>
    <div aria-live="polite">
      {query.trim().length < minimum ? <p className="hint">Type at least {minimum} characters to search.</p>
        : loading ? <p role="status">Searching players…</p>
        : error ? <div role="alert"><p>{error}</p><button type="button" onClick={() => setRetry(n => n + 1)}>Retry search</button></div>
        : !players.length ? <p className="hint">No players found.</p>
        : <ul className="search-results">{players.map(player => {
          const reason = selectionError(player, selected, opposite);
          return <li key={player.id}><div><strong>{player.fullName}</strong><small>{player.team || '—'} · {player.position || '—'}</small></div>
            <button type="button" disabled={disabled || !!reason} onClick={() => onSelect(player)}
              aria-label={action === 'roster' ? `Add ${player.fullName} to roster` : `Select ${player.fullName}`}>
              {reason || (action === 'roster' ? 'Add to roster' : 'Select')}</button></li>;
        })}</ul>}
    </div>
    {players.length >= 20 && <p className="hint">Showing the first 20 matches. Refine your search to find a specific player.</p>}
  </div>;
}
