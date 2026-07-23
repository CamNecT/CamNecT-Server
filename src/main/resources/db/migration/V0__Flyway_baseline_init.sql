-- V0__Flyway_baseline_init.sql
-- Baseline 마이그레이션: 기존 운영 DB의 스키마를 V0로 기록
-- 이 파일은 Users 테이블이 이미 존재하는 상태를 "V0 완료"로 표시
-- 따라서 실제 SQL 실행은 하지 않고, 단순 플레이스홀더로 사용됨

-- Flyway가 이 파일을 인식하고 flyway_schema_history에 기록하면,
-- V1, V2, V3는 정상적으로 순차 실행됨
SELECT 1;
