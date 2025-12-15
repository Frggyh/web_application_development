package com.example.demo.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 回答更新请求 DTO（用于修改回答，不需要 questionId）
 */
@Data
public class AnswerUpdateRequest {

    @NotBlank(message = "回答内容不能为空")
    @Size(max = 5000, message = "回答内容长度不能超过5000个字符")
    private String content;
}

