import request from '@/utils/request'

const userService = {
    getUsers(params = {}) {
        return request({
            url: '/admin/users',
            method: 'get',
            params
        })
    },

    updateUser(userId, data) {
        return request({
            url: `/admin/users/${userId}`,
            method: 'put',
            data
        })
    }
}

export default userService

