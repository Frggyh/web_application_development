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
}