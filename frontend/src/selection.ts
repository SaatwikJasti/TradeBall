import type { Player, Roster } from './types';
export function selectionError(player: Player, own: Player[], opposite: Player[]): string | null {
  if (own.some(p => p.id === player.id)) return 'Already selected';
  if (opposite.some(p => p.id === player.id)) return 'Selected on the other side';
  return null;
}
export function tradeError(outgoing: Player[], incoming: Player[], roster: Roster | null): string | null {
  if (!roster) return 'Create or select a roster first.';
  if (!outgoing.length || !incoming.length) return 'Select at least one player on each side.';
  const all = [...outgoing, ...incoming].map(p => p.id);
  if (new Set(all).size !== all.length) return 'Each player can appear only once in a trade.';
  if (outgoing.some(p => !roster.players.some(r => r.id === p.id))) return 'Outgoing players must belong to your selected roster.';
  return null;
}
