package com.example.demo.Repository;

import com.example.demo.Entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // 您可以根据需要添加更多查询方法，例如：
    // List<Course> findByCollege(String college);
}