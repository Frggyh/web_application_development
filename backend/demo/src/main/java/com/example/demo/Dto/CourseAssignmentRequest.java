package com.example.demo.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class CourseAssignmentRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotNull(message = "教师ID不能为空")
    private Long teacherId;
}