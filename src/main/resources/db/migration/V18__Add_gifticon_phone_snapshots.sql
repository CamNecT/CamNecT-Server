-- V13에서 삭제된 전화번호를 추정하거나 계정 정보로 소급해서 채우지 않는다.
-- 기존 주문은 NULL로 두고 관리자 엑셀에서 연락처 확인 대상으로 표시한다.
ALTER TABLE gifticon_purchases ADD COLUMN buyer_phone VARCHAR(20) NULL;
ALTER TABLE gifticon_purchases ADD COLUMN recipient_phone VARCHAR(20) NULL;
