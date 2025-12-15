package com.example.demo.Controller;

import com.example.demo.Dto.AdminUserUpdateRequest;
import com.example.demo.Dto.AnswerCreationRequest;
import com.example.demo.Dto.AnswerUpdateRequest;
import com.example.demo.Dto.CourseAssignmentRequest;
import com.example.demo.Dto.QuestionCreationRequest;
import com.example.demo.Dto.QuestionUpdateRequest;
import com.example.demo.Dto.ResourceCreationRequest;
import com.example.demo.Dto.ResourceUpdateRequest;
import com.example.demo.Entity.Answer;
import com.example.demo.Entity.Course;
import com.example.demo.Entity.Question;
import com.example.demo.Entity.Resource;
import com.example.demo.Entity.User;
import com.example.demo.Service.AnswerService;
import com.example.demo.Service.QuestionService;
import com.example.demo.Service.ResourceService;
import com.example.demo.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    // ==========================================================
    // 教师管理 (所有方法都继承了类上的 ROLE_ADMIN 权限)
    // ==========================================================

    /**
     * 0.1 获取教师列表（分页）
     */
    @GetMapping("/teachers")
    public ResponseEntity<Page<User>> getTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<User> teachers = userService.getTeachers(page, size);
        return ResponseEntity.ok(teachers);
    }

    /**
     * 0.2 通用用户列表（支持角色、关键词筛选）
     */
    @GetMapping("/users")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword
    ) {
        Page<User> users = userService.getUsers(page, size, role, keyword);
        return ResponseEntity.ok(users);
    }

    /**
     * 0.3 管理员更新用户（角色、密码、简介等）
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request
    ) {
        try {
            User updatedUser = userService.adminUpdateUser(id, request);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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

    // ==========================================================
    // 学习资源管理 (管理员专用)
    // ==========================================================

    /**
     * 管理员：获取所有资源列表（支持关键字搜索）
     */
    @GetMapping("/resources")
    public ResponseEntity<Page<Resource>> getAllResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        try {
            Page<Resource> resources = resourceService.searchAllResources(
                    keyword,
                    PageRequest.of(page, size)
            );
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 管理员：修改资源说明信息
     */
    @PutMapping("/resources/{id}")
    public ResponseEntity<Resource> updateResourceInfo(
            @PathVariable Long id,
            @RequestBody @Valid ResourceUpdateRequest request
    ) {
        try {
            Resource updatedResource = resourceService.updateResourceInfo(id, request);
            return ResponseEntity.ok(updatedResource);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 管理员：删除资源
     */
    @DeleteMapping("/resources/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        try {
            resourceService.deleteResource(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==========================================================
    // 问答内容管理 (管理员专用)
    // ==========================================================

    /**
     * 管理员：获取所有问题列表（支持关键字搜索和状态筛选）
     */
    @GetMapping("/questions")
    public ResponseEntity<Page<Question>> getAllQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        try {
            Page<Question> questions = questionService.findAllQuestions(
                    keyword,
                    status,
                    PageRequest.of(page, size)
            );
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 管理员：获取所有回答列表（支持关键字搜索）
     */
    @GetMapping("/answers")
    public ResponseEntity<Page<Answer>> getAllAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        try {
            Page<Answer> answers = answerService.findAllAnswers(
                    keyword,
                    PageRequest.of(page, size)
            );
            return ResponseEntity.ok(answers);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 管理员：修改问题
     */
    @PutMapping("/questions/{id}")
    public ResponseEntity<Question> updateQuestion(
            @PathVariable Long id,
            @RequestBody @Valid QuestionUpdateRequest request
    ) {
        try {
            Question updatedQuestion = questionService.updateQuestionInfo(id, request);
            return ResponseEntity.ok(updatedQuestion);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 管理员：修改回答
     */
    @PutMapping("/answers/{id}")
    public ResponseEntity<Answer> updateAnswer(
            @PathVariable Long id,
            @RequestBody @Valid AnswerUpdateRequest request
    ) {
        try {
            Answer updatedAnswer = answerService.updateAnswerInfo(id, request);
            return ResponseEntity.ok(updatedAnswer);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 管理员：删除问题
     */
    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        try {
            questionService.deleteQuestion(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 管理员：删除回答
     */
    @DeleteMapping("/answers/{id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long id) {
        try {
            answerService.deleteAnswer(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}