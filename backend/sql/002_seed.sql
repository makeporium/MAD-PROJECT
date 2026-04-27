USE mad_app;

INSERT INTO community_rooms (name, description) VALUES
('Night Feeding Support', 'Share tips and support for late-night feeding'),
('First Week Postpartum', 'For mothers in their first week after delivery'),
('Feeling Lonely', 'A safe space to share when you feel isolated'),
('Partner Support', 'Navigating relationships during postpartum'),
('Self-Care Corner', 'Tips and encouragement for taking care of yourself');

INSERT INTO recommendations (title, category, description, match_score) VALUES
('10-minute breathing routine', 'Self-care', 'A simple guided breathing routine for stressful moments.', 94),
('Sleep recovery tips', 'Sleep', 'Habits to improve rest in short windows.', 90),
('Healthy feeding patterns', 'Feeding', 'Practical suggestions around feeding stress.', 86);

INSERT INTO events (title, description, event_date, mode, join_link) VALUES
('Live postpartum support session', 'Weekly live support with a counselor', DATE_ADD(NOW(), INTERVAL 2 DAY), 'online', 'https://meet.example.com/postpartum'),
('Mindfulness mini workshop', 'Calming exercises and anxiety management', DATE_ADD(NOW(), INTERVAL 5 DAY), 'online', 'https://meet.example.com/mindful');

INSERT INTO users (firebase_uid, email, name, role) VALUES
('dummy-expert-uid-123', 'expert@example.com', 'Dr. Sarah (Expert)', 'expert');

INSERT INTO testimonials (user_id, content) VALUES
(1, 'This app changed my life. I finally feel understood and supported during my postpartum journey.');

INSERT INTO info_resources (title, topic, excerpt, content, image_url, tags, views, author_id) VALUES
('Understanding Postpartum Depression', 'PPD', 'Signs, symptoms, and when to ask for help.', 'Postpartum depression (PPD) is a complex mix of physical, emotional, and behavioral changes that happen in some women after giving birth. PPD is a type of depression that happens within 4 weeks after delivery. The diagnosis of postpartum depression is based not only on the length of time between delivery and onset but on the severity of the depression. If you feel like this, please seek help.', 'https://example.com/image1.jpg', 'depression,mental health', 150, 1),
('Coping with Emotional Swings', 'Coping', 'Daily coping strategies and routines.', 'Emotional swings are common. Try breathing exercises, mindfulness, and talking to your partner or friends about your feelings. Acknowledge that your hormones are shifting rapidly. Taking 10 minutes a day just for yourself can make a huge difference in navigating these emotions.', 'https://example.com/image2.jpg', 'emotions,coping', 85, 1),
('How Partners Can Support Recovery', 'Partner', 'What meaningful support looks like.', 'Partners play a crucial role. This means taking over night feeds, making sure the mother is hydrated, and managing household chores without being asked. Emotional support involves listening without always trying to "fix" things, and reassuring the mother that she is doing a great job.', 'https://example.com/image3.jpg', 'partner,support', 230, 1);

-- Update existing resources with better images
UPDATE info_resources SET image_url = 'https://images.unsplash.com/photo-1555252333-9f8e92e65df9?q=80&w=1000' WHERE topic = 'Sleep';
UPDATE info_resources SET image_url = 'https://images.unsplash.com/photo-1531983412531-1f49a365ffed?q=80&w=1000' WHERE topic = 'PPD';
UPDATE info_resources SET image_url = 'https://images.unsplash.com/photo-1551853665-5e2378ce0f3c?q=80&w=1000' WHERE topic = 'Coping';
UPDATE info_resources SET image_url = 'https://images.unsplash.com/photo-1536640712257-2a2412b4f528?q=80&w=1000' WHERE topic = 'Partner';

-- Add more resources
INSERT INTO info_resources (title, topic, excerpt, content, image_url, tags, views) VALUES
('The Science of Baby Sleep', 'Sleep', 'Why babies wake up and how to manage your own rest.', 'Full scientific guide...', 'https://images.unsplash.com/photo-1520206159572-46107354335b?q=80&w=1000', 'sleep,routine', 450),
('Self-Care for Busy Moms', 'Self-care', '5-minute routines that actually make a difference.', 'Self care guide...', 'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?q=80&w=1000', 'self-care,mental-health', 320),
('Postpartum Nutrition', 'Self-care', 'Fueling your body for recovery and energy.', 'Nutrition guide...', 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?q=80&w=1000', 'food,health', 180);

-- ─────────────────────────────────────────────────────────────────────────────
-- HOW TO MAKE A USER AN EXPERT IN THE DATABASE (Admin SQL)
-- ─────────────────────────────────────────────────────────────────────────────
-- Option 1: Promote by email address
--   UPDATE users SET role = 'expert' WHERE email = 'someone@example.com';
--
-- Option 2: Promote by database ID
--   UPDATE users SET role = 'expert' WHERE id = 3;
--
-- Option 3: See all current users and their roles
--   SELECT id, name, email, role FROM users;
--
-- Option 4: Demote an expert back to regular user
--   UPDATE users SET role = 'user' WHERE email = 'someone@example.com';
-- ─────────────────────────────────────────────────────────────────────────────
