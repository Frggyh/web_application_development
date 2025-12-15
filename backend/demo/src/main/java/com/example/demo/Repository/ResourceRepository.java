package com.example.demo.Repository;

import com.example.demo.Entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    /**
     * 根据课程ID和关键字（标题/描述）搜索资源，并实现分页
     * 同时也需要考虑可见性（visibility）限制。
     * * @param courseId 课程ID
     * @param keyword 搜索关键字
     * @param visibility 可见性范围 ('ALL' 或 'CLASS_ONLY')
     * @param pageable 分页信息
     * @return 分页结果
     */

    @Query("SELECT r FROM Resource r " +
            "WHERE r.course.id = :courseId " +
            "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%) " +
            "AND r.visibility IN :visibility") // 假设 visibility 是一个列表，用于匹配 ALL 或 CLASS_ONLY
    Page<Resource> findByCourseIdAndKeywordAndVisibility(
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("visibility") Iterable<String> visibility,
            Pageable pageable);


    /**
     * 查找某个用户上传的所有资源 (用于学生个人中心)
     */
    Page<Resource> findByUploaderId(Long uploaderId, Pageable pageable);

    /**
     * 根据课程ID、可见性列表和关键字进行资源搜索
     */
    @Query("SELECT r FROM Resource r " +
            "WHERE r.course.id = :courseId " +
            "AND r.visibility IN :visibilityList " +
            "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%)")
    Page<Resource> findByCourseIdAndVisibilityInAndKeyword(
            @Param("courseId") Long courseId,
            @Param("visibilityList") List<String> visibilityList,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 管理员：按关键字搜索所有资源（平台所有课程）
     */
    @Query("SELECT r FROM Resource r " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "r.title LIKE CONCAT('%', :keyword, '%') OR r.description LIKE CONCAT('%', :keyword, '%'))")
    Page<Resource> findAllByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 统计指定学生上传的资源的总下载次数
     */
    @Query("SELECT COALESCE(SUM(r.downloadCount), 0) FROM Resource r WHERE r.uploader.id = :studentId")
    Long sumDownloadCountByStudentId(@Param("studentId") Long studentId);
}