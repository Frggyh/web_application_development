package com.example.demo.Service;

import com.example.demo.Dto.ResourceCreationRequest;
import com.example.demo.Dto.ResourceUpdateRequest;
import com.example.demo.Entity.Course;
import com.example.demo.Entity.Resource;
import com.example.demo.Entity.User;
import com.example.demo.Repository.CourseRepository;
import com.example.demo.Repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Objects;

@Service
public class ResourceService {

    // 从 application.properties 读取文件存储目录
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserService userService;

    /**
     * 上传文件并将资源记录到数据库
     * 适用于学生和教师上传
     */
    public Resource uploadResource(MultipartFile file, ResourceCreationRequest request, User uploader) throws IOException {

        // 1. 验证文件和元数据
        if (file.isEmpty() || request.getCourseId() == null) {
            throw new IllegalArgumentException("文件或课程信息缺失。");
        }

        // 2. 查找关联的 Course
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("课程不存在。"));

        // 2.1 教师上传时，必须是该课程的授课教师
        if ("TEACHER".equals(uploader.getRole())) {
            if (!Objects.equals(course.getTeacherId(), uploader.getId())) {
                // 使用 SecurityException 让 Controller 返回 403，而不是 400
                throw new SecurityException("无权限：您不是该课程的授课教师，不能为该课程上传资源。");
            }
        }

        // 3. 处理文件存储
        // 使用 UUID 确保文件名唯一，防止覆盖
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String storageFilename = UUID.randomUUID().toString() + fileExtension;

        // 确保上传目录存在，并正确拼接路径，避免缺少路径分隔符导致找不到文件
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path copyLocation = uploadPath.resolve(storageFilename);
        Files.copy(file.getInputStream(), copyLocation);

        // 4. 构建 Resource 实体
        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setCourse(course);
        resource.setUploader(uploader);
        resource.setUploadTime(LocalDateTime.now());
        resource.setFilePath(storageFilename); // 存储唯一文件名
        resource.setFileName(originalFilename); // 存储原始文件名
        resource.setFileMimeType(file.getContentType());

        // 5. 设置可见性 (只有教师可以设置 'CLASS_ONLY')
        if ("TEACHER".equals(uploader.getRole()) && "CLASS_ONLY".equalsIgnoreCase(request.getVisibility())) {
            resource.setVisibility("CLASS_ONLY");
        } else {
            // 未指定时默认为 'ALL'
            resource.setVisibility("ALL");
        }
        return resourceRepository.save(resource);
    }

    /**
     * 获取资源文件路径（用于下载）
     */
    public Path getResourceFilePath(String filename) {
        return Paths.get(uploadDir).resolve(filename).normalize();
    }

    /**
     * 更新资源下载次数
     */
    public Resource incrementDownloadCount(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在。"));
        resource.setDownloadCount(resource.getDownloadCount() + 1);
        return resourceRepository.save(resource);
    }

    /**
     * 浏览和搜索资源 (权限逻辑在 Controller 中处理)
     * visibilityList 应该包含查询用户可以看到的所有可见性设置 (例如：['ALL', 'CLASS_ONLY'])
     */
    public Page<Resource> searchResources(Long courseId, String keyword, List<String> visibilityList, Pageable pageable) {
        // 如果关键字为空，则用空字符串代替，确保 LIKE 语句可以匹配所有
        String searchKeyword = keyword == null ? "" : keyword;
        return resourceRepository.findByCourseIdAndKeywordAndVisibility(courseId, searchKeyword, visibilityList, pageable);
    }

    // 查找单个资源
    public Resource findById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在。"));
    }

    /**
     * 管理员：搜索所有资源（不限制课程，支持关键字）
     */
    public Page<Resource> searchAllResources(String keyword, Pageable pageable) {
        // 如果关键字为空，传入 null 让查询匹配所有记录
        String searchKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;
        return resourceRepository.findAllByKeyword(searchKeyword, pageable);
    }

    // 删除资源
    public void deleteResource(Long id) {
        resourceRepository.deleteById(id);
    }

    // 获取用户上传的资源列表 (学生个人中心)
    public Page<Resource> findMyResources(Long uploaderId, Pageable pageable) {
        return resourceRepository.findByUploaderId(uploaderId, pageable);
    }

    // 修改资源说明信息（适用于管理员和上传者）
    public Resource updateResourceInfo(Long id, ResourceUpdateRequest request) {
        Resource existing = resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在。"));

        if (request.getTitle() != null) {
            existing.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }

        // 只有教师或管理员可以修改可见性，这里暂时简化成仅在上传时设置可见性
        // 可以通过删除后重新上传修改

        return resourceRepository.save(existing);
    }

    /**
     * 教师或学生搜索资源时，根据角色和课程ID确定可见性列表
     * @param role 用户角色
     * @return 允许查看的 visibility 列表
     */
    private List<String> determineVisibilityList(String role) {
        if ("ADMIN".equals(role)) {
            // 管理员可以看到所有资源
            return Arrays.asList("ALL", "CLASS_ONLY");
        } else if ("TEACHER".equals(role) || "STUDENT".equals(role)) {
            // 教师和学生默认可以看到所有公开资源
            return Collections.singletonList("ALL");
        } else {
            // 其他（理论上不应该出现，因为接口需要认证）
            return Collections.emptyList();
        }
    }


    /**
     * 资源搜索和过滤：增加可见性过滤和权限检查
     * @param courseId 课程ID
     * @param keyword 关键字
     * @param currentUser 当前登录用户
     * @param pageable 分页信息
     * @return 分页结果
     */
    public Page<Resource> searchResources(
            Long courseId,
            String keyword,
            User currentUser,
            Pageable pageable) {

        String role = currentUser.getRole();
        Long userId = currentUser.getId();

        // 1. 根据用户角色确定默认的可见性列表 (ALL, CLASS_ONLY, 或两者都有)
        List<String> visibilityList = determineVisibilityList(role);

        // 2. 对于 TEACHER 和 STUDENT，还需要进行课程权限检查，以确定能否看到 CLASS_ONLY 资源
        if ("TEACHER".equals(role)) {
            // 教师只能看到自己所教授课程的 CLASS_ONLY 资源
            List<Long> taughtCourseIds = userService.findTaughtCourseIds(userId);
            if (taughtCourseIds.contains(courseId)) {
                // 如果教师负责该课程，则可以查看 CLASS_ONLY 资源
                visibilityList = Arrays.asList("ALL", "CLASS_ONLY");
            }
        } else if ("STUDENT".equals(role)) {
            // 学生只能看到自己已选课程的 CLASS_ONLY 资源
            // !!! 注意: 我们的模型中没有明确的学生选课表，这里假设学生被分配到该课程
            // 简化的逻辑：如果学生拥有该课程的 ID，则可以查看 CLASS_ONLY 资源

            // 由于没有选课逻辑，这里我们暂时假定：学生只要登录，就可以查看该课程下的 CLASS_ONLY 资源，
            // 但生产环境必须基于选课逻辑。为了继续推进，我们暂时不将 CLASS_ONLY 加入学生查询列表。
        }

        // 最终的查询逻辑: 查询关键字、课程ID，以及在 computedVisibilityList 中的资源
        String searchKeyword = keyword == null ? "" : keyword;

        // 修改 ResourceRepository 的查询方法以适应这个逻辑
        // 您需要在 ResourceRepository 中添加一个新的方法来支持这个查询，或者修改旧方法。

        //假设您在 ResourceRepository 中有一个方法如下：
        return resourceRepository.findByCourseIdAndVisibilityInAndKeyword(
                courseId,
                visibilityList,
                searchKeyword,
                pageable);
    }

    // ResourceService.java (在现有内容中添加)

    public Resource updateResource(Long id, ResourceCreationRequest request) {
        Resource existing = resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在。"));

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        // 不能修改课程ID和上传者，除非是特殊管理员操作，这里只允许修改标题和描述。

        return resourceRepository.save(existing);
    }

    /**
     * 统计指定学生上传的资源的总下载次数
     */
    public Long sumDownloadCountByStudentId(Long studentId) {
        return resourceRepository.sumDownloadCountByStudentId(studentId);
    }

    // 还需要在 ResourceService 中添加一个 findById(Long id) 方便 @PreAuthorize 调用

}