package com.example.demo.Filter;

import com.example.demo.Util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User; // 导入 Spring Security User 类
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.warn("JWT Token 解析失败: " + e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 1. 从 Claims 中提取所有信息
            Claims claims = jwtUtil.extractAllClaims(jwt);
            String roleClaim = (String) claims.get("role");

            // 2. 构建权限列表
            List<SimpleGrantedAuthority> authorities;
            if (roleClaim != null) {
                authorities = Collections.singletonList(new SimpleGrantedAuthority(roleClaim));
            } else {
                authorities = Collections.emptyList();
            }

            // 3. 验证 Token 有效性（仍需要通过 UserDetailsService 检查用户是否存在）
            UserDetails userDetailsFromDb = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetailsFromDb)) {

                // 4. ★★★ 关键修正：使用提取的用户名和权限，创建一个新的 Spring Security User 实例 ★★★
                // 密码设为 null，因为我们不再需要它
                UserDetails authenticatedUser = new User(username, "", authorities);

                // 5. 使用这个新的 User 实例创建认证对象
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        authenticatedUser, null, authenticatedUser.getAuthorities());

                authenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        chain.doFilter(request, response);
    }
}