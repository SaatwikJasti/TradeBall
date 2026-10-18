import type { Category, Evaluation } from './types';
const names: Record<Category, string> = { PTS: 'Points', REB: 'Rebounds', AST: 'Assists', STL: 'Steals', BLK: 'Blocks', THREE_PM: '3-pointers', FG_PCT: 'FG %', FT_PCT: 'FT %', TO: 'Turnovers' };
const format = (value: number) => value.toFixed(2);
const signed = (value: number) => `${value > 0 ? '+' : ''}${format(value)}`;
export function TradeResult({ result }: { result: Evaluation }) {
  return <section className="result" aria-labelledby="result-title">
    <div className="result-heading"><div><p className="eyebrow">EVALUATION {result.tradeId != null ? `#${result.tradeId}` : ''}</p>
      <h2 id="result-title">{result.verdict.replaceAll('_', ' ').toLowerCase()}</h2></div>
      <div className="score"><strong>{result.score}</strong><span>/ 100</span></div></div>
    <p>{result.explanation}</p>
    <dl className="totals"><div><dt>Outgoing value</dt><dd>{format(result.outgoingFantasyScore)}</dd></div>
      <div><dt>Incoming value</dt><dd>{format(result.incomingFantasyScore)}</dd></div></dl>
    <h3>Nine-category impact</h3>
    <p className="hint">Change is incoming minus outgoing. Impact is reported by the backend; fewer turnovers can be positive.</p>
    <div className="table-wrap" tabIndex={0} aria-label="Scrollable category results"><table>
      <caption className="sr-only">Actual category values returned by TradeBall</caption>
      <thead><tr><th scope="col">Category</th><th scope="col">Outgoing</th><th scope="col">Incoming</th><th scope="col">Change</th><th scope="col">Z-score change</th><th scope="col">Impact</th></tr></thead>
      <tbody>{result.categoryAnalysis.map(row => <tr key={row.category}>
        <th scope="row">{names[row.category]}</th><td>{format(row.outgoingValue)}</td><td>{format(row.incomingValue)}</td>
        <td>{signed(row.delta)}{row.category.endsWith('_PCT') ? ' pp' : ''}</td><td>{signed(row.zScoreDelta)}</td>
        <td><span className={`impact ${row.impact.toLowerCase()}`}>{row.impact.toLowerCase()}</span></td>
      </tr>)}</tbody></table></div>
    {!!result.signals.length && <p><strong>Signals: </strong>{result.signals.join(', ').replaceAll('_', ' ').toLowerCase()}</p>}
    {!!result.strengths.length && <p><strong>Strengths: </strong>{result.strengths.join(' · ')}</p>}
    {!!result.weaknesses.length && <p><strong>Weaknesses: </strong>{result.weaknesses.join(' · ')}</p>}
    <p className="hint">Model: {result.modelVersion}. Evaluation saved to your account’s trade history.</p>
  </section>;
}
