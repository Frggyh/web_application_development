package com.example.demo.Dto;

import lombok.Data;

/**
 * 管理员更新用户信息的请求体。
 * 支持更新角色、重置密码以及教师的扩展信息。
 */
@Data
public class AdminUserUpdateRequest {
    private String role;
    private String password;
    private String title;
    private String introduction;
}

