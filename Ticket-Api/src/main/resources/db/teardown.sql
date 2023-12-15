SET
foreign_key_checks = 0;

truncate table announce_tb;
truncate table captcha_tb;
truncate table council_tb;
truncate table credential_code_tb;
truncate table event;
truncate table notice_tb;
truncate table registration_tb;
truncate table sector;
truncate table user_tb;

SET
foreign_key_checks = 1;


insert into user_tb(user_id, email, email_confirmed, pwd, sequence, status, role)
-- council 비밀번호 Council@123, admin 비밀번호 Admin@123, user~3 비밀번호 User@1234
VALUES (1, 'council@jnu.ac.kr', false, '$2a$10$iH6JVDTpjdq0azNVVjmluu5jHy3NG92zoNjeA0x.EwLL.tTMIvUmq', -2, '불합격',
        'USER'),
       (2, 'admin@jnu.ac.kr', false, '$2a$10$I0J8oSZ.7Mq3itr1g5DnTeRAZQydvN1qp8CWck2dvxNOnO.bPrKhK', -2, '불합격',
        'ADMIN'),
       (3, 'user@jnu.ac.kr', false, '$$2a$10$S6jHnlYjhNDGeQd8HXbxjONA0U15TT.g.FjMwOCTok4YOxx6A8C7y', -2, '불합격',
        'USER'),
        (4, 'user2@jnu.ac.kr', false, '$$2a$10$S6jHnlYjhNDGeQd8HXbxjONA0U15TT.g.FjMwOCTok4YOxx6A8C7y', -2, '불합격',
        'USER'),
       (5, 'user3@jnu.ac.kr', false, '$$2a$10$S6jHnlYjhNDGeQd8HXbxjONA0U15TT.g.FjMwOCTok4YOxx6A8C7y', -2, '불합격',
        'USER');
insert into council_tb(id, name, phone_num, student_num, user_id)
VALUES (1, '학생회장', '010-1234-5678', '2019101234', 1);
update user_tb
set role = 'COUNCIL'
where user_id = 1;

insert into event(event_id, event_code, event_status, end_at, start_at)
values (1, '596575', 'OPEN', TIMESTAMPADD(MINUTE, 30, current_time), TIMESTAMPADD(MINUTE, -30, current_time));

insert into sector(sector_id, issue_amount, name, remaining_amount, reserve, sector_capacity, sector_number, event_id)
values (1, 40, '사회대 / 농대 / 수의대 / 치전원', 40, 0, 40, '1구간', 1),
       (2, 15, '경영대 / 인문대', 15, 0, 15, '2구간', 1),
       (3, 45, '사범대 / 예술대 / AI융합대 / 본부직할', 45, 0, 45, '3구간', 1),
       (4, 70, '공대 / 간호-의대 1학년', 70, 0, 70, '4구간', 1),
       (5, 30, '자연대 / 약대 / 생활대', 30, 0, 30, '5구간', 1);

insert into registration_tb(id, affiliation, car_num, created_at, email, is_light, is_saved, name, student_num,
                            phone_num, sector_id, user_id, is_deleted)
values (1, '공과대학', '1234가1234', current_time, 'user@jnu.ac.kr', true, true, '이진혁', '215555', '010-000-0000', 4, 3,
        false),
       (2, '경영대학', '가1234', current_time, 'council@jnu.ac.kr', true, true, '박영규', '192155', '010-000-0000', 4, 1,
        false),
       (3, '농대', '나1234', current_time, 'user2l@jnu.ac.kr', true, true, '임채승', '1821555', '010-000-0000', 4, 1, true),
       (4, '의대', '다1234', current_time, 'user3@jnu.ac.kr', true, false, '김동완', '172155', '010-000-0000', 4, 1, true),
       (5, '인문대', '라1234', current_time, 'admin@jnu.ac.kr', true, false, '이윤성', '162155', '010-000-0000', 4, 1,false);

insert into captcha_tb(id, answer, image_name)
values (1, '1234', '1234.png'), (2, '5678', '5678.png');

