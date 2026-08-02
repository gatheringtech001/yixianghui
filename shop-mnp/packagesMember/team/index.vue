<template>
	<view class="container">
		<view class="page-header" v-if="total > 0">
			<text class="header-title">共邀请 {{ total }} 人</text>
		</view>
		<view class="customer-list" v-if="customers.length > 0">
			<view class="customer-card" v-for="customer in customers" :key="customer.id">
				<u-avatar v-if="customer.src" :src="customer.src" size="large"></u-avatar>
				<u-avatar v-else :text="avatarText(customer.name)" bg-color="#EFEBDF" size="large"></u-avatar>
				<view class="info">
					<text class="customer-name">{{ customer.name || '微信用户' }}</text>
					<text class="customer-phone">{{ customer.phone || '未绑定手机' }}</text>
					<text class="customer-time" v-if="customer.joinTime">加入时间 {{ customer.joinTime }}</text>
				</view>
				<view class="status">
					<text class="status-tag joined">已加入</text>
				</view>
			</view>
		</view>
		<view class="load-more" v-if="customers.length > 0">
			<u-loadmore :status="loadStatus" />
		</view>
		<view class="empty" v-else-if="!loading">
			<u-empty text="暂无邀请记录" mode="list"></u-empty>
		</view>
		<view class="loading-wrap" v-if="loading && customers.length === 0">
			<u-loading mode="circle" color="#FF7D00"></u-loading>
		</view>
	</view>
</template>

<script>
	import './index.scss'
	import { getConsultantChildren } from '@/api/member/index'
	import { normalizeImageUrl, formatDateTimeFull } from '@/utils/consultant'

	export default {
		data() {
			return {
				host: this.$host,
				loading: false,
				customers: [],
				pageNum: 1,
				pageSize: 10,
				total: 0,
				loadStatus: 'loadmore'
			}
		},
		onLoad() {
			this.loadInviteList(true)
		},
		onPullDownRefresh() {
			this.loadInviteList(true).finally(() => {
				uni.stopPullDownRefresh()
			})
		},
		onReachBottom() {
			if (this.loadStatus === 'loadmore') {
				this.loadInviteList(false)
			}
		},
		methods: {
			avatarText(name) {
				return (name || '客').slice(0, 1)
			},
			async loadInviteList(refresh) {
				if (refresh) {
					this.pageNum = 1
					this.loadStatus = 'loadmore'
				} else if (this.loadStatus !== 'loadmore') {
					return
				}
				this.loading = true
				this.loadStatus = 'loading'
				try {
					const res = await getConsultantChildren({
						pageNum: this.pageNum,
						pageSize: this.pageSize
					})
					if (res.code !== 200) {
						uni.showToast({
							title: res.msg || '加载邀请列表失败',
							icon: 'none'
						})
						if (refresh) this.customers = []
						this.loadStatus = 'nomore'
						return
					}
					const list = res.rows || []
					this.total = res.total || 0
					const mapped = list.map(item => ({
						id: item.userId,
						name: item.nickName || '',
						phone: item.phonenumber || '',
						src: normalizeImageUrl(this.host, item.avatar),
						joinTime: formatDateTimeFull(item.createTime)
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
			}
		}
	}
</script>
