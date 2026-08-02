<template>
	<view class="page_container">
		<u-navbar
			class="weapp-nav-box detail-nav"
			:is-back="true"
			:title="navTitle"
			title-color="#111111"
			:title-size="40"
			:title-bold="true"
			:title-width="520"
			:background="{ background: '#EFEBDF' }"
			:border-bottom="true"
		/>
		<scroll-view
			class="page_scroll"
			:style="{ height: pageScrollHeight + 'px' }"
			scroll-y
			:show-scrollbar="false"
		>
			<view class="head" v-if="detailInfo">
				<image :src="host + detailInfo.activityCover" mode="aspectFill"></image>
			</view>
			<view class="activity-sheet" v-if="detailInfo">
				<view class="activity-title-row">
					<view class="status-pill" :class="statusClass">{{ statusText }}</view>
					<view class="activity-name">{{ detailInfo.activityName }}</view>
					<view
						class="btn-collect"
						:class="{ collected: !!collectId }"
						@click.stop="toggleCollect"
					>{{ collectId ? '已收藏' : '收藏' }}</view>
				</view>
				<view class="activity-progress-block">
					<view class="activity-progress-head">
						<text class="field-label">报名人数</text>
						<text class="field-value">{{ activitySignCount }} / {{ detailInfo.maxCount || 0 }}</text>
					</view>
					<view class="progress">
						<view class="progress-bar" :style="{ width: enrollRatio }"></view>
					</view>
				</view>
				<view class="activity-field" v-if="detailInfo.tags && detailInfo.tags != ''">
					<text class="field-label">活动标签</text>
					<view class="activity-tags">
						<text
							class="outline-chip"
							v-for="(tag, j) in detailInfo.tags.split(/[\,|，]/)" 
							:key="j"
						>{{ tag }}</text>
					</view>
				</view>
				<view class="activity-field">
					<text class="field-label">活动地点</text>
					<view class="activity-nav" @click="navigateToLocation">
						<text class="nav-address">{{ detailInfo.address }}</text>
						<view class="nav-btn">导航</view>
					</view>
				</view>
				<view class="activity-field">
					<text class="field-label">报名截止时间</text>
					<text class="field-value">{{ detailInfo.signEndTime }}</text>
				</view>
				<view class="activity-field">
					<text class="field-label">活动时间</text>
					<text class="field-value">{{ detailInfo.activityTime }}</text>
				</view>
				<view class="activity-field">
					<text class="field-label">报名费用</text>
					<text class="field-value fee-free" v-if="isActivityFree(detailInfo)">免费</text>
					<text class="field-value fee-paid" v-else>￥{{ getActivityPrice(detailInfo) }} / 人</text>
				</view>
				<view class="activity-field activity-description">
					<text class="field-label">活动详情介绍</text>
					<view class="description-content">
						<u-parse :html="detailInfo.content" />
					</view>
				</view>
			</view>
		</scroll-view>
		<view class="bottom">
			<button class="share-btn" open-type="share">
				<u-icon name="share-fill" color="#701018" size="32"></u-icon>
				<text class="btn-label">分享</text>
			</button>
			<view
				v-if="activityPhase === 'applying'"
				class="apply-btn"
				:class="{ disabled: detailInfo && activitySignCount == detailInfo.maxCount }"
				@click="apply"
			>{{ detailInfo && activitySignCount == detailInfo.maxCount ? '名额已满' : applyBtnText }}</view>
			<view v-else class="apply-btn disabled">{{ statusText }}</view>
		</view>
		<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>

