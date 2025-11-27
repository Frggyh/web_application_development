package com.example.demo.Controller;

import com.example.demo.Dto.ResourceCreationRequest;
import com.example.demo.Entity.Resource;
import com.example.demo.Entity.User;
import com.example.demo.Service.ResourceService;
import com.example.demo.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
     * 使用 @RequestParam 接收文件，使用 @ModelAttribute 接收 JSON/表单数据
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> uploadResource(
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") @Valid ResourceCreationRequest request,
            Authentication authentication) {

        try {
            // 获取当前登录用户（确保用户存在）
            User uploader = userService.findUserByUsername(authentication.getName());

            if (uploader == null) {
                // 如果 Token 是有效的，但用户在 DB 中被删除了，返回 404/403
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            Resource savedResource = resourceService.uploadResource(file, request, uploader);
            return new ResponseEntity<>(savedResource, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            // 记录文件存储错误
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
    // ... 未来可添加 deleteResource, updateResourceInfo 等方法
}