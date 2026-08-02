<template>
	<view class="container">
		<view class="summary">
			<view class="summary-item">
				<text class="label">累计收益</text>
				<text class="value">¥ {{ summary.totalIncomeText }}</text>
			</view>
			<view class="summary-divider"></view>
			<view class="summary-item">
				<text class="label">待结算</text>
				<text class="value pending">¥ {{ summary.pendingAmountText }}</text>
			</view>
		</view>
		<view class="record-list" v-if="list.length > 0">
			<view class="record-card" v-for="item in list" :key="item.id">
				<view class="main">
					<text class="title">{{ item.productName }}</text>
					<text class="date">{{ item.tradeDate || item.createTime || '' }}</text>
				</view>
				<view class="side">
					<text class="amount">+{{ item.incomeText }}</text>
					<text class="status" :class="item.statusClass">{{ item.statusText }}</text>
				</view>
			</view>
		</view>
		<view class="load-more" v-if="list.length > 0">
			<u-loadmore :status="loadStatus" />
		</view>
		<view class="empty" v-else-if="!loading">
			<u-empty text="暂无收支明细" mode="list"></u-empty>
		</view>
		<view class="loading-wrap" v-if="loading && list.length === 0">
			<u-loading mode="circle" color="#FF7D00"></u-loading>
		</view>
	</view>
</template>

<script>
	import './index.scss'
	import { getConsultantIncomeList } from '@/api/member/index'
	import { formatMoney } from '@/utils/consultant'

	export default {
		data() {
			return {
				loading: false,
				list: [],
				pageNum: 1,
				pageSize: 10,
				total: 0,
				loadStatus: 'loadmore',
				summary: {
					totalIncomeText: '0.00',
					pendingAmountText: '0.00'
				}
			}
		},
		onLoad() {
			this.loadIncomeList(true)
		},
		onPullDownRefresh() {
			this.loadIncomeList(true).finally(() => {
				uni.stopPullDownRefresh()
			})
		},
		onReachBottom() {
			if (this.loadStatus === 'loadmore') {
				this.loadIncomeList(false)
			}
		},
		methods: {
			async loadIncomeList(refresh) {
				if (refresh) {
					this.pageNum = 1
					this.loadStatus = 'loadmore'
				} else if (this.loadStatus !== 'loadmore') {
					return
				}
				this.loading = true
				this.loadStatus = 'loading'
				try {
					const res = await getConsultantIncomeList({
						pageNum: this.pageNum,
						pageSize: this.pageSize
					})
					if (res.code !== 200) {
						uni.showToast({
							title: res.msg || '加载收支明细失败',
							icon: 'none'
						})
						if (refresh) this.list = []
						this.loadStatus = 'nomore'
						return
					}
					const rows = res.rows || []
					this.total = res.total || 0
					const mapped = rows.map(item => ({
						id: item.incomeId,
						productName: item.productName || '收益记录',
						tradeDate: item.tradeDate,
						createTime: item.createTime,
						incomeText: formatMoney(item.consultantIncome),
						statusText: item.settlement == 1 ? '已结算' : '待结算',
						statusClass: item.settlement == 0 ? 'pending' : 'settled'
					}))
					this.list = refresh ? mapped : this.list.concat(mapped)
					if (res.summary) {
						this.summary = {
							totalIncomeText: formatMoney(res.summary.totalIncome),
							pendingAmountText: formatMoney(res.summary.pendingAmount)
						}
					}
					const hasMore = this.list.length < this.total
					this.loadStatus = hasMore ? 'loadmore' : 'nomore'
					if (hasMore) this.pageNum += 1
				} catch (e) {
					if (refresh) this.list = []
					this.loadStatus = 'nomore'
					uni.showToast({
						title: e.message || '加载失败',
						icon: 'none'
					})
				} finally {
					this.loading = false
				}
			}
		}
	}
</script>
