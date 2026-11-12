"""Validate three independent reads and return at most four source-supported tags."""
import argparse
import json
from collections import Counter
from pathlib import Path
from select_tags import HERE, decode, select, NONCANONICAL

def predict(row, responses, allowed):
    ballots = [select(row, raw, allowed) for raw in responses]
    votes = Counter()
    ranks = Counter()
    evidence = {}
    for ballot in ballots:
        for rank,(tag,quote) in enumerate(ballot):
            votes[tag] += 1
            ranks[tag] += 4-rank
            evidence.setdefault(tag,quote)
    winners=sorted((tag for tag,n in votes.items() if n>=2),key=lambda t:(-votes[t],-ranks[t],t))
    return [[tag,evidence[tag]] for tag in winners[:4]]

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('source',type=Path);p.add_argument('output',type=Path);p.add_argument('reads',type=Path,nargs=3)
    a=p.parse_args()
    rows=[decode(line) for line in a.source.read_text(encoding='utf-8-sig').split('\n') if line.strip()]
    keys={str(r['id']) for r in rows}
    raw=[decode(f.read_text(encoding='utf-8-sig')) for f in a.reads]
    if len(keys)!=len(rows) or any(k.keys()!=keys for k in raw):raise ValueError('Source/reader ID mismatch')
    allowed=set(map(int,decode((HERE/'dictionary.json').read_text(encoding='utf8'))))-NONCANONICAL
    results={str(r['id']):predict(r,[s[str(r['id'])] for s in raw],allowed) for r in rows}
    a.output.write_text(json.dumps(results,ensure_ascii=False,indent=1),encoding='utf8')
if __name__=='__main__':main()

