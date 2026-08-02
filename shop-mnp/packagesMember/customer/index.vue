<template>
	<view class="container">
		<view class="page-header" v-if="total > 0">
			<text class="header-title">共 {{ total }} 位客户</text>
		</view>
		<view class="customer-list" v-if="customers.length > 0">
			<view class="customer-card" v-for="customer in customers" :key="customer.id">
				<u-avatar :text="customerAvatarText(customer.name)" bg-color="#EFEBDF" size="large"></u-avatar>
				<view class="info">
					<text class="customer-name">{{ customer.name || '未命名客户' }}</text>
					<text class="customer-phone">{{ customer.phone || '暂无联系电话' }}</text>
					<text class="customer-meta" v-if="customer.customerNo">编号 {{ customer.customerNo }}</text>
					<text class="customer-meta" v-if="customer.signTime">登记 {{ customer.signTime }}</text>
				</view>
				<view class="phone" v-if="customer.phone">
					<view class="call-btn" @click="callMobile(customer)">
						<u-icon name="phone-fill" size="36" color="#fff" />
					</view>
				</view>
			</view>
		</view>
		<view class="load-more" v-if="customers.length > 0">
			<u-loadmore :status="loadStatus" />
		</view>
		<view class="empty" v-else-if="!loading">
			<u-empty text="暂无客户" mode="list"></u-empty>
		</view>
		<view class="loading-wrap" v-if="loading && customers.length === 0">
			<u-loading mode="circle" color="#FF7D00"></u-loading>
		</view>
	</view>
</template>

<script>
	import './index.scss'
	import { getCustomerList } from '@/api/member/index'
	import { formatDateTime } from '@/utils/consultant'

	export default {
		data() {
			return {
				loading: false,
				customers: [],
				pageNum: 1,
				pageSize: 10,
				total: 0,
				loadStatus: 'loadmore'
			}
		},
		onLoad() {
			this.loadCustomers(true)
		},
		onPullDownRefresh() {
			this.loadCustomers(true).finally(() => {
				uni.stopPullDownRefresh()
			})
		},
		onReachBottom() {
			if (this.loadStatus === 'loadmore') {
				this.loadCustomers(false)
			}
		},
		methods: {
			customerAvatarText(name) {
				return (name || '客').slice(0, 1)
			},
			async loadCustomers(refresh) {
				if (refresh) {
					this.pageNum = 1
					this.loadStatus = 'loadmore'
				} else if (this.loadStatus !== 'loadmore') {
					return
				}
				this.loading = true
				this.loadStatus = 'loading'
				try {
					const res = await getCustomerList({
						pageNum: this.pageNum,
						pageSize: this.pageSize
					})
					if (res.code !== 200) {
						uni.showToast({
							title: res.msg || '加载客户失败',
							icon: 'none'
						})
						if (refresh) this.customers = []
						this.loadStatus = 'nomore'
						return
					}
					const list = res.rows || []
					this.total = res.total || 0
					const mapped = list.map(item => ({
						id: item.customerId,
						name: item.customerName || '',
						phone: item.linkMobile || '',
						customerNo: item.customerNo || '',
						signTime: formatDateTime(item.signTime || item.createTime)
					}))
					this.customers = refresh ? mapped : this.customers.concat(mapped)
					const hasMore = this.customers.length < this.total
					this.loadStatus = hasMore ? 'loadmore' : 'nomore'
					if (hasMore) this.pageNum += 1
				} catch (e) {
					if (refresh) this.customers = []
					this.loadStatus = 'nomore'
					uni.showToast({
						title: e.message || '加载失败',
						icon: 'none'
					})
				} finally {
					this.loading = false
				}
			},
			callMobile(item) {
				if (!item.phone) {
					uni.showToast({ title: '暂无联系电话', icon: 'none' })
					return
				}
				wx.makePhoneCall({ phoneNumber: item.phone })
			}
		}
	}
</script>
