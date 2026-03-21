<template>
  <div>
    <nav v-if="showNav" class="app-topbar">
      <span class="app-brand">⚽ OBS Controller</span>
      <div class="app-nav">
        <RouterLink to="/" class="app-navlink">Control</RouterLink>
        <RouterLink to="/config" class="app-navlink">Config</RouterLink>
        <RouterLink to="/preview" class="app-navlink">Preview</RouterLink>
      </div>
    </nav>
    <RouterView />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const showNav = computed(() => {
  return route.path !== '/scoreboard' && route.path !== '/tournament' && route.path !== '/intro' && route.path !== '/replay' && route.path !== '/download'
})
</script>

<style>
body {
  color: #eee;
}

.app-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 1rem;
  background-color: #0f0f23;
  border-bottom: 1px solid #2a2a4a;
  position: sticky;
  top: 0;
  z-index: 100;
}

.app-brand {
  font-size: 0.8rem;
  font-weight: 700;
  color: #666;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.app-nav {
  display: flex;
  gap: 0.25rem;
}

.app-navlink {
  padding: 0.3rem 0.85rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #888;
  text-decoration: none;
  transition: background 0.15s, color 0.15s;
}

.app-navlink.router-link-active {
  background-color: rgba(255, 255, 255, 0.12);
  color: #fff;
}
</style>
