USE web_course;

-- 明文密码: password123
-- 对应的 BCrypt 加密值: $2a$10$R9n78u9sQvG5F2D9W2N8e.I/Yw/zK0PqO7E4F2B3B1D5F9H0C7E5F6
SET @encrypted_password = '$2a$10$R9n78u9sQvG5F2D9W2N8e.I/Yw/zK0PqO7E4F2B3B1D5F9H0C7E5F6';


-- ----------------------------------------------------
-- 1. 用户表 (user) - 至少 5 条
-- ----------------------------------------------------
INSERT INTO user (username, password, role, profile) VALUES
('admin01', @encrypted_password, 'ADMIN', '平台超级管理员'),
('teacher_math', @encrypted_password, 'TEACHER', '数学系教授，高级职称'),
('teacher_cs', @encrypted_password, 'TEACHER', '计算机学院讲师'),
('student_alice', @encrypted_password, 'STUDENT', '计算机学院2023级'),
('student_bob', @encrypted_password, 'STUDENT', '数学系2024级'),
('student_charlie', @encrypted_password, 'STUDENT', '经管学院2022级');

-- 存储用户 ID 供外键关联
SET @admin_id = (SELECT id FROM user WHERE username = 'admin01');
SET @teacher_math_id = (SELECT id FROM user WHERE username = 'teacher_math');
SET @teacher_cs_id = (SELECT id FROM user WHERE username = 'teacher_cs');
SET @student_alice_id = (SELECT id FROM user WHERE username = 'student_alice');


-- ----------------------------------------------------
-- 2. 课程表 (course) - 至少 5 条
-- ----------------------------------------------------
INSERT INTO course (name, description, college) VALUES
('高等数学 A', '理工科基础，极限与微积分', '数学系'),
('数据结构与算法', '计算机科学核心课程', '计算机学院'),
('大学物理', '经典力学、电磁学基础', '物理学院'),
('线性代数', '向量空间、矩阵运算', '数学系'),
('经济学原理', '宏观经济与微观经济基础', '经管学院');

-- 存储课程 ID 供外键关联
SET @course_math_id = (SELECT id FROM course WHERE name = '高等数学 A');
SET @course_data_id = (SELECT id FROM course WHERE name = '数据结构与算法');
SET @course_physics_id = (SELECT id FROM course WHERE name = '大学物理');
SET @course_linear_id = (SELECT id FROM course WHERE name = '线性代数');


-- ----------------------------------------------------
-- 3. 教师-课程关联表 (teacher_course) - 至少 5 条记录
-- ----------------------------------------------------
INSERT INTO teacher_course (teacher_id, course_id) VALUES
(@teacher_math_id, @course_math_id),      -- 数学老师教高数
(@teacher_math_id, @course_linear_id),     -- 数学老师教线代
(@teacher_cs_id, @course_data_id),         -- 计算机老师教数据结构
(@teacher_cs_id, @course_math_id),         -- 计算机老师协教高数
(@admin_id, @course_physics_id);           -- 管理员作为特例，也教一门课


-- ----------------------------------------------------
-- 4. 学习资源表 (resource) - 至少 5 条
-- ----------------------------------------------------
-- 注意：filePath 路径是模拟的，实际需指向文件存储服务
INSERT INTO resource (title, description, file_path, file_type, upload_time, course_id, uploader_id) VALUES
('高数期末复习大纲', '期末重点知识点整理', '/files/math/review.pdf', 'PDF', NOW(), @course_math_id, @teacher_math_id),
('数据结构笔记第一章', '线性表和链表', '/files/cs/ds_chap1.zip', 'ZIP', NOW() - INTERVAL 1 DAY, @course_data_id, @teacher_cs_id),
('大学物理实验报告模板', '标准实验报告格式', '/files/physics/template.doc', 'DOC', NOW() - INTERVAL 5 HOUR, @course_physics_id, @admin_id),
('线代习题集（含答案）', '提高运算能力的必备习题', '/files/math/linear_ex.pdf', 'PDF', NOW() - INTERVAL 2 DAY, @course_linear_id, @student_alice_id), -- 学生上传资源
('栈与队列基础知识图解', '计算机学院内部资料', '/files/cs/stack_queue.png', 'PNG', NOW(), @course_data_id, @teacher_cs_id);