ALTER TABLE gifticon_export_batches
    DROP CHECK chk_gifticon_export_delivery_status,
    ADD CONSTRAINT chk_gifticon_delivery_attempt_status
        CHECK (delivery_status IN ('READY', 'SENDING', 'SUBMITTED', 'FAILED', 'LEGACY_UNKNOWN'));
