package com.example.demo.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException e) {
        System.out.println("=== 参数验证失败 ===");
        e.printStackTrace();
        StringBuilder errors = new StringBuilder();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        });
        System.out.println("验证错误: " + errors.toString());
        return new ResponseEntity<>("参数验证失败: " + errors.toString(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipartException(MultipartException e) {
        System.out.println("=== Multipart 解析失败 ===");
        e.printStackTrace();
        System.out.println("错误信息: " + e.getMessage());
        return new ResponseEntity<>("文件上传失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        System.out.println("=== 文件大小超限 ===");
        e.printStackTrace();
        return new ResponseEntity<>("文件大小超过限制", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        System.out.println("=== 未捕获的异常 ===");
        System.out.println("异常类型: " + e.getClass().getName());
        System.out.println("异常信息: " + e.getMessage());
        e.printStackTrace();
        return new ResponseEntity<>("服务器错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

