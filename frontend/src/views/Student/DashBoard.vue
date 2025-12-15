<template>
  <div class="student-dashboard-container">
    <el-row :gutter="20">
      <!-- 欢迎卡片 -->
      <el-col :span="24">
        <el-card>
          <h2>🎓 欢迎来到学习中心，{{ user?.username }}</h2>
          <el-divider />
          <p>开始您的学习之旅吧！浏览资源、提问问题、与教师互动。</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 学习统计 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="我的提问" :value="stats.questionCount">
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="已回答" :value="stats.answeredCount">
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="已下载资源" :value="stats.downloadCount">
            <template #suffix>次</template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="可用课程" :value="stats.courseCount">
            <template #suffix>门</template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card header="快捷操作">
          <el-space wrap>
            <el-button type="primary" @click="goToResources">
              <el-icon><Reading /></el-icon>
              浏览学习资源
            </el-button>
            <el-button type="success" @click="goToAsk">
              <el-icon><QuestionFilled /></el-icon>
              我要提问
            </el-button>
            <el-button type="warning" @click="goToReminders">
              <el-icon><Bell /></el-icon>
              查看回答
            </el-button>
            <el-button @click="goToProfile">
              <el-icon><User /></el-icon>
              个人中心
            </el-button>
          </el-space>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新回答提醒 -->
    <el-row :gutter="20" style="margin-top: 20px;" v-if="stats.newAnswerCount > 0">
      <el-col :span="24">
        <el-alert
            title="您有新的问题回答"
            :description="`教师已回答了您的 ${stats.newAnswerCount} 个问题，点击查看。`"
            type="success"
            show-icon
            :closable="false"
        >
          <template #default>
            <el-button size="small" type="success" @click="goToReminders">
              查看回答
            </el-button>
          </template>
        </el-alert>
      </el-col>
    </el-row>

    <!-- 推荐课程 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card header="推荐课程">
          <el-empty v-if="recommendedCourses.length === 0" description="暂无推荐课程" />

          <el-row :gutter="15" v-else>
            <el-col :span="8" v-for="course in recommendedCourses" :key="course.id">
              <el-card shadow="hover" style="margin-bottom: 15px;">
                <h3>{{ course.name }}</h3>
                <el-divider />
                <p>{{ course.college }}</p>
                <p style="color: #909399; font-size: 14px;">
                  {{ course.description }}
                </p>
                <el-button type="primary" size="small" style="margin-top: 10px;">
                  查看详情
                </el-button>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import request from '@/utils/request'
import courseService from '@/api/courseService'
import { ElMessage } from 'element-plus'
import { Reading, QuestionFilled, Bell, User } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

const recommendedCourses = ref([])

// 学习统计
const stats = reactive({
  questionCount: 0,
  answeredCount: 0,
  downloadCount: 0,
  courseCount: 0,
  newAnswerCount: 0
})

// 获取统计数据
const fetchStats = async () => {
  try {
    // 获取已回答问题数量
    const answeredResponse = await request.get('/qa/student/answered-count')
    stats.answeredCount = answeredResponse || 0
    stats.newAnswerCount = answeredResponse || 0 // 简化处理，实际应该是未读数量

    // TODO: 获取其他统计数据（需要后端接口）
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 获取推荐课程
const fetchRecommendedCourses = async () => {
  try {
    const response = await courseService.getCourses()
    // 随机选择3门课程作为推荐
    recommendedCourses.value = response.slice(0, 3)
  } catch (error) {
    console.error('加载推荐课程失败:', error)
  }
}

// 导航方法
const goToResources = () => {
  router.push('/student/resources')
}

const goToAsk = () => {
  router.push('/student/qa/ask')
}

const goToReminders = () => {
  router.push('/student/qa/answered-reminders')
}

const goToProfile = () => {
  router.push('/student/profile')
}

onMounted(() => {
  fetchStats()
  fetchRecommendedCourses()
})
</script>

<style scoped>
.student-dashboard-container {
  padding: 20px;
}
</style>