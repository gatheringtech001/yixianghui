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
							<text class="info-value">{{ activityDetail.maxCount ? remainCount + ' / ' + activityDetail.maxCount : '不限人数' }}</text>
						</view>
					</view>
				</view>

				<view class="form-card">
					<view class="card-title">报名信息</view>
					<view class="form-guide">请填写实际联系人的姓名、手机号和参与人数，方便活动组织方确认报名。</view>
					<view class="form-row">
						<view class="row-label"><text class="required-mark">*</text>报名联系人（必填）</view>
						<u-input v-model="signName" @input="contactEdited.name = true" type="text" maxlength="30" border placeholder="请输入联系人的真实姓名" />
						<view class="row-hint">请填写真实姓名；默认带入的昵称如非实名，请修改。</view>
					</view>
					<view class="form-row">
						<view class="row-label"><text class="required-mark">*</text>联系电话（必填）</view>
						<u-input v-model="signMobile" @input="contactEdited.mobile = true" type="number" maxlength="11" border placeholder="请输入11位手机号码" />
						<view class="row-hint">请核对并填写正确的11位手机号，确保活动组织方能联系到您。</view>
					</view>
					<view class="form-row">
						<view class="row-label">参与人数（必填）</view>
						<u-number-box v-model="signCount" :min="1" :max="maxSignCount" :step="1" />
						<view class="row-hint">包含您本人；如替他人报名，请填写实际参与总人数。</view>
					</view>
					<view class="form-row">
						<view class="row-label">备注</view>
						<u-input v-model="remark" type="textarea" border placeholder="选填，如有特殊情况可留言" />
					</view>
				</view>

				<view v-if="isFreeSignup" class="price-card">
					<view class="card-title">免费报名</view>
					<view class="form-guide">本次报名无需在小程序付款。如活动介绍另有行程费、定金等说明，请按介绍联系活动组织方确认。</view>
				</view>
				<view v-else class="price-card">
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
				<view v-if="isFreeSignup" class="free-label">免费报名</view>
				<view v-else class="price_view">
					￥<text>{{ payAmountText }}</text>
				</view>
				<view class="text_view">{{ isFreeSignup ? '无需小程序付款' : '应付金额' }}</view>
			</view>
			<button class="pay_button_view" :disabled="submitting || signupSubmitted" :loading="submitting" @click="submitOrder">{{ submitting ? '正在提交' : '确认报名' }}</button>
		</view>

		<u-popup v-model="noticePopup.show" @touchmove.stop.prevent mode="bottom" border-radius="20" :closeable="true">
			<view class="notice_popup_view">
				<view class="notice_title_view">活动报名须知</view>
				<scroll-view scroll-y class="notice_content_view">
					<view v-if="isFreeSignup" class="notice_text">
						<text>1. 请确认报名联系人、联系电话和参与人数准确，以便活动组织方联系您。</text>
						<text>2. 本次为免费报名，不进入小程序付款流程；活动介绍中另行说明的费用请联系活动组织方确认。</text>
						<text>3. 报名成功后可在「我的预约」查看、修改或取消预约，并查看活动客服二维码。</text>
					</view>
					<view v-else class="notice_text">
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
	import { getActivityInfo, addActivityOrder, createActivityPendingOrder } from '@/api/activity/index'
	import { getInfo } from '@/api/public'
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
				submitting: false,
				signupSubmitted: false,
				contactEdited: { name: false, mobile: false },
				noticePopup: {
					show: false
				}
			}
		},
		computed: {
			isFreeSignup() {
				return this.isActivityFree(this.activityDetail)
			},
			coverUrl() {
				if (!this.activityDetail || !this.activityDetail.activityCover) {
					return '/static/home-design/entry-stay.jpg'
				}
				const cover = this.activityDetail.activityCover
				if (cover.startsWith('http') || cover.startsWith('/static/')) return cover
				return this.host + cover
			},
			unitPrice() {
				if (this.isFreeSignup) return 0
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
			async prefillContactInfo() {
				this.applyContactDefaults(uni.getStorageSync('userInfo'))
				try {
					const res = await getInfo()
					if (res && res.code === 200 && !this.submitting && !this.signupSubmitted) this.applyContactDefaults(res.data)
				} catch (_) {
					// 获取失败仍可使用缓存或手动填写，不阻止报名。
				}
			},
			applyContactDefaults(userInfo) {
				if (!userInfo || typeof userInfo !== 'object') return
				const name = String(userInfo.nickName || '').trim()
				const mobile = String(userInfo.phonenumber || userInfo.mobile || '').trim()
				if (!this.contactEdited.name && name && name !== '微信小程序用户') this.signName = name
				if (!this.contactEdited.mobile && /^1[3-9]\d{9}$/.test(mobile)) this.signMobile = mobile
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
				if (!String(this.signName || '').trim()) {
					return '请填写报名联系人姓名'
				}
				if (String(this.signName).trim().length > 30) return '联系人姓名不能超过30个字'
				if (!/^1[3-9]\d{9}$/.test(String(this.signMobile).trim())) {
					return '请填写正确的11位手机号码'
				}
				if (!Number.isInteger(Number(this.signCount)) || Number(this.signCount) < 1) {
					return '参与人数请填写大于0的整数'
				}
				if (this.activityDetail.maxCount && Number(this.remainCount) < Number(this.signCount)) return '剩余名额不足，请调整参与人数'
				if (Number(this.signCount) > this.maxSignCount) return '参与人数超出本次可报名人数'
				if (!this.isFreeSignup && this.payAmount <= 0) {
					return '活动价格未配置'
				}
				return ''
			},
			async submitOrder() {
				if (this.submitting || this.signupSubmitted) return
				const errorText = this.validateForm()
				if (errorText) {
					uni.showToast({
						title: errorText,
						icon: 'none'
					})
					return
				}
				this.submitting = true
				try {
					const params = {
						activityId: this.activityDetail.activityId,
						signName: this.signName.trim(),
						signMobile: String(this.signMobile).trim(),
						signCount: Number(this.signCount),
						remark: String(this.remark || '').trim()
					}
					const res = await (this.isFreeSignup ? addActivityOrder(params) : createActivityPendingOrder(params))
					const order = (res && res.data) || {}
					if (!res || res.code !== 200 || !order.orderId) throw new Error((res && res.msg) || '报名未成功，请到我的预约核对后重试')
					this.signupSubmitted = true
					if (this.isFreeSignup) {
						uni.redirectTo({
							url: `/packagesMember/MyActivity/detail/index?orderId=${order.orderId}&showTeacher=1`,
							fail: () => uni.showToast({ icon: 'none', title: '报名成功，请到我的预约查看' })
						})
						return
					}
					uni.showToast({ icon: 'none', title: '报名订单创建成功' })
					setTimeout(() => {
						uni.redirectTo({
							url: `/packagesMall/CashierDesk/ActivityCashierDesk?orderAmount=${order.moneyPayable}&orderId=${order.orderId}&orderNo=${order.orderNo}`
						})
					}, 1200)
				} catch (err) {
					uni.showToast({ title: (err && err.message) || '报名失败，请重试', icon: 'none' })
				} finally {
					this.submitting = false
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'ActivityConfirmOrder.scss';
</style>
