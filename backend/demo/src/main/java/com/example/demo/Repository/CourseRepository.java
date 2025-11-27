package com.example.demo.Repository;

import com.example.demo.Entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    /**
     * 查找某个教师教授的所有课程ID
     */
    @Query("SELECT c.id FROM Course c WHERE c.teacherId = :teacherId")
    List<Long> findCourseIdsByTeacherId(@Param("teacherId") Long teacherId);


}