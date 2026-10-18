// Bundle the existing client rather than introduce a second fetch/JWT implementation.
import '../../www/js/tradeball-api.js';
import type { ApiFailure, ExistingApi } from './types';

declare global {
  interface Window {
    TradeBallApi: ExistingApi;
    TRADEBALL_API_BASE?: string;
    onTradeBallUnauthorized?: () => void;
    Capacitor?: { getPlatform?: () => string };
  }
}
const configured = import.meta.env.VITE_API_BASE as string | undefined;
if (configured) window.TRADEBALL_API_BASE = configured;
else if (!window.TRADEBALL_API_BASE && window.Capacitor?.getPlatform?.() !== 'android') {
  window.TRADEBALL_API_BASE = import.meta.env.DEV ? '/api/v1' : 'http://localhost:18080/api/v1';
}
export const api = window.TradeBallApi;
export function message(error: unknown): string {
  const failure = error as ApiFailure;
  if (failure?.status === 401) return 'Your session expired. Sign in again to continue.';
  if (failure?.status === 400) return `Invalid request: ${failure.message}`;
  return failure?.message || 'Something went wrong. Please try again.';
}
