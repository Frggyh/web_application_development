package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 教师回答实体
 */
@Data
@Entity
@Table(name = "answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5000)
    private String content; // 回答内容

    // 回答者 (教师)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replier_id", nullable = false)
    private User replier;

    // 关联的问题
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private LocalDateTime answerTime; // 回答时间 (系统记录)

    // 附件路径（支持图片附件）
    @Column(length = 255)
    private String attachmentPath;
    
    // 附件原始文件名
    @Column(length = 255)
    private String attachmentFileName;

    // 是否被提问者查阅过 (用于通知提醒)
    @Column(columnDefinition = "boolean default false")
    private Boolean isReadByAsker = false;
}