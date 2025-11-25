USE web_course;

-- 明文密码: password123
-- 对应的 BCrypt 加密值: $2a$10$R9n78u9sQvG5F2D9W2N8e.I/Yw/zK0PqO7E4F2B3B1D5F9H0C7E5F6
SET @encrypted_password = '$2a$10$R9n78u9sQvG5F2D9W2N8e.I/Yw/zK0PqO7E4F2B3B1D5F9H0C7E5F6';


INSERT INTO user (username, password, role, profile) VALUES
('admin01', @encrypted_password, 'ADMIN', '平台超级管理员'),
('teacher_math', @encrypted_password, 'TEACHER', '数学系教授，高级职称'),
('teacher_cs', @encrypted_password, 'TEACHER', '计算机学院讲师'),
('student_alice', @encrypted_password, 'STUDENT', '计算机学院2023级'),
('student_bob', @encrypted_password, 'STUDENT', '数学系2024级'),
('student_charlie', @encrypted_password, 'STUDENT', '经管学院2022级');

