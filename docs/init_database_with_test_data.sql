CREATE DATABASE IF NOT EXISTS webcourse;

USE webcourse;


-- 1. 清空现有数据（可选，用于重置测试环境）
-- 注意：由于外键约束，需要按顺序删除
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE answer;
TRUNCATE TABLE question;
TRUNCATE TABLE resource;
TRUNCATE TABLE course;
TRUNCATE TABLE user;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 2. 插入用户数据
-- ============================================
-- 密码：所有测试账号密码统一为 "123456"
-- 管理员账号
INSERT INTO user (username, password, role, title, introduction, email, avatar) VALUES
('admin', '123456', 'ADMIN', NULL, NULL, 'admin@example.com', NULL);

-- 教师账号
INSERT INTO user (username, password, role, title, introduction, email, avatar) VALUES
('teacher_zhang', '123456', 'TEACHER', '教授', '计算机科学与技术学院教授，主要研究方向为人工智能和机器学习。', 'zhang@example.com', NULL),
('teacher_li', '123456', 'TEACHER', '副教授', '数学系副教授，擅长高等数学和线性代数教学。', 'li@example.com', NULL),
('teacher_wang', '123456', 'TEACHER', '讲师', '软件工程学院讲师，专注于Web开发和数据库技术。', 'wang@example.com', NULL);

-- 学生账号
INSERT INTO user (username, password, role, title, introduction, email, avatar) VALUES
('student_alice', '123456', 'STUDENT', NULL, NULL, 'alice@example.com', NULL),
('student_bob', '123456', 'STUDENT', NULL, NULL, 'bob@example.com', NULL),
('student_charlie', '123456', 'STUDENT', NULL, NULL, 'charlie@example.com', NULL),
('student_david', '123456', 'STUDENT', NULL, NULL, 'david@example.com', NULL);

-- ============================================
-- 3. 插入课程数据
-- ============================================
-- 使用变量存储教师ID，确保外键关系正确

SET @teacher_zhang_id = (SELECT id FROM user WHERE username = 'teacher_zhang');
SET @teacher_li_id = (SELECT id FROM user WHERE username = 'teacher_li');
SET @teacher_wang_id = (SELECT id FROM user WHERE username = 'teacher_wang');

INSERT INTO course (name, description, college, teacher_id) VALUES
('Java程序设计', '本课程介绍Java编程语言的基础知识和面向对象编程思想，包括语法、集合框架、多线程等内容。', '计算机科学与技术学院', @teacher_zhang_id),
('高等数学', '高等数学是理工科学生的重要基础课程，涵盖微积分、极限、导数、积分等核心内容。', '数学系', @teacher_li_id),
('Web应用开发', '学习现代Web开发技术，包括前端框架、后端API设计、数据库操作等。', '软件工程学院', @teacher_wang_id),
('数据结构与算法', '深入理解常用数据结构和算法，提高编程能力和问题解决能力。', '计算机科学与技术学院', @teacher_zhang_id),
('线性代数', '学习矩阵运算、向量空间、特征值等线性代数核心概念。', '数学系', @teacher_li_id);

-- ============================================
-- 4. 插入学习资源数据
-- ============================================
-- 使用变量存储课程ID和用户ID

SET @course_java_id = (SELECT id FROM course WHERE name = 'Java程序设计');
SET @course_math_id = (SELECT id FROM course WHERE name = '高等数学');
SET @course_web_id = (SELECT id FROM course WHERE name = 'Web应用开发');
SET @course_ds_id = (SELECT id FROM course WHERE name = '数据结构与算法');
SET @course_linear_id = (SELECT id FROM course WHERE name = '线性代数');

SET @student_alice_id = (SELECT id FROM user WHERE username = 'student_alice');

INSERT INTO resource (title, description, course_id, uploader_id, upload_time, file_path, file_name, file_mime_type, download_count, visibility) VALUES
('Java基础语法PPT', '第一章：Java语言概述和基础语法', @course_java_id, @teacher_zhang_id, '2024-01-15 10:00:00', 'resources/java_basics.pdf', 'java_basics.pdf', 'application/pdf', 25, 'ALL'),
('Java集合框架教程', '详细讲解List、Set、Map等集合类的使用方法', @course_java_id, @teacher_zhang_id, '2024-01-20 14:30:00', 'resources/java_collections.pdf', 'java_collections.pdf', 'application/pdf', 18, 'ALL'),
('高等数学第一章习题', '极限与连续章节的练习题和答案', @course_math_id, @teacher_li_id, '2024-01-10 09:00:00', 'resources/math_chapter1.pdf', 'math_chapter1.pdf', 'application/pdf', 42, 'ALL'),
('Web开发入门指南', 'HTML、CSS、JavaScript基础教程', @course_web_id, @teacher_wang_id, '2024-01-25 16:00:00', 'resources/web_basics.pdf', 'web_basics.pdf', 'application/pdf', 30, 'ALL'),
('Vue.js实战教程', 'Vue.js框架的实战项目案例', @course_web_id, @teacher_wang_id, '2024-02-01 11:00:00', 'resources/vue_tutorial.pdf', 'vue_tutorial.pdf', 'application/pdf', 15, 'ALL'),
('数据结构算法题集', '常见数据结构和算法的练习题', @course_ds_id, @teacher_zhang_id, '2024-01-18 13:00:00', 'resources/ds_algo.pdf', 'ds_algo.pdf', 'application/pdf', 35, 'ALL'),
('线性代数复习资料', '矩阵运算和向量空间的复习材料', @course_linear_id, @teacher_li_id, '2024-01-22 10:30:00', 'resources/linear_algebra.pdf', 'linear_algebra.pdf', 'application/pdf', 28, 'ALL'),
('学生分享：Java学习笔记', '个人整理的Java学习笔记，供大家参考', @course_java_id, @student_alice_id, '2024-02-05 15:00:00', 'resources/student_java_notes.pdf', 'student_java_notes.pdf', 'application/pdf', 8, 'ALL');

-- ============================================
-- 5. 插入问题数据
-- ============================================
-- 使用变量存储学生ID

SET @student_bob_id = (SELECT id FROM user WHERE username = 'student_bob');
SET @student_charlie_id = (SELECT id FROM user WHERE username = 'student_charlie');
SET @student_david_id = (SELECT id FROM user WHERE username = 'student_david');

INSERT INTO question (title, content, asker_id, course_id, ask_time, attachment_path, attachment_file_name, status, is_new) VALUES
('Java中如何理解多态？', '老师您好，我在学习Java面向对象编程时，对多态的概念有些困惑。能否详细解释一下多态的实现机制和使用场景？', @student_alice_id, @course_java_id, '2024-02-10 09:30:00', NULL, NULL, 'ANSWERED', false),
('关于集合框架的选择', '请问ArrayList和LinkedList有什么区别？在什么场景下应该使用哪个？', @student_bob_id, @course_java_id, '2024-02-12 14:20:00', NULL, NULL, 'ANSWERED', false),
('极限计算问题', '老师，这道题的极限应该如何计算？\nlim(x→0) (sin x) / x = ?', @student_charlie_id, @course_math_id, '2024-02-15 10:15:00', NULL, NULL, 'ANSWERED', false),
('Vue组件通信问题', '在Vue中，父子组件之间如何进行数据传递？有没有最佳实践？', @student_alice_id, @course_web_id, '2024-02-18 16:45:00', NULL, NULL, 'ANSWERED', false),
('关于Spring Boot的配置', 'Spring Boot的application.properties和application.yml有什么区别？', @student_bob_id, @course_web_id, '2024-02-20 11:30:00', NULL, NULL, 'UNANSWERED', true),
('二叉树遍历算法', '请问前序、中序、后序遍历的区别是什么？能否给出代码示例？', @student_charlie_id, @course_ds_id, '2024-02-22 09:00:00', NULL, NULL, 'UNANSWERED', true),
('矩阵的逆矩阵计算', '如何计算一个3x3矩阵的逆矩阵？步骤是什么？', @student_david_id, @course_linear_id, '2024-02-25 15:20:00', NULL, NULL, 'UNANSWERED', true),
('Java异常处理最佳实践', '在Java中，什么时候应该使用try-catch，什么时候应该抛出异常？', @student_alice_id, @course_java_id, '2024-02-28 10:00:00', NULL, NULL, 'UNANSWERED', true);

-- ============================================
-- 6. 插入答案数据
-- ============================================
-- 使用变量存储问题ID

SET @question_polymorphism_id = (SELECT id FROM question WHERE title = 'Java中如何理解多态？');
SET @question_collection_id = (SELECT id FROM question WHERE title = '关于集合框架的选择');
SET @question_limit_id = (SELECT id FROM question WHERE title = '极限计算问题');
SET @question_vue_communication_id = (SELECT id FROM question WHERE title = 'Vue组件通信问题');

INSERT INTO answer (content, replier_id, question_id, answer_time, attachment_path, attachment_file_name, is_read_by_asker) VALUES
('多态是面向对象编程的三大特性之一。简单来说，多态允许不同类的对象对同一消息做出不同的响应。\n\n在Java中，多态主要通过以下方式实现：\n1. 方法重写（Override）：子类重写父类的方法\n2. 向上转型：父类引用指向子类对象\n3. 动态绑定：运行时根据实际对象类型调用相应方法\n\n使用场景：\n- 提高代码的可扩展性和可维护性\n- 实现接口编程，降低耦合度\n- 统一处理不同类型的对象', @teacher_zhang_id, @question_polymorphism_id, '2024-02-10 15:30:00', NULL, NULL, true),
('ArrayList和LinkedList的主要区别：\n\n1. **底层实现**：\n   - ArrayList：基于动态数组\n   - LinkedList：基于双向链表\n\n2. **性能特点**：\n   - ArrayList：随机访问快（O(1)），插入删除慢（O(n)）\n   - LinkedList：随机访问慢（O(n)），插入删除快（O(1)）\n\n3. **使用场景**：\n   - ArrayList：适合频繁查询、较少插入删除的场景\n   - LinkedList：适合频繁插入删除、较少查询的场景', @teacher_zhang_id, @question_collection_id, '2024-02-12 16:00:00', NULL, NULL, true),
('这是一个重要的极限，结果是1。\n\n证明方法：\n使用洛必达法则：\nlim(x→0) (sin x) / x = lim(x→0) cos x / 1 = cos 0 = 1\n\n或者使用泰勒展开：\nsin x = x - x³/3! + x⁵/5! - ...\n因此 (sin x) / x = 1 - x²/3! + x⁴/5! - ...\n当x→0时，结果为1。', @teacher_li_id, @question_limit_id, '2024-02-15 14:00:00', NULL, NULL, true),
('Vue组件通信有多种方式：\n\n1. **Props（父传子）**：\n   - 父组件通过props向子组件传递数据\n   - 子组件通过props接收数据\n\n2. **$emit（子传父）**：\n   - 子组件通过$emit触发事件\n   - 父组件监听事件并处理\n\n3. **provide/inject（跨级传递）**：\n   - 祖先组件provide数据\n   - 后代组件inject数据\n\n4. **Vuex/Pinia（状态管理）**：\n   - 用于全局状态管理\n   - 适合复杂应用\n\n最佳实践：\n- 简单场景用props和$emit\n- 复杂场景用状态管理工具', @teacher_wang_id, @question_vue_communication_id, '2024-02-18 18:00:00', NULL, NULL, false);

-- ============================================
-- 7. 验证数据
-- ============================================
-- 查看插入的数据统计

SELECT '用户统计' AS '数据表', COUNT(*) AS '记录数' FROM user
UNION ALL
SELECT '课程统计', COUNT(*) FROM course
UNION ALL
SELECT '资源统计', COUNT(*) FROM resource
UNION ALL
SELECT '问题统计', COUNT(*) FROM question
UNION ALL
SELECT '答案统计', COUNT(*) FROM answer;

-- 查看各角色用户数量
SELECT role, COUNT(*) AS count FROM user GROUP BY role;

-- 查看课程及其教师
SELECT c.id, c.name, c.college, u.username AS teacher_name, u.title AS teacher_title
FROM course c
LEFT JOIN user u ON c.teacher_id = u.id
ORDER BY c.id;

-- ============================================
-- 测试账号
-- ============================================
-- 管理员账号：
--   用户名：admin
--   密码：123456
--
-- 教师账号：
--   用户名：teacher_zhang（Java程序设计、数据结构与算法）
--   用户名：teacher_li（高等数学、线性代数）
--   用户名：teacher_wang（Web应用开发）
--   密码：123456
--
-- 学生账号：
--   用户名：student_alice
--   用户名：student_bob
--   用户名：student_charlie
--   用户名：student_david
--   密码：123456


