package com.example.demo.Controller;

import com.example.demo.Dto.PasswordUpdateRequest;
import com.example.demo.Entity.User;
import com.example.demo.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()") // 所有登录用户都可以访问个人中心
public class ProfileController {

    @Autowired
    private UserService userService;

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
}