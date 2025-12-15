package com.example.demo.Service;

import com.example.demo.Dto.QuestionCreationRequest;
import com.example.demo.Dto.QuestionUpdateRequest;
import com.example.demo.Entity.Course;
import com.example.demo.Entity.Question;
import com.example.demo.Entity.User;
import com.example.demo.Repository.CourseRepository;
import com.example.demo.Repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuestionService {

    // 使用与ResourceService相同的文件上传目录配置
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserService userService;

    /**
     * 学生提问并上传附件
     */
    public Question createQuestion(
            QuestionCreationRequest request,
            MultipartFile attachment,
            User asker) throws IOException {

        // 1. 验证用户角色 (确保是学生)
        if (!"STUDENT".equals(asker.getRole())) {
            // 在 Controller 中应使用 @PreAuthorize 或抛出更恰当的异常
            throw new IllegalStateException("只有学生可以提问。");
        }

        // 2. 查找关联的 Course
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("课程不存在。"));

        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setCourse(course);
        question.setAsker(asker);
        question.setAskTime(LocalDateTime.now());

        // 3. 处理附件（如果存在）
        if (attachment != null && !attachment.isEmpty()) {
            String originalFilename = StringUtils.cleanPath(attachment.getOriginalFilename());
            
            // 直接使用原始文件名存储（如果文件已存在，则覆盖）
            // 确保上传目录存在，并正确拼接路径
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            Path copyLocation = uploadPath.resolve(originalFilename);
            
            System.out.println("=== 问题附件上传 ===");
            System.out.println("原始文件名: " + originalFilename);
            System.out.println("存储文件名: " + originalFilename + " (使用原始文件名)");
            System.out.println("上传目录: " + uploadPath.toString());
            System.out.println("完整保存路径: " + copyLocation.toString());
            
            Files.copy(attachment.getInputStream(), copyLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("文件保存成功，路径: " + copyLocation.toString());
            System.out.println("文件是否存在: " + Files.exists(copyLocation));

            // 存储文件名和原始文件名都使用原始文件名
            question.setAttachmentPath(originalFilename);
            question.setAttachmentFileName(originalFilename);
            
            System.out.println("已设置 attachmentPath: " + originalFilename);
            System.out.println("已设置 attachmentFileName: " + originalFilename);
        }

        question.setStatus("UNANSWERED");
        question.setIsNew(true); // 标记为新问题

        return questionRepository.save(question);
    }

    /**
     * 提问搜索：按学科、关键字检索
     */
    public Page<Question> searchQuestions(
            Long courseId,
            String keyword,
            String status, // 可选参数：UNANSWERED 或 ANSWERED
            Pageable pageable) {

        String searchKeyword = keyword == null ? "" : keyword;

        return questionRepository.findByCourseIdAndStatusAndKeyword(
                courseId,
                searchKeyword,
                status,
                pageable);
    }

    /**
     * 获取单个问题详情
     */
    public Question findById(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("问题不存在。"));
    }

    /**
     * 更新问题状态为已回答
     */
    public void markAsAnswered(Long questionId) {
        Question question = findById(questionId);
        question.setStatus("ANSWERED");
        question.setIsNew(false); // 回答后不再是新问题
        questionRepository.save(question);
    }

    /**
     * 获取用户提出的问题列表 (学生个人中心，可按状态过滤)
     */
    public Page<Question> findMyQuestions(Long askerId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return questionRepository.findByAskerIdAndStatus(askerId, status, pageable);
        }
        return questionRepository.findByAskerId(askerId, pageable);
    }

    /**
     * 删除问题 (管理员/提问者本人)
     */
    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }

    /**
     * 统计教师所上课程的未回答问题数量 (用于教师新问题提醒)
     */
    public long countUnansweredQuestionsByCourse(Long courseId) {
        return questionRepository.countByCourseIdAndStatus(courseId, "UNANSWERED");
    }

    public Question updateQuestion(Long id, QuestionCreationRequest request) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("问题不存在。"));

        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        // 不允许修改所属课程

        return questionRepository.save(existing);
    }

    /**
     * 管理员：修改问题信息（不需要 courseId）
     */
    public Question updateQuestionInfo(Long id, QuestionUpdateRequest request) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("问题不存在。"));

        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        // 不允许修改所属课程

        return questionRepository.save(existing);
    }

    /**
     * 统计指定教师所教授课程的未回答问题数量
     */
    public Long countUnansweredQuestionsForTeacher(Long teacherId) {
        // 1. 获取该教师负责的所有课程ID
        List<Long> courseIds = userService.findTaughtCourseIds(teacherId);

        if (courseIds.isEmpty()) {
            return 0L; // 如果没有负责任何课程，则返回0
        }

        // 2. 使用 Repository 进行计数
        return questionRepository.countUnansweredByCourseIds(courseIds);
    }

    /**
     * 统计指定学生提出的问题总数
     */
    public Long countQuestionsByStudentId(Long studentId) {
        return questionRepository.countQuestionsByStudentId(studentId);
    }

    /**
     * 统计指定学生提出的、已被回答的问题数量
     */
    public Long countAnsweredQuestionsByStudentId(Long studentId) {
        return questionRepository.countAnsweredQuestionsByStudentId(studentId);
    }

    /**
     * 统计指定学生提出的、已被回答的问题数量（别名方法，保持兼容性）
     */
    public Long countAnsweredQuestionsForStudent(Long studentId) {
        return questionRepository.countAnsweredQuestionsByStudentId(studentId);
    }

    /**
     * 管理员：查询所有问题（支持关键字搜索和状态筛选，不限制课程）
     */
    public Page<Question> findAllQuestions(String keyword, String status, Pageable pageable) {
        String searchKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;
        String searchStatus = (status == null || status.isBlank()) ? null : status;
        return questionRepository.findAllByKeywordAndStatus(searchKeyword, searchStatus, pageable);
    }

    /**
     * 学生全站搜索：支持按课程、教师、关键字、状态筛选
     * @param courseId 课程ID（可选）
     * @param teacherId 教师ID（可选）
     * @param keyword 关键字（可选）
     * @param status 状态（可选）
     * @param pageable 分页信息
     * @return 分页结果
     */
    public Page<Question> searchAllQuestions(Long courseId, Long teacherId, String keyword, String status, Pageable pageable) {
        Long searchCourseId = (courseId != null && courseId > 0) ? courseId : null;
        Long searchTeacherId = (teacherId != null && teacherId > 0) ? teacherId : null;
        String searchKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;
        String searchStatus = (status == null || status.isBlank()) ? null : status;
        return questionRepository.searchAllQuestions(searchCourseId, searchTeacherId, searchKeyword, searchStatus, pageable);
    }

    /**
     * 获取问题附件文件路径
     */
    public Path getAttachmentFilePath(String attachmentPath) {
        if (attachmentPath == null || attachmentPath.isEmpty()) {
            throw new IllegalArgumentException("附件路径为空");
        }
        // 使用 Paths.get 和 resolve 正确拼接路径，并使用 normalize 规范化路径
        Path uploadPath = Paths.get(uploadDir);
        Path filePath = uploadPath.resolve(attachmentPath).normalize();
        System.out.println("问题附件路径 - uploadDir: " + uploadDir);
        System.out.println("问题附件路径 - attachmentPath: " + attachmentPath);
        System.out.println("问题附件路径 - 完整路径: " + filePath.toString());
        return filePath;
    }
}