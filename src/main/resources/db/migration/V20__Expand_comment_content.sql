-- Match the API's 5,000-character limit, including multibyte UTF-8 content.
-- V18/V19 are reserved by the alternative gifticon phone PRs #305/#304.
ALTER TABLE comments MODIFY COLUMN content TEXT NOT NULL;
