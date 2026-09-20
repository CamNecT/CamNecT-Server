-- Match the API's 5,000-character limit, including multibyte UTF-8 content.
ALTER TABLE comments MODIFY COLUMN content TEXT NOT NULL;
