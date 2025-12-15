package com.example.demo.Repository;

import com.example.demo.Entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * 根据课程ID、关键字（标题/内容）和状态搜索问题，并支持分页
     * @param courseId 课程ID
     * @param keyword 搜索关键字
     * @param status 问题状态 ("UNANSWERED", "ANSWERED")
     * @param pageable 分页信息
     * @return 分页结果
     */
    @Query("SELECT q FROM Question q " +
            "WHERE q.course.id = :courseId " +
            "AND (:status IS NULL OR q.status = :status) " +
            "AND (q.title LIKE %:keyword% OR q.content LIKE %:keyword%)")
    Page<Question> findByCourseIdAndStatusAndKeyword(
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);

    /**
     * 查找某个用户提出的所有问题 (用于学生个人中心)
     */
    Page<Question> findByAskerId(Long askerId, Pageable pageable);

    /**
     * 查找某个用户提出的、按状态过滤的问题列表
     */
    Page<Question> findByAskerIdAndStatus(Long askerId, String status, Pageable pageable);

    /**
     * 统计某个课程下未回答的问题数量 (用于教师新问题提醒)
     */
    long countByCourseIdAndStatus(Long courseId, String status);

    /**
     * 统计指定课程ID列表中，状态为 'UNANSWERED' 的问题数量
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.course.id IN :courseIds AND q.status = 'UNANSWERED'")
    Long countUnansweredByCourseIds(@Param("courseIds") List<Long> courseIds);

    /**
     * 统计指定学生提出的、状态为 'ANSWERED' 的问题数量
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.asker.id = :studentId AND q.status = 'ANSWERED'")
    Long countAnsweredQuestionsByStudentId(@Param("studentId") Long studentId);

    /**
     * 统计指定学生提出的问题总数
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.asker.id = :studentId")
    Long countQuestionsByStudentId(@Param("studentId") Long studentId);

    /**
     * 管理员：查询所有问题（支持关键字搜索和状态筛选，不限制课程）
     */
    @Query("SELECT q FROM Question q " +
            "WHERE (:status IS NULL OR q.status = :status) " +
            "AND (:keyword IS NULL OR :keyword = '' OR q.title LIKE CONCAT('%', :keyword, '%') OR q.content LIKE CONCAT('%', :keyword, '%'))")
    Page<Question> findAllByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);

    /**
     * 学生全站搜索：支持按课程、教师、关键字、状态筛选
     * @param courseId 课程ID（可选）
     * @param teacherId 教师ID（可选，通过课程关联）
     * @param keyword 关键字（可选，搜索标题和内容）
     * @param status 状态（可选：UNANSWERED, ANSWERED）
     * @param pageable 分页信息
     * @return 分页结果
     */
    @Query("SELECT q FROM Question q " +
            "WHERE (:courseId IS NULL OR q.course.id = :courseId) " +
            "AND (:teacherId IS NULL OR q.course.teacherId = :teacherId) " +
            "AND (:status IS NULL OR q.status = :status) " +
            "AND (:keyword IS NULL OR :keyword = '' OR q.title LIKE CONCAT('%', :keyword, '%') OR q.content LIKE CONCAT('%', :keyword, '%'))")
    Page<Question> searchAllQuestions(
            @Param("courseId") Long courseId,
            @Param("teacherId") Long teacherId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);
}