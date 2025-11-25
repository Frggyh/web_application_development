package com.example.demo.Service; // 注意包名大写

import com.example.demo.Entity.Course; // 导入实体类
import com.example.demo.Repository.CourseRepository; // 导入 Repository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    /**
     * 获取所有课程列表 (目前没有任何过滤或分页逻辑)
     * @return 数据库中所有课程的列表
     */
    public List<Course> findAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * 示例：添加一门新课程 (管理员功能，简化实现)
     */
    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }
}