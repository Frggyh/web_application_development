import request from '@/utils/request'

const courseService = {
    /**
     * 获取所有课程列表
     * 后端接口: GET /api/courses (所有已认证用户可访问)
     * @returns {Promise}
     */
    getCourses() {
        return request({
            url: '/courses',
            method: 'get'
        })
    },

    /**
     * 创建新课程
     * 后端接口: POST /api/courses (需要 TEACHER 权限)
     * @param {Object} courseData - { name, description, college }
     * @returns {Promise}
     */
    createCourse(courseData) {
        return request({
            url: '/courses',
            method: 'post',
            data: courseData
        })
    },

    /**
     * 更新课程信息
     * 后端接口: PUT /api/courses/{id} (需要 ADMIN 或课程创建者权限)
     * @param {number} courseId - 课程ID
     * @param {Object} courseData - { name, description, college }
     * @returns {Promise}
     */
    updateCourse(courseId, courseData) {
        return request({
            url: `/courses/${courseId}`,
            method: 'put',
            data: courseData
        })
    },

    /**
     * 删除课程
     * 后端接口: DELETE /api/courses/{id} (需要 ADMIN 权限)
     * @param {number} courseId - 课程ID
     * @returns {Promise}
     */
    deleteCourse(courseId) {
        return request({
            url: `/courses/${courseId}`,
            method: 'delete'
        })
    },

    /**
     * 根据ID获取单个课程详情
     * @param {number} courseId - 课程ID
     * @returns {Promise}
     */
    getCourseById(courseId) {
        return request({
            url: `/courses/${courseId}`,
            method: 'get'
        })
    }
}

export default courseService