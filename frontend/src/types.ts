export interface Player {
  id: number; externalId: string; firstName: string; lastName: string; fullName: string;
  position: string | null; team: string | null; age: number | null; active: boolean;
}
export interface User { id: number; email: string; displayName: string; role: 'USER' | 'ADMIN'; createdAt: string }
export interface Auth { accessToken: string; tokenType: string; user: User }
export interface Roster { id: number; name: string; players: Player[]; createdAt: string; updatedAt: string }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }
export type Category = 'PTS' | 'REB' | 'AST' | 'STL' | 'BLK' | 'THREE_PM' | 'FG_PCT' | 'FT_PCT' | 'TO';
export interface CategoryResult { category: Category; incomingValue: number; outgoingValue: number; delta: number; zScoreDelta: number; impact: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE' }
export interface Evaluation {
  tradeId: number | null; score: number; verdict: 'GREAT_TRADE' | 'FAIR_TRADE' | 'POOR_TRADE';
  incomingFantasyScore: number; outgoingFantasyScore: number; categoryAnalysis: CategoryResult[];
  strengths: string[]; weaknesses: string[]; signals: string[]; explanation: string; modelVersion: string;
}
export interface ApiFailure extends Error { status?: number; code?: string; body?: unknown }
export interface ExistingApi {
  login(email: string, password: string): Promise<Auth>;
  me(): Promise<User>;
  getToken(): string | null;
  setToken(token: string | null): void;
  setUser(user: User | null): void;
  listRosters(): Promise<Roster[]>;
  getRoster(id: number): Promise<Roster>;
  createRoster(name: string): Promise<Roster>;
  addRosterPlayer(rosterId: number, playerId: number): Promise<Roster>;
  searchPlayers(query: string, page?: number, size?: number): Promise<Page<Player>>;
  evaluateTrade(incoming: number[], outgoing: number[], rosterId?: number): Promise<Evaluation>;
}
