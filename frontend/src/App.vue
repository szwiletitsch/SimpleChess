<script setup lang="ts">
import AppSidebar from "@/vue_components/AppSidebar.vue";
</script>

<template>
  <div class="app-layout">
    <AppSidebar />
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-background);
  color: var(--color-text);
}

.app-main {
  flex: 1;
  min-width: 0;
  padding: 24px;
}

@media (max-width: 760px) {
  .app-layout {
    flex-direction: column;
  }

  .app-main {
    padding: 16px;
  }
}
</style>
