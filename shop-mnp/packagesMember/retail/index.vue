<template>
	<view class="page">
		<view class="hero">
			<view class="hero-bg"></view>
			<view class="profile">
				<image class="avatar" :src="avatar" mode="aspectFill"></image>
				<view class="profile-info">
					<view class="name">{{ userName }}</view>
					<view class="meta" v-if="consultantNo">顾问编号 {{ consultantNo }}</view>
					<view class="meta" v-if="mobile">{{ maskPhone(mobile) }}</view>
				</view>
				<view class="badge">康养顾问</view>
			</view>
		</view>

		<view class="stats-card" v-if="!loading">
			<view class="stats-header">
				<text class="stats-title">收益概览</text>
				<view class="stats-link" @click="goIncome">
					<text>收支明细</text>
					<u-icon name="arrow-right" color="#999" size="22"></u-icon>
				</view>
			</view>
			<view class="income-main" @click="goIncome">
				<view class="income-label">累计收益（元）</view>
				<view class="income-value">{{ formatMoney(stat.totalIncome) }}</view>
				<view class="income-tip">推广订单产生的收益总和</view>
			</view>
			<view class="income-sub">
				<view class="income-sub-item" @click="goIncome">
					<view class="sub-label">待结算</view>
					<view class="sub-value pending">{{ formatMoney(stat.pendingAmount) }}</view>
					<view class="sub-tip">尚未到账</view>
				</view>
				<view class="income-sub-divider"></view>
				<view class="income-sub-item" @click="goCash">
					<view class="sub-label">已提现</view>
					<view class="sub-value">{{ formatMoney(stat.withdrawnAmount) }}</view>
					<view class="sub-tip">已成功到账</view>
				</view>
			</view>
			<view class="promo-row">
				<view class="promo-item" @click="goCustomer">
					<view class="promo-value">{{ stat.customerCount || 0 }}</view>
					<view class="promo-label">我的客户（人）</view>
				</view>
				<view class="promo-divider"></view>
				<view class="promo-item" @click="goTeam">
					<view class="promo-value">{{ stat.inviteCount || 0 }}</view>
					<view class="promo-label">我的邀请（人）</view>
				</view>
			</view>
		</view>
		<view class="stats-card loading-card" v-else>
			<u-loading mode="circle" color="#FF7D00"></u-loading>
			<text class="loading-text">加载数据中...</text>
		</view>

		<view class="section">
			<view class="section-title">快捷功能</view>
			<view class="action-grid">
				<view class="action-item" @click="goTeam">
					<view class="action-icon team">
						<u-icon name="account-fill" color="#fff" size="40"></u-icon>
					</view>
					<text class="action-label">我的邀请</text>
				</view>
				<view class="action-item" @click="goCustomer">
					<view class="action-icon customer">
						<u-icon name="man-add-fill" color="#fff" size="40"></u-icon>
					</view>
					<text class="action-label">我的客户</text>
				</view>
				<view class="action-item" @click="goIncome">
					<view class="action-icon income">
						<u-icon name="red-packet-fill" color="#fff" size="40"></u-icon>
					</view>
					<text class="action-label">收支明细</text>
				</view>
				<view class="action-item" @click="goCash">
					<view class="action-icon cash">
						<u-icon name="rmb-circle-fill" color="#fff" size="40"></u-icon>
					</view>
					<text class="action-label">提现记录</text>
				</view>
			</view>
		</view>

		<view class="invite-banner" @click="goShare">
			<view class="invite-left">
				<view class="invite-title">邀请好友加入</view>
				<view class="invite-desc">分享邀请码，好友下单您可获得奖励</view>
				<view class="invite-count">已推广 {{ stat.inviteCount || 0 }} 人</view>
			</view>
			<view class="invite-btn">
				<text>去邀请</text>
				<u-icon name="arrow-right" color="#FF7D00" size="24"></u-icon>
			</view>
		</view>
	</view>
</template>

<script>
	import './index.scss'
	import { getInfo } from '@/api/public'
	import { getConsultantStat } from '@/api/member/index'
	import { formatMoney, maskPhone, normalizeImageUrl } from '@/utils/consultant'
	import { syncConsultantStorage } from '@/utils/login'

	export default {
		data() {
			return {
				host: this.$host,
				loading: false,
				userName: '用户名称',
				avatar: '/static/img/user_pic.jpg',
				consultantNo: '',
				mobile: '',
				stat: {
					withdrawnAmount: 0,
					pendingAmount: 0,
					totalIncome: 0,
					customerCount: 0,
					inviteCount: 0
				}
			}
		},
		onShow() {
			this.loadPageData()
		},
		onPullDownRefresh() {
			this.loadPageData().finally(() => {
				uni.stopPullDownRefresh()
			})
		},
		methods: {
			formatMoney,
			maskPhone,
			async loadPageData() {
				this.loading = true
				try {
					await Promise.all([this.loadUserInfo(), this.loadStat()])
				} finally {
					this.loading = false
				}
			},
			async loadUserInfo() {
				try {
					const res = await getInfo()
					const user = res.data || {}
					syncConsultantStorage(res.consultant)
					const consultant = res.consultant
					this.userName = consultant?.consultantName || user.nickName || '用户名称'
					this.consultantNo = consultant?.consultantNo || ''
					this.mobile = consultant?.mobile || user.phonenumber || ''
					this.avatar = normalizeImageUrl(this.host, user.avatar) || '/static/img/user_pic.jpg'
				} catch (e) {
					const userInfo = uni.getStorageSync('userInfo')
					const consultant = uni.getStorageSync('consultant')
					if (userInfo) {
						this.userName = consultant?.consultantName || userInfo.nickName || '用户名称'
						this.consultantNo = consultant?.consultantNo || ''
						this.mobile = consultant?.mobile || userInfo.phonenumber || ''
						this.avatar = normalizeImageUrl(this.host, userInfo.avatar) || '/static/img/user_pic.jpg'
					}
				}
			},
			async loadStat() {
				try {
					const res = await getConsultantStat()
					if (res.code === 200 && res.data) {
						this.stat = {
							withdrawnAmount: res.data.withdrawnAmount || 0,
							pendingAmount: res.data.pendingAmount || 0,
							totalIncome: res.data.totalIncome || 0,
							customerCount: res.data.customerCount || 0,
							inviteCount: res.data.inviteCount || 0
						}
					}
				} catch (e) {
					uni.showToast({
						title: e.message || '加载统计数据失败',
						icon: 'none'
					})
				}
			},
			goCustomer() {
				uni.navigateTo({ url: '/packagesMember/customer/index' })
			},
			goTeam() {
				uni.navigateTo({ url: '/packagesMember/team/index' })
			},
			goIncome() {
				uni.navigateTo({ url: '/packagesMember/retail/income/index' })
			},
			goCash() {
				uni.navigateTo({ url: '/packagesMember/retail/cash/index' })
			},
			goShare() {
				uni.navigateTo({ url: './invite/index' })
			}
		}
	}
</script>
