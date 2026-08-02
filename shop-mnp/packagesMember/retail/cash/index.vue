<template>
	<view class="container">
		<view class="record-list" v-if="list.length > 0">
			<view class="record-card" v-for="item in list" :key="item.id">
				<view class="main">
					<text class="title">{{ item.cashTypeText }}</text>
					<text class="date">{{ item.createTime || '' }}</text>
					<text class="pay-no" v-if="item.payNo">单号：{{ item.payNo }}</text>
				</view>
				<view class="side">
					<text class="amount">-{{ item.moneyText }}</text>
					<text class="status" :class="item.statusClass">{{ item.statusText }}</text>
				</view>
			</view>
		</view>
		<view class="load-more" v-if="list.length > 0">
			<u-loadmore :status="loadStatus" />
		</view>
		<view class="empty" v-else-if="!loading">
			<u-empty text="暂无提现记录" mode="list"></u-empty>
		</view>
		<view class="loading-wrap" v-if="loading && list.length === 0">
			<u-loading mode="circle" color="#FF7D00"></u-loading>
		</view>
	</view>
</template>

<script>
	import './index.scss'
	import { getConsultantCashList } from '@/api/member/index'
	import { formatMoney } from '@/utils/consultant'

	export default {
		data() {
			return {
				loading: false,
				list: [],
				pageNum: 1,
				pageSize: 10,
				total: 0,
				loadStatus: 'loadmore'
			}
		},
		onLoad() {
			this.loadCashList(true)
		},
		onPullDownRefresh() {
			this.loadCashList(true).finally(() => {
				uni.stopPullDownRefresh()
			})
		},
		onReachBottom() {
			if (this.loadStatus === 'loadmore') {
				this.loadCashList(false)
			}
		},
		methods: {
			getStatusText(status) {
				const map = { '0': '处理中', '1': '提现成功', '2': '提现失败' }
				return map[status] || '处理中'
			},
			getStatusClass(status) {
				if (status == '1') return 'success'
				if (status == '2') return 'failed'
				return 'pending'
			},
			async loadCashList(refresh) {
				if (refresh) {
					this.pageNum = 1
					this.loadStatus = 'loadmore'
				} else if (this.loadStatus !== 'loadmore') {
					return
				}
				this.loading = true
				this.loadStatus = 'loading'
				try {
					const res = await getConsultantCashList({
						pageNum: this.pageNum,
						pageSize: this.pageSize
					})
					if (res.code !== 200) {
						uni.showToast({
							title: res.msg || '加载提现记录失败',
							icon: 'none'
						})
						if (refresh) this.list = []
						this.loadStatus = 'nomore'
						return
					}
					const rows = res.rows || []
					this.total = res.total || 0
					const mapped = rows.map(item => ({
						id: item.cashId,
						cashTypeText: item.cashType || '提现',
						createTime: item.createTime,
						payNo: item.payNo,
						status: item.status,
						moneyText: formatMoney(item.money),
						statusText: this.getStatusText(item.status),
						statusClass: this.getStatusClass(item.status)
					}))
					this.list = refresh ? mapped : this.list.concat(mapped)
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
