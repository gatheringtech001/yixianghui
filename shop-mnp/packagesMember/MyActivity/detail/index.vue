<template>
	<view class="page article" v-if="detailInfo">
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
					<text class="field-label">实付金额</text>
					<text class="field-value accent">￥{{ formatMoney(detailInfo.payMoney) }}</text>
				</view>
				<view class="sign-card">
					<text class="card-title">预约详情</text>
					<view class="sign-row" v-if="!isEdit">
						<text class="sign-label">联系人</text>
						<text class="sign-value">{{ signForm.signName || '-' }}</text>
					</view>
					<view class="sign-row" v-else>
						<text class="sign-label">联系人</text>
						<u-input class="sign-input" type="text" v-model="signForm.signName" border />
					</view>
					<view class="sign-row">
						<text class="sign-label">参与人数</text>
						<text class="sign-value" v-if="!isEdit || !canEditSignCount">{{ signForm.signCount || 0 }} 人</text>
						<u-number-box v-else v-model="signForm.signCount" :min="1" :max="editMaxCount" :step="1" />
					</view>
					<view class="sign-tip" v-if="isEdit && isPaidLockedCount">
						付费报名人数已锁定，如需调整请取消后重新报名
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
				<view v-if="detailInfo.orderStatus == '1' && detailInfo.payStatus == '1'" class="apply-btn ghost" @click="handleCancel">取消预约</view>
				<view v-if="canShowEditBtn" class="apply-btn primary" @click="handleEdit">{{ isEdit ? '保存修改' : '预约修改' }}</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getActivityOrderInfo, cancelActivityOrder, editActivityOrder, syncActivityOrderPay, syncActivityOrderRefund } from '@/api/activity/index'
	export default {
		data() {
			return {
				host: this.$host,
				detailInfo: null,
				orderId: null,
				isEdit: false,
				originalSignCount: 0,
				signForm: {
					signCount: 0,
					signMobile: '',
					signName: '',
				}
			}
		},
		computed: {
			coverUrl() {
				if (!this.detailInfo || !this.detailInfo.activityCover) return '/static/home-design/entry-stay.jpg'
				const cover = this.detailInfo.activityCover
				if (cover.startsWith('http') || cover.startsWith('/static/')) return cover.startsWith('/') ? cover : cover
				return this.host + cover
			},
			isPaidLockedCount() {
				return !!(this.detailInfo
					&& String(this.detailInfo.payStatus) === '1'
					&& Number(this.detailInfo.payMoney) > 0)
			},
			canEditSignCount() {
				// 付费已支付：人数锁定；待支付/免费报名可改人数
				return !this.isPaidLockedCount
			},
			canShowEditBtn() {
				if (!this.detailInfo) return false
				const payStatus = String(this.detailInfo.payStatus)
				const orderStatus = String(this.detailInfo.orderStatus)
				return payStatus === '0' || (orderStatus === '1' && payStatus === '1')
			},
			editMaxCount() {
				const max = Number(this.detailInfo && this.detailInfo.maxCount) || 99
				return Math.max(max, this.signForm.signCount || 1)
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
		},
		onShow() {
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
				uni.setNavigationBarTitle({
					title: info.activityName || '活动预约'
				})
			},
			async getDetail(orderId) {
				if (!orderId) return
				let { data } = await getActivityOrderInfo(orderId)
				this.applyOrderData(data)
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
				const isPaid = this.detailInfo.payStatus == '1'
				uni.showModal({
					title: '提示',
					content: isUnpaid ? '确认取消该订单？' : (isPaid ? '确认取消预约？付费订单将发起退款' : '确认取消？'),
					success: async (modalRes) => {
						if (!modalRes.confirm) return
						try {
							await cancelActivityOrder(this.detailInfo.orderId)
							uni.showToast({
								icon: 'none',
								title: isUnpaid ? '订单已取消' : (isPaid ? '已提交退款' : '取消预约成功')
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
				if (this.isEdit) {
					if (!this.signForm.signName || !String(this.signForm.signName).trim()) {
						uni.showToast({ icon: 'none', title: '请填写联系人' })
						return
					}
					if (!this.signForm.signMobile || !String(this.signForm.signMobile).trim()) {
						uni.showToast({ icon: 'none', title: '请填写联系电话' })
						return
					}
					const signCount = this.canEditSignCount ? this.signForm.signCount : this.originalSignCount
					let params = {
						orderId: this.detailInfo.orderId,
						signCount: signCount,
						signMobile: String(this.signForm.signMobile).trim(),
						signName: String(this.signForm.signName).trim()
					}
					editActivityOrder(params).then(() => {
						uni.showToast({
							icon: 'none',
							title: '预约信息修改成功'
						})
						this.isEdit = false
						this.getDetail(this.detailInfo.orderId)
					}).catch((err) => {
						uni.showToast({
							icon: 'none',
							title: (err && err.message) || '修改失败'
						})
					})
				} else {
					this.isEdit = true
				}
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
</style>
