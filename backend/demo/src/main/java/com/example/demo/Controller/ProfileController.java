package com.example.demo.Controller;

import com.example.demo.Dto.PasswordUpdateRequest;
import com.example.demo.Entity.User;
import com.example.demo.Service.QuestionService;
import com.example.demo.Service.ResourceService;
import com.example.demo.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()") // 所有登录用户都可以访问个人中心
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private ResourceService resourceService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 获取当前登录用户的个人信息
     */
    @GetMapping
    public ResponseEntity<User> getMyProfile(Authentication authentication) {
        // 使用 authentication.getName() 获取当前用户的 username
        User user = userService.findUserByUsername(authentication.getName());
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // 为了安全，不返回密码
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    /**
     * 更新当前登录用户的个人信息 (例如邮箱、简介等)
     */
    @PutMapping
    public ResponseEntity<User> updateMyProfile(
            @RequestBody User updateDetails,
            Authentication authentication) {

        try {
            User updatedUser = userService.updateProfile(authentication.getName(), updateDetails);
            // 为了安全，不返回密码
            updatedUser.setPassword(null);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 修改当前登录用户的密码
     */
    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @RequestBody @Valid PasswordUpdateRequest request,
            Authentication authentication) {

        try {
            userService.updatePassword(
                    authentication.getName(),
                    request.getOldPassword(),
                    request.getNewPassword()
            );
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 成功修改，无返回内容
        } catch (IllegalArgumentException e) {
            // 旧密码错误或用户不存在
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取当前登录用户的学习统计数据
     * 返回：提问数、已回答数、资源下载次数
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getMyStats(Authentication authentication) {
        try {
            User user = userService.findUserByUsername(authentication.getName());
            if (user == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Long userId = user.getId();
            Map<String, Long> stats = new HashMap<>();

            // 1. 提问总数
            Long questionCount = questionService.countQuestionsByStudentId(userId);
            stats.put("questionCount", questionCount != null ? questionCount : 0L);

            // 2. 已回答问题数
            Long answeredCount = questionService.countAnsweredQuestionsByStudentId(userId);
            stats.put("answeredCount", answeredCount != null ? answeredCount : 0L);

            // 3. 资源下载次数（统计学生上传的资源的总下载次数）
            Long downloadCount = resourceService.sumDownloadCountByStudentId(userId);
            stats.put("downloadCount", downloadCount != null ? downloadCount : 0L);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (file.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // 验证文件类型（只允许图片）
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "只允许上传图片文件");
                return ResponseEntity.badRequest().body(error);
            }

            // 生成唯一文件名
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String storageFilename = "avatar_" + UUID.randomUUID().toString() + fileExtension;

            // 确保上传目录存在
            Path uploadPath = Paths.get(uploadDir, "avatars");
            Files.createDirectories(uploadPath);

            // 保存文件
            Path filePath = uploadPath.resolve(storageFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 更新用户头像路径（相对路径，用于前端访问）
            String avatarPath = "avatars/" + storageFilename;
            User user = userService.findUserByUsername(authentication.getName());
            user.setAvatar(avatarPath);
            userService.updateProfile(authentication.getName(), user);

            Map<String, String> response = new HashMap<>();
            response.put("avatar", avatarPath);
            response.put("message", "头像上传成功");

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "文件上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取头像文件
     * 注意：由于浏览器img标签无法携带Authorization header，这里允许已认证用户访问
     * 但需要验证用户身份（通过@PreAuthorize在类级别已设置）
     */
    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> getAvatar(
            @PathVariable String filename,
            Authentication authentication) {
        try {
            // 移除可能的查询参数（如 ?t=timestamp）
            String cleanFilename = filename.split("\\?")[0];
            
            Path filePath = Paths.get(uploadDir, "avatars", cleanFilename).normalize();
            
            // 安全检查：确保路径在允许的目录内
            Path uploadPath = Paths.get(uploadDir, "avatars").normalize();
            if (!filePath.startsWith(uploadPath)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            
            org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(filePath);

            if (resource.exists() && resource.isReadable()) {
                // 根据文件扩展名确定Content-Type
                String contentType = "image/jpeg"; // 默认
                String lowerFilename = cleanFilename.toLowerCase();
                if (lowerFilename.endsWith(".png")) {
                    contentType = "image/png";
                } else if (lowerFilename.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (lowerFilename.endsWith(".webp")) {
                    contentType = "image/webp";
                }
                
                return ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                        .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(resource);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}