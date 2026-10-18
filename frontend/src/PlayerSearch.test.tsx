import { describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PlayerSearch } from './PlayerSearch';
import type { Player } from './types';
const player = (id: number, fullName: string) => ({ id, fullName, team: 'FIX', position: 'PG' } as Player);
describe('player search', () => {
  it('ignores a slow response for an older query', async () => {
    const user = userEvent.setup();
    let old!: (players: Player[]) => void;
    const find = vi.fn().mockImplementationOnce(() => new Promise(resolve => { old = resolve; }))
      .mockResolvedValueOnce([player(2, 'New Player')]);
    render(<PlayerSearch label="Find players" find={find} selected={[]} onSelect={vi.fn()} />);
    await user.type(screen.getByLabelText('Find players'), 'old');
    await vi.waitFor(() => expect(find).toHaveBeenCalledWith('old'));
    await user.clear(screen.getByLabelText('Find players'));
    await user.type(screen.getByLabelText('Find players'), 'new');
    expect(await screen.findByText('New Player')).toBeInTheDocument();
    await act(async () => old([player(1, 'Old Player')]));
    expect(screen.queryByText('Old Player')).not.toBeInTheDocument();
    expect(screen.getByText('New Player')).toBeInTheDocument();
  });
  it('shows an API error, supports retry, and renders an empty result', async () => {
    const user = userEvent.setup();
    const find = vi.fn().mockRejectedValueOnce(new Error('Network request failed')).mockResolvedValueOnce([]);
    render(<PlayerSearch label="Find players" find={find} selected={[]} onSelect={vi.fn()} />);
    expect(screen.getByText(/Type at least 2/)).toBeInTheDocument();
    await user.type(screen.getByLabelText('Find players'), 'missing');
    expect(await screen.findByRole('alert')).toHaveTextContent('Network request failed');
    await user.click(screen.getByRole('button', { name: 'Retry search' }));
    expect(await screen.findByText('No players found.')).toBeInTheDocument();
  });
});
