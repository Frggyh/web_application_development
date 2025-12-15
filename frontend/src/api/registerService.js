import request from '@/utils/request'

const registerService = {
    /**
     * 学生用户注册
     * 后端接口: POST /api/users/register
     * 注意：后端会自动设置 role 为 'STUDENT'
     * @param {Object} userData - { username, password }
     * @returns {Promise}
     */
    registerStudent(userData) {
        return request({
            url: '/users/register',
            method: 'post',
            data: userData
        })
    }
}

export default registerService