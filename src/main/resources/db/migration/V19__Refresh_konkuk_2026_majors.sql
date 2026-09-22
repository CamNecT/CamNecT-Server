-- Refresh Konkuk University's 2026 Seoul undergraduate admissions catalog.
-- Historic majors remain for existing profile/education references and are marked inactive.
CREATE TEMPORARY TABLE konkuk_2026_major_source (
    sort_order INT NOT NULL PRIMARY KEY,
    major_name_kor VARCHAR(100) NOT NULL UNIQUE,
    major_name_eng VARCHAR(100) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO konkuk_2026_major_source (sort_order, major_name_kor, major_name_eng) VALUES
    (1, '국어국문학과', NULL),
    (2, '영어영문학과', NULL),
    (3, '중어중문학과', NULL),
    (4, '철학과', NULL),
    (5, '사학과', NULL),
    (6, '지리학과', NULL),
    (7, '미디어커뮤니케이션학과', NULL),
    (8, '문화콘텐츠학과', NULL),
    (9, '문과대학자유전공학부', NULL),
    (10, '수학과', NULL),
    (11, '물리학과', NULL),
    (12, '화학과', NULL),
    (13, '이과대학자유전공학부', NULL),
    (14, '건축학부', NULL),
    (15, '사회환경공학부', NULL),
    (16, '기계·로봇·자동차공학부', NULL),
    (17, '전기전자공학부', NULL),
    (18, '화공·생명·에너지공학부', 'Chemical, Biomolecular & Energy Engineering'),
    (19, '컴퓨터공학부', NULL),
    (20, '재료공학과', NULL),
    (21, '항공우주·모빌리티공학과', NULL),
    (22, '생물공학과', NULL),
    (23, '산업공학과', NULL),
    (24, '산업경영융합학부', 'School of Interdisciplinary Industrial Management'),
    (25, '공과대학자유전공학부', NULL),
    (26, '정치외교학과', NULL),
    (27, '경제학과', NULL),
    (28, '행정학과', NULL),
    (29, '국제무역학과', NULL),
    (30, '응용통계학과', NULL),
    (31, '사회과학대학융합전공학부', NULL),
    (32, '경영학과', NULL),
    (33, '기술경영학과', NULL),
    (34, '부동산학과', NULL),
    (35, '첨단바이오공학부', NULL),
    (36, '시스템생명공학과', NULL),
    (37, '융합생명공학과', NULL),
    (38, '융합과학기술원자유전공학부', NULL),
    (39, '동물자원·식품과학·유통학부', NULL),
    (40, '환경보건·산림조경학부', NULL),
    (41, '생명과학특성학과', NULL),
    (42, '식량자원과학과', NULL),
    (43, '생명과학대학자유전공학부', NULL),
    (44, '수의예과', NULL),
    (45, '커뮤니케이션디자인학과', NULL),
    (46, '산업디자인학과', NULL),
    (47, '의상디자인학과', NULL),
    (48, '리빙디자인학과', NULL),
    (49, '현대미술학과', NULL),
    (50, '영상학과', NULL),
    (51, '매체연기학과', NULL),
    (52, '일어교육과', NULL),
    (53, '수학교육과', NULL),
    (54, '체육교육과', NULL),
    (55, '음악교육과', NULL),
    (56, '교육공학과', NULL),
    (57, '영어교육과', NULL),
    (58, 'KU자유전공학부', NULL),
    (59, '문화미디어학과', 'Culture & Media'),
    (60, '국제통상비즈니스학과', 'International Commerce & Business'),
    (61, 'AI디자인학과', 'AI Design'),
    (62, '컴퓨터소프트웨어학과', 'Computer Software');

-- This is a direct 2025-to-2026 rename; retain the major_id and references.
UPDATE majors m
JOIN institutions i ON i.institution_id = m.institution_id
LEFT JOIN majors current_name ON current_name.institution_id = m.institution_id
    AND current_name.major_name_kor = '화공·생명·에너지공학부'
SET m.major_name_kor = '화공·생명·에너지공학부',
    m.major_name_eng = 'Chemical, Biomolecular & Energy Engineering',
    m.updated_at = NOW(6)
WHERE i.institution_name_kor = '건국대학교'
  AND m.major_name_kor = '화공학부'
  AND current_name.major_id IS NULL;

-- Existing names and IDs are preserved. New rows use stable, catalog-specific codes.
INSERT INTO majors (created_at, updated_at, is_active, major_code, major_name_eng,
                    major_name_kor, sort_order, institution_id)
SELECT NOW(6), NOW(6), b'1', CONCAT('KU2026-', LPAD(t.sort_order, 3, '0')),
       COALESCE(t.major_name_eng, t.major_name_kor), t.major_name_kor,
       t.sort_order, i.institution_id
FROM institutions i
CROSS JOIN konkuk_2026_major_source t
WHERE i.institution_name_kor = '건국대학교'
  AND NOT EXISTS (
      SELECT 1 FROM majors m
      WHERE m.institution_id = i.institution_id AND m.major_name_kor = t.major_name_kor
  );

UPDATE majors m
JOIN institutions i ON i.institution_id = m.institution_id
JOIN konkuk_2026_major_source t ON t.major_name_kor = m.major_name_kor
SET m.major_name_eng = t.major_name_eng,
    m.updated_at = NOW(6)
WHERE i.institution_name_kor = '건국대학교'
  AND t.major_name_eng IS NOT NULL;

UPDATE majors m
JOIN institutions i ON i.institution_id = m.institution_id
LEFT JOIN konkuk_2026_major_source t ON t.major_name_kor = m.major_name_kor
SET m.is_active = (t.major_name_kor IS NOT NULL),
    m.sort_order = COALESCE(t.sort_order, m.sort_order),
    m.updated_at = NOW(6)
WHERE i.institution_name_kor = '건국대학교';

DROP TEMPORARY TABLE konkuk_2026_major_source;
