CREATE TABLE announce_image_tb (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(255) NOT NULL,
    announce_id BIGINT,

    created_at DATETIME(6),
    FOREIGN KEY (announce_id) REFERENCES announce_tb(id) ON DELETE CASCADE
);