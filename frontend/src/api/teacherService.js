import request from '@/utils/request'

const teacherService = {
    /**
     * 获取所有教师列表（用于下拉选择）
     * 后端接口: GET /api/users/teachers (所有登录用户可访问)
     * @param {Object} params - 查询参数 { page, size }
     * @returns {Promise}
     */
    getTeachers(params = {}) {
        return request({
            url: '/users/teachers',
            method: 'get',
            params
        })
    },

    /**
     * 创建教师账号
     * 后端接口: POST /api/admin/teachers
     * @param {Object} teacherData - { username, password, role: 'TEACHER', title, introduction }
     * @returns {Promise}
     */
    createTeacher(teacherData) {
        return request({
            url: '/admin/teachers',
            method: 'post',
            data: {
                ...teacherData,
                role: 'TEACHER' // 确保角色是教师
            }
        })
    },

    /**
     * 更新教师信息
     * 后端接口: PUT /api/admin/teachers/{id}
     * @param {number} teacherId - 教师ID
     * @param {Object} updateData - { title, introduction }
     * @returns {Promise}
     */
    updateTeacher(teacherId, updateData) {
        return request({
            url: `/admin/teachers/${teacherId}`,
            method: 'put',
            data: updateData
        })
    },

    /**
     * 删除教师账号
     * 后端接口: DELETE /api/admin/teachers/{id}
     * @param {number} teacherId - 教师ID
     * @returns {Promise}
     */
    deleteTeacher(teacherId) {
        return request({
            url: `/admin/teachers/${teacherId}`,
            method: 'delete'
        })
    },

    /**
     * 为教师分配课程
     * 后端接口: POST /api/admin/teachers/assign-course
     * @param {Object} assignmentData - { courseId, teacherId }
     * @returns {Promise}
     */
    assignCourse(assignmentData) {
        return request({
            url: '/admin/teachers/assign-course',
            method: 'post',
            data: assignmentData
        })
    }
}

export default teacherService