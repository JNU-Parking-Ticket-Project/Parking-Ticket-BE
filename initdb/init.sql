create table BATCH_JOB_EXECUTION_SEQ
(
    ID         bigint not null,
    UNIQUE_KEY char   not null,
    constraint UNIQUE_KEY_UN
        unique (UNIQUE_KEY)
)
    collate = utf8mb4_unicode_ci;

create table BATCH_JOB_INSTANCE
(
    JOB_INSTANCE_ID bigint       not null
        primary key,
    VERSION         bigint       null,
    JOB_NAME        varchar(100) not null,
    JOB_KEY         varchar(32)  not null,
    constraint JOB_INST_UN
        unique (JOB_NAME, JOB_KEY)
)
    collate = utf8mb4_unicode_ci;

create table BATCH_JOB_EXECUTION
(
    JOB_EXECUTION_ID           bigint        not null
        primary key,
    VERSION                    bigint        null,
    JOB_INSTANCE_ID            bigint        not null,
    CREATE_TIME                datetime(6)   not null,
    START_TIME                 datetime(6)   null,
    END_TIME                   datetime(6)   null,
    STATUS                     varchar(10)   null,
    EXIT_CODE                  varchar(2500) null,
    EXIT_MESSAGE               varchar(2500) null,
    LAST_UPDATED               datetime(6)   null,
    JOB_CONFIGURATION_LOCATION varchar(2500) null,
    constraint JOB_INST_EXEC_FK
        foreign key (JOB_INSTANCE_ID) references BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
)
    collate = utf8mb4_unicode_ci;

create table BATCH_JOB_EXECUTION_CONTEXT
(
    JOB_EXECUTION_ID   bigint        not null
        primary key,
    SHORT_CONTEXT      varchar(2500) not null,
    SERIALIZED_CONTEXT text          null,
    constraint JOB_EXEC_CTX_FK
        foreign key (JOB_EXECUTION_ID) references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
)
    collate = utf8mb4_unicode_ci;

create table BATCH_JOB_EXECUTION_PARAMS
(
    JOB_EXECUTION_ID bigint       not null,
    TYPE_CD          varchar(6)   not null,
    KEY_NAME         varchar(100) not null,
    STRING_VAL       varchar(250) null,
    DATE_VAL         datetime(6)  null,
    LONG_VAL         bigint       null,
    DOUBLE_VAL       double       null,
    IDENTIFYING      char         not null,
    constraint JOB_EXEC_PARAMS_FK
        foreign key (JOB_EXECUTION_ID) references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
)
    collate = utf8mb4_unicode_ci;

create table BATCH_JOB_SEQ
(
    ID         bigint not null,
    UNIQUE_KEY char   not null,
    constraint UNIQUE_KEY_UN
        unique (UNIQUE_KEY)
)
    collate = utf8mb4_unicode_ci;

create table BATCH_STEP_EXECUTION
(
    STEP_EXECUTION_ID  bigint        not null
        primary key,
    VERSION            bigint        not null,
    STEP_NAME          varchar(100)  not null,
    JOB_EXECUTION_ID   bigint        not null,
    START_TIME         datetime(6)   not null,
    END_TIME           datetime(6)   null,
    STATUS             varchar(10)   null,
    COMMIT_COUNT       bigint        null,
    READ_COUNT         bigint        null,
    FILTER_COUNT       bigint        null,
    WRITE_COUNT        bigint        null,
    READ_SKIP_COUNT    bigint        null,
    WRITE_SKIP_COUNT   bigint        null,
    PROCESS_SKIP_COUNT bigint        null,
    ROLLBACK_COUNT     bigint        null,
    EXIT_CODE          varchar(2500) null,
    EXIT_MESSAGE       varchar(2500) null,
    LAST_UPDATED       datetime(6)   null,
    constraint JOB_EXEC_STEP_FK
        foreign key (JOB_EXECUTION_ID) references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
)
    collate = utf8mb4_unicode_ci;

