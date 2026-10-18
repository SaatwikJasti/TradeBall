"""Summarize raw benchmark reports with nearest-rank percentiles; stdlib only."""
import json
import math
import sys
from pathlib import Path


def percentile(values, fraction):
    values = sorted(values)
    return values[max(0, math.ceil(len(values) * fraction) - 1)]


for filename in sys.argv[1:]:
    report = json.loads(Path(filename).read_text(encoding="utf-8"))
    print(f"\n{filename}")
    print("scope,players_per_side,n,errors,error_rate,p50_ms,p95_ms,p99_ms,min_sql,max_sql,min_selects,max_selects,inserts")
    for scope in ("all", 1, 2):
        for size in (1, 3, 10):
            samples = [s for s in report["samples"] if s["playersPerSide"] == size
                       and (scope == "all" or s["round"] == scope)]
            latencies = [s["latencyMs"] for s in samples]
            errors = sum(s["status"] != 200 for s in samples)
            print(",".join(map(str, [scope, size, len(samples), errors, errors / len(samples),
                  *[round(percentile(latencies, p), 3) for p in (0.5, 0.95, 0.99)],
                  min(s["statements"] for s in samples), max(s["statements"] for s in samples),
                  min(s["selects"] for s in samples), max(s["selects"] for s in samples),
                  sorted(set(s["inserts"] for s in samples))])))
