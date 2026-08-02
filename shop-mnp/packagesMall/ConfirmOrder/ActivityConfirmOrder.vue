<template>
	<view class="page_view">
		<scroll-view
			class="page_scroll"
			:style="{ height: pageScrollHeight + 'px' }"
			scroll-y
			:show-scrollbar="false"
		>
			<view v-if="activityDetail">
				<view class="course-card">
					<image :src="coverUrl" mode="aspectFill" />
					<view class="course-body">
						<view class="course-name">{{ activityDetail.activityName }}</view>
						<view class="course-summary" v-if="activityDetail.description">{{ activityDetail.description }}</view>
					</view>
				</view>

				<view class="info-card">
					<view class="card-title">活动信息</view>
					<view class="info-grid">
						<view class="info-item" v-if="activityDetail.activityTime">
							<text class="info-label">活动时间</text>
							<text class="info-value">{{ activityDetail.activityTime }}</text>
						</view>
						<view class="info-item" v-if="activityDetail.address">
							<text class="info-label">活动地点</text>
							<text class="info-value">{{ activityDetail.address }}</text>
						</view>
						<view class="info-item" v-if="activityDetail.signEndTime">
							<text class="info-label">报名截止</text>
							<text class="info-value">{{ activityDetail.signEndTime }}</text>
						</view>
						<view class="info-item">
							<text class="info-label">剩余名额</text>
							<text class="info-value">{{ remainCount }} / {{ activityDetail.maxCount || 0 }}</text>
						</view>
					</view>
				</view>

				<view class="form-card">
					<view class="card-title">报名信息</view>
					<view class="form-row">
						<view class="row-label">联系人</view>
						<u-input v-model="signName" type="text" border placeholder="请输入报名姓名" />
					</view>
					<view class="form-row">
						<view class="row-label">联系电话</view>
						<u-input v-model="signMobile" type="number" maxlength="11" border placeholder="请输入手机号" />
						<view class="row-hint">方便活动组织方与您联系确认</view>
					</view>
					<view class="form-row">
						<view class="row-label">参与人数</view>
						<u-number-box v-model="signCount" :min="1" :max="maxSignCount" :step="1" />
					</view>
					<view class="form-row">
						<view class="row-label">备注</view>
						<u-input v-model="remark" type="textarea" border placeholder="选填，如有特殊情况可留言" />
					</view>
					<view class="agreement-row" @click="agreedNotice = !agreedNotice">
						<view class="agreement-check" @click.stop>
							<u-checkbox v-model="agreedNotice"></u-checkbox>
						</view>
						<view class="agreement-text">
							我已阅读
							<text class="agreement-link" @click.stop="openNoticePopup">《活动报名须知》</text>
						</view>
					</view>
				</view>

				<view class="price-card">
					<view class="card-title">费用明细</view>
					<view class="price-list">
						<view class="price-item">
							<text>活动单价</text>
							<text>￥{{ unitPriceText }}</text>
						</view>
						<view class="price-item">
							<text>报名人数</text>
							<text>{{ signCount }} 人</text>
						</view>
						<view class="price-item discount" v-if="memberDiscount > 0">
							<text>会员优惠</text>
							<text>-￥{{ memberDiscountText }}</text>
						</view>
						<view class="price-item total">
							<text>应付金额</text>
							<text class="value">￥{{ payAmountText }}</text>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<view class="page_foot_view" v-if="activityDetail">
			<view class="foot_price_view">
				<view class="price_view">
					￥<text>{{ payAmountText }}</text>
				</view>
				<view class="text_view">应付金额</view>
			</view>
			<view class="pay_button_view" @click="submitOrder">提交报名</view>
		</view>

		<u-popup v-model="noticePopup.show" @touchmove.stop.prevent mode="bottom" border-radius="20" :closeable="true">
			<view class="notice_popup_view">
				<view class="notice_title_view">活动报名须知</view>
				<scroll-view scroll-y class="notice_content_view">
					<view class="notice_text">
						<text>1. 提交报名后将生成待支付订单，请在规定时间内完成支付。</text>
						<text>2. 报名成功后请保持手机畅通，活动组织方可能会与您联系确认。</text>
						<text>3. 如需取消待支付订单，可在「我的活动」中操作。</text>
						<text>4. 活动名额有限，以支付成功为准。</text>
					</view>
				</scroll-view>
			</view>
		</u-popup>
	</view>
</template>

