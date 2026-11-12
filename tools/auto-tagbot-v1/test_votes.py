import unittest

from votes import VIEWS, decide, parse_json, prompts_for, validate_ballot


def response(row, items, roles=(), route="ordinary"):
    return {str(row["id"]): {"route": route, "items": list(items), "roles": list(roles)}}


class CandidateRoutingTest(unittest.TestCase):
    def test_real_4084_job_concert(self):
        row = {
            "id": "1211892",
            "title": "[고용노동부] 2026 취업준비콘서트 일자리톡톡 유한양행",
            "body": "신청기간은 원문에서 확인해 주세요.\n\n\n\n원문 및 관련 자료\n"
                    "https://job.konkuk.ac.kr/bbs/job/4084/1211892/artclView.do\n"
                    "https://job.konkuk.ac.kr/CrossEditor/binary/images/000410/20261112100442586_84L2U29U.png",
        }
        # All three archived Astra responses used this exact phrase.
        ballots = {view: response(row, [[107, "취업준비콘서트"]]) for view in VIEWS}
        result = decide(row, ballots)
        self.assertEqual(result["public_tag_ids"], [107])
        self.assertEqual(result["auto_candidates"][0]["evidence"],
                         {view: "취업준비콘서트" for view in VIEWS})
        self.assertEqual(result["review_candidates"], [])
        self.assertEqual(result["role_index"], [])
        self.assertEqual(len(set(prompts_for(row).values())), 3)

    def test_all_two_vote_candidates_survive_public_cap(self):
        row = {"id": "over-cap", "title": "채용", "body": "마케팅 데이터분석 프론트엔드 백엔드 영업 인사"}
        items = [[50, "마케팅"], [51, "데이터분석"], [52, "프론트엔드"],
                 [53, "백엔드"], [61, "영업"], [62, "인사"]]
        ballots = {
            "spec": response(row, items[:4]),
            "neutral": response(row, items[:2] + items[4:]),
            "search": response(row, items[2:]),
        }
        result = decide(row, ballots)
        self.assertEqual(result["auto_candidates"], [])
        self.assertEqual(len(result["public_tag_ids"]), 4)
        self.assertEqual({item["tag_id"] for item in result["review_candidates"]},
                         {tag for tag, _ in items})

    def test_unanimous_and_two_of_three_with_optional_intern_conversion(self):
        row = {"id": 2, "title": "영업 인턴 모집", "body": "정규직 전환 가능. 마케팅 우대."}
        common = [[84, "인턴 모집"], [107, "정규직 전환 가능"]]
        # The frozen selector removes the optional conversion from every eligible ballot.
        ballots = {
            "spec": response(row, common + [[50, "마케팅"]], [[61, "영업"]], "conversion"),
            "neutral": response(row, common, [[61, "영업"]], "intern"),
            "search": response(row, common, route="intern"),
        }
        result = decide(row, ballots)
        self.assertEqual([t["tag_id"] for t in result["auto_candidates"]], [84])
        self.assertEqual([t["tag_id"] for t in result["review_candidates"]], [61])
        self.assertEqual(result["role_index"][0]["evidence"], {"spec": "영업", "neutral": "영업"})

    def test_incomplete_and_malformed_ballots_fail_closed(self):
        row = {"id": 7, "title": "멘토링 모집", "body": ""}
        valid = response(row, [[90, "멘토링"]])
        for ballots in ({}, {"spec": valid, "search": valid},
                        {"spec": valid, "neutral": valid, "other": valid}):
            with self.subTest(ballots=ballots), self.assertRaises(ValueError):
                decide(row, ballots)
        invalid = [
            {}, {"8": valid["7"]}, {"7": {"route": "ordinary", "items": []}},
            response(row, [[117, "멘토링"]]), response(row, [[90, "근거 없음"]]),
            response(row, [[90, "멘토링"], [90, "멘토링 모집"]]),
        ]
        for ballot in invalid:
            with self.subTest(ballot=ballot), self.assertRaises(ValueError):
                validate_ballot(row, "spec", ballot)
        with self.assertRaises(ValueError):
            parse_json('{"7":{},"7":{}}')


if __name__ == "__main__":
    unittest.main()
