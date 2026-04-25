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

INSERT INTO info_resources (title, topic, excerpt, content_url) VALUES
('Understanding Postpartum Depression', 'PPD', 'Signs, symptoms, and when to ask for help.', 'https://example.org/ppd-guide'),
('Coping with Emotional Swings', 'Coping', 'Daily coping strategies and routines.', 'https://example.org/coping'),
('How Partners Can Support Recovery', 'Partner', 'What meaningful support looks like.', 'https://example.org/partner-support');
