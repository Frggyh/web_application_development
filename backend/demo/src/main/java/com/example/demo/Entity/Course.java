package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 课程名称

    private String description; // 课程内容描述

    private String college; // 开课学院

    // ★★★ 修正点：添加 teacherId 字段 ★★★
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
}