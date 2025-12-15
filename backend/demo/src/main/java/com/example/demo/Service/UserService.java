package com.example.demo.Service;

import com.example.demo.Dto.AdminUserUpdateRequest;
import com.example.demo.Entity.Course;
import com.example.demo.Entity.User;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Lazy
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 核心方法：为 SecurityConfig 提供用户加载逻辑
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("用户未找到: " + username);
        }

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())
        );

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    // ★★★ 修正点：添加 findUserByUsername 方法，用于 Controller 获取完整的 User 实体 (包含ID) ★★★
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 通用：用户修改密码 (需要新密码和旧密码进行验证)
     */
    public User updatePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在。");
        }

        // 1. 验证旧密码是否匹配 (由于我们使用的是 NoOpPasswordEncoder，直接比较即可)
        // 实际生产环境：应该使用 passwordEncoder.matches(oldPassword, user.getPassword())
        if (!user.getPassword().equals(oldPassword)) {
            throw new IllegalArgumentException("旧密码不正确。");
        }

        // 2. 更新新密码
        // user.setPassword(passwordEncoder.encode(newPassword)); // 生产环境做法
        user.setPassword(newPassword); // ★★★ 暂时使用明文密码存储（与 NoOpPasswordEncoder 保持一致）★★★

        return userRepository.save(user);
    }

    /**
     * 通用：更新个人信息 (例如职称、简介、邮箱、头像等)
     */
    public User updateProfile(String username, User updateDetails) {
        User existing = userRepository.findByUsername(username);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在。");
        }

        if (updateDetails.getTitle() != null) {
            existing.setTitle(updateDetails.getTitle());
        }
        if (updateDetails.getIntroduction() != null) {
            existing.setIntroduction(updateDetails.getIntroduction());
        }
        if (updateDetails.getEmail() != null) {
            existing.setEmail(updateDetails.getEmail());
        }
        if (updateDetails.getAvatar() != null) {
            existing.setAvatar(updateDetails.getAvatar());
        }
        // 阻止用户修改角色和ID

        return userRepository.save(existing);
    }

    // 用户注册逻辑 (不再加密)
    public User registerStudent(User user) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 保持明文密码存储
        // user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setRole("STUDENT");
        return userRepository.save(user);
    }

    /**
     * [Admin/Controller专用] 创建教师账号
     */
    public User createTeacher(User teacher) {
        if (!"TEACHER".equals(teacher.getRole())) {
            throw new IllegalArgumentException("只能创建教师角色。");
        }
        // TODO: 实际项目中应使用 BCryptPasswordEncoder 对密码进行加密
        // 此处沿用 NoOpPasswordEncoder
        return userRepository.save(teacher);
    }

    /**
     * [Admin/Controller专用] 更新教师信息 (简介/职称等)
     */
    public User updateTeacherProfile(Long id, User updateDetails) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("教师不存在。"));

        if (!"TEACHER".equals(existing.getRole())) {
            throw new IllegalStateException("ID非教师角色。");
        }

        // 允许修改 title(职称) 和 introduction(简介) 字段
        if (updateDetails.getTitle() != null) {
            existing.setTitle(updateDetails.getTitle());
        }
        if (updateDetails.getIntroduction() != null) {
            existing.setIntroduction(updateDetails.getIntroduction());
        }
        // 允许修改用户名、邮箱等通用字段...

        return userRepository.save(existing);
    }

    /**
     * [Admin/Controller专用] 删除教师账号
     */
    public void deleteTeacher(Long id) {
        User teacher = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("教师不存在。"));

        if (!"TEACHER".equals(teacher.getRole())) {
            throw new IllegalStateException("ID非教师角色，禁止删除。");
        }
        // 【重要】实际项目需处理该教师关联的课程、回答、资源等数据的级联删除或转移！
        userRepository.delete(teacher);
    }

    /**
     * [Admin/Controller专用] 设置教师讲授课程
     * 将课程分配给指定的教师
     */
    public Course assignCourseToTeacher(Long courseId, Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("教师不存在或ID无效。"));

        if (!"TEACHER".equals(teacher.getRole())) {
            throw new IllegalArgumentException("用户不是教师角色，无法分配课程。");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在。"));

        // 假设 Course 实体使用 Long teacherId 关联
        // 请确保您的 Course 实体中有 setTeacherId(Long id) 方法
        course.setTeacherId(teacherId);

        return courseRepository.save(course);
    }

    /**
     * 获取某个教师教授的所有课程ID列表
     */
    public List<Long> findTaughtCourseIds(Long teacherId) {
        return courseRepository.findCourseIdsByTeacherId(teacherId);
    }

    /**
     * [Admin/Controller专用] 分页查询教师列表
     */
    public Page<User> getTeachers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findByRoleIgnoreCase("TEACHER", pageable);
    }

    /**
     * [Admin/Controller专用] 通用用户列表，可按角色、用户名过滤
     */
    public Page<User> getUsers(int page, int size, String role, String keyword) {
        Pageable pageable = PageRequest.of(page, size);

        if (role != null && !role.isBlank() && keyword != null && !keyword.isBlank()) {
            return userRepository.findByRoleIgnoreCaseAndUsernameContainingIgnoreCase(role, keyword, pageable);
        }

        if (role != null && !role.isBlank()) {
            return userRepository.findByRoleIgnoreCase(role, pageable);
        }

        if (keyword != null && !keyword.isBlank()) {
            return userRepository.findByUsernameContainingIgnoreCase(keyword, pageable);
        }

        return userRepository.findAll(pageable);
    }

    /**
     * [Admin/Controller专用] 更新用户的角色/密码/简介等
     */
    public User adminUpdateUser(Long id, AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在。"));

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole().toUpperCase());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(request.getPassword());
        }

        if (request.getTitle() != null) {
            user.setTitle(request.getTitle());
        }

        if (request.getIntroduction() != null) {
            user.setIntroduction(request.getIntroduction());
        }

        return userRepository.save(user);
    }
}