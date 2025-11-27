package com.example.demo.Controller;

import com.example.demo.Dto.AnswerCreationRequest;
import com.example.demo.Dto.QuestionCreationRequest;
import com.example.demo.Entity.Answer;
import com.example.demo.Entity.Question;
import com.example.demo.Entity.User;
import com.example.demo.Service.AnswerService;
import com.example.demo.Service.QuestionService;
import com.example.demo.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/qa")
public class QuestionAnswerController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private UserService userService;

    // ==========================================================
    // 1. 学生提问 (Question) 接口
    // ==========================================================

    /**
     * 学生提问 (支持附件上传)
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')") // 只有学生可以提问
    @PostMapping(value = "/questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Question> createQuestion(
            @RequestPart(value = "data") @Valid QuestionCreationRequest request,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            Authentication authentication) {
        try {
            User asker = userService.findUserByUsername(authentication.getName());
            if (asker == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            Question question = questionService.createQuestion(request, attachment, asker);
            return new ResponseEntity<>(question, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // 课程不存在等
        } catch (Exception e) {
            // 文件存储或其他内部错误
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 查询和搜索问题 (通用功能)
     * 支持分页、按课程ID、关键字和状态搜索
     */
    @PreAuthorize("isAuthenticated()") // 所有登录用户都可以查看问答
    @GetMapping("/questions")
    public ResponseEntity<Page<Question>> searchQuestions(
            @RequestParam(required = true) Long courseId,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String status, // UNANSWERED, ANSWERED
            Pageable pageable) {

        try {
            Page<Question> questions = questionService.searchQuestions(courseId, keyword, status, pageable);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 删除问题 (提问者本人或管理员)
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @questionService.findById(#id).asker.username == authentication.name")
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

    // ==========================================================
    // 2. 教师回答 (Answer) 接口
    // ==========================================================

    /**
     * 教师回答问题 (支持附件上传)
     */
    @PreAuthorize("hasAuthority('ROLE_TEACHER')") // 只有教师可以回答
    @PostMapping(value = "/answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Answer> createAnswer(
            @RequestPart(value = "data") @Valid AnswerCreationRequest request,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            Authentication authentication) {
        try {
            User replier = userService.findUserByUsername(authentication.getName());
            if (replier == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            // TODO: 【核心权限】在 Service 层或此处的 @PreAuthorize 中，需要检查 replier 是否是该问题所属课程的授课教师。

            Answer answer = answerService.createAnswer(request, attachment, replier);
            return new ResponseEntity<>(answer, HttpStatus.CREATED);

        }catch (SecurityException e) {
            // 捕获自定义的 SecurityException 并返回 403
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // 非教师尝试回答
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // 问题ID不存在等
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 查看某个问题的回答列表
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/questions/{questionId}/answers")
    public ResponseEntity<List<Answer>> getAnswersByQuestionId(@PathVariable Long questionId) {
        try {
            List<Answer> answers = answerService.getAnswersByQuestionId(questionId);
            return ResponseEntity.ok(answers);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 修改回答 (回答者本人或管理员)
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @answerService.findById(#id).replier.username == authentication.name")
    @PutMapping("/answers/{id}")
    public ResponseEntity<Answer> updateAnswer(@PathVariable Long id, @RequestBody @Valid AnswerCreationRequest request) {
        try {
            Answer updatedAnswer = answerService.updateAnswer(id, request);
            return ResponseEntity.ok(updatedAnswer);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==========================================================
    // 3. 教师提醒功能
    // ==========================================================

    /**
     * 教师新问题提醒：获取所上课程的未回答问题数量
     */
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @GetMapping("/teacher/unanswered-count/{courseId}")
    public ResponseEntity<Long> getUnansweredCount(@PathVariable Long courseId) {
        try {
            // TODO: 【核心权限】需要校验 courseId 是否是当前登录教师所教的课程。
            long count = questionService.countUnansweredQuestionsByCourse(courseId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 修改问题 (提问者本人或管理员)
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @questionService.findById(#id).asker.username == authentication.name")
    @PutMapping("/questions/{id}")
    public ResponseEntity<Question> updateQuestion(
            @PathVariable Long id,
            @RequestBody @Valid QuestionCreationRequest request) { // 复用创建DTO
        try {
            Question updatedQuestion = questionService.updateQuestion(id, request);
            return ResponseEntity.ok(updatedQuestion);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}