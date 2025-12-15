package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生提问实体
 */
@Data
@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title; // 提问标题

    @Column(nullable = false, length = 5000)
    private String content; // 提问内容

    // 提问者 (学生)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asker_id", nullable = false)
    private User asker;

    // 所属课程
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime askTime; // 提问时间 (系统记录)

    // 附件路径（支持上传图片作为附件）
    @Column(length = 255)
    private String attachmentPath;
    
    // 附件原始文件名
    @Column(length = 255)
    private String attachmentFileName;

    // 问题状态：UNANSWERED, ANSWERED
    @Column(length = 20, nullable = false)
    private String status = "UNANSWERED";

    // 标记是否已被老师查阅过 (用于新问题提醒)
    @Column(columnDefinition = "boolean default false")
    private Boolean isNew = true;
}