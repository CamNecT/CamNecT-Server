-- Preserve order-time contacts. V13 deleted old numbers; never infer them from current accounts.
ALTER TABLE gifticon_purchases ADD COLUMN buyer_phone VARCHAR(20) NULL;
ALTER TABLE gifticon_purchases ADD COLUMN recipient_phone VARCHAR(20) NULL;
