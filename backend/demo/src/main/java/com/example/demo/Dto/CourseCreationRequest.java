package com.example.demo.Dto; // ★★★ 注意包名是 demo.Dto，没有 Controller ★★★

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CourseCreationRequest {

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 255, message = "课程名称长度不能超过255个字符")
    private String name;

    @Size(max = 2000, message = "描述长度不能超过2000个字符")
    private String description;

    // ★★★ 修正点：添加 college 字段 ★★★
    private String college;
}