<template>
	<view :class="displayModeClass" class="page article" v-if="detailInfo">
		<scroll-view class="page-scroll" scroll-y :show-scrollbar="false">
			<view class="head">
				<image :src="coverUrl" mode="aspectFill" />
			</view>
			<view class="detail-box">
				<view class="title-row">
					<view class="status" :class="statusClass">{{ orderStatusText }}</view>
					<view class="info">{{ detailInfo.activityName }}</view>
				</view>
				<view class="item">
					<text class="field-label">活动地点</text>
					<text class="field-value">{{ detailInfo.address }}</text>
				</view>
				<view class="item">
					<text class="field-label">活动时间</text>
					<text class="field-value">{{ detailInfo.activityTime }}</text>
				</view>
				<view class="item" v-if="detailInfo.payStatus == '0'">
					<text class="field-label">应付金额</text>
					<text class="field-value accent">￥{{ formatMoney(detailInfo.moneyPayable) }}</text>
				</view>
				<view class="item" v-else-if="Number(detailInfo.payMoney) > 0">
					<text class="field-label">当前报名金额</text>
					<text class="field-value accent">￥{{ formatMoney(detailInfo.payMoney) }}</text>
				</view>
				<activity-teacher-contact ref="teacherContact" v-if="canShowTeacher" v-model="showTeacherPopup" />
				<view class="sign-card">
					<text class="card-title">预约详情</text>
					<view class="sign-row" v-if="!isEdit">
						<text class="sign-label">报名联系人</text>
						<text class="sign-value">{{ signForm.signName || '-' }}</text>
					</view>
					<view class="sign-row" v-else>
						<text class="sign-label">报名联系人</text>
						<u-input class="sign-input" type="text" v-model="signForm.signName" border />
					</view>
					<view class="sign-row">
						<text class="sign-label">参与人数</text>
						<text class="sign-value" v-if="!isEdit || !canEditSignCount">{{ signForm.signCount || 0 }} 人</text>
						<u-number-box v-else v-model="signForm.signCount" :min="1" :max="editMaxCount" :step="1" :input-height="isSeniorMode ? 112 : 50" :input-width="isSeniorMode ? 112 : 80" :size="isSeniorMode ? 42 : 26" />
					</view>
					<view class="sign-tip" v-if="isEdit">
						{{ isPaidBooking ? '增加人数需支付差价，减少人数需提交退款申请；管理员批准并退款成功后生效。' : '免费活动仅限本人报名，人数固定为1。' }}
					</view>
					<view class="sign-row" v-if="!isEdit">
						<text class="sign-label">联系电话</text>
						<text class="sign-value">{{ signForm.signMobile || '-' }}</text>
					</view>
					<view class="sign-row" v-else>
						<text class="sign-label">联系电话</text>
						<u-input class="sign-input" type="number" maxlength="11" v-model="signForm.signMobile" border />
					</view>
				</view>
				<view class="sign-card change-card" v-if="adjustment">
					<text class="card-title">{{ adjustmentTitle }}</text>
					<view class="sign-tip">{{ adjustment.fromCount }} 人 → {{ adjustment.toCount }} 人 · ￥{{ formatMoney(adjustment.amount) }}</view>
					<view class="sign-tip">{{ adjustmentTip }}</view>
					<view class="sign-tip" v-if="adjustment.reviewNote">审核说明：{{ adjustment.reviewNote }}</view>
					<view class="change-actions" v-if="adjustment.state === 'PAYMENT'">
						<button class="change-pay" :disabled="adjustmentBusy" @click="payDifference">支付差价</button>
						<button class="change-cancel" :disabled="adjustmentBusy" @click="cancelDifference">取消本次调整</button>
					</view>
				</view>
				<view v-if="adjustmentLoadFailed" class="sign-card change-card" @click="getDetail(orderId)">报名调整状态未能加载，点击重试</view>
			</view>
		</scroll-view>
		<view class="bottom">
			<button class="share-btn" open-type="share">
				<u-icon name="share-fill" color="#701018" size="32"></u-icon>
				<text class="btn-label">分享</text>
			</button>
			<view class="action-group">
				<view v-if="detailInfo.payStatus == '0'" class="apply-btn primary" @click="goPay">去支付</view>
				<view v-if="detailInfo.payStatus == '0'" class="apply-btn ghost" @click="handleCancel">取消订单</view>
				<view v-if="detailInfo.orderStatus == '1' && detailInfo.payStatus == '1' && !hasActiveAdjustment" class="apply-btn ghost" @click="handleCancel">{{ isPaidBooking ? '申请退款' : '取消预约' }}</view>
				<view v-if="canShowEditBtn" class="apply-btn primary" @click="handleEdit">{{ isEdit ? '保存修改' : '预约修改' }}</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getActivityOrderInfo, cancelActivityOrder, editActivityOrder, syncActivityOrderPay, syncActivityOrderRefund, getBookingAdjustment, prepayBookingAdjustment, syncBookingAdjustment, cancelBookingAdjustment } from '@/api/activity/index'
	import ActivityTeacherContact from './ActivityTeacherContact.vue'
	export default {
		components: { ActivityTeacherContact },
		data() {
			return {
				host: this.$host,
				detailInfo: null,
				orderId: null,
				teacherPromptPending: false,
				showTeacherPopup: false,
				isEdit: false,
				originalSignCount: 0,
				adjustment: null,
				adjustmentBusy: false,
				adjustmentLoadFailed: false,
				saving: false,
				signForm: {
					signCount: 0,
					signMobile: '',
					signName: '',
				}
			}
		},
		computed: {
			canShowTeacher() {
				return !!(this.detailInfo && String(this.detailInfo.orderStatus) === '1'
					&& String(this.detailInfo.payStatus) === '1')
			},
			coverUrl() {
				if (!this.detailInfo || !this.detailInfo.activityCover) return '/static/home-design/entry-stay.jpg'
				const cover = this.detailInfo.activityCover
				if (cover.startsWith('http') || cover.startsWith('/static/')) return cover.startsWith('/') ? cover : cover
				return this.host + cover
			},
			isPaidBooking() {
				return !!(this.detailInfo
					&& String(this.detailInfo.payStatus) === '1'
					&& Number(this.detailInfo.payMoney) > 0)
			},
			canEditSignCount() {
				return !!this.detailInfo && (String(this.detailInfo.payStatus) === '0' || this.isPaidBooking) && !this.hasActiveAdjustment
			},
			hasActiveAdjustment() {
				return this.adjustmentLoadFailed || (!!this.adjustment && ['PAYMENT', 'REVIEW', 'PROCESSING'].includes(this.adjustment.state))
			},
			adjustmentTitle() {
				return ({ PAYMENT: '待支付差价', REVIEW: '退款申请待审核', PROCESSING: '已批准，退款处理中', COMPLETED: '人数调整已完成', REJECTED: '退款申请未通过', CANCELLED: '本次调整已取消' })[this.adjustment && this.adjustment.state] || '报名调整'
			},
			adjustmentTip() {
				return ({ PAYMENT: '差价到账后新增人数才生效；未支付不影响原预约。', REVIEW: '管理员审核前，原报名人数和名额保持不变。', PROCESSING: '退款到账确认后再更新人数，请勿重复申请。', REJECTED: '原预约保持不变。', CANCELLED: '原预约保持不变。', COMPLETED: '已更新预约，请以当前预约人数为准。' })[this.adjustment && this.adjustment.state] || ''
			},
			canShowEditBtn() {
				if (!this.detailInfo || this.adjustmentLoadFailed) return false
				const payStatus = String(this.detailInfo.payStatus)
				const orderStatus = String(this.detailInfo.orderStatus)
				return payStatus === '0' || (orderStatus === '1' && payStatus === '1')
			},
			editMaxCount() {
				const max = Number(this.detailInfo && this.detailInfo.maxCount)
				if (!max) return Math.max(99, this.originalSignCount)
				return Math.max(this.originalSignCount, max - Number(this.detailInfo.signCount || 0) + (this.isPaidBooking ? this.originalSignCount : 0))
			},
			orderStatusText() {
				if (!this.detailInfo) return ''
				if (this.detailInfo.payStatus == '0') return '待支付'
				if (this.detailInfo.payStatus == '2') return '已取消'
				if (this.detailInfo.payStatus == '3') return '退款中'
				if (this.detailInfo.payStatus == '4') return '已退款'
				if (this.detailInfo.orderStatus == '1') return '已报名'
				return '处理中'
			},
			statusClass() {
				if (!this.detailInfo) return ''
				if (this.detailInfo.payStatus == '0') return 'pending'
				if (this.detailInfo.payStatus == '2' || this.detailInfo.payStatus == '4') return ''
				if (this.detailInfo.payStatus == '3') return 'pending'
				if (this.detailInfo.orderStatus == '1') return 'success'
				return ''
			}
		},
		onLoad(option) {
			this.orderId = option.orderId || option.id
			this.teacherPromptPending = option.showTeacher === '1'
		},
		onShow() {
			if (this.$refs.teacherContact) this.$refs.teacherContact.loadQr()
			if (this.orderId) {
				this.getDetail(this.orderId)
			}
		},
		methods: {
			formatMoney(value) {
				const num = Number(value)
				return Number.isFinite(num) ? num.toFixed(2) : '0.00'
			},
			applyOrderData(data) {
				const info = (data && data.activityInfo) || {}
				this.detailInfo = {
					...info,
					activityOrderId: data.orderId,
					orderId: data.orderId,
					orderStatus: data.status,
					payStatus: data.payStatus,
					moneyPayable: data.moneyPayable,
					payMoney: data.payMoney,
					orderNo: data.orderNo
				}
				this.signForm.signCount = data.signCount
				this.signForm.signMobile = data.signMobile
				this.signForm.signName = data.signName
				this.originalSignCount = data.signCount
				this.isEdit = false
				// 仅服务端确认报名成功后弹一次；日常查看预约只展示二维码卡片。
				if (!this.canShowTeacher) this.showTeacherPopup = false
				if (this.canShowTeacher && this.teacherPromptPending) {
					this.showTeacherPopup = true
					this.teacherPromptPending = false
				}
				uni.setNavigationBarTitle({
					title: info.activityName || '活动预约'
				})
			},
			async getDetail(orderId) {
				if (!orderId) return
				let { data } = await getActivityOrderInfo(orderId)
				this.applyOrderData(data)
				try {
					const change = await getBookingAdjustment(orderId)
					this.adjustment = change.data || null
					this.adjustmentLoadFailed = false
				} catch (error) { this.adjustmentLoadFailed = true }
				if (this.adjustment && this.adjustment.state === 'PAYMENT') {
					try {
						const synced = await syncBookingAdjustment(this.adjustment.adjustmentId)
						this.adjustment = synced.data
						if (this.adjustment.state === 'COMPLETED') this.applyOrderData((await getActivityOrderInfo(orderId)).data)
					} catch (e) { /* 未到账时保留原预约与补款入口 */ }
				}
				// 待支付：主动查单；退款中：主动查退款，避免回调延迟一直显示退款中
				if (data && String(data.payStatus) === '0') {
					try {
						await syncActivityOrderPay(orderId)
						const refreshed = await getActivityOrderInfo(orderId)
						this.applyOrderData(refreshed.data)
					} catch (e) {
						// 微信侧未支付成功时忽略
					}
				} else if (data && String(data.payStatus) === '3') {
					try {
						await syncActivityOrderRefund(orderId)
						const refreshed = await getActivityOrderInfo(orderId)
						this.applyOrderData(refreshed.data)
					} catch (e) {
						// 微信侧退款未完成时忽略
					}
				}
			},
			goPay() {
				if (!this.detailInfo || !this.detailInfo.orderId) return
				uni.navigateTo({
					url: `/packagesMall/CashierDesk/ActivityCashierDesk?orderAmount=${this.detailInfo.moneyPayable}&orderId=${this.detailInfo.orderId}&orderNo=${this.detailInfo.orderNo || ''}`
				})
			},
			async handleCancel() {
				const isUnpaid = this.detailInfo.payStatus == '0'
				const isPaid = this.isPaidBooking
				uni.showModal({
					title: '提示',
					content: isUnpaid ? '确认取消该订单？' : (isPaid ? '提交取消报名退款申请？需管理员批准，退款成功后取消预约。' : '确认取消本人预约？'),
					success: async (modalRes) => {
						if (!modalRes.confirm) return
						try {
							await cancelActivityOrder(this.detailInfo.orderId)
							uni.showToast({
								icon: 'none',
								title: isUnpaid ? '订单已取消' : (isPaid ? '退款申请已提交，待审核' : '取消预约成功')
							})
							setTimeout(() => { uni.navigateBack() }, 2000)
						} catch (err) {
							uni.showToast({
								icon: 'none',
								title: (err && err.message) || '取消失败'
							})
						}
					}
				})
			},
			handleEdit() {
				if (this.saving) return
				if (this.isEdit) {
					if (!this.signForm.signName || !String(this.signForm.signName).trim()) {
						uni.showToast({ icon: 'none', title: '请填写联系人' })
						return
					}
					if (!/^1[3-9]\d{9}$/.test(String(this.signForm.signMobile || '').trim())) {
						uni.showToast({ icon: 'none', title: '请填写正确的11位手机号码' })
						return
					}
					const signCount = this.canEditSignCount ? this.signForm.signCount : this.originalSignCount
					let params = {
						orderId: this.detailInfo.orderId,
						signCount: signCount,
						signMobile: String(this.signForm.signMobile).trim(),
						signName: String(this.signForm.signName).trim()
					}
					this.saving = true
					const countChanged = this.isPaidBooking && Number(signCount) !== Number(this.originalSignCount)
					editActivityOrder(params).then(async () => {
						uni.showToast({
							icon: 'none',
							title: countChanged ? (Number(signCount) > Number(this.originalSignCount) ? '请支付新增人数差价' : '退款申请已提交，待审核') : '预约信息修改成功'
						})
						this.isEdit = false
						await this.getDetail(this.detailInfo.orderId)
					}).catch((err) => {
						uni.showToast({
							icon: 'none',
							title: (err && err.message) || '修改失败'
						})
					}).finally(() => { this.saving = false })
				} else {
					this.isEdit = true
				}
			},
			async payDifference() {
				if (this.adjustmentBusy || !this.adjustment || this.adjustment.state !== 'PAYMENT') return
				this.adjustmentBusy = true
				try {
					const { data } = await prepayBookingAdjustment(this.adjustment.adjustmentId)
					if (!data || !data.timeStamp || !data.nonceStr || !(data.packageVal || data.package) || !data.paySign) throw new Error('支付参数不完整，请稍后重试')
					await new Promise((resolve, reject) => uni.requestPayment({ ...data, package: data.packageVal || data.package, success: resolve, fail: reject }))
					const result = await syncBookingAdjustment(this.adjustment.adjustmentId)
					uni.showToast({ icon: 'none', title: result.data.state === 'COMPLETED' ? '补款成功，人数已更新' : '支付结果确认中，请稍后刷新' })
				} catch (error) {
					uni.showToast({ icon: 'none', title: /cancel/.test(String(error && error.errMsg)) ? '已取消支付，原预约不变' : ((error && error.message) || '支付未完成，原预约不变') })
				} finally { this.adjustmentBusy = false; await this.getDetail(this.orderId) }
			},
			cancelDifference() {
				if (this.adjustmentBusy || !this.adjustment) return
				uni.showModal({ title: '取消本次人数调整？', content: '只取消未支付的新增人数，原预约保持不变。', success: async result => {
					if (!result.confirm) return
					this.adjustmentBusy = true
					try { await cancelBookingAdjustment(this.adjustment.adjustmentId); await this.getDetail(this.orderId) }
					catch (error) { uni.showToast({ icon: 'none', title: (error && error.message) || '未能确认关闭，请稍后重试' }) }
					finally { this.adjustmentBusy = false }
				} })
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'index.scss';

	.sign-tip {
		margin: -4rpx 0 16rpx;
		padding: 0 4rpx;
		font-size: 22rpx;
		line-height: 1.5;
		color: #999999;
	}
	.change-actions { display: flex; flex-wrap: wrap; gap: 16rpx; }
	.change-actions button { flex: 1 1 220rpx; min-width: 0; margin: 0; font-size: 28rpx; line-height: 1.5; padding: 20rpx 12rpx; border-radius: 40rpx; }
	.change-pay { background: #701018; color: #fff; }
	.change-cancel { background: #fff5f0; color: #701018; }
	.change-card .sign-tip { color: #665958; margin: 16rpx 0; }
	.senior-mode .change-actions button { font-size: 36rpx; min-height: 96rpx; }
	.senior-mode .sign-card .sign-row { grid-template-columns: minmax(0, 1fr); }
	.senior-mode .sign-card .sign-label, .senior-mode .sign-card .sign-value { font-size: 36rpx; white-space: normal; }
	.senior-mode .sign-tip { font-size: 32rpx; }
</style>
