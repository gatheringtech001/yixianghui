<template>
	<view :class="displayModeClass" class="page_container">
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
				<image :src="activityCoverUrl" mode="aspectFill" @tap.stop="previewDetailImages(detailInfo.activityCover)"></image>
				<view
					class="collect-star"
					:class="{ collected: !!collectId }"
					:aria-label="collectId ? '取消收藏' : '收藏活动'"
					@click.stop="toggleCollect"
				>
					<u-icon
						:name="collectId ? 'heart-fill' : 'heart'"
						color="#701018"
						size="44"
					></u-icon>
				</view>
			</view>
			<view class="activity-sheet" v-if="detailInfo">
				<view class="activity-title-row">
					<view class="status-pill" :class="statusClass">{{ statusText }}</view>
					<view class="activity-name">{{ detailInfo.activityName }}</view>
				</view>
				<view class="activity-progress-block">
					<view class="activity-progress-head">
						<text class="field-label">已报人数</text>
						<text class="field-value">{{ activitySignCount }}{{ activityMaxCount ? ' / ' + activityMaxCount + ' 人' : ' 人 · 不限人数' }}</text>
					</view>
					<view class="progress" v-if="activityMaxCount">
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
				<view class="activity-description">
					<text class="field-label">活动详情介绍</text>
					<view class="description-content">
						<u-parse :html="displayRichText(detailInfo.content)" :preview="true" />
					</view>
				</view>
			</view>
		</scroll-view>
		<view class="bottom">
			<button class="share-btn" open-type="share">
				<u-icon name="share-fill" color="#701018" size="32"></u-icon>
				<text class="btn-label">分享</text>
			</button>
			<view v-if="activeActivityOrder" class="apply-btn" @click="viewMyBooking">查看我的预约</view>
			<view
				v-else-if="activityPhase === 'applying'"
				class="apply-btn"
				:class="{ disabled: activityFull }"
				@click="apply"
			>{{ activityFull ? '名额已满' : applyBtnText }}</view>
			<view v-else class="apply-btn disabled">{{ statusText }}</view>
		</view>
		<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>

<script>
	import { getActivityInfo, getActivityOrderList } from '@/api/activity/index'
	import { goodsCollect, deleteCollect, goodsCollectList } from '@/api/member/index'
	import { parseInvitePageOptions } from '@/utils/invite'
	import sharePageMixin from '@/utils/sharePageMixin'
	import { openActivityLocation } from '@/utils/mapNavigation'
	import { getActivityPhase, getActivityPhaseText } from '@/utils/activityPhase'
	import { runWithAuth, bindPageAuthPopup } from '@/utils/login'
	import { prepareRichTextHtml } from '@/utils/richText'
	import { detailImageUrl, previewDetailImages } from '@/utils/detailImagePreview'
	import { findActiveActivityOrder } from '@/utils/activityOrderState'
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	export default {
		mixins: [sharePageMixin],
		components: {
			AuthProfilePopup
		},
		data() {
			return {
				host: this.$host,
				detailInfo: null,
				activityId: null,
				pageScrollHeight: 0,
				collectId: null,
				activeActivityOrder: null
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
			this.loadBookingState()
		},
		onReady() {
			this.setPageScrollHeight()
		},
		computed: {
			activityCoverUrl() {
				return detailImageUrl(this.detailInfo && this.detailInfo.activityCover, this.host)
			},
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
				if (!this.activityMaxCount) return '0%'
				const ratio = (this.activitySignCount / this.activityMaxCount) * 100
				return `${Math.min(ratio, 100)}%`
			},
			activityMaxCount() {
				const max = Number(this.detailInfo && this.detailInfo.maxCount)
				return Number.isFinite(max) && max > 0 ? max : 0
			},
			activityFull() {
				return this.activityMaxCount > 0 && this.activitySignCount >= this.activityMaxCount
			},
			activitySignCount() {
				if (!this.detailInfo) return 0
				return Number(this.detailInfo.signCount || this.detailInfo.signcount) || 0
			},
			applyBtnText() {
				if (!this.detailInfo) return '立即报名'
				if (this.isActivityFree(this.detailInfo)) return '立即报名'
				return `立即报名 ￥${this.getActivityPrice(this.detailInfo)}/人`
			}
		},
		methods: {
			previewDetailImages(current, images) {
				return previewDetailImages.call(this, current, images)
			},
			async loadBookingState() {
				const token = uni.getStorageSync('token')
				const userInfo = uni.getStorageSync('userInfo')
				if (!token || !userInfo || !this.activityId) {
					this.activeActivityOrder = null
					return
				}
				try {
					const res = await getActivityOrderList({
						activityId: this.activityId,
						pageNum: 1,
						pageSize: 20
					})
					this.activeActivityOrder = findActiveActivityOrder(res && res.rows, this.activityId)
				} catch (e) {
					console.warn('loadBookingState failed', e)
				}
			},
			viewMyBooking() {
				if (!this.activeActivityOrder) return
				uni.navigateTo({
					url: `/packagesMember/MyActivity/detail/index?orderId=${this.activeActivityOrder.orderId}`
				})
			},
			getShareConfig() {
				const cover = this.detailInfo && this.detailInfo.activityCover
				return {
					title: (this.detailInfo && this.detailInfo.activityName) || '逸享荟精彩活动',
					path: '/packagesMall/Activity/detail/index',
					query: { id: this.activityId },
					imageUrl: cover ? (cover.startsWith('http') ? cover : this.host + cover) : ''
				}
			},
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
				if (data && data.content) {
					data.content = prepareRichTextHtml(data.content, this.host)
				}
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
				if (this.activeActivityOrder) {
					this.viewMyBooking()
					return
				}
				if (this.activityPhase !== 'applying') return
				if (this.activityFull) return
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
				uni.navigateTo({
					url: `/packagesMall/ConfirmOrder/ActivityConfirmOrder?id=${this.detailInfo.activityId}`
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'index.scss';
</style>