<script>
	import { getActivityInfo, createActivityPendingOrder } from '@/api/activity/index'
	import { getActivityPhase } from '@/utils/activityPhase'

	export default {
		data() {
			return {
				host: this.$host,
				pageScrollHeight: 0,
				activityId: null,
				activityDetail: null,
				signName: '',
				signMobile: '',
				signCount: 1,
				remark: '',
				agreedNotice: false,
				noticePopup: {
					show: false
				}
			}
		},
		computed: {
			coverUrl() {
				if (!this.activityDetail || !this.activityDetail.activityCover) {
					return '/static/home-design/entry-stay.jpg'
				}
				const cover = this.activityDetail.activityCover
				if (cover.startsWith('http') || cover.startsWith('/static/')) return cover
				return this.host + cover
			},
			unitPrice() {
				if (!this.activityDetail) return 0
				const vipPrice = Number(this.activityDetail.vipPrice)
				const price = Number(this.activityDetail.price)
				if (vipPrice > 0) return vipPrice
				return price > 0 ? price : 0
			},
			unitPriceText() {
				return this.formatMoney(this.unitPrice)
			},
			listPrice() {
				return Number(this.activityDetail && this.activityDetail.price) || 0
			},
			memberDiscount() {
				if (!this.activityDetail) return 0
				const price = Number(this.activityDetail.price) || 0
				const vipPrice = Number(this.activityDetail.vipPrice) || 0
				if (vipPrice > 0 && price > vipPrice) {
					return (price - vipPrice) * this.signCount
				}
				return 0
			},
			memberDiscountText() {
				return this.formatMoney(this.memberDiscount)
			},
			payAmount() {
				return this.unitPrice * this.signCount
			},
			payAmountText() {
				return this.formatMoney(this.payAmount)
			},
			remainCount() {
				if (!this.activityDetail) return 0
				const maxCount = Number(this.activityDetail.maxCount) || 0
				const signCount = Number(this.activityDetail.signCount || this.activityDetail.signcount) || 0
				if (!maxCount) return '不限'
				return Math.max(maxCount - signCount, 0)
			},
			maxSignCount() {
				if (!this.activityDetail || !this.activityDetail.maxCount) return 99
				const maxCount = Number(this.activityDetail.maxCount) || 0
				const signCount = Number(this.activityDetail.signCount || this.activityDetail.signcount) || 0
				return Math.max(maxCount - signCount, 1)
			}
		},
		onLoad(options) {
			const userInfo = uni.getStorageSync('userInfo')
			if (!userInfo || userInfo === '' || userInfo === undefined) {
				uni.showToast({
					icon: 'none',
					title: '报名请先登录'
				})
				setTimeout(() => {
					uni.navigateTo({
						url: '/packagesPublic/login/login'
					})
				}, 1500)
				return
			}
			if (!options.id) return
			this.activityId = options.id
			this.prefillContactInfo()
			this.loadActivityDetail(options.id)
		},
		onReady() {
			this.setPageScrollHeight()
		},
		methods: {
			setPageScrollHeight() {
				this.$nextTick(() => {
					const windowInfo = uni.getWindowInfo ? uni.getWindowInfo() : uni.getSystemInfoSync()
					const footH = uni.upx2px(160) + (windowInfo.safeAreaInsets ? windowInfo.safeAreaInsets.bottom : 0)
					this.pageScrollHeight = windowInfo.windowHeight - footH
				})
			},
			prefillContactInfo() {
				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo) return
				this.signName = userInfo.nickName || ''
				this.signMobile = userInfo.phonenumber || userInfo.mobile || ''
			},
			formatMoney(value) {
				const num = Number(value)
				if (!num && num !== 0) return '0.00'
				return num.toFixed(2)
			},
			isActivityFree(item) {
				if (!item) return true
				const isFree = item.isFree
				return isFree === 1 || isFree === '1' || isFree === null || isFree === undefined
			},
			loadActivityDetail(id) {
				getActivityInfo(id).then(res => {
					const data = res.data || {}
					if (this.isActivityFree(data)) {
						uni.showToast({
							icon: 'none',
							title: '免费活动请直接报名'
						})
						setTimeout(() => {
							uni.redirectTo({
								url: `/packagesMall/Activity/detail/index?id=${id}`
							})
						}, 1500)
						return
					}
					this.activityDetail = data
					this.$nextTick(() => {
						this.setPageScrollHeight()
					})
				}).catch(() => {
					uni.showToast({
						title: '活动信息加载失败',
						icon: 'none'
					})
				})
			},
			openNoticePopup() {
				this.noticePopup.show = true
			},
			validateForm() {
				if (!this.activityDetail) {
					return '活动信息未加载'
				}
				if (String(this.activityDetail.status) !== '1') {
					return '活动未开放报名'
				}
				const phase = getActivityPhase(this.activityDetail)
				if (phase === 'ended') {
					return '活动已结束'
				}
				if (phase === 'closed') {
					return '报名已截止'
				}
				if (!this.signName.trim()) {
					return '请输入报名姓名'
				}
				if (!/^1\d{10}$/.test(String(this.signMobile).trim())) {
					return '请输入正确号码'
				}
				if (!this.signCount || this.signCount < 1) {
					return '请填写报名人数'
				}
				if (!this.agreedNotice) {
					return '请阅读并同意活动报名须知'
				}
				if (this.payAmount <= 0) {
					return '活动价格未配置'
				}
				return ''
			},
			submitOrder() {
				const errorText = this.validateForm()
				if (errorText) {
					uni.showToast({
						title: errorText,
						icon: 'none'
					})
					return
				}
				this.$u.throttle(() => {
					const params = {
						activityId: this.activityDetail.activityId,
						signName: this.signName.trim(),
						signMobile: String(this.signMobile).trim(),
						signCount: this.signCount,
						remark: this.remark.trim()
					}
					createActivityPendingOrder(params).then(res => {
						const order = res.data || {}
						if (!order.orderId) {
							uni.showToast({
								title: res.msg || '订单创建失败',
								icon: 'none'
							})
							return
						}
						uni.showToast({
							icon: 'none',
							title: '报名订单创建成功'
						})
						setTimeout(() => {
							uni.redirectTo({
								url: `/packagesMall/CashierDesk/ActivityCashierDesk?orderAmount=${order.moneyPayable}&orderId=${order.orderId}&orderNo=${order.orderNo}`
							})
						}, 1200)
					}).catch((err) => {
						uni.showToast({
							title: (err && err.message) || '订单创建失败',
							icon: 'none'
						})
					})
				}, 500)
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'ActivityConfirmOrder.scss';
</style>
