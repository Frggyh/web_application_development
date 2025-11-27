package com.example.demo.Service;

import com.example.demo.Dto.AnswerCreationRequest;
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
import java.util.UUID;

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

        // ... 3. 处理附件（不变） ...

        Answer answer = new Answer();
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


}