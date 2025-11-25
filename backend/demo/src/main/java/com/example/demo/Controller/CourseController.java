package com.example.demo.Controller;

// ★★★ 修正点：根据您的文件结构，Dto 在 demo 包下，而不是 Controller 包下 ★★★
import com.example.demo.Dto.CourseCreationRequest;
import com.example.demo.Entity.Course;
import com.example.demo.Entity.User; // 导入 User 实体
import com.example.demo.Service.CourseService;
import com.example.demo.Service.UserService; // 导入 UserService
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; // 导入 Authentication
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired // 注入 UserService 以获取用户实体
    private UserService userService;

    // GET /api/courses: 对所有已认证用户开放 (默认行为)
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.findAllCourses());
    }

    // ★★★ 新增：只有 TEACHER 角色可以创建课程 ★★★
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @PostMapping
    public ResponseEntity<Course> createCourse(@Valid @RequestBody CourseCreationRequest request, Authentication authentication) {

        // 1. 获取当前登录用户的 ID，并将其作为创建者 ID
        User currentUser = userService.findUserByUsername(authentication.getName());

        Course newCourse = new Course();
        newCourse.setName(request.getName());
        newCourse.setDescription(request.getDescription());
        newCourse.setCollege(request.getCollege());
        newCourse.setTeacherId(currentUser.getId()); // ★★★ 设置创建者 ID ★★★

        return new ResponseEntity<>(courseService.saveCourse(newCourse), HttpStatus.CREATED);
    }

    // ★★★ 新增：更新课程（只有 ADMIN 或 课程创建者可以） ★★★
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id,
                                               @Valid @RequestBody CourseCreationRequest request,
                                               Authentication authentication) {

        // 1. 获取当前登录用户的用户名
        String currentUsername = authentication.getName();

        // 2. 根据课程ID获取课程
        Course existingCourse = courseService.findCourseById(id);
        if (existingCourse == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // 3. 检查所有权：如果是 TEACHER，必须是课程的创建者
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {

            // 获取完整的 User 实体以进行比较
            User currentUser = userService.findUserByUsername(currentUsername);

            // 检查当前用户ID是否等于课程的teacher_id
            if (!existingCourse.getTeacherId().equals(currentUser.getId())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN); // 403 Forbidden
            }
        }

        // 4. 执行更新操作
        existingCourse.setName(request.getName());
        existingCourse.setDescription(request.getDescription());
        existingCourse.setCollege(request.getCollege());

        return ResponseEntity.ok(courseService.saveCourse(existingCourse));
    }

    // ... 其他方法（例如 deleteCourse）
}