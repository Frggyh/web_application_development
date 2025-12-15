<template>
  <div class="resource-upload-container">
    <el-card shadow="hover">
      <template #header>
        <div class="header">
          <div>
            <h2>上传学习资料</h2>
            <p class="sub-title">分享您的学习笔记、资料和文档，帮助其他同学共同进步。</p>
          </div>
        </div>
      </template>

      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="form"
      >
        <el-form-item label="所属课程" prop="courseId">
          <el-select
              v-model="form.courseId"
              placeholder="请选择课程"
              filterable
              style="width: 100%"
          >
            <el-option
                v-for="course in courses"
                :key="course.id"
                :label="`${course.name}(${course.college})`"
                :value="course.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="资源标题" prop="title">
          <el-input
              v-model="form.title"
              maxlength="255"
              show-word-limit
              placeholder="请输入资源标题，例如：高等数学第一章笔记"
          />
        </el-form-item>

        <el-form-item label="资源简介" prop="description">
          <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="简单说明资源内容、适用章节等"
          />
        </el-form-item>

        <el-form-item label="上传文件" prop="file">
          <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-exceed="handleExceed"
              :file-list="fileList"
              accept=".pdf,.ppt,.pptx,.doc,.docx,.zip,.rar,image/*"
          >
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、PPT、Word、压缩包及图片，单个文件不超过 40MB。
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">上传资源</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3 style="margin-bottom: 12px;">我上传的资源</h3>
      <el-empty v-if="!loading && myResources.length === 0" description="您还没有上传过资源" />

      <el-table
          v-else
          :data="myResources"
          v-loading="loading"
          border
          stripe
      >
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column label="所属课程" width="180">
          <template #default="scope">
            {{ scope.row.course?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
        <el-table-column prop="downloadCount" label="下载次数" width="110" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
                size="small"
                type="danger"
                @click="handleDelete(scope.row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-if="pagination.total > pagination.size"
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[5, 10, 20]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchMyResources"
          @current-change="fetchMyResources"
          style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import courseService from '@/api/courseService'
import resourceService from '@/api/resourceService'

const courses = ref([])
const myResources = ref([])
const loading = ref(false)
const submitting = ref(false)

const formRef = ref(null)
const uploadRef = ref(null)
const fileList = ref([])
const selectedFile = ref(null)

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const form = reactive({
  courseId: null,
  title: '',
  description: '',
})

const rules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change', type: 'number' }],
  title: [{ required: true, message: '请输入资源标题', trigger: 'blur' }],
  file: [{ validator: (_, __, cb) => (selectedFile.value ? cb() : cb(new Error('请先选择文件'))), trigger: 'change' }],
}

const fetchCourses = async () => {
  try {
    const response = await courseService.getCourses()
    courses.value = response || []
    if (!form.courseId && courses.value.length > 0) {
      form.courseId = courses.value[0].id
    }
  } catch (error) {
    ElMessage.error('加载课程列表失败')
    console.error(error)
  }
}

const fetchMyResources = async () => {
  loading.value = true
  try {
    const response = await resourceService.getMyUploads({
      page: pagination.page - 1,
      size: pagination.size,
    })
    myResources.value = (response.content || []).map(r => ({
      ...r,
      uploadTime: r.uploadTime,
    }))
    pagination.total = response.totalElements || 0
  } catch (error) {
    ElMessage.warning('加载我的资源列表失败')
    myResources.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleFileChange = (file) => {
  console.log('文件选择事件:', file)
  if (file && file.raw) {
    selectedFile.value = file.raw
    fileList.value = [file]
    console.log('文件已保存:', file.raw.name, file.raw.size, 'bytes')
  } else {
    console.error('文件对象无效:', file)
    ElMessage.error('文件选择失败，请重试')
  }
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!selectedFile.value) {
      ElMessage.warning('请先选择要上传的文件')
      return
    }

    submitting.value = true
    try {
      // 保证数据完整性
      if (!form.courseId) {
        ElMessage.error('请选择课程')
        submitting.value = false
        return
      }
      if (!form.title || form.title.trim() === '') {
        ElMessage.error('请输入资源标题')
        submitting.value = false
        return
      }

      const requestData = {
        title: form.title.trim(),
        description: form.description?.trim() || '',
        courseId: form.courseId,
      }

      console.log('准备上传，数据:', requestData)
      console.log('文件对象:', selectedFile.value)
      console.log('文件名:', selectedFile.value?.name)
      console.log('文件大小:', selectedFile.value?.size, 'bytes')
      console.log('文件类型:', selectedFile.value?.type)

      if (!selectedFile.value || selectedFile.value.size === 0) {
        ElMessage.error('文件无效或文件大小为 0，请重新选择文件')
        submitting.value = false
        return
      }

      const formData = new FormData()
      formData.append('file', selectedFile.value, selectedFile.value.name)
      // 确保 data 部分有正确的 Content-Type
      const jsonBlob = new Blob([JSON.stringify(requestData)], { type: 'application/json' })
      formData.append('data', jsonBlob, 'data.json')

      console.log('FormData 已构建，准备发送请求')
      await resourceService.uploadResource(formData)
      ElMessage.success('资源上传成功！')
      handleReset()
      await fetchMyResources()
    } catch (error) {
      console.error('上传失败：', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleReset = () => {
  formRef.value?.resetFields()
  uploadRef.value?.clearFiles()
  selectedFile.value = null
  fileList.value = []
  Object.assign(form, {
    courseId: courses.value.length > 0 ? courses.value[0].id : null,
    title: '',
    description: '',
  })
}

const handleDelete = async (resourceId) => {
  try {
    await ElMessageBox.confirm('确定要删除该资源吗？删除后无法恢复。', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await resourceService.deleteResource(resourceId)
    ElMessage.success('资源删除成功')
    await fetchMyResources()
  } catch (error) {
    if (error === 'cancel') return
    console.error('删除失败：', error)
  }
}

onMounted(() => {
  fetchCourses()
  fetchMyResources()
})
</script>

<style scoped>
.resource-upload-container {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sub-title {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.form {
  max-width: 800px;
}
</style>

