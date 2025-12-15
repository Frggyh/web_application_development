package com.example.demo.Repository;

import com.example.demo.Entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /**
     * 查找某个问题的所有回答 (通常只有一个，但设计上允许多个)
     */
    List<Answer> findByQuestionId(Long questionId);

    /**
     * 查找某个教师的所有回答 (用于教师个人中心)
     */
    List<Answer> findByReplierId(Long replierId);

    /**
     * 统计某个学生尚未阅读的回答数量
     */
    long countByQuestionAskerIdAndIsReadByAskerFalse(Long askerId);

    /**
     * 管理员：查询所有回答（支持关键字搜索，不限制问题）
     */
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Answer a " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR a.content LIKE CONCAT('%', :keyword, '%'))")
    org.springframework.data.domain.Page<Answer> findAllByKeyword(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);
}