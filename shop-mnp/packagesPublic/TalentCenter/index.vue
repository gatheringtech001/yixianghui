<template>
	<view class="talent-gate">
		<web-view v-if="talentCenterUrl" :src="talentCenterUrl" />
		<view v-else>
			<text>{{ error || '正在打开达人中心…' }}</text>
			<button v-if="error" @click="checkAccess">重新尝试</button>
		</view>
	</view>
</template>

<script>
	import { getInfo } from '@/api/public'
	import { isAuthorizedUser, syncConsultantStorage } from '@/utils/login'
	const TALENT_CENTER_URL = 'https://gatheringtech.com/talent/register?return_to=%2F'
	export default {
		data() { return { talentCenterUrl: '', checking: false, error: '' } },
		onShow() { this.checkAccess() },
		methods: {
			async checkAccess() {
				if (this.checking) return
				this.checking = true
				this.error = ''
				this.talentCenterUrl = ''
				try {
					let consultant = null
					if (isAuthorizedUser()) {
						const res = await getInfo()
						if (res.code !== 200) throw new Error(res.msg || '申请状态获取失败')
						consultant = res.consultant
						syncConsultantStorage(consultant)
					}
					if (consultant && consultant.consultantId && consultant.status === '01') {
						this.talentCenterUrl = TALENT_CENTER_URL
					} else {
						await new Promise((resolve, reject) => uni.redirectTo({
							url: '/packagesMember/retail/apply/index', success: resolve, fail: reject
						}))
					}
				} catch (error) { this.error = error.message || '达人中心打开失败，请重试' }
				finally { this.checking = false }
			}
		}
	}
</script>

<style scoped>
	.talent-gate { min-height: 100vh; padding: 80rpx 40rpx; box-sizing: border-box; background: #EFEBDF; color: #701018; text-align: center; font-size: 30rpx; }
	button { margin-top: 32rpx; background: #701018; color: #fff; border-radius: 18rpx; font-size: 30rpx; }
</style>
