import request from '@/utils/request'

const qaService = {
    /**
     * 管理员：获取所有问题列表（支持关键字搜索和状态筛选）
     * 后端接口: GET /api/admin/questions?page=&size=&keyword=&status=
     */
    adminGetAllQuestions(params = {}) {
        return request({
            url: '/admin/questions',
            method: 'get',
            params
        })
    },

    /**
     * 管理员：获取所有回答列表（支持关键字搜索）
     * 后端接口: GET /api/admin/answers?page=&size=&keyword=
     */
    adminGetAllAnswers(params = {}) {
        return request({
            url: '/admin/answers',
            method: 'get',
            params
        })
    },

    /**
     * 管理员：修改问题
     * 后端接口: PUT /api/admin/questions/{id}
     */
    adminUpdateQuestion(questionId, data) {
        return request({
            url: `/admin/questions/${questionId}`,
            method: 'put',
            data
        })
    },

    /**
     * 管理员：修改回答
     * 后端接口: PUT /api/admin/answers/{id}
     */
    adminUpdateAnswer(answerId, data) {
        return request({
            url: `/admin/answers/${answerId}`,
            method: 'put',
            data
        })
    },

    /**
     * 管理员：删除问题
     * 后端接口: DELETE /api/admin/questions/{id}
     */
    adminDeleteQuestion(questionId) {
        return request({
            url: `/admin/questions/${questionId}`,
            method: 'delete'
        })
    },

    /**
     * 管理员：删除回答
     * 后端接口: DELETE /api/admin/answers/{id}
     */
    adminDeleteAnswer(answerId) {
        return request({
            url: `/admin/answers/${answerId}`,
            method: 'delete'
        })
    },

    /**
     * 学生：删除自己的问题
     * 后端接口: DELETE /api/qa/questions/{id}
     */
    studentDeleteQuestion(questionId) {
        return request({
            url: `/qa/questions/${questionId}`,
            method: 'delete'
        })
    },

    /**
     * 学生：修改自己的问题
     * 后端接口: PUT /api/qa/questions/{id}
     */
    studentUpdateQuestion(questionId, data) {
        return request({
            url: `/qa/questions/${questionId}`,
            method: 'put',
            data
        })
    },

    /**
     * 教师：获取自己课程的未回答问题总数
     * 后端接口: GET /api/qa/teacher/unanswered-count
     */
    teacherGetUnansweredCount() {
        return request({
            url: '/qa/teacher/unanswered-count',
            method: 'get'
        })
    },

    /**
     * 教师：按课程查看问题列表（可按关键字、状态筛选）
     * 后端接口: GET /api/qa/questions?courseId=&keyword=&status=&page=&size=
     */
    teacherSearchQuestions(params = {}) {
        return request({
            url: '/qa/questions',
            method: 'get',
            params
        })
    },

    /**
     * 通用：查看某个问题的回答列表
     * 后端接口: GET /api/qa/questions/{questionId}/answers
     */
    getAnswersByQuestionId(questionId) {
        return request({
            url: `/qa/questions/${questionId}/answers`,
            method: 'get'
        })
    },

    /**
     * 教师：回答问题（不带附件）
     * 后端接口: POST /api/qa/answers (multipart/form-data)
     */
    teacherCreateAnswer({ questionId, content }) {
        const formData = new FormData()
        const payload = { questionId, content }
        const blob = new Blob([JSON.stringify(payload)], { type: 'application/json' })
        formData.append('data', blob, 'data.json')

        return request({
            url: '/qa/answers',
            method: 'post',
            data: formData
        })
    },

    /**
     * 教师：修改自己的回答（仅修改内容）
     * 后端接口: PUT /api/qa/answers/{id}
     */
    teacherUpdateAnswer(answerId, { questionId, content }) {
        return request({
            url: `/qa/answers/${answerId}`,
            method: 'put',
            data: { questionId, content }
        })
    },

    /**
     * 学生：全站问答搜索（支持按课程、教师、关键字、状态筛选）
     * 后端接口: GET /api/qa/questions/search?courseId=&teacherId=&keyword=&status=&page=&size=
     */
    searchAllQuestions(params = {}) {
        return request({
            url: '/qa/questions/search',
            method: 'get',
            params
        })
    }
}

export default qaService

