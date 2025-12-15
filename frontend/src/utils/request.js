import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const service = axios.create({
    baseURL: 'http://localhost:8080/api', // 后端基础地址
    timeout: 15000, // 请求超时时间
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
service.interceptors.request.use(
    config => {
        // 从 localStorage 获取 token
        const token = localStorage.getItem('token')

        if (token) {
            // 添加 JWT Token 到请求头
            config.headers['Authorization'] = `Bearer ${token}`
        }

        // 如果是上传文件（FormData），删除 Content-Type 让浏览器自动设置（包含 boundary）（试过了不然会报错。。）
        if (config.data instanceof FormData) {
            delete config.headers['Content-Type']
        }

        console.log('📤 请求:', config.method.toUpperCase(), config.url)
        return config
    },
    error => {
        console.error('❌ 请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    response => {
        console.log('📥 响应:', response.config.url, response.data)

        // 直接返回响应数据（后端返回格式已经是 JSON 对象）
        return response.data
    },
    error => {
        console.error('❌ 响应错误:', error)

        // 处理不同的错误状态码
        if (error.response) {
            const status = error.response.status
            const errorData = error.response.data

            switch (status) {
                case 400:
                    ElMessage.error(errorData?.message || '请求参数错误')
                    break

                case 401:
                    ElMessage.error('登录已过期，请重新登录')
                    // 清除本地存储
                    localStorage.removeItem('token')
                    localStorage.removeItem('user')
                    // 跳转到登录页
                    setTimeout(() => {
                        window.location.href = '/login'
                    }, 1500)
                    break

                case 403:
                    ElMessage.error('没有权限访问该资源')
                    break

                case 404:
                    ElMessage.error('请求的资源不存在')
                    break

                case 409:
                    // 冲突错误（如用户名已存在）
                    ElMessage.error(errorData?.message || errorData || '操作冲突')
                    break

                case 500:
                    ElMessage.error('服务器内部错误，请稍后重试')
                    break

                default:
                    ElMessage.error(errorData?.message || errorData || '请求失败')
            }
        } else if (error.request) {
            // 请求已发出，但没有收到响应
            ElMessage.error('网络连接失败，请检查您的网络')
        } else {
            // 其他错误
            ElMessage.error(error.message || '请求失败')
        }

        return Promise.reject(error)
    }
)

export default service