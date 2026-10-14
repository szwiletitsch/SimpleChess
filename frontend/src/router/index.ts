import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/chess',
            name: 'chess',
            component: () => import('../vue_views/ChessGameView.vue'),
        },
        {
            path: '/',
            name: 'home',
            component: () => import('../vue_views/HomeView.vue'),
        },
    ],
})

export default router