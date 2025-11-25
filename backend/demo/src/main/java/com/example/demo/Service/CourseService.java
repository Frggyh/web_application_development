package com.example.demo.Service;

import com.example.demo.Entity.Course; // 导入实体类
import com.example.demo.Repository.CourseRepository; // 导入 Repository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional; // 新增导入，用于 findCourseById

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    /**
     * 获取所有课程列表
     */
    public List<Course> findAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * 根据ID查找课程，用于Controller中的更新和权限检查
     */
    public Course findCourseById(Long id) {
        // 使用 Optional 来处理找不到的情况
        Optional<Course> courseOptional = courseRepository.findById(id);
        return courseOptional.orElse(null);
    }


    /**
     * 保存或更新一门课程 (只接收一个 Course 实体)
     */
    // ★★★ 修正点：确保 saveCourse 方法只接收一个 Course 参数 ★★★
    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }
}