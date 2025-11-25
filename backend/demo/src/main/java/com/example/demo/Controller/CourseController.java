package com.example.demo.Controller;

import com.example.demo.Controller.Dto.CourseCreationRequest;
import com.example.demo.Entity.Course;
import com.example.demo.Service.CourseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // ★★★ 导入 @PreAuthorize ★★★
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    // GET /api/courses: 对所有已认证用户开放 (默认行为)
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        // 由于我们在 SecurityConfig 中设置了 anyRequest().authenticated()，所以所有请求都必须带Token
        return ResponseEntity.ok(courseService.findAllCourses());
    }

    // ★★★ 新增：只有 TEACHER 角色可以创建课程 ★★★
    // 注意：hasRole('TEACHER') 会自动在传入的 'TEACHER' 前面加上 'ROLE_'
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @PostMapping
    public ResponseEntity<Course> createCourse(@Valid @RequestBody CourseCreationRequest request) {

        Course newCourse = new Course();
        newCourse.setName(request.getName());
        newCourse.setDescription(request.getDescription());

        Course savedCourse = courseService.saveCourse(newCourse);

        return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
    }
}