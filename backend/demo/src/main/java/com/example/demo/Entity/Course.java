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

    // 实际项目中，通常需要关联授课教师，我们稍后在 Repository 中处理关联查询。
    // private Long teacherId;
}