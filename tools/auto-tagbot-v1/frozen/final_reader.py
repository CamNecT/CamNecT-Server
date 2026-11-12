"""Read-only employment tagger. Three local model reads, or replay saved Astra responses."""
import argparse,hashlib,json,sys
from pathlib import Path
from collections import Counter
from urllib.request import build_opener,ProxyHandler
from run_job import local_read, NoRedirect
import importlib.util

ROOT=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location("employment_rules_v2",ROOT/'model2/select_tags.py')
rules=importlib.util.module_from_spec(spec);spec.loader.exec_module(rules)
PROMPTS=('model2/prompt.txt','prompt-neutral.txt','prompt-search.txt')

def support(ballots,limit,minimum=2):
    votes=Counter();points=Counter();evidence={}
    for ballot in ballots:
        for rank,(tag,quote) in enumerate(ballot):
            votes[tag]+=1;points[tag]+=len(ballot)-rank if limit is None else 4-rank
            evidence.setdefault(tag,quote)
    tags=sorted((t for t,n in votes.items() if n>=minimum),key=lambda t:(-votes[t],-points[t],t))
    return [[tag,evidence[tag]] for tag in tags[:limit]]

def merge(row,reads,allowed,minimum=2):
    ballots=[rules.choose(row,v,allowed) for v in reads]
    tags=support([one[0] for one in ballots],4,minimum)
    roles=support([one[1] for one in ballots],None)
    return {'tags':tags,'roles':roles}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('source',type=Path);p.add_argument('output',type=Path)
    group=p.add_mutually_exclusive_group(required=True);group.add_argument('--reads',type=Path,nargs=3);group.add_argument('--model')
    p.add_argument('--roles-out',type=Path);p.add_argument('--audit-output',type=Path);p.add_argument('--port',type=int,default=8000)
    p.add_argument('--minimum-votes',type=int,choices=[2,3],default=2);args=p.parse_args()
    lock=rules.decode((ROOT/'final-reader-lock.json').read_text(encoding='utf8'))
    for name,expected in lock['model'].items():
        if hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=expected:raise ValueError('Reader resources differ from frozen recipe')
    rows=[{k:r[k] for k in ['id','title','body']} for r in (rules.decode(s) for s in args.source.read_text(encoding='utf-8-sig').split('\n') if s.strip())]
    keys={str(r['id']) for r in rows}
    if not rows or len(keys)!=len(rows):raise ValueError('Invalid or duplicate source IDs')
    saved=[rules.decode(f.read_text(encoding='utf-8-sig')) for f in args.reads] if args.reads else []
    if any(d.keys()!=keys for d in saved):raise ValueError('Missing or extra reader IDs')
    allowed=set(map(int,rules.decode((ROOT/'model2/dictionary.json').read_text(encoding='utf8'))))-rules.NONCANONICAL
    prompts=[(ROOT/name).read_text(encoding='utf8') for name in PROMPTS]
    client=build_opener(ProxyHandler({}),NoRedirect());results={}
    for r in rows:
        i=str(r['id'])
        raw=[v[i] for v in saved] if saved else [local_read(r,s,args.model,args.port,client) for s in prompts]
        results[i]=merge(r,raw,allowed,args.minimum_votes)
    for dest,data in [(args.output,{i:r['tags'] for i,r in results.items()}),(args.roles_out,{i:r['roles'] for i,r in results.items()}),(args.audit_output,results)]:
        if dest:dest.write_text(json.dumps(data,ensure_ascii=False,indent=1),encoding='utf8')
if __name__=='__main__':main()

