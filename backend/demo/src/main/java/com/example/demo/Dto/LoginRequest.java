package com.example.demo.Dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}