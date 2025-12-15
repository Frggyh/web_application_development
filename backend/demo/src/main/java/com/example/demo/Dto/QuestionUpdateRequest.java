package com.example.demo.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 问题更新请求 DTO（用于修改问题，不需要 courseId）
 */
@Data
public class QuestionUpdateRequest {

    @NotBlank(message = "问题标题不能为空")
    @Size(max = 255, message = "问题标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "问题内容不能为空")
    @Size(max = 5000, message = "问题内容长度不能超过5000个字符")
    private String content;
}

