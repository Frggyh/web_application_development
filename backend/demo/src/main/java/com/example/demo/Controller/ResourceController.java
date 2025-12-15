package com.example.demo.Controller;

import com.example.demo.Dto.ResourceCreationRequest;
import com.example.demo.Entity.Resource;
import com.example.demo.Entity.User;
import com.example.demo.Service.ResourceService;
import com.example.demo.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ResourceLoader; // 用于加载文件

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private UserService userService;

    // 注意：ResourceLoader 和 FileSystemResource 用于下载文件
    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * 1. 资源上传接口 (学生和教师都可以上传)
     * 使用 @RequestPart 接收文件和 JSON 数据
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> uploadResource(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "data", required = false) String dataJson,
            Authentication authentication) {

        System.out.println("=== 资源上传请求到达 ===");
        System.out.println("文件名: " + (file != null ? file.getOriginalFilename() : "null"));
        System.out.println("文件大小: " + (file != null ? file.getSize() : "null"));
        System.out.println("JSON 字符串: " + dataJson);
        System.out.println("当前用户: " + (authentication != null ? authentication.getName() : "null"));

        // 手动解析 JSON
        ResourceCreationRequest request = null;
        if (dataJson != null && !dataJson.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                request = objectMapper.readValue(dataJson, ResourceCreationRequest.class);
                System.out.println("解析后的请求数据: title=" + request.getTitle() + ", courseId=" + request.getCourseId());
            } catch (Exception e) {
                System.out.println("JSON 解析失败: " + e.getMessage());
                e.printStackTrace();
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } else {
            System.out.println("警告: data 部分为空");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // 验证请求数据
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            System.out.println("错误: 资源标题为空");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (request.getCourseId() == null) {
            System.out.println("错误: 课程ID为空");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            // 获取当前登录用户（确保用户存在）
            User uploader = userService.findUserByUsername(authentication.getName());

            if (uploader == null) {
                System.out.println("错误: 用户不存在");
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            Resource savedResource = resourceService.uploadResource(file, request, uploader);
            System.out.println("资源上传成功，ID: " + savedResource.getId());
            return new ResponseEntity<>(savedResource, HttpStatus.CREATED);

        } catch (SecurityException e) {
            System.out.println("权限错误: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            System.out.println("参数错误: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            System.out.println("IO错误: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.out.println("未知错误: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 2. 资源浏览与搜索接口 (通用功能)
     * 支持按课程ID、关键字搜索，并返回分页结果
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<Page<Resource>> searchResources(
            @RequestParam Long courseId,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Pageable pageable,
            Authentication authentication) { // ★★★ 接收当前登录用户 ★★★

        try {
            User currentUser = userService.findUserByUsername(authentication.getName());

            // 将 currentUser 传递给 Service
            Page<Resource> resources = resourceService.searchResources(
                    courseId,
                    keyword,
                    currentUser,
                    pageable);

            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 3. 文件下载接口 (通用功能)
     * 下载文件并增加下载计数
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@PathVariable Long id) {

        try {
            // 1. 查找资源并更新下载计数
            Resource resource = resourceService.incrementDownloadCount(id);

            // 2. 获取文件路径并加载文件
            Path filePath = resourceService.getResourceFilePath(resource.getFilePath());
            org.springframework.core.io.Resource fileResource = resourceLoader.getResource("file:" + filePath.toString());

            if (!fileResource.exists()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 3. 设置响应头
            String contentType = resource.getFileMimeType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream"; // 默认类型
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFileName() + "\"")
                    .body(fileResource);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 资源不存在
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ResourceController.java (补充修改资源接口)

    /**
     * 修改资源信息（仅限上传者或管理员）
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @resourceService.findById(#id).uploader.username == authentication.name")
    @PutMapping("/{id}")
    public ResponseEntity<Resource> updateResource(
            @PathVariable Long id,
            @RequestBody @Valid ResourceCreationRequest request) { // 可以复用上传时的DTO
        try {
            Resource updatedResource = resourceService.updateResource(id, request);
            return ResponseEntity.ok(updatedResource);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * 获取当前用户上传的资源列表（学生/教师个人中心）
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-uploads")
    public ResponseEntity<Page<Resource>> getMyUploads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            User currentUser = userService.findUserByUsername(authentication.getName());
            Page<Resource> resources = resourceService.findMyResources(
                    currentUser.getId(),
                    PageRequest.of(page, size));
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 删除资源（限制为上传者或管理员）
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @resourceService.findById(#id).uploader.username == authentication.name")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        try {
            resourceService.deleteResource(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}