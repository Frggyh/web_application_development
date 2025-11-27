package com.example.demo.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class ResourceCreationRequest {

    @NotBlank(message = "资源标题不能为空")
    @Size(max = 255, message = "资源标题长度不能超过255个字符")
    private String title;

    @Size(max = 2000, message = "简介长度不能超过2000个字符")
    private String description;

    @NotNull(message = "所属课程ID不能为空")
    private Long courseId; // 所属课程ID

    // 教师可设置的可见性（学生上传时可忽略，由系统默认）
    private String visibility;
}