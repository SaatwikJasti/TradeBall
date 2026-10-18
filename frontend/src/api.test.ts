import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from './api';
beforeEach(() => { localStorage.clear(); window.TRADEBALL_API_BASE = '/api/v1'; window.onTradeBallUnauthorized = undefined; vi.restoreAllMocks(); });
describe('existing JavaScript API integration', () => {
  it('uses the existing JWT keys and sends IDs, roster scope, and Bearer authorization', async () => {
    const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ score: 70 }), { status: 200 }));
    api.setToken('local-test-token');
    await api.evaluateTrade([3], [1], 7);
    expect(localStorage.getItem('tradeball.accessToken')).toBe('local-test-token');
    expect(fetch).toHaveBeenCalledWith('/api/v1/trades/evaluate', expect.objectContaining({
      method: 'POST', headers: expect.objectContaining({ Authorization: 'Bearer local-test-token' }),
      body: JSON.stringify({ incomingPlayerIds: [3], outgoingPlayerIds: [1], rosterId: 7 })
    }));
  });
  it('clears the shared session and notifies React after a protected request returns 401', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ message: 'Authentication failed' }), { status: 401 }));
    const expired = vi.fn(); window.onTradeBallUnauthorized = expired;
    api.setToken('expired'); localStorage.setItem('tradeball.user', '{}');
    await expect(api.me()).rejects.toMatchObject({ status: 401 });
    expect(api.getToken()).toBeNull(); expect(localStorage.getItem('tradeball.user')).toBeNull(); expect(expired).toHaveBeenCalledOnce();
  });
  it('does not classify incorrect login credentials as an expired existing session', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ message: 'Authentication failed' }), { status: 401 }));
    const expired = vi.fn(); window.onTradeBallUnauthorized = expired;
    await expect(api.login('tester@example.com', 'wrong')).rejects.toMatchObject({ status: 401 });
    expect(expired).not.toHaveBeenCalled();
  });
});
