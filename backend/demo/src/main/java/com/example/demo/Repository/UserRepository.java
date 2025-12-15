package com.example.demo.Repository;

import com.example.demo.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// 泛型参数：第一个是实体类（User），第二个是实体类的主键类型（Long）
public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);

    Page<User> findByRoleIgnoreCase(String role, Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<User> findByRoleIgnoreCaseAndUsernameContainingIgnoreCase(String role, String keyword, Pageable pageable);
}