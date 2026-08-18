CREATE TABLE class_room (
    id BIGINT NOT NULL AUTO_INCREMENT,
    school_type VARCHAR(255) NOT NULL,
    grade INT NOT NULL,
    class_number INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE student (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    birth DATE NOT NULL,
    sex TINYINT NULL,
    phone VARCHAR(255) NULL,
    parent_phone VARCHAR(255) NULL,
    school VARCHAR(255) NULL,
    memo VARCHAR(255) NULL,
    is_graduated BIT NOT NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE teacher (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NULL,
    birth DATE NULL,
    sex TINYINT NULL,
    phone VARCHAR(255) NULL,
    teacher_type TINYINT NULL,
    memo VARCHAR(255) NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE student_class (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_room_id BIGINT NOT NULL,
    school_year INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_student_class_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_student_class_room FOREIGN KEY (class_room_id) REFERENCES class_room (id)
) ENGINE=InnoDB;

CREATE TABLE teacher_class (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    class_room_id BIGINT NOT NULL,
    school_year INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_teacher_class_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id),
    CONSTRAINT fk_teacher_class_room FOREIGN KEY (class_room_id) REFERENCES class_room (id)
) ENGINE=InnoDB;

CREATE TABLE attendance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_class_id BIGINT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_studentclass_date UNIQUE (student_class_id, date),
    CONSTRAINT fk_attendance_student_class FOREIGN KEY (student_class_id) REFERENCES student_class (id)
) ENGINE=InnoDB;

CREATE TABLE attendance_teacher (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_teacher_date UNIQUE (teacher_id, date),
    CONSTRAINT fk_attendance_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id)
) ENGINE=InnoDB;

CREATE TABLE parent_attendance (
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

CREATE TABLE student_qr (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    qr_secret VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_student_qr_student UNIQUE (student_id),
    CONSTRAINT uk_student_qr_secret UNIQUE (qr_secret),
    CONSTRAINT fk_student_qr_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB;

CREATE TABLE new_friend (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    birth DATE NOT NULL,
    phone VARCHAR(255) NULL,
    student_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_new_friend_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB;

CREATE TABLE message_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    content TEXT NULL,
    error_message VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_message_log_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB;

CREATE TABLE pray (
    id BIGINT NOT NULL AUTO_INCREMENT,
    date DATE NOT NULL,
    prayer VARCHAR(255) NULL,
    pray_content VARCHAR(255) NULL,
    recitation VARCHAR(255) NULL,
    declaration VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
