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

    // 简化后的个人简介/职称等信息
    private String profile;
}