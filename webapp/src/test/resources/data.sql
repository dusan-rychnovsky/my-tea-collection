
-- TeaType
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (1, 'Blend', NULL);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (2, 'Chen Pi', 1);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (3, 'Rose', 1);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (4, 'White Tea', NULL);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (5, 'Bai Mu Dan', 4);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (6, 'Shou Mei', 4);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (7, 'Yellow Tea', NULL);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (8, 'Green Tea', NULL);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (9, 'Long Jing', 8);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (10, 'Bi Luo Chun', 8);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (11, 'Mao Feng', 8);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (12, 'Hou Kui', 8);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (13, 'Sencha', 8);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (14, 'Kabusecha', 8);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (15, 'Shincha', 8);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (16, 'Genmaicha', 8);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (17, 'Oolong', NULL);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (18, 'Yan Cha', 17);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (19, 'Dan Cong', 17);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (20, 'Bao Zhong', 17);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (21, 'Dong Ding', 17);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (22, 'GABA', 17);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (23, 'Red Tea', NULL);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (24, 'Dian Hong', 23);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (25, 'Dark Tea', NULL);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (26, 'Sheng Puerh', 25);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (27, 'Shu Puerh', 25);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (28, 'Liu Bao', 25);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (29, 'Fu Zhuan', 25);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (30, 'Liu An', 25);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (31, 'Tian Jian', 25);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (32, 'Huang Pian',25);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (33, 'Yabao', NULL);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (34, 'Darjeeling', NULL);
INSERT INTO myteacollection.TeaTypes (id, name, parent_id) VALUES (35, 'Purple Tea', NULL);

-- Vendor
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (1, 'Mei Leaf', 'https://meileaf.com');
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (2, 'Meetea', 'https://www.meetea.cz');
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (3, 'Chuť Čaje', 'https://www.chutcaje.cz/');
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (4, 'Lao Tea', 'https://www.laoteashop.cz/');
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (5, 'Klasek Tea', 'https://www.darjeeling.cz/');
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (6, 'Banna House', 'https://www.bannahouse.cz/');
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (7, 'TeaGardenia', 'https://www.teagardenia.com/');
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (8, 'Global Tea Hut', 'https://globalteahut.org/');
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (9, 'Amana', 'https://www.caj.cz/');
INSERT INTO myteacollection.Vendors (id, name, url) VALUES (10, 'Orijin Tea', 'https://www.orijintea.com/');

-- Tag
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (1, 'meetea-2025-jan', 'Čajové předplatné Meetea, leden 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (2, 'meetea-2024-dec', 'Čajové předplatné Meetea, prosinec 2024');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (3, 'gift-adam', 'Dárek od Adama');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (4, 'meetea-2025-feb', 'Čajové předplatné Meetea, únor 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (5, 'meetea-2024-nov', 'Čajové předplatné Meetea, listopad 2024');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (6, 'meetea-2024-oct', 'Čajové předplatné Meetea, říjen 2024');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (7, 'meetea-2024-sep', 'Čajové předplatné Meetea, září 2024');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (8, 'meetea-2024-aug', 'Čajové předplatné Meetea, srpen 2024');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (9, 'meetea-2024-jul', 'Čajové předplatné Meetea, červenec 2024');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (10, 'meetea-2023-dec', 'Čajové předplatné Meetea, prosinec 2023');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (11, 'meetea-2025-mar', 'Čajové předplatné Meetea, březen 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (12, 'meetea-2025-apr', 'Čajové předplatné Meetea, duben 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (13, 'meetea-2025-may', 'Čajové předplatné Meetea, květen 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (14, 'gift-filip', 'Dárek od Filipa');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (15, 'meetea-2025-jun', 'Čajové předplatné Meetea, červen 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (16, 'meetea-2025-jul', 'Čajové předplatné Meetea, červenec 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (17, 'meetea-2025-aug', 'Čajové předplatné Meetea, srpen 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (18, 'meetea-2025-sep', 'Čajové předplatné Meetea, září 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (19, 'globalteahut-2025-summer', 'Global Tea Hut Subscription, Summer 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (20, 'globalteahut-2025-autumn', 'Global Tea Hut Subscription, Autumn 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (21, 'meetea-2025-oct', 'Čajové předplatné Meetea, říjen 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (22, 'meetea-2025-nov', 'Čajové předplatné Meetea, listopad 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (23, 'globalteahut-2026-winter', 'Global Tea Hut Subscription, Winter 2026');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (24, 'gift-tomas', 'Dárek od Tomáše Čapka');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (25, 'meetea-2025-dec', 'Čajové předplatné Meetea, prosinec 2025');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (26, 'globalteahut-2026-spring', 'Global Tea Hut Subscription, Spring 2026');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (27, 'globalteahut-2026-summer', 'Global Tea Hut Subscription, Summer 2026');
INSERT INTO myteacollection.Tags (id, label, description)
VALUES (28, 'gift-honza', 'Dárek od Honzy Žáčka');
