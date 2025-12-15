<template>
  <div class="resource-management-container">
    <el-card shadow="hover">
      <div class="header">
        <div>
          <h2>学习资源管理</h2>
          <p class="sub-title">查看、审核并管理平台上的所有学习资源，可删除违规资源或修改资源说明。</p>
        </div>
      </div>

      <div class="filters">
        <el-input
            v-model="filters.keyword"
            placeholder="输入资源标题或简介关键字"
            style="width: 300px"
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

      <el-table
          :data="resources"
          v-loading="loading"
          border
          stripe
          empty-text="暂无资源数据"
          style="margin-top: 20px;"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="资源标题" min-width="200" />
        <el-table-column label="所属课程" width="180">
          <template #default="scope">
            {{ scope.row.course?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="上传者" width="140">
          <template #default="scope">
            {{ scope.row.uploader?.username || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column prop="description" label="简介" min-width="220" show-overflow-tooltip />
        <el-table-column prop="downloadCount" label="下载次数" width="110" />
        <el-table-column label="可见性" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.visibility === 'ALL' ? 'success' : 'warning'">
              {{ scope.row.visibility === 'ALL' ? '全体可见' : '仅本班' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">修改说明</el-button>
            <el-button
                size="small"
                type="danger"
                @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :current-page="pagination.page"
            :page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 修改资源说明对话框 -->
    <el-dialog
        v-model="dialogVisible"
        title="修改资源说明"
        width="500px"
    >
      <el-form
          ref="formRef"
          :model="resourceForm"
          :rules="formRules"
          label-width="100px"
      >
        <el-form-item label="资源标题" prop="title">
          <el-input
              v-model="resourceForm.title"
              maxlength="255"
              show-word-limit
              placeholder="请输入资源标题"
          />
        </el-form-item>

        <el-form-item label="资源简介" prop="description">
          <el-input
              v-model="resourceForm.description"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="请输入资源简介"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="dialogSubmitting" @click="handleSubmit">
            保存修改
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import resourceService from '@/api/resourceService'

const resources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogSubmitting = ref(false)
const currentResourceId = ref(null)

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0,
})

const filters = reactive({
  keyword: '',
})

const formRef = ref(null)
const resourceForm = reactive({
  title: '',
  description: '',
})

const formRules = {
  title: [{ required: true, message: '请输入资源标题', trigger: 'blur' }],
}

const fetchResources = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
    }
    if (filters.keyword) {
      params.keyword = filters.keyword
    }

    const response = await resourceService.getAllResources(params)
    resources.value = response.content || []
    pagination.total = response.totalElements || 0
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

const openEditDialog = (resource) => {
  currentResourceId.value = resource.id
  resourceForm.title = resource.title || ''
  resourceForm.description = resource.description || ''
  dialogVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return

    dialogSubmitting.value = true
    try {
      await resourceService.adminUpdateResource(currentResourceId.value, {
        title: resourceForm.title.trim(),
        description: resourceForm.description?.trim() || '',
      })
      ElMessage.success('资源说明已更新')
      dialogVisible.value = false
      await fetchResources()
    } catch (error) {
      console.error('更新失败：', error)
    } finally {
      dialogSubmitting.value = false
    }
  })
}

const handleDelete = (resource) => {
  ElMessageBox.confirm(
      `确定删除资源「${resource.title}」吗？删除后无法恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
    .then(async () => {
      try {
        await resourceService.adminDeleteResource(resource.id)
        ElMessage.success('资源删除成功')
        // 如果当前页只有一个数据被删除，且不是第一页，则自动回退一页
        if (resources.value.length === 1 && pagination.page > 1) {
          pagination.page -= 1
        }
        await fetchResources()
      } catch (error) {
        console.error('删除失败：', error)
      }
    })
    .catch(() => {})
}

onMounted(() => {
  fetchResources()
})
</script>

<style scoped>
.resource-management-container {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.sub-title {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.filters {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

