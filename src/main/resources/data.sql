-- Insert initial roles into role table
-- (ON CONFLICT DO NOTHING prevents duplicates if run multiple times)

INSERT INTO role (name) VALUES ('USER')       ON CONFLICT (name) DO NOTHING;
INSERT INTO role (name) VALUES ('HOST')       ON CONFLICT (name) DO NOTHING;
INSERT INTO role (name) VALUES ('MODERATOR')  ON CONFLICT (name) DO NOTHING;
INSERT INTO role (name) VALUES ('ADMIN')      ON CONFLICT (name) DO NOTHING;
