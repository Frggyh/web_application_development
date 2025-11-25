-- SQL 脚本：创建 'web_course' 数据库

-- 1. 尝试删除旧的数据库实例（如果存在，仅在开发环境建议使用）
-- DROP DATABASE IF EXISTS web_course;

-- 2. 创建数据库
-- CHARACTER SET utf8mb4 和 COLLATE utf8mb4_unicode_ci 确保数据库能正确处理中文和特殊字符。
CREATE DATABASE IF NOT EXISTS web_course
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- 3. 切换到新创建的数据库
USE web_course;

-- 确保我们操作的是正确的数据库
USE web_course;


-- 1. 用户表 (User)
-- 包含 管理员(ADMIN)、教师(TEACHER)、学生(STUDENT) 三种角色

CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(255) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码 (需加密存储)',
    role VARCHAR(50) NOT NULL COMMENT '用户角色：ADMIN, TEACHER, STUDENT',
    profile TEXT COMMENT '个人简介/职称/班级信息'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;



-- 2. 课程表 (Course)
CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    name VARCHAR(255) NOT NULL COMMENT '课程名称',
    description TEXT COMMENT '课程内容描述',
    college VARCHAR(255) COMMENT '开课学院'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- 3. 学习资源表 (Resource)
-- 包含外键 course_id 和 uploader_id
CREATE TABLE IF NOT EXISTS resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资源ID',
    title VARCHAR(255) NOT NULL COMMENT '资源标题',
    description TEXT COMMENT '简介/说明',
    file_path VARCHAR(512) NOT NULL COMMENT '文件存储路径',
    file_type VARCHAR(50) COMMENT '文件类型',
    download_count BIGINT DEFAULT 0 COMMENT '下载次数',
    upload_time DATETIME NOT NULL COMMENT '上传时间',

    -- 外键关联课程
    course_id BIGINT NOT NULL COMMENT '所属课程ID',
    CONSTRAINT fk_resource_course
        FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,

    -- 外键关联上传者
    uploader_id BIGINT NOT NULL COMMENT '上传者ID',
    CONSTRAINT fk_resource_uploader
        FOREIGN KEY (uploader_id) REFERENCES user(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;



-- 4. 教师-课程关联表 (Teacher_Course)
-- 这是一个多对多关系的中间表，用于管理一个教师可以教授多门课程
CREATE TABLE IF NOT EXISTS teacher_course (
    teacher_id BIGINT NOT NULL COMMENT '教师ID (User ID)',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    PRIMARY KEY (teacher_id, course_id),

    CONSTRAINT fk_tc_teacher
        FOREIGN KEY (teacher_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_tc_course
        FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;