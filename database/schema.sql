-- Smart Appointment Management System - MySQL setup
-- Run as an administrator:  mysql -u root -p < database/schema.sql
-- Change CHANGE_ME_PASSWORD before running. Do not commit your real password.

CREATE DATABASE IF NOT EXISTS smart_appointment_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'sams_user'@'localhost' IDENTIFIED BY 'CHANGE_ME_PASSWORD';
GRANT ALL PRIVILEGES ON smart_appointment_db.* TO 'sams_user'@'localhost';
FLUSH PRIVILEGES;

USE smart_appointment_db;

CREATE TABLE IF NOT EXISTS pets (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    breed        VARCHAR(100),
    age          INT          NOT NULL,
    owner_name   VARCHAR(100) NOT NULL,
    owner_phone  VARCHAR(20)  NOT NULL,
    owner_email  VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS appointments (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id            BIGINT       NOT NULL,
    appointment_date  DATE         NOT NULL,
    appointment_time  TIME         NOT NULL,
    reason            VARCHAR(255) NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_appointment_pet FOREIGN KEY (pet_id) REFERENCES pets (id) ON DELETE CASCADE,
    INDEX idx_appt_date (appointment_date)
);
