```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import router from "@/router";

type Theme = 'light' | 'dark'

const theme = ref<Theme>('light')

function applyTheme(value: Theme) {
  theme.value = value
  document.documentElement.dataset.theme = value
  localStorage.setItem('theme', value)
}

function toggleTheme() {
  applyTheme(theme.value === 'dark' ? 'light' : 'dark')
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')

  if (savedTheme === 'light' || savedTheme === 'dark') {
    applyTheme(savedTheme)
  } else {
    const prefersDark = window.matchMedia(
        '(prefers-color-scheme: dark)',
    ).matches

    applyTheme(prefersDark ? 'dark' : 'light')
  }
})
</script>

<template>
  <aside class="app-sidebar">
    <div class="sidebar-header">
      <div class="sidebar-logo">♟</div>
      <div class="sidebar-brand">
        <h2>Chess Project</h2>
        <span>Play and improve</span>
      </div>
    </div>

    <div class="sidebar-divider"></div>

    <nav class="sidebar-navigation" aria-label="Main navigation">
      <span class="sidebar-section-label">MENU</span>

      <button class="sidebar-item active" type="button" @click='router.push(`/chess`)'>
        <span class="sidebar-icon">♟</span>
        <span>Chess vs Bot</span>
      </button>

      <!-- Add more navigation items here -->
    </nav>

    <div class="sidebar-spacer"></div>

    <div class="sidebar-features">
      <span class="sidebar-section-label">FEATURES</span>

      <!-- Future features can go here -->
      <slot name="features">
        <p class="sidebar-placeholder">
          More features coming soon
        </p>
      </slot>
    </div>

    <div class="sidebar-divider"></div>

    <button
        class="theme-toggle"
        type="button"
        @click="toggleTheme"
        :aria-label="`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`"
    >
      <span class="theme-icon" aria-hidden="true">
        {{ theme === 'dark' ? '☀' : '☾' }}
      </span>

      <span class="theme-label">
        {{ theme === 'dark' ? 'Light mode' : 'Dark mode' }}
      </span>

      <span class="theme-switch" :class="{ enabled: theme === 'dark' }">
        <span class="theme-switch-knob"></span>
      </span>
    </button>

    <div class="sidebar-footer">
      <span class="footer-indicator"></span>
      <span>{{ theme === 'dark' ? 'Dark theme enabled' : 'Light theme enabled' }}</span>
    </div>
  </aside>
</template>

<style scoped>
.app-sidebar {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: 200px;
  min-height: 100%;
  padding: 24px 16px;
  box-sizing: border-box;

  color: var(--chess-text);
  background: var(--chess-panel);
  border-right: 1px solid var(--chess-border);
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px 20px;
}

.sidebar-logo {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 12px;
  background: var(--chess-control);
  color: var(--chess-accent);
  font-size: 26px;
}

.sidebar-brand h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.sidebar-brand span {
  display: block;
  margin-top: 4px;
  color: var(--chess-text-muted);
  font-size: 12px;
}

.sidebar-divider {
  height: 1px;
  background: var(--chess-divider);
  margin: 4px 0 20px;
}

.sidebar-section-label {
  display: block;
  padding: 0 10px;
  margin-bottom: 10px;
  color: var(--chess-text-muted);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.3px;
}

.sidebar-navigation {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--chess-text);
  font: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: background 160ms ease, border-color 160ms ease;
}

.sidebar-item:hover {
  background: var(--chess-control-hover);
}

.sidebar-item.active {
  background: var(--chess-control);
  border-color: var(--chess-border-control);
  font-weight: 600;
}

.sidebar-icon {
  width: 20px;
  text-align: center;
  font-size: 19px;
}

.sidebar-spacer {
  flex: 1;
  min-height: 32px;
}

.sidebar-features {
  margin-bottom: 24px;
}

.sidebar-placeholder {
  padding: 0 10px;
  color: var(--chess-text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.theme-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 10px;
  border: 1px solid var(--chess-border);
  border-radius: 12px;
  background: var(--chess-card);
  color: var(--chess-text);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 160ms ease, border-color 160ms ease;
}

.theme-toggle:hover {
  background: var(--chess-control-hover);
  border-color: var(--chess-border-hover);
}

.theme-icon {
  width: 22px;
  font-size: 20px;
  text-align: center;
}

.theme-label {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
}

.theme-switch {
  display: flex;
  align-items: center;
  width: 32px;
  height: 18px;
  padding: 2px;
  border-radius: 99px;
  background: var(--chess-border-hover);
  transition: background 160ms ease;
}

.theme-switch.enabled {
  background: var(--chess-accent);
}

.theme-switch-knob {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 3px rgb(0 0 0 / 20%);
  transition: transform 160ms ease;
}

.theme-switch.enabled .theme-switch-knob {
  transform: translateX(14px);
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 0 4px;
  color: var(--chess-text-muted);
  font-size: 11px;
}

.footer-indicator {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--chess-accent);
}

@media (max-width: 760px) {
  .app-sidebar {
    width: 100%;
    min-height: auto;
    padding: 12px 16px;
    border-right: none;
    border-bottom: 1px solid var(--chess-border);
  }

  .sidebar-header {
    padding-bottom: 12px;
  }

  .sidebar-divider,
  .sidebar-spacer,
  .sidebar-features,
  .sidebar-footer,
  .sidebar-section-label {
    display: none;
  }

  .sidebar-navigation {
    flex-direction: row;
  }

  .sidebar-item {
    width: auto;
  }

  .theme-toggle {
    margin-top: 8px;
  }
}
</style>
```