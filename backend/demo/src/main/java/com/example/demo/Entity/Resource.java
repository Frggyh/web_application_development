package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;



/**
 * 学习资源实体
 */
@Data
@Entity
@Table(name = "resource")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title; // 资源标题

    @Column(length = 2000)
    private String description; // 资源简介/说明

    // 关联的课程
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // 上传者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false)
    private LocalDateTime uploadTime; // 上传时间 (系统记录)

    // 文件存储路径和名称
    @Column(nullable = false)
    private String filePath; // 文件在服务器上的存储路径

    @Column(nullable = false, length = 255)
    private String fileName; // 原始文件名

    @Column(length = 50)
    private String fileMimeType; // 文件MIME类型

    // 下载次数
    @Column(columnDefinition = "int default 0")
    private Integer downloadCount = 0;

    // 可见性：'ALL' (全部学生), 'CLASS_ONLY' (仅本班学生)
    // 默认为 'ALL'
    @Column(nullable = false, length = 10)
    private String visibility = "ALL";
}