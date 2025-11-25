package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

// 导入关联的实体类，确保使用您的大写 Entity 包名
import com.example.demo.Entity.Course;
import com.example.demo.Entity.User;

@Data
@Entity
@Table(name = "resource")
public class Resource {

    // ★★★ 修正的主键定义 ★★★
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String filePath;

    private String fileType;

    private Long downloadCount = 0L;

    @Column(nullable = false)
    private LocalDateTime uploadTime = LocalDateTime.now();

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;
}