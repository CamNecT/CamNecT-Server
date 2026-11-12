"""Employment-only reader. Replay three Astra reads, or run three reads on a local model."""
import argparse
import hashlib
import json
import sys
from pathlib import Path
from urllib.request import build_opener, HTTPRedirectHandler, ProxyHandler, Request
P=Path(__file__).resolve().parent
sys.path.insert(0,str(P/'model'))
from consensus import predict
from select_tags import decode, NONCANONICAL

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self,*args):
        raise RuntimeError("Local model endpoint redirected")

def local_read(row,prompt,model,port,client):
    messages=[{'role':'system','content':prompt},{'role':'user','content':json.dumps(row,ensure_ascii=False)}]
    req=Request(f'http://127.0.0.1:{port}/v1/chat/completions',data=json.dumps({'model':model,'messages':messages,'stream':False}).encode(),headers={'Content-Type':'application/json'})
    with client.open(req,timeout=300) as res:
        raw=decode(decode(res.read())['choices'][0]['message']['content'])
    if raw.keys()!={str(row['id'])}:raise ValueError('Expected only the supplied notice ID')
    return raw[str(row['id'])]

def main():
    args=argparse.ArgumentParser(description=__doc__)
    args.add_argument('source',type=Path);args.add_argument('out',type=Path)
    g=args.add_mutually_exclusive_group(required=True)
    g.add_argument('--reads',nargs=3,type=Path);g.add_argument('--model')
    args.add_argument('--port',type=int,default=8000);a=args.parse_args()
    for frozen in decode((P/'run-lock.json').read_text(encoding='utf8'))['model']:
        if hashlib.sha256((P/frozen['file']).read_bytes()).hexdigest()!=frozen['sha256']:raise ValueError('Model resources changed')
    rows=[{k:r[k] for k in ('id','title','body')} for r in (decode(l) for l in a.source.read_text(encoding='utf-8-sig').split('\n') if l.strip())]
    keys={str(r['id']) for r in rows}
    if not rows or len(keys)!=len(rows):raise ValueError('Missing or duplicate notice IDs')
    stored=[decode(f.read_text(encoding='utf-8-sig')) for f in a.reads] if a.reads else []
    if any(s.keys()!=keys for s in stored):raise ValueError('Missing or extra notice predictions')
    prompt=(P/'model/prompt.txt').read_text(encoding='utf8')
    allowed=set(map(int,decode((P/'model/dictionary.json').read_text(encoding='utf8'))))-NONCANONICAL
    client=build_opener(ProxyHandler({}),NoRedirect())
    results={}
    for r in rows:
        i=str(r['id'])
        ballot=[s[i] for s in stored] if stored else [local_read(r,prompt,a.model,a.port,client) for _ in range(3)]
        results[i]=predict(r,ballot,allowed)
    a.out.write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf8')
if __name__=='__main__':main()

