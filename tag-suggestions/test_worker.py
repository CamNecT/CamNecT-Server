import unittest

from worker import suggest


class SuggestionTest(unittest.TestCase):
    def test_only_automatic_candidates_and_verbatim_evidence(self):
        def classify(row):
            self.assertEqual(row["body"], "서버 개발")
            return {
                "auto_candidates": [{"tag_id": 53, "evidence": "서버"}, {"tag_id": 107, "evidence": "없는 표현"}],
                "review_candidates": [{"tag_id": 65}],
            }
        self.assertEqual(suggest(classify, {"title": "개발자", "content": "서버 개발", "boardCode": "INFO"}),
                         [{"id": 53, "evidence": "서버"}, {"id": 107, "evidence": None}])

    def test_empty_or_invalid_input_never_calls_reader(self):
        for text in [" " * 20001, None]:
            with self.assertRaises(ValueError):
                suggest(None, {"title": "제목", "content": text, "boardCode": "QUESTION"})
        self.assertEqual(suggest(None, {"title": "", "content": "   ", "boardCode": "QUESTION"}), [])
