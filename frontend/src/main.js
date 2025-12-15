import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { createPinia } from 'pinia';
import { useAuthStore } from '@/stores/authStore';

import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(ElementPlus);

const authStore = useAuthStore();

authStore.initAuth().finally(() => {
app.mount('#app');
});