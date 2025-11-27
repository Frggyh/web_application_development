package com.example.demo.Repository;

import com.example.demo.Entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * 统计某个课程下未回答的问题数量 (用于教师新问题提醒)
     */
    long countByCourseIdAndStatus(Long courseId, String status);
}