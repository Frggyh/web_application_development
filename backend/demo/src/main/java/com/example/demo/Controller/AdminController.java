package com.example.demo.Controller;

import com.example.demo.Dto.CourseAssignmentRequest;
import com.example.demo.Entity.Course;
import com.example.demo.Entity.User;
import com.example.demo.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 确保导入此包
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
// ★★★ 修正点：添加管理员权限注解 ★★★
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    // ==========================================================
    // 教师管理 (所有方法都继承了类上的 ROLE_ADMIN 权限)
    // ==========================================================

    /**
     * 1. 添加教师账号
     */
    @PostMapping("/teachers")
    public ResponseEntity<User> createTeacher(@RequestBody @Valid User teacher) {
        // ... (方法体不变)
        try {
            teacher.setRole("TEACHER");
            User newTeacher = userService.createTeacher(teacher);
            return new ResponseEntity<>(newTeacher, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 2. 更新教师信息 (简介、职称等)
     */
    @PutMapping("/teachers/{id}")
    public ResponseEntity<User> updateTeacher(@PathVariable Long id, @RequestBody User updateDetails) {
        // ... (方法体不变)
        try {
            User updatedTeacher = userService.updateTeacherProfile(id, updateDetails);
            return ResponseEntity.ok(updatedTeacher);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 3. 删除教师账号
     */
    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        // ... (方法体不变)
        try {
            userService.deleteTeacher(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 4. 设置教师讲授课程 (分配课程)
     */
    @PostMapping("/teachers/assign-course")
    public ResponseEntity<Course> assignCourse(@RequestBody @Valid CourseAssignmentRequest request) {
        // ... (方法体不变)
        try {
            Course course = userService.assignCourseToTeacher(request.getCourseId(), request.getTeacherId());
            return ResponseEntity.ok(course);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}