package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // 简化后的核心字段：角色
    @Column(nullable = false)
    private String role; // ADMIN, TEACHER, STUDENT

    @Column(length = 100)
    private String title; // 职称 (教师专用)

    @Column(length = 500)
    private String introduction; // 简介 (教师专用)

    @Column(length = 100)
    private String email; // 邮箱

    @Column(length = 500)
    private String avatar; // 头像URL或文件路径
}