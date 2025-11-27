package com.example.demo.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class AnswerCreationRequest {

    @NotBlank(message = "回答内容不能为空")
    @Size(max = 5000, message = "回答内容长度不能超过5000个字符")
    private String content;

    @NotNull(message = "关联问题ID不能为空")
    private Long questionId; // 关联的问题ID

    // 附件文件名，实际的文件内容会作为 MultipartFile 在 Controller 中接收
    private String attachmentFileName;
}