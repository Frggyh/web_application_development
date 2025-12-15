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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
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
            System.out.println("创建问题失败 - IllegalArgumentException: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // 课程不存在等
        } catch (Exception e) {
            // 文件存储或其他内部错误
            System.out.println("创建问题失败 - Exception: " + e.getMessage());
            e.printStackTrace();
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
     * 获取当前登录用户自己的提问列表，可按状态筛选
     * 说明：依赖全局 SecurityConfig 已经对 /api/** 做了认证拦截，
     * 这里不再使用 @PreAuthorize，避免表达式评估异常导致 403。
     */
    @GetMapping("/my-questions")
    public ResponseEntity<Page<Question>> getMyQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        try {
            User student = userService.findUserByUsername(authentication.getName());
            if (student == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
            Page<Question> questions = questionService.findMyQuestions(
                    student.getId(),
                    status,
                    PageRequest.of(page, size)
            );
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

    /**
     * [教师专用] 获取自己课程的未回答问题数量，用于提醒
     */
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @GetMapping("/teacher/unanswered-count")
    public ResponseEntity<Long> getUnansweredQuestionCount(Authentication authentication) {
        try {
            User teacher = userService.findUserByUsername(authentication.getName());
            Long count = questionService.countUnansweredQuestionsForTeacher(teacher.getId());
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [学生专用] 获取自己提出的、已被回答的问题数量，用于提醒
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @GetMapping("/student/answered-count")
    public ResponseEntity<Long> getAnsweredQuestionCount(Authentication authentication) {
        try {
            User student = userService.findUserByUsername(authentication.getName());
            Long count = questionService.countAnsweredQuestionsForStudent(student.getId());
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 全站问答搜索：支持按课程、教师、关键字、状态筛选
     */
    @PreAuthorize("isAuthenticated()") // 登录用户都可以搜索
    @GetMapping("/questions/search")
    public ResponseEntity<Page<Question>> searchAllQuestions(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String status, // UNANSWERED, ANSWERED
            Pageable pageable) {
        try {
            Page<Question> questions = questionService.searchAllQuestions(
                    courseId, teacherId, keyword, status, pageable);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 下载问题附件
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/questions/{questionId}/attachment")
    public ResponseEntity<org.springframework.core.io.Resource> downloadQuestionAttachment(
            @PathVariable Long questionId) {
        try {
            Question question = questionService.findById(questionId);
            if (question.getAttachmentPath() == null || question.getAttachmentPath().isEmpty()) {
                System.out.println("问题 " + questionId + " 没有附件");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 获取文件路径
            Path filePath = questionService.getAttachmentFilePath(question.getAttachmentPath());
            org.springframework.core.io.Resource fileResource = 
                    new org.springframework.core.io.FileSystemResource(filePath);

            System.out.println("尝试下载问题附件 - 问题ID: " + questionId);
            System.out.println("文件路径: " + filePath.toString());
            System.out.println("文件是否存在: " + fileResource.exists());

            if (!fileResource.exists()) {
                System.out.println("文件不存在: " + filePath.toString());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 获取文件名（优先使用原始文件名，如果没有则使用存储的文件名）
            String filename = question.getAttachmentFileName();
            System.out.println("问题附件原始文件名: " + filename);
            System.out.println("问题附件存储路径: " + question.getAttachmentPath());
            
            if (filename == null || filename.isEmpty()) {
                filename = question.getAttachmentPath();
                System.out.println("使用存储文件名: " + filename);
            } else {
                System.out.println("使用原始文件名: " + filename);
            }
            
            // 根据文件扩展名确定 Content-Type（使用原始文件名判断类型）
            String contentType = determineContentType(filename);
            
            // 对文件名进行 URL 编码，确保特殊字符正确处理
            String encodedFilename = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");

            System.out.println("最终下载文件名: " + filename);
            System.out.println("Content-Type: " + contentType);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(fileResource);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 下载回答附件
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/answers/{answerId}/attachment")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAnswerAttachment(
            @PathVariable Long answerId) {
        try {
            Answer answer = answerService.findById(answerId);
            if (answer.getAttachmentPath() == null || answer.getAttachmentPath().isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 获取文件路径
            Path filePath = answerService.getAttachmentFilePath(answer.getAttachmentPath());
            org.springframework.core.io.Resource fileResource = 
                    new org.springframework.core.io.FileSystemResource(filePath);

            if (!fileResource.exists()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 获取文件名（优先使用原始文件名，如果没有则使用存储的文件名）
            String filename = answer.getAttachmentFileName();
            if (filename == null || filename.isEmpty()) {
                filename = answer.getAttachmentPath();
            }
            
            // 根据文件扩展名确定 Content-Type
            String contentType = determineContentType(filename);
            
            // 对文件名进行 URL 编码
            String encodedFilename = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(fileResource);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 根据文件扩展名确定 Content-Type
     */
    private String determineContentType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }
        
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFilename.endsWith(".doc") || lowerFilename.endsWith(".docx")) {
            return "application/msword";
        } else if (lowerFilename.endsWith(".xls") || lowerFilename.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else if (lowerFilename.endsWith(".ppt") || lowerFilename.endsWith(".pptx")) {
            return "application/vnd.ms-powerpoint";
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerFilename.endsWith(".zip")) {
            return "application/zip";
        } else if (lowerFilename.endsWith(".rar")) {
            return "application/x-rar-compressed";
        } else {
            return "application/octet-stream";
        }
    }
}