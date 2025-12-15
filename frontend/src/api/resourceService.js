import request from '@/utils/request'

const resourceService = {
    /**
     * 按课程和关键字搜索学习资源（分页）
     * 后端接口: GET /api/resources?courseId=&keyword=&page=&size=
     */
    searchResources(params = {}) {
        return request({
            url: '/resources',
            method: 'get',
            params,
        })
    },

    /**
     * 上传学习资源（教师或学生）
     * 后端接口: POST /api/resources/upload
     * data 需要是 FormData，包含:
     *  - file: 文件本身
     *  - data: JSON 字符串，包含 { title, description, courseId, visibility? }
     */
    uploadResource(formData) {
        return request({
            url: '/resources/upload',
            method: 'post',
            data: formData,
            // 不手动设置 Content-Type，让浏览器自动设置 multipart/form-data 和 boundary（试过了不然会报错。。）
        })
    },

    /**
     * 下载资源文件
     * 后端接口: GET /api/resources/download/{id}
     * 这里返回的是一个二进制流，前端需要手动触发浏览器下载。
     */
    downloadResource(id) {
        return request({
            url: `/resources/download/${id}`,
            method: 'get',
            responseType: 'blob',
        })
    },

    /**
     * 获取当前用户上传的资源列表
     * 后端接口: GET /api/resources/my-uploads?page=&size=
     */
    getMyUploads(params = {}) {
        return request({
            url: '/resources/my-uploads',
            method: 'get',
            params,
        })
    },

    /**
     * 删除资源（仅限上传者或管理员）
     * 后端接口: DELETE /api/resources/{id}
     */
    deleteResource(id) {
        return request({
            url: `/resources/${id}`,
            method: 'delete',
        })
    },

    // ==========================================================
    // 管理员资源管理接口
    // ==========================================================

    /**
     * 管理员：获取所有资源列表（支持关键字搜索）
     * 后端接口: GET /api/admin/resources?page=&size=&keyword=
     */
    getAllResources(params = {}) {
        return request({
            url: '/admin/resources',
            method: 'get',
            params,
        })
    },

    /**
     * 管理员：修改资源说明信息
     * 后端接口: PUT /api/admin/resources/{id}
     */
    adminUpdateResource(id, data) {
        return request({
            url: `/admin/resources/${id}`,
            method: 'put',
            data,
        })
    },

    /**
     * 管理员：删除资源
     * 后端接口: DELETE /api/admin/resources/{id}
     */
    adminDeleteResource(id) {
        return request({
            url: `/admin/resources/${id}`,
            method: 'delete',
        })
    },
}

export default resourceService


