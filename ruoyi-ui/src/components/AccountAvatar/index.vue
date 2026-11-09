<template>
  <span class="account-avatar">
    <img v-if="src && !failed" :src="src" alt="账号头像" @error="failed = true">
    <span v-else class="avatar-fallback" role="img" aria-label="默认头像">
      <i class="el-icon-user-solid" aria-hidden="true" />
    </span>
  </span>
</template>

<script>
export default {
  name: 'AccountAvatar',
  props: {
    src: { type: String, default: '' }
  },
  data() {
    return { failed: false }
  },
  watch: {
    src() {
      this.failed = false
    }
  }
}
</script>

<style scoped>
.account-avatar,
.account-avatar img,
.avatar-fallback {
  display: inline-flex;
  width: 40px;
  height: 40px;
  border-radius: 10px;
}
.account-avatar { flex-shrink: 0; overflow: hidden; }
.account-avatar img { object-fit: cover; }
.avatar-fallback {
  align-items: center;
  justify-content: center;
  background: #ecf5ff;
  color: #409eff;
  font-size: 23px;
}
</style>
