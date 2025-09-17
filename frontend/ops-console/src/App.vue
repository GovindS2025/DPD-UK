<template>
  <div id="app">
    <Toast />
    <ConfirmDialog />
    
    <div class="layout-wrapper">
      <AppTopbar />
      <AppSidebar />
      
      <div class="layout-main">
        <router-view />
      </div>
      
      <AppFooter />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import AppTopbar from '@/components/layout/AppTopbar.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'

const notificationStore = useNotificationStore()

onMounted(() => {
  notificationStore.connect()
})
</script>

<style scoped>
.layout-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-main {
  flex: 1;
  padding: 2rem;
  margin-left: 300px;
  margin-top: 70px;
}
</style>
