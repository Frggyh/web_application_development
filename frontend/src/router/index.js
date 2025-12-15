import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { ElMessage } from 'element-plus'

// 路由配置
const routes = [
    // 公共路由
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/LoginPage.vue'),
        meta: { requiresAuth: false, title: '登录' }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/auth/RegisterPage.vue'),
        meta: { requiresAuth: false, title: '注册' }
    },

    // 管理员路由
    {
        path: '/admin',
        component: () => import('@/views/Layout/AdminLayout.vue'),
        meta: { requiresAuth: true, role: 'ADMIN' },
        redirect: '/admin/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'AdminDashboard',
                component: () => import('@/views/admin/DashBoard.vue'),
                meta: { title: '首页' }
            },
            {
                path: 'courses',
                name: 'AdminCourses',
                component: () => import('@/views/Course/CourseListPage.vue'),
                meta: { title: '课程管理' }
            },
            {
                path: 'teachers',
                name: 'AdminTeachers',
                component: () => import('@/views/admin/TeacherManagement.vue'),
                meta: { title: '教师账号管理' }
            },
            {
                path: 'users',
                name: 'AdminUsers',
                component: () => import('@/views/admin/UserManagement.vue'),
                meta: { title: '用户管理' }
            },
            {
                path: 'resources',
                name: 'AdminResources',
                component: () => import('@/views/admin/ResourceManagement.vue'),
                meta: { title: '学习资源管理' }
            },
            {
                path: 'qa',
                name: 'AdminQA',
                component: () => import('@/views/admin/QAManagement.vue'),
                meta: { title: '问答内容管理' }
            }
        ]
    },

    // 教师路由
    {
        path: '/teacher',
        component: () => import('@/views/Layout/TeacherLayout.vue'),
        meta: { requiresAuth: true, role: 'TEACHER' },
        redirect: '/teacher/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'TeacherDashboard',
                component: () => import('@/views/teacher/DashBoard.vue'),
                meta: { title: '首页' }
            },
            {
                path: 'courses',
                name: 'TeacherCourses',
                component: () => import('@/views/teacher/MyCourses.vue'),
                meta: { title: '我的课程' }
            },
            {
                path: 'qa-duty',
                name: 'TeacherQADuty',
                component: () => import('@/views/teacher/QADuty.vue'),
                meta: { title: '答疑区提醒' }
            },
            {
                path: 'resources/upload',
                name: 'TeacherResourceUpload',
                component: () => import('@/views/teacher/ResourceRelease.vue'),
                meta: { title: '资源上传与管理' }
            },
            {
                path: 'profile',
                name: 'TeacherProfile',
                component: () => import('@/views/teacher/Profile.vue'),
                meta: { title: '个人信息修改' }
            }
        ]
    },

    // 学生路由
    {
        path: '/student',
        component: () => import('@/views/Layout/StudentLayout.vue'),
        meta: { requiresAuth: true, role: 'STUDENT' },
        redirect: '/student/dashboard',
        children: [
            {
                path: 'profile',
                name: 'StudentProfile',
                component: () => import('@/views/student/Profile.vue'),
                meta: { title: '个人中心' }
            },
            {
                path: 'dashboard',
                name: 'StudentDashboard',
                component: () => import('@/views/student/DashBoard.vue'),
                meta: { title: '首页' }
            },
            {
                path: 'resources',
                name: 'StudentResources',
                component: () => import('@/views/student/ResourceBrowser.vue'),
                meta: { title: '学习资源浏览' }
            },
            {
                path: 'resources/upload',
                name: 'StudentResourceUpload',
                component: () => import('@/views/student/ResourceUpload.vue'),
                meta: { title: '学习资料上传' }
            },
            {
                path: 'qa/ask',
                name: 'StudentAsk',
                component: () => import('@/views/student/AskQuestion.vue'),
                meta: { title: '我要提问' }
            },
            {
                path: 'qa/answered-reminders',
                name: 'StudentAnsweredReminders',
                component: () => import('@/views/student/AnsweredReminders.vue'),
                meta: { title: '已回答提醒' }
            },
            {
                path: 'qa/search',
                name: 'StudentQASearch',
                component: () => import('@/views/Student/QASearch.vue'),
                meta: { title: '全站问答搜索' }
            }
        ]
    },

    // 根路径重定向
    {
        path: '/',
        redirect: to => {
            const authStore = useAuthStore()
            if (!authStore.isLoggedIn) {
                return '/login'
            }
            // 根据角色跳转
            switch (authStore.userRole) {
                case 'ADMIN':
                    return '/admin/dashboard'
                case 'TEACHER':
                    return '/teacher/dashboard'
                case 'STUDENT':
                    return '/student/dashboard'
                default:
                    return '/login'
            }
        }
    },

    // 404 页面（必须放在最后）
    {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/NotFound.vue'),
        meta: { title: '页面未找到' }
    }
]

// 创建路由实例
const router = createRouter({
    history: createWebHistory(),
    routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()

    // 设置页面标题
    document.title = to.meta.title
        ? `${to.meta.title} - 在线学习平台`
        : '在线学习平台'

    // 不需要认证的页面，直接放行
    if (!to.meta.requiresAuth) {
        // 如果已登录，访问登录页时重定向到主页
        if (to.path === '/login' && authStore.isLoggedIn) {
            return next('/')
        }
        return next()
    }

    // 需要认证的页面
    if (!authStore.isLoggedIn) {
        ElMessage.warning('请先登录')
        return next({
            path: '/login',
            query: { redirect: to.fullPath } // 保存原始路径，登录后跳转
        })
    }

    // 检查角色权限
    if (to.meta.role && to.meta.role !== authStore.userRole) {
        ElMessage.error('您没有权限访问该页面')
        return next('/')
    }

    // 所有检查通过，放行
    next()
})

export default router