"""Run only on the developer workstation to review reusable routing predicates."""
import argparse
import concurrent.futures
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent
VIEWS = ("구직자 검색 의도와 원문 근거", "오분류와 반례", "취업팀 운영과 직무 분류")
MODEL = "gpt-6-astra"


def review(view, executable, policy):
    prompt = (f"역할은 {view} 독립 품질 판정자다. 서버 취업공고 규칙을 개발 중이다. "
              "아래 JSON의 각 rule은 모델 없는 판독기가 제안한 tag_id와 제목 regex 일치 시 적용한다. "
              "exclude_title은 모든 규칙에, exclude는 개별 규칙에 적용한다. quote_in_title은 "
              "제안의 원문 인용이 공고 제목에 있어야 한다는 뜻이다. 조건을 충족한 공고에 "
              "해당 태그를 자동부여 후보로 추천할 근거가 적절한지, 반례가 흔하면 거부한다. "
              "이는 재사용할 규칙의 사전 심사이며 신규 공고의 표나 실측 정확도가 아니다. "
              "도구를 쓰지 말고 각 규칙 ID마다 JSON {approve: boolean, reason: 한 문장}을 값으로 둔 "
              "JSON 객체 하나만 출력한다. 태그명 107 취업 84 인턴십 109 면접 79 서포터즈 77 공모전 "
              "19 디자인 46~64 모집 직무 90 멘토링 88 창업.\n" + policy.decode())
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "vote.json"
        proc = subprocess.run([executable, "exec", "--ephemeral", "--ignore-user-config",
                               "--skip-git-repo-check", "-s", "read-only", "--disable", "shell_tool",
                               "--disable", "apps", "--disable", "plugins", "--disable", "multi_agent",
                               "-c", 'web_search="disabled"', "-c", 'model_reasoning_effort="high"',
                               "-m", MODEL, "-C", tmp, "--json", "-o", str(out), "-"],
                              input=prompt.encode(), capture_output=True, cwd=tmp, timeout=360, check=True)
        events = [json.loads(line) for line in proc.stdout.splitlines() if line.strip()]
        session = next(e["thread_id"] for e in events if e["type"] == "thread.started")
        if not any(e["type"] == "turn.completed" for e in events):
            raise ValueError("Incomplete model turn")
        votes = json.loads(out.read_text(encoding="utf-8-sig"))
        if set(votes) != {r["id"] for r in json.loads(policy)["rules"]} or any(
                type(v.get("approve")) is not bool or not v.get("reason") for v in votes.values()):
            raise ValueError("Incomplete rule review")
        return {"view": view, "session": session, "model": MODEL, "rules": votes}


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--codex", required=True)
    ap.add_argument("--out", required=True, type=Path)
    args = ap.parse_args()
    policy = (ROOT / "server" / "policy.json").read_bytes()
    with concurrent.futures.ThreadPoolExecutor(3) as pool:
        reviews = list(pool.map(lambda view: review(view, args.codex, policy), VIEWS))
    if len({r["session"] for r in reviews}) != 3:
        raise ValueError("Expected three independent reviews")
    manifest = {"scope": "development_rule_review", "sha256": hashlib.sha256(policy).hexdigest(),
                "reviews": reviews}
    args.out.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
