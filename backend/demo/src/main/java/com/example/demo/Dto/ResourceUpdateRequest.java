package com.example.demo.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 资源更新请求 DTO（用于修改资源说明信息，不需要 courseId）
 */
@Data
public class ResourceUpdateRequest {

    @NotBlank(message = "资源标题不能为空")
    @Size(max = 255, message = "资源标题长度不能超过255个字符")
    private String title;

    @Size(max = 2000, message = "简介长度不能超过2000个字符")
    private String description;
}

