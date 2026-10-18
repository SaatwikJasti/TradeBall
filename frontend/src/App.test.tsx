import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Evaluation, Player, Roster, User } from './types';
const mocks = vi.hoisted(() => ({
  login: vi.fn(), me: vi.fn(), getToken: vi.fn(), setToken: vi.fn(), setUser: vi.fn(),
  listRosters: vi.fn(), createRoster: vi.fn(), addRosterPlayer: vi.fn(),
  searchPlayers: vi.fn(), evaluateTrade: vi.fn()
}));
vi.mock('./api', () => ({ api: mocks, message: (error: { status?: number; message: string }) =>
  error.status === 400 ? `Invalid request: ${error.message}` : error.message }));
import { App } from './App';

const player = (id: number, fullName: string): Player => ({ id, fullName, externalId: String(id),
  firstName: fullName.split(' ')[0], lastName: fullName.split(' ')[1], team: 'FIX', position: 'PG', age: 25, active: true });
const hart = player(1, 'Josh Hart');
const curry = player(2, 'Stephen Curry');
const jokic = player(3, 'Nikola Jokic');
const account: User = { id: 1, email: 'tester@example.com', displayName: 'Tester', role: 'USER', createdAt: '' };
const roster: Roster = { id: 7, name: 'My Team', players: [hart, curry], createdAt: '', updatedAt: '' };
const result: Evaluation = { tradeId: 99, score: 67, verdict: 'GREAT_TRADE', incomingFantasyScore: 8.3,
  outgoingFantasyScore: 2.1, categoryAnalysis: [{ category: 'TO', incomingValue: 1, outgoingValue: 2,
    delta: -1, zScoreDelta: 0.8, impact: 'POSITIVE' }], explanation: 'Explanation returned by the API.',
  modelVersion: 'fixture-model', signals: [], strengths: [], weaknesses: [] };

beforeEach(() => {
  vi.resetAllMocks();
  mocks.getToken.mockReturnValue(null);
  mocks.login.mockResolvedValue({ accessToken: 'test-token', tokenType: 'Bearer', user: account });
  mocks.listRosters.mockResolvedValue([roster]);
  mocks.searchPlayers.mockImplementation(async (query: string) => ({ content: query.toLowerCase().includes('hart') ? [hart] : [jokic] }));
  mocks.evaluateTrade.mockResolvedValue(result);
});
async function signIn() {
  const user = userEvent.setup();
  render(<App />);
  await user.type(screen.getByLabelText('Email'), account.email);
  await user.type(screen.getByLabelText('Password'), 'Password1!');
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  await screen.findByRole('heading', { name: 'Your roster' });
  return user;
}
async function selectBoth(user: ReturnType<typeof userEvent.setup>) {
  await user.click(await screen.findByRole('button', { name: 'Select Josh Hart' }));
  await user.type(screen.getByLabelText('Search incoming players'), 'Jokic');
  await user.click(await screen.findByRole('button', { name: 'Select Nikola Jokic' }));
}

describe('trade comparison', () => {
  it('validates selections, prevents duplicates/overlap, sends roster-scoped IDs, and clears stale results', async () => {
    const user = await signIn();
    await user.click(screen.getByRole('button', { name: 'Evaluate trade' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Select at least one player on each side');
    expect(mocks.evaluateTrade).not.toHaveBeenCalled();
    await selectBoth(user);
    expect(screen.getByRole('button', { name: 'Select Josh Hart' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Select Nikola Jokic' })).toBeDisabled();
    await user.clear(screen.getByLabelText('Search incoming players'));
    await user.type(screen.getByLabelText('Search incoming players'), 'Hart');
    expect(await screen.findByText('Selected on the other side')).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Evaluate trade' }));
    await screen.findByRole('heading', { name: /great trade/i });
    expect(mocks.evaluateTrade).toHaveBeenCalledWith([3], [1], 7);
    expect(screen.getByText('67')).toBeInTheDocument();
    expect(screen.getByText('Explanation returned by the API.')).toBeInTheDocument();
    expect(screen.getByRole('row', { name: /Turnovers/ })).toHaveTextContent(/-1\.00\s*\+0\.80\s*positive/);
    await user.click(screen.getByRole('button', { name: 'Remove Josh Hart from outgoing' }));
    expect(screen.queryByRole('heading', { name: /great trade/i })).not.toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: /Outgoing players/ })).getByRole('button', { name: 'Select Josh Hart' })).toBeEnabled();
  });
  it('shows pending and API validation states, preserves selections, and allows retry', async () => {
    const user = await signIn(); await selectBoth(user);
    let reject!: (reason: unknown) => void;
    mocks.evaluateTrade.mockImplementationOnce(() => new Promise((_, no) => { reject = no; }));
    await user.click(screen.getByRole('button', { name: 'Evaluate trade' }));
    expect(screen.getByRole('status')).toHaveTextContent('Evaluating your trade');
    expect(screen.getByRole('button', { name: 'Evaluating…' })).toBeDisabled();
    reject({ status: 400, message: 'Roster changed; refresh your players.' });
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid request: Roster changed');
    expect(screen.getByRole('button', { name: 'Remove Josh Hart from outgoing' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Evaluate trade' }));
    await screen.findByRole('heading', { name: /great trade/i });
  });
  it('returns to sign-in and removes private selections when the client reports expiry', async () => {
    const user = await signIn(); await selectBoth(user);
    mocks.evaluateTrade.mockImplementationOnce(async () => {
      window.onTradeBallUnauthorized?.(); throw { status: 401, message: 'Expired' };
    });
    await user.click(screen.getByRole('button', { name: 'Evaluate trade' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Your session expired');
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.queryByText('Josh Hart')).not.toBeInTheDocument();
  });
  it('clears selections when switching rosters', async () => {
    mocks.listRosters.mockResolvedValue([roster, { ...roster, id: 8, name: 'Other Team', players: [jokic] }]);
    const user = await signIn(); await selectBoth(user);
    await user.selectOptions(screen.getByLabelText('Selected roster'), '8');
    expect(screen.getByText('No outgoing players selected.')).toBeInTheDocument();
    expect(screen.getByText('No incoming players selected.')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Select Nikola Jokic' })).toBeEnabled();
  });
  it('lets an account with no roster create one and add a real player before outgoing selection', async () => {
    mocks.listRosters.mockResolvedValue([]);
    mocks.createRoster.mockResolvedValue({ ...roster, players: [] });
    mocks.addRosterPlayer.mockResolvedValue({ ...roster, players: [jokic] });
    const user = await signIn();
    expect(screen.getByRole('button', { name: 'Evaluate trade' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Create roster' }));
    expect(await screen.findByText(/Your roster is empty/)).toBeInTheDocument();
    await user.type(screen.getByLabelText('Search players to add to roster'), 'Jokic');
    await user.click(await screen.findByRole('button', { name: 'Add Nikola Jokic to roster' }));
    expect(mocks.addRosterPlayer).toHaveBeenCalledWith(7, 3);
    await waitFor(() => expect(screen.getByLabelText('Selected roster')).toHaveTextContent('1 player'));
    expect(await screen.findByRole('button', { name: 'Select Nikola Jokic' })).toBeEnabled();
  });
});
