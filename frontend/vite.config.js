import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            // ★★★ 2. 添加别名配置：将 @ 映射到 /src 目录 ★★★
            '@': path.resolve(__dirname, './src'),
        },
    },
})
