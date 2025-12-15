<template>
  <div class="answered-reminders-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>已回答提醒</span>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0">
            <el-icon><Bell /></el-icon>
          </el-badge>
        </div>
      </template>

      <el-alert
          title="教师已回答您的问题"
          type="success"
          description="点击查看按钮可以查看教师的回答内容"
          :closable="false"
          style="margin-bottom: 20px;"
          v-if="unreadCount > 0"
      />

      <el-empty v-if="answeredQuestions.length === 0 && !loading" description="暂无已回答的问题" />

      <el-table :data="answeredQuestions" v-loading="loading" style="width: 100%" v-else>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-badge :is-dot="!scope.row.isRead" type="danger">
              <el-icon :color="scope.row.isRead ? '#67C23A' : '#F56C6C'">
                <Message />
              </el-icon>
            </el-badge>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="问题标题" min-width="250" />

        <el-table-column label="课程" width="150">
          <template #default="scope">
            <el-tag type="info">{{ scope.row.courseName }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="answerTime" label="回答时间" width="180" />

        <el-table-column label="回答者" width="120">
          <template #default="scope">
            <el-tag type="success">{{ scope.row.replierName }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button
                size="small"
                type="primary"
                @click="viewAnswer(scope.row)"
            >
              查看回答
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchAnsweredQuestions"
          style="margin-top: 20px; justify-content: center;"
      />
    </el-card>

    <!-- 查看回答对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="currentQuestion?.title"
        width="700px"
    >
      <el-descriptions :column="1" border v-if="currentQuestion">
        <el-descriptions-item label="我的问题">
          <div style="white-space: pre-wrap;">{{ currentQuestion.content }}</div>
        </el-descriptions-item>

        <el-descriptions-item label="教师回答">
          <div style="white-space: pre-wrap; color: #409EFF;">
            {{ currentAnswer?.content }}
          </div>
        </el-descriptions-item>

        <el-descriptions-item label="回答者">
          {{ currentAnswer?.replierName }}
        </el-descriptions-item>

        <el-descriptions-item label="回答时间">
          {{ currentAnswer?.answerTime }}
        </el-descriptions-item>

        <el-descriptions-item label="附件" v-if="currentAnswer?.attachmentPath">
          <el-button type="primary" size="small" @click="downloadAttachment">
            下载附件
          </el-button>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="success" @click="markAsRead" v-if="!currentQuestion?.isRead">
          标记为已读
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Bell, Message } from '@element-plus/icons-vue'

const answeredQuestions = ref([])
const loading = ref(false)
const unreadCount = ref(0)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const currentQuestion = ref(null)
const currentAnswer = ref(null)

// 获取已回答问题列表
const fetchAnsweredQuestions = async () => {
  loading.value = true
  try {
    // 1) 获取“已回答问题数量”用于徽标提示
    const countResponse = await request.get('/qa/student/answered-count')
    unreadCount.value = countResponse || 0

    // 2) 获取当前学生自己的、状态为 ANSWERED 的问题列表
    const response = await request.get('/qa/my-questions', {
      params: {
        status: 'ANSWERED',
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })

    const content = response.content || []
    answeredQuestions.value = content.map(q => ({
      ...q,
      courseName: q.course?.name || '',
      answerTime: q.askTime, // 暂用提问时间占位，详细回答时间在弹窗中再查
      replierName: '', // 在查看详情时通过 /qa/questions/{id}/answers 获取
      isRead: false, // 目前“已读”状态只在前端本地维护
    }))
    total.value = response.totalElements || content.length || 0
  } catch (error) {
    ElMessage.error('加载已回答问题失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 查看回答
const viewAnswer = async (question) => {
  try {
    currentQuestion.value = question

    // 获取回答详情
    const response = await request.get(`/qa/questions/${question.id}/answers`)

    if (response && response.length > 0) {
      currentAnswer.value = response[0] // 假设一个问题只有一个回答
    } else {
      currentAnswer.value = null
    }

    dialogVisible.value = true

    // 如果未读，自动标记为已读
    if (!question.isRead) {
      await markAsReadInternal(question.id)
    }
  } catch (error) {
    ElMessage.error('加载回答详情失败')
    console.error(error)
  }
}

// 标记为已读
const markAsRead = async () => {
  if (!currentQuestion.value) return
  await markAsReadInternal(currentQuestion.value.id)
  dialogVisible.value = false
}

// 内部标记为已读方法
const markAsReadInternal = async (questionId) => {
  try {
    // TODO: 后端需要提供标记已读接口
    // await request.put(`/qa/questions/${questionId}/mark-read`)

    // 更新本地数据
    const question = answeredQuestions.value.find(q => q.id === questionId)
    if (question) {
      question.isRead = true
    }

    if (unreadCount.value > 0) {
      unreadCount.value--
    }
  } catch (error) {
    console.error(error)
  }
}

// 下载附件
const downloadAttachment = () => {
  if (currentAnswer.value?.attachmentPath) {
    // TODO: 实现附件下载
    ElMessage.info('附件下载功能待实现')
  }
}

onMounted(() => {
  fetchAnsweredQuestions()
})
</script>

<style scoped>
.answered-reminders-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>