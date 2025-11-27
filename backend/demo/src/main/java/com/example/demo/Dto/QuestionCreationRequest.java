package com.example.demo.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class QuestionCreationRequest {

    @NotBlank(message = "问题标题不能为空")
    @Size(max = 255, message = "问题标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "问题内容不能为空")
    @Size(max = 5000, message = "问题内容长度不能超过5000个字符")
    private String content;

    @NotNull(message = "所属课程ID不能为空")
    private Long courseId; // 所属课程ID

    // 附件文件名，实际的文件内容会作为 MultipartFile 在 Controller 中接收
    private String attachmentFileName;
}