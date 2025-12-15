<template>
  <div class="course-selection-container">
    <el-card>
      <h2>课程选课中心</h2>
      <el-divider />
      <el-alert
          title="该页面供学生浏览所有开放的课程并进行选课操作。"
          type="success"
          show-icon
          :closable="false"
      />

      <el-empty description="可选课程列表 (待联调)" />

      <el-row :gutter="20">
        <el-col :span="12">
          <el-button type="primary" disabled>浏览所有课程</el-button>
        </el-col>
        <el-col :span="12">
          <el-button type="info" disabled>查看我的选课</el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
// 课程选课页面逻辑（待实现）
</script>

<style scoped>
.course-selection-container {
  padding: 20px;
}
</style>