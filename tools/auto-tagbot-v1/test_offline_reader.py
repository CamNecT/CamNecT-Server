import unittest

from offline_reader import classify


class OfflineReaderTests(unittest.TestCase):
    def classify(self, title, body=""):
        row = {"title": title, "body": body}
        result = classify(row)
        self.assertEqual(result, classify(row))
        for records in result.values():
            for tag in records:
                self.assertTrue(tag["evidence"] in title or tag["evidence"] in body)
        return tuple([t["tag_id"] for t in result[k]] for k in ("suggestions", "role_index"))

    def test_optional_and_committed_conversion(self):
        title = "체험형 인턴 모집"
        self.assertEqual(self.classify(title, "우수자에 한해 정규직 전환 가능")[0], [84])
        self.assertEqual(self.classify(title, "전원 정규직으로 전환")[0], [84, 107])

    def test_parallel_track(self):
        self.assertEqual(self.classify("정규직 및 인턴 모집")[0], [84, 107])

    def test_duties_and_eligibility(self):
        tags, roles = self.classify("신입사원 채용", "담당 업무: 서비스 기획, 영상 편집. 우대 사항: 영업 인턴 경험")
        self.assertEqual(tags, [107, 47, 58])
        self.assertEqual(roles, [47, 58])

    def test_recruitment_presentation(self):
        self.assertEqual(self.classify("마케팅 채용 설명회", "모집 분야: 영업. 멘토 소개: 인사 매니저"), ([107], []))

    def test_curriculum_is_searchable_without_creating_hiring_roles(self):
        tags, roles = self.classify("데이터 분석 경진대회", "공모 주제: 생성형 AI")
        self.assertEqual(tags, [77, 51, 56])
        self.assertEqual(roles, [])

    def test_bracketed_recruitment_and_overlapping_role(self):
        tags, roles = self.classify("[인턴] [기업] 브랜드 마케팅")
        self.assertEqual(tags, [84, 59, 50])
        self.assertEqual(roles, [59, 50])

    def test_empty_and_preparation(self):
        self.assertEqual(self.classify(""), ([], []))
        self.assertEqual(self.classify("취업준비콘서트", "신청기간은 원문에서 확인해 주세요."), ([107], []))

    def test_role_index_can_exceed_public_limit(self):
        tags, roles = self.classify("모집 공고", "모집 직무: 영업, 재무, 마케팅, 백엔드, 프론트엔드")
        self.assertEqual(len(tags), 4)
        self.assertEqual(roles, [61, 63, 50, 53, 52])


if __name__ == "__main__":
    unittest.main()
