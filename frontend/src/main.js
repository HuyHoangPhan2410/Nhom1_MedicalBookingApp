import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import api from './api/axios';
import { createContainer, CONTAINER_KEY } from './core/container';
import './assets/main.css';

const app = createApp(App);

// Composition Root: tiêm các phụ thuộc (DIP) cho toàn bộ ứng dụng
app.provide(CONTAINER_KEY, createContainer(api, import.meta.env));
app.use(createPinia());
app.use(router);

app.mount('#app');
