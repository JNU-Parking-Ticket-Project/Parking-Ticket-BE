SET foreign_key_checks = 0;

truncate table announce_tb;
truncate table captcha_tb;
truncate table council_tb;
truncate table credential_code_tb;
truncate table event;
truncate table notice_tb;
truncate table registration_tb;
truncate table sector;
truncate table user_tb;

SET foreign_key_checks = 1;


insert into user_tb(user_id, email, email_confirmed, pwd, sequence, status, role)
-- council 비밀번호 Council@123, admin 비밀번호 Admin@123
VALUES (1, 'council@jnu.ac.kr', false, '$2a$10$iH6JVDTpjdq0azNVVjmluu5jHy3NG92zoNjeA0x.EwLL.tTMIvUmq', -2, '불합격', 'USER'),
       (2, 'admin@jnu.ac.kr', false, '$2a$10$I0J8oSZ.7Mq3itr1g5DnTeRAZQydvN1qp8CWck2dvxNOnO.bPrKhK', -2, '불합격', 'ADMIN');
insert into council_tb(id, name, phone_num, student_num, user_id)
VALUES (1, '학생회장', '010-1234-5678', '2019101234', 1);
update user_tb set role = 'COUNCIL' where user_id = 1;
