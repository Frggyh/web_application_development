<template>
  <div class="resource-browser-container">
    <el-card shadow="hover">
      <template #header>
        <div class="header">
          <div>
            <h2>学习资源浏览</h2>
            <p class="sub-title">按课程和关键字快速查找对你开放的学习资料。</p>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-select
            v-model="filters.courseId"
            placeholder="请选择课程"
            style="min-width: 220px"
            filterable
            clearable
            @change="handleSearch"
        >
          <el-option
              v-for="course in courses"
              :key="course.id"
              :label="`${course.name}（${course.college}）`"
              :value="course.id"
          />
        </el-select>

        <el-input
            v-model="filters.keyword"
            placeholder="输入标题或简介关键字"
            style="width: 260px"
            clearable
            @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-empty
          v-if="!loading && resources.length === 0"
          description="暂无相关资源，请尝试选择其他课程或关键字"
      />

      <el-table
          v-else
          :data="resources"
          v-loading="loading"
          border
          stripe
      >
        <el-table-column prop="title" label="标题" min-width="220" />

        <el-table-column label="课程" width="160">
          <template #default="scope">
            <el-tag type="info">{{ scope.row.courseName }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="uploaderName" label="上传者" width="120" />

        <el-table-column prop="uploadTime" label="上传时间" width="180" />

        <el-table-column prop="description" label="简介" min-width="260" show-overflow-tooltip />

        <el-table-column prop="downloadCount" label="下载次数" width="110" />

        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button
                size="small"
                type="primary"
                @click="handleDownload(scope.row)"
            >
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :current-page="pagination.page"
            :page-size="pagination.size"
            :page-sizes="[5, 10, 20, 50]"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import courseService from '@/api/courseService'
import resourceService from '@/api/resourceService'

const courses = ref([])
const resources = ref([])
const loading = ref(false)

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const filters = reactive({
  courseId: null,
  keyword: '',
})

const fetchCourses = async () => {
  try {
    const response = await courseService.getCourses()
    courses.value = response || []
    // 默认选中第一个课程，便于学生直接看到资源
    if (!filters.courseId && courses.value.length > 0) {
      filters.courseId = courses.value[0].id
    }
  } catch (error) {
    ElMessage.error('加载课程列表失败')
    console.error(error)
  }
}

const fetchResources = async () => {
  if (!filters.courseId) {
    resources.value = []
    pagination.total = 0
    return
  }

  loading.value = true
  try {
    const params = {
      courseId: filters.courseId,
      keyword: filters.keyword || '',
      page: pagination.page - 1,
      size: pagination.size,
    }
    const response = await resourceService.searchResources(params)

    const content = response.content || []
    resources.value = content.map(r => ({
      ...r,
      courseName: r.course?.name || '',
      uploaderName: r.uploader?.username || '',
      uploadTime: r.uploadTime,
    }))
    pagination.total = response.totalElements ?? content.length ?? 0
  } catch (error) {
    ElMessage.error('加载资源列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchResources()
}

const handleReset = () => {
  filters.keyword = ''
  // 课程保持当前选择，避免一下子清空列表
  pagination.page = 1
  fetchResources()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchResources()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchResources()
}

const handleDownload = async (resource) => {
  try {
    const blob = await resourceService.downloadResource(resource.id)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = resource.fileName || resource.title
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('下载失败，请稍后重试')
    console.error(error)
  }
}

onMounted(async () => {
  await fetchCourses()
  await fetchResources()
})
</script>

<style scoped>
.resource-browser-container {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sub-title {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>