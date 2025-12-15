package com.example.demo.Controller; // 注意包名大写

import com.example.demo.Dto.LoginRequest; // 导入 DTOs
import com.example.demo.Dto.LoginResponse;
import com.example.demo.Entity.User;
import com.example.demo.Service.UserService;
import com.example.demo.Util.JwtUtil; // 导入工具类

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager; // 认证管理器
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.BadCredentialsException; // 认证失败异常

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager; // Spring Security 核心组件

    @Autowired
    private JwtUtil jwtUtil; // JWT 工具类


    // 1. 用户注册接口
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.registerStudent(user);
            registeredUser.setPassword(null);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); // 409
        }
    }

    // 2. 用户登录接口
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginRequest authenticationRequest) throws Exception {
        try {
            // 1. 调用 AuthenticationManager 进行用户认证
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            // 认证失败，返回 401 Unauthorized
            return new ResponseEntity<>("用户名或密码错误！", HttpStatus.UNAUTHORIZED);
        }

        // 2. 认证成功后，从 UserService (UserDetailsService) 中加载完整的 UserDetails
        final UserDetails userDetails = userService
                .loadUserByUsername(authenticationRequest.getUsername());

        // 2.1 获取完整的 User 实体以便返回用户ID
        User user = userService.findUserByUsername(authenticationRequest.getUsername());

        // 3. 使用 JwtUtil 生成 JWT Token
        final String jwt = jwtUtil.generateToken(userDetails);

        // 4. 构建响应体
        LoginResponse response = new LoginResponse();
        response.setToken(jwt);
        response.setUsername(userDetails.getUsername());

        // 提取角色信息 (例如：ROLE_STUDENT -> STUDENT)
        userDetails.getAuthorities().stream().findFirst().ifPresent(auth -> {
            String fullRole = auth.getAuthority();
            response.setRole(fullRole.replace("ROLE_", ""));
        });
        if (user != null) {
            response.setUserId(user.getId());
        }

        // 返回 200 OK 和 Token
        return ResponseEntity.ok(response);
    }

    /**
     * 获取教师列表（所有登录用户可访问，用于下拉选择）
     */
    @GetMapping("/teachers")
    public ResponseEntity<?> getTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size
    ) {
        try {
            org.springframework.data.domain.Page<User> teachers = userService.getTeachers(page, size);
            return ResponseEntity.ok(teachers);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}