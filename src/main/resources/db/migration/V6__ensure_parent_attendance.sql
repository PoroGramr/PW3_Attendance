-- Older attendance databases predate this table. Fresh databases already
-- create it in V1, so leave those untouched.
CREATE TABLE IF NOT EXISTS parent_attendance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    date DATE NOT NULL,
    father_status VARCHAR(255) NOT NULL,
    mother_status VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_parent_attendance_student_date UNIQUE (student_id, date),
    CONSTRAINT fk_parent_attendance_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB;
