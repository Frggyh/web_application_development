package com.example.demo.Service;

import com.example.demo.Dto.AnswerCreationRequest;
import com.example.demo.Dto.AnswerUpdateRequest;
import com.example.demo.Entity.Answer;
import com.example.demo.Entity.Question;
import com.example.demo.Entity.User;
import com.example.demo.Repository.AnswerRepository;
import com.example.demo.Repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class AnswerService {

    @Value("${file.upload-dir}") // 同样使用文件上传配置
    private String uploadDir;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionService questionService; // 用于更新问题状态

    @Autowired
    private UserService userService;

    /**
     * 教师回答学生问题
     */
    public Answer createAnswer(
            AnswerCreationRequest request,
            MultipartFile attachment,
            User replier) throws IOException {

        // 1. 验证用户角色 (确保是教师)
        if (!"TEACHER".equals(replier.getRole())) {
            throw new IllegalStateException("只有教师可以回答问题。");
        }

        // 2. 查找关联的 Question
        Question question = questionService.findById(request.getQuestionId());

        // 权限检查：确保该教师是该课程的授课教师
        Long courseIdOfQuestion = question.getCourse().getId();
        List<Long> taughtCourseIds = userService.findTaughtCourseIds(replier.getId());
        if (!taughtCourseIds.contains(courseIdOfQuestion)) {
            throw new SecurityException("无权限：该教师不负责该课程的问答。");
        }

        Answer answer = new Answer();
        
        // 3. 处理附件（如果存在）
        if (attachment != null && !attachment.isEmpty()) {
            String originalFilename = StringUtils.cleanPath(attachment.getOriginalFilename());

            // 直接使用原始文件名存储（如果文件已存在，则覆盖）
            // 确保上传目录存在，并正确拼接路径
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            Path copyLocation = uploadPath.resolve(originalFilename);
            Files.copy(attachment.getInputStream(), copyLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // 存储文件名和原始文件名都使用原始文件名
            answer.setAttachmentPath(originalFilename);
            answer.setAttachmentFileName(originalFilename);
        }
        answer.setContent(request.getContent());
        answer.setQuestion(question);
        answer.setReplier(replier);
        answer.setAnswerTime(LocalDateTime.now());

        // 4. 保存回答并更新问题状态 (不变)
        Answer savedAnswer = answerRepository.save(answer);
        questionService.markAsAnswered(question.getId());

        return savedAnswer;
    }

    /**
     * 获取某个问题的回答列表
     */
    public List<Answer> getAnswersByQuestionId(Long questionId) {
        return answerRepository.findByQuestionId(questionId);
    }

    /**
     * 修改回答 (教师本人/管理员)
     */
    public Answer updateAnswer(Long answerId, AnswerCreationRequest request) {
        Answer existing = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("回答不存在。"));

        existing.setContent(request.getContent());

        return answerRepository.save(existing);
    }

    /**
     * 管理员：修改回答信息（不需要 questionId）
     */
    public Answer updateAnswerInfo(Long answerId, AnswerUpdateRequest request) {
        Answer existing = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("回答不存在。"));

        existing.setContent(request.getContent());

        return answerRepository.save(existing);
    }

    /**
     * 删除回答 (教师本人/管理员)
     * 同时检查是否需要将 Question 状态改回 UNANSWERED
     */
    public void deleteAnswer(Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("回答不存在。"));

        Long questionId = answer.getQuestion().getId();
        answerRepository.delete(answer);

        // 检查问题是否还有其他回答
        if (answerRepository.findByQuestionId(questionId).isEmpty()) {
            // 如果没有其他回答，将问题状态改回 UNANSWERED
            Question question = questionService.findById(questionId);
            question.setStatus("UNANSWERED");
            questionRepository.save(question);
        }
    }

    /**
     * 管理员：查询所有回答（支持关键字搜索，不限制问题）
     */
    public Page<Answer> findAllAnswers(String keyword, Pageable pageable) {
        String searchKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;
        return answerRepository.findAllByKeyword(searchKeyword, pageable);
    }

    /**
     * 获取回答附件文件路径
     */
    public Path getAttachmentFilePath(String attachmentPath) {
        if (attachmentPath == null || attachmentPath.isEmpty()) {
            throw new IllegalArgumentException("附件路径为空");
        }
        // 使用 Paths.get 和 resolve 正确拼接路径，并使用 normalize 规范化路径
        Path uploadPath = Paths.get(uploadDir);
        Path filePath = uploadPath.resolve(attachmentPath).normalize();
        System.out.println("回答附件路径 - uploadDir: " + uploadDir);
        System.out.println("回答附件路径 - attachmentPath: " + attachmentPath);
        System.out.println("回答附件路径 - 完整路径: " + filePath.toString());
        return filePath;
    }

    /**
     * 根据ID获取回答
     */
    public Answer findById(Long answerId) {
        return answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("回答不存在。"));
    }

}