<template>
  <div class="resource-release-container">
    <el-card shadow="hover">
      <template #header>
        <div class="header">
          <div>
            <h2>资源发布与管理</h2>
            <p class="sub-title">在此上传课程资料、讲义和习题等学习资源。</p>
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
                :label="`${course.name}（${course.college}）`"
                :value="course.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="资源标题" prop="title">
          <el-input
              v-model="form.title"
              maxlength="255"
              show-word-limit
              placeholder="请输入资源标题，例如：第3章例题解析"
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

        <el-form-item label="可见范围">
          <el-radio-group v-model="form.visibility">
            <el-radio-button label="ALL">全体学生可见</el-radio-button>
            <el-radio-button label="CLASS_ONLY">仅本班学生</el-radio-button>
          </el-radio-group>
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
                支持 PDF、PPT、Word、压缩包及图片，单个文件不超过 50MB。
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

      <h3 style="margin-bottom: 12px;">已发布资源（当前课程）</h3>
      <el-empty v-if="!loading && resources.length === 0" description="当前课程还没有发布资源" />

      <el-table
          v-else
          :data="resources"
          v-loading="loading"
          border
          stripe
      >
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column prop="description" label="简介" min-width="260" show-overflow-tooltip />
        <el-table-column prop="downloadCount" label="下载次数" width="110" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import courseService from '@/api/courseService'
import resourceService from '@/api/resourceService'

const courses = ref([])
const resources = ref([])
const loading = ref(false)
const submitting = ref(false)

const formRef = ref(null)
const uploadRef = ref(null)
const fileList = ref([])
const selectedFile = ref(null)

const form = reactive({
  courseId: null,
  title: '',
  description: '',
  visibility: 'ALL',
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

const fetchResources = async () => {
  if (!form.courseId) {
    resources.value = []
    return
  }
  loading.value = true
  try {
    const response = await resourceService.searchResources({
      courseId: form.courseId,
      keyword: '',
      page: 0,
      size: 50,
    })
    const content = response.content || []
    resources.value = content.map(r => ({
      ...r,
      uploadTime: r.uploadTime,
    }))
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
  fileList.value = [file]
}

const handleExceed = () => {
  ElMessage.warning('一次仅能上传一个文件')
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    if (!selectedFile.value) {
      ElMessage.error('请先选择要上传的文件')
      return
    }

    submitting.value = true
    try {
      const formData = new FormData()
      formData.append('file', selectedFile.value)
      formData.append(
        'data',
        new Blob(
          [
            JSON.stringify({
              title: form.title,
              description: form.description,
              courseId: form.courseId,
              visibility: form.visibility,
            }),
          ],
          { type: 'application/json' },
        ),
      )

      await resourceService.uploadResource(formData)
      ElMessage.success('资源上传成功')
      handleReset(false)
      await fetchResources()
    } catch (error) {
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

const handleReset = (resetCourse = true) => {
  formRef.value?.resetFields()
  if (!resetCourse && form.courseId) {
    // 保留当前课程
    form.courseId = form.courseId
  } else if (courses.value.length > 0) {
    form.courseId = courses.value[0].id
  }
  form.title = ''
  form.description = ''
  form.visibility = 'ALL'
  selectedFile.value = null
  fileList.value = []
  uploadRef.value?.clearFiles()
}

watch(
  () => form.courseId,
  () => {
    fetchResources()
  },
)

onMounted(async () => {
  await fetchCourses()
  await fetchResources()
})
</script>

<style scoped>
.resource-release-container {
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
  max-width: 680px;
}
</style>