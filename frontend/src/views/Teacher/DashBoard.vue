<template>
  <div class="teacher-dashboard-container">
    <el-row :gutter="20">
      <!-- 欢迎卡片 -->
      <el-col :span="24">
        <el-card>
          <h2>👋 欢迎回来，{{ user?.username }} {{ user?.title ? `(${user.title})` : '' }}</h2>
          <el-divider />
          <p>这是教师工作台，您可以在这里管理课程、回答学生提问、发布学习资源。</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计数据 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="我的课程" :value="stats.courseCount">
            <template #suffix>门</template>
          </el-statistic>
          <div style="margin-top: 10px;">
            <el-button type="primary" size="small" @click="goToCourses">
              管理课程
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="待回答问题" :value="stats.unansweredCount">
            <template #suffix>个</template>
          </el-statistic>
          <div style="margin-top: 10px;">
            <el-button type="warning" size="small" @click="goToQADuty">
              去回答
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="已发布资源" :value="stats.resourceCount">
            <template #suffix">个</template>
</el-statistic>
<div style="margin-top: 10px;">
<el-button type="success" size="small" @click="goToResources">
  管理资源
</el-button>
</div>
</el-card>
</el-col>
</el-row>

<!-- 快捷操作 -->
<el-row :gutter="20" style="margin-top: 20px;">
<el-col :span="24">
  <el-card header="快捷操作">
    <el-space wrap>
      <el-button type="primary" @click="goToCourses">
        <el-icon><Plus /></el-icon>
        创建新课程
      </el-button>
      <el-button type="success" @click="goToResources">
        <el-icon><Upload /></el-icon>
        上传资源
      </el-button>
      <el-button type="warning" @click="goToQADuty">
        <el-icon><ChatDotRound /></el-icon>
        查看问答
      </el-button>
      <el-button @click="goToProfile">
        <el-icon><User /></el-icon>
        个人设置
      </el-button>
    </el-space>
  </el-card>
</el-col>
</el-row>

<!-- 待处理提醒 -->
<el-row :gutter="20" style="margin-top: 20px;" v-if="stats.unansweredCount > 0">
<el-col :span="24">
  <el-alert
      title="您有待回答的问题"
      :description="`当前有 ${stats.unansweredCount} 个学生问题等待回答，请及时处理。`"
      type="warning"
      show-icon
      :closable="false"
  >
    <template #default>
      <el-button size="small" type="warning" @click="goToQADuty">
        立即查看
      </el-button>
    </template>
  </el-alert>
</el-col>
</el-row>
</div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Plus, Upload, ChatDotRound, User } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

// 统计数据
const stats = reactive({
  courseCount: 0,
  unansweredCount: 0,
  resourceCount: 0
})

// 获取统计数据
const fetchStats = async () => {
  try {
    // 获取未回答问题数量
    const qaResponse = await request.get('/qa/teacher/unanswered-count')
    stats.unansweredCount = qaResponse || 0

    // TODO: 获取课程数量和资源数量（需要后端接口）
    // const courseResponse = await request.get('/teacher/courses/count')
    // stats.courseCount = courseResponse || 0

    // const resourceResponse = await request.get('/teacher/resources/count')
    // stats.resourceCount = resourceResponse || 0
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 导航方法
const goToCourses = () => {
  router.push('/teacher/courses')
}

const goToQADuty = () => {
  router.push('/teacher/qa-duty')
}

const goToResources = () => {
  router.push('/teacher/resources/upload')
}

const goToProfile = () => {
  router.push('/teacher/profile')
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.teacher-dashboard-container {
  padding: 20px;
}
</style>