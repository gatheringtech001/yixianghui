<template>
	<view class="page"></view>
</template>

<script>
	export default {
		onLoad() {
			const navList = (uni.getStorageSync('cls') || []).filter(v => v.parentId == 0)
			const matched = navList.find(v => v.linkType === 'activity')
			if (matched && matched.categoryId) {
				uni.setStorageSync('currentCls', matched.categoryId)
			}
			uni.switchTab({
				url: '/pages/classify/classify'
			})
		}
	}
</script>

<style scoped lang="scss">
	.page {
		min-height: 100vh;
		background-color: #f4f5f7;
	}
</style>
