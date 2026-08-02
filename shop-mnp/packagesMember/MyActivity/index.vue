<template>
	<view class="page">
		<view class="acticity-list" v-if="activityList.length > 0">
			<view class="list" v-for="(item, index) in activityList" :key="index" @click.stop="activityOrderDetail(item)">
				<view class="thumb">
					<image :src="host+ item.activityCover" mode=""></image>
				</view>
				<view class="item">
					<view class="title-row">
						<text class="two-omit">{{item.activityName}}</text>
						<text class="status-tag pending" v-if="item.payStatus == '0'">待支付</text>
						<text class="status-tag" v-else-if="item.payStatus == '2'">已取消</text>
						<text class="status-tag pending" v-else-if="item.payStatus == '3'">退款中</text>
						<text class="status-tag" v-else-if="item.payStatus == '4'">已退款</text>
						<text class="status-tag success" v-else-if="item.orderStatus == '1'">已报名</text>
					</view>
					<view class="price-more">
						<view class="depreciate">
							<text>活动时间：{{item.activityTime}}</text>
						</view>
						<view class="depreciate" v-if="item.payStatus == '0'">
							<text class="pending-tag">应付 ￥{{ formatMoney(item.moneyPayable) }}</text>
						</view>
						<view class="depreciate" v-else-if="item.payStatus == '2'">
							<text class="free-tag" v-if="Number(item.payMoney) > 0">原付 ￥{{ formatMoney(item.payMoney) }}</text>
							<text class="free-tag" v-else>未支付取消</text>
						</view>
						<view class="depreciate" v-else-if="item.payStatus == '3' || item.payStatus == '4'">
							<text class="paid-tag">实付 ￥{{ formatMoney(item.payMoney || item.moneyPayable) }}</text>
						</view>
						<view class="depreciate" v-else-if="item.payStatus == '1' || Number(item.payMoney) > 0">
							<text class="paid-tag">已付 ￥{{ formatMoney(item.payMoney || item.moneyPayable) }}</text>
						</view>
						<view class="depreciate" v-else>
							<text class="free-tag">免费报名</text>
						</view>
					</view>
				</view>
			</view>
		</view>
		<view class="empty" v-else>
			<u-empty text="暂无活动预约" mode="list"></u-empty>
		</view>
	</view>
</template>

<script>
	import { getActivityOrderList, syncActivityOrderPay, syncActivityOrderRefund } from '@/api/activity/index'
	export default {
		data() {
			return {
				host: this.$host,
				activityList: []
			};
		},
		onShow() {
			this.getMyActivity()
		},
		methods:{
			formatMoney(value) {
				const num = Number(value)
				return Number.isFinite(num) ? num.toFixed(2) : '0.00'
			},
			mapRows(rows) {
				return (rows || []).map(row => {
					const info = row.activityInfo || {}
					return {
						...info,
						orderId: row.orderId,
						activityOrderId: row.orderId,
						orderStatus: row.status,
						payStatus: row.payStatus,
						moneyPayable: row.moneyPayable,
						payMoney: row.payMoney,
						orderNo: row.orderNo
					}
				})
			},
			async getMyActivity() {
				let data = await getActivityOrderList()
				this.activityList = this.mapRows(data.rows)
				// 待支付：主动查单；退款中：主动查退款，修复回调延迟卡状态
				const pendingPay = (data.rows || []).filter(row => String(row.payStatus) === '0').slice(0, 5)
				const pendingRefund = (data.rows || []).filter(row => String(row.payStatus) === '3').slice(0, 5)
				if (pendingPay.length === 0 && pendingRefund.length === 0) return
				for (const row of pendingPay) {
					try {
						await syncActivityOrderPay(row.orderId)
					} catch (e) {}
				}
				for (const row of pendingRefund) {
					try {
						await syncActivityOrderRefund(row.orderId)
					} catch (e) {}
				}
				data = await getActivityOrderList()
				this.activityList = this.mapRows(data.rows)
			},
			
			// 预约详情
			activityOrderDetail(item){
				uni.navigateTo({
					url: `./detail/index?orderId=${item.orderId}`
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'index.scss';
</style>