<script>
	import { getActivityInfo, addActivityOrder } from '@/api/activity/index'
	import { goodsCollect, deleteCollect, goodsCollectList } from '@/api/member/index'
	import { buildShareAppMessage, parseInvitePageOptions } from '@/utils/invite'
	import { openActivityLocation } from '@/utils/mapNavigation'
	import { getActivityPhase, getActivityPhaseText } from '@/utils/activityPhase'
	import { runWithAuth, bindPageAuthPopup } from '@/utils/login'
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	export default {
		components: {
			AuthProfilePopup
		},
		data() {
			return {
				host: this.$host,
				detailInfo: null,
				activityId: null,
				pageScrollHeight: 0,
				collectId: null
			}
		},
		onLoad(option) {
			parseInvitePageOptions(option)
			this.activityId = option.id
			this.getDetail(option.id)
		},
		onShow() {
			bindPageAuthPopup(this)
			this.loadCollectState()
		},
		onReady() {
			this.setPageScrollHeight()
		},
		computed: {
			navTitle() {
				return (this.detailInfo && this.detailInfo.activityName) || '活动详情'
			},
			activityPhase() {
				return getActivityPhase(this.detailInfo)
			},
			statusText() {
				if (!this.detailInfo) return ''
				return getActivityPhaseText(this.activityPhase)
			},
			statusClass() {
				if (!this.detailInfo) return ''
				if (this.activityPhase === 'applying') return 'active'
				if (this.activityPhase === 'closed') return 'closed'
				return 'ended'
			},
			enrollRatio() {
				if (!this.detailInfo || !this.detailInfo.maxCount) return '0%'
				const signCount = this.detailInfo.signCount || this.detailInfo.signcount || 0
				const ratio = (signCount / this.detailInfo.maxCount) * 100
				return `${Math.min(ratio, 100)}%`
			},
			activitySignCount() {
				if (!this.detailInfo) return 0
				return this.detailInfo.signCount || this.detailInfo.signcount || 0
			},
			applyBtnText() {
				if (!this.detailInfo) return '立即报名'
				if (this.isActivityFree(this.detailInfo)) return '立即报名'
				return `立即报名 ￥${this.getActivityPrice(this.detailInfo)}/人`
			}
		},
		onShareAppMessage() {
			return buildShareAppMessage({
				title: (this.detailInfo && this.detailInfo.activityName) || '逸享荟精彩活动',
				path: '/packagesMall/Activity/detail/index',
				query: {
					id: this.activityId
				}
			})
		},
		methods: {
			isActivityFree(item) {
				if (!item) return true
				const isFree = item.isFree
				return isFree === 1 || isFree === '1' || isFree === null || isFree === undefined
			},
			getActivityPrice(item) {
				if (this.isActivityFree(item)) return 0
				return item.vipPrice || item.price || 0
			},
			setPageScrollHeight() {
				this.$nextTick(() => {
					const query = uni.createSelectorQuery().in(this)
					query.select('.weapp-nav-box').boundingClientRect()
					query.select('.bottom').boundingClientRect()
					query.exec((res) => {
						const sys = uni.getSystemInfoSync()
						const navH = (res[0] && res[0].height) || (sys.statusBarHeight + 44)
						const footH = (res[1] && res[1].height) || uni.upx2px(128)
						this.pageScrollHeight = sys.windowHeight - navH - footH
					})
				})
			},
			async getDetail(id) {
				let { data } = await getActivityInfo(id)
				this.detailInfo = data
				this.loadCollectState()
				this.$nextTick(() => {
					this.setPageScrollHeight()
				})
			},
			async loadCollectState() {
				const token = uni.getStorageSync('token')
				const userInfo = uni.getStorageSync('userInfo')
				if (!token || !userInfo || !this.activityId) {
					this.collectId = null
					return
				}
				try {
					const res = await goodsCollectList({ pageNum: 1, pageSize: 500 })
					const rows = (res && res.rows) || []
					const item = rows.find(v => {
						const type = v.collectType || 'goods'
						return type === 'activity' && String(v.activityId) === String(this.activityId)
					})
					this.collectId = item ? item.collectId : null
				} catch (e) {
					console.warn('loadCollectState failed', e)
				}
			},
			toggleCollect() {
				if (!this.activityId) return
				runWithAuth(this, (ok) => {
					if (!ok) return
					if (this.collectId) {
						deleteCollect({ collectId: this.collectId }).then(() => {
							this.collectId = null
							uni.showToast({ title: '已取消收藏', icon: 'none' })
						}).catch(err => {
							uni.showToast({ title: (err && err.message) || '取消失败', icon: 'none' })
						})
						return
					}
					goodsCollect({
						collectType: 'activity',
						activityId: this.activityId
					}).then(res => {
						const data = (res && res.data) || {}
						if (data.collectId) {
							this.collectId = data.collectId
						} else {
							this.loadCollectState()
						}
						uni.showToast({ title: '收藏成功', icon: 'none' })
					}).catch(err => {
						uni.showToast({ title: (err && err.message) || '收藏失败', icon: 'none' })
					})
				})
			},
			navigateToLocation() {
				openActivityLocation(this.detailInfo)
			},
			apply() {
				if (!this.detailInfo) return
				if (this.activityPhase !== 'applying') return
				if (this.activitySignCount == this.detailInfo.maxCount) return
				let userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || userInfo == '' || userInfo == undefined) {
					uni.showToast({
						icon: 'none',
						title: '活动报名请先登录~'
					})
					setTimeout(() => {
						uni.navigateTo({
							url: '/packagesPublic/login/login'
						})
					}, 2000)
					return
				}
				if (!this.isActivityFree(this.detailInfo)) {
					uni.navigateTo({
						url: `/packagesMall/ConfirmOrder/ActivityConfirmOrder?id=${this.detailInfo.activityId}`
					})
					return
				}
				let _this = this
				uni.showModal({
					title: '活动预约确认',
					content: '',
					placeholderText: '请输入报名人数',
					editable: true,
					confirmText: '确定报名',
					cancelText: '取消',
					success: res => {
						if (res.confirm) {
							const signCount = parseInt(res.content, 10)
							if (!signCount || signCount < 1) {
								uni.showToast({
									icon: 'none',
									title: '请输入有效报名人数'
								})
								return
							}
							let params = {
								activityId: _this.detailInfo.activityId,
								signCount: signCount
							}
							addActivityOrder(params).then(() => {
								uni.showToast({
									icon: 'none',
									title: '活动报名成功~'
								})
								_this.getDetail(_this.activityId)
							}).catch(err => {
								uni.showToast({
									icon: 'none',
									title: (err && err.message) || '报名失败'
								})
							})
						}
					}
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'index.scss';
</style>