create table BATCH_STEP_EXECUTION_CONTEXT
(
    STEP_EXECUTION_ID  bigint        not null
        primary key,
    SHORT_CONTEXT      varchar(2500) not null,
    SERIALIZED_CONTEXT text          null,
    constraint STEP_EXEC_CTX_FK
        foreign key (STEP_EXECUTION_ID) references BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
)
    collate = utf8mb4_unicode_ci;

create table BATCH_STEP_EXECUTION_SEQ
(
    ID         bigint not null,
    UNIQUE_KEY char   not null,
    constraint UNIQUE_KEY_UN
        unique (UNIQUE_KEY)
)
    collate = utf8mb4_unicode_ci;

create table announce_tb
(
    id         bigint auto_increment
        primary key,
    content    varchar(5000) default '내용을 입력해주세요.' not null,
    title      varchar(255)  default '제목을 입력해주세요.' not null,
    created_at datetime(6)                         null
);

create table captcha_tb
(
    id         bigint auto_increment
        primary key,
    answer     varchar(255) not null,
    image_name varchar(255) not null
);

create table credential_code_tb
(
    id    bigint auto_increment
        primary key,
    code  varchar(255) not null,
    email varchar(255) not null,
    constraint UK_jsm2ippxb7f3t87s098b2s7un
        unique (email)
);

create table event
(
    event_id     bigint auto_increment
        primary key,
    event_code   varchar(255)     null,
    event_status varchar(255)     null,
    end_at       datetime(6)      null,
    start_at     datetime(6)      null,
    title        varchar(255)     null,
    publish      bit default b'0' null,
    is_deleted   bit              null
);

create table notice_tb
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)                         null,
    modified_at datetime(6)                         null,
    content     varchar(5000) default '내용을 입력해주세요.' not null
);

create table sector
(
    sector_id            bigint auto_increment
        primary key,
    issue_amount         int          not null,
    name                 varchar(255) not null,
    remaining_amount     int          null,
    reserve              int          not null,
    sector_capacity      int          not null,
    sector_number        varchar(255) null,
    event_id             bigint       null,
    init_sector_capacity int          not null,
    init_reserve         int          not null,
    is_deleted           bit          null,
    constraint FKp0a5yto1h1qs48e4ser90pv11
        foreign key (event_id) references event (event_id)
);

create table user_tb
(
    user_id         bigint auto_increment
        primary key,
    email           varchar(255)                not null,
    email_confirmed bit          default b'0'   not null,
    pwd             varchar(255)                not null,
    sequence        int                         not null,
    status          varchar(255) default '불합격'  not null,
    role            varchar(255) default 'USER' not null,
    constraint UK_2dlfg6wvnxboknkp9d1h75icb
        unique (email)
);

create table council_tb
(
    id          bigint auto_increment
        primary key,
    name        varchar(255) not null,
    phone_num   varchar(255) not null,
    user_id     bigint       null,
    student_num varchar(255) not null,
    constraint FKfnyqpaioro515ktnam1ygd56x
        foreign key (user_id) references user_tb (user_id)
);

create table registration_tb
(
    id          bigint auto_increment
        primary key,
    affiliation varchar(255)     null,
    car_num     varchar(255)     not null,
    created_at  datetime(6)      not null,
    email       varchar(255)     not null,
    is_light    bit              not null,
    is_saved    bit              not null,
    name        varchar(255)     not null,
    phone_num   varchar(255)     not null,
    student_num varchar(255)     not null,
    sector_id   bigint           not null,
    user_id     bigint           null,
    is_deleted  bit default b'0' not null,
    constraint FK7p6t377o5h1q0bheapm2t91f5
        foreign key (sector_id) references sector (sector_id),
    constraint FKlfurts4ouptujftlouj21atoe
        foreign key (user_id) references user_tb (user_id)
);

