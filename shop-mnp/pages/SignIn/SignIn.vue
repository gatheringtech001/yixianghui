<template>
	<view class="page">
		<u-navbar
			class="weapp-nav-box coins-nav"
			:is-back="false"
			title="签到领金币"
			title-color="#111111"
			:title-size="40"
			:title-bold="true"
			:title-width="420"
			:background="{ background: '#ffffff' }"
			:border-bottom="false"
		/>

		<scroll-view class="coins-scroll" scroll-y :show-scrollbar="false">
			<view class="coin-summary">
				<image class="coin-summary-bg" src="/static/home-design/coin-hero-bg.jpg" mode="aspectFill" />
				<view class="coin-summary-body">
					<text class="coin-label">我的金币</text>
					<view class="coin-number">
						<text>{{ (userData && userData.golden) || 0 }}</text>
						<image src="/static/home-design/coin-token.png" mode="aspectFit" />
						<text class="coin-unit">金币</text>
					</view>
					<text class="coin-rate"><text class="accent">100金币</text> = 1元钱</text>
				</view>
			</view>

			<view class="signin">
				<view class="signin-title">
					已连续签到<text class="accent">{{ signDays }}</text>天
				</view>
				<view class="days">
					<view
						class="day"
						v-for="(reward, index) in signDayRewards"
						:key="index"
						:class="{ active: index + 1 <= signDays }"
					>
						<text class="day-label">第{{ index + 1 }}天</text>
						<image src="/static/home-design/coin-token.png" mode="aspectFit" />
						<text class="day-reward">金币+{{ reward }}</text>
					</view>
				</view>
				<view class="tomorrow">
					<u-icon name="calendar" color="#701018" size="28" />
					<text>明日签到可得 <text class="accent">{{ tomorrowReward }}</text> 金币</text>
				</view>
				<!-- canSign=true 表示今天还能签 -->
				<view class="sign-btn" :class="{ disabled: !canSign }" @click="onSign">
					{{ canSign ? '立即签到' : '今日已签到' }}
				</view>
			</view>

			<view class="task-title">做任务 赚金币</view>
			<view class="task-list">
				<view class="task">
					<view class="task-icon invite">
						<image src="/static/home-design/task-invite.png" mode="aspectFit" />
					</view>
					<view class="task-copy">
						<view class="task-name">邀请新用户</view>
						<view class="task-desc">邀请新用户个人注册 赚取10金币</view>
					</view>
					<button class="task-action" open-type="share" hover-class="none">去邀请</button>
				</view>
				<view class="task">
					<view class="task-icon order">
						<image src="/static/home-design/task-order.png" mode="aspectFit" />
					</view>
					<view class="task-copy">
						<view class="task-name">下单送积分</view>
						<view class="task-desc">预计得金币 下单实付1元奖励1金币</view>
					</view>
					<view class="task-action" @click="goServicePage">去下单</view>
				</view>
			</view>
		</scroll-view>

		<view class="sigin-hint">
			<view class="cu-modal" :class="{ show: showSign }">
				<view class="cu-dialog">
					<view class="cu-bar bg-white justify-end">
						<view class="content">签到成功</view>
						<view class="action">
							<text class="cuIcon-close" @click="closeSign"></text>
						</view>
					</view>
					<view class="sigin-content">
						<view class="icon">
							<text class="iconfont icon-signin"></text>
						</view>
						<view class="title">
							<view>恭喜您，连续签到第<text>{{ signDays }}</text>天，获得<text>{{ lastSignGold }}</text>金币</view>
						</view>
						<view class="btn" @click="closeSign">
							<text>我知道了</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<TabBar :tabBarShow="2"></TabBar>
		<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>

<script>
	import TabBar from '@/components/TabBar/TabBar.vue'
	import { parseInvitePageOptions } from '@/utils/invite'
	import sharePageMixin from '@/utils/sharePageMixin'
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	import { runWithAuth, bindPageAuthPopup } from '@/utils/login'
	import { signIn, signInList, signInRule } from '@/api/signIn/index'
	import { getInfo } from '@/api/public'
	export default {
		mixins: [sharePageMixin],
		components: {
			TabBar,
			AuthProfilePopup,
		},
		data() {
			return {
				userData: {},
				canSign: true,
				showSign: false,
				list: [],
				signDays: 0,
				goldRule: [],
				lastSignGold: 0
			}
		},
		computed: {
			signDayRewards() {
				if (this.goldRule && this.goldRule.length) return this.goldRule
				return [1, 2, 3, 5, 6, 8, 10]
			},
			tomorrowReward() {
				const rewards = this.signDayRewards
				if (!rewards.length) return 1
				// 明日奖励：未签时=今天签完后再下一天；已签时=当前连续天数的下一天（第7天封顶）
				const nextDayIndex = this.canSign
					? Math.min(this.signDays + 1, rewards.length - 1)
					: Math.min(this.signDays, rewards.length - 1)
				return rewards[nextDayIndex]
			}
		},
		onLoad(options) {
			parseInvitePageOptions(options)
		},
		onReady() {
			uni.hideTabBar()
		},
		onShow() {
			bindPageAuthPopup(this)
			let userInfo = uni.getStorageSync('userInfo')
			if (!userInfo || userInfo == '' || userInfo == undefined) return
			this.getSignInList()
			this.getGoldRule()
			this.userData = uni.getStorageSync('userData') || {}
		},
		methods: {
			getShareConfig() {
				return {
					title: '邀请新用户入住，奖励10金币',
					path: '/pages/home/home'
				}
			},
			goToArticle() {
				uni.navigateTo({
					url: '/packagesPublic/Article/index?id=3'
				})
			},
			async getGoldRule() {
				let { data } = await signInRule()
				this.goldRule = Array.isArray(data) ? data : []
			},
			formatDateYmd(date) {
				const year = date.getFullYear()
				const month = String(date.getMonth() + 1).padStart(2, '0')
				const day = String(date.getDate()).padStart(2, '0')
				return `${year}-${month}-${day}`
			},
			/** 统一签到业务日为 yyyy-MM-dd；兼容 tradeData=yyyyMMdd / createTime */
			normalizeSignDate(item) {
				if (!item) return ''
				const tradeData = item.tradeData != null ? String(item.tradeData) : ''
				if (/^\d{8}$/.test(tradeData)) {
					return `${tradeData.slice(0, 4)}-${tradeData.slice(4, 6)}-${tradeData.slice(6, 8)}`
				}
				if (tradeData && /^\d{4}-\d{2}-\d{2}/.test(tradeData)) {
					return tradeData.slice(0, 10)
				}
				if (item.createTime) {
					return String(item.createTime).split(' ')[0]
				}
				return ''
			},
			shiftDateYmd(baseYmd, offsetDays) {
				const parts = baseYmd.split('-').map(Number)
				const date = new Date(parts[0], parts[1] - 1, parts[2])
				date.setDate(date.getDate() + offsetDays)
				return this.formatDateYmd(date)
			},
			/**
			 * 从 startYmd 往前数连续有签天数（含 startYmd）
			 */
			countStreakFrom(signDateSet, startYmd) {
				let streak = 0
				let cursor = startYmd
				const max = Math.max(this.signDayRewards.length, 7)
				for (let i = 0; i < max; i++) {
					if (!signDateSet.has(cursor)) break
					streak++
					cursor = this.shiftDateYmd(cursor, -1)
				}
				return streak
			},
			async getSignInList() {
				const { data } = await signInList({ businessType: '签到' })
				const rows = Array.isArray(data) ? data : []
				this.list = rows
				const signDateSet = new Set()
				rows.forEach(v => {
					const d = this.normalizeSignDate(v)
					if (d) signDateSet.add(d)
				})
				const today = this.formatDateYmd(new Date())
				const signedToday = signDateSet.has(today)
				this.canSign = !signedToday
				if (signedToday) {
					this.signDays = this.countStreakFrom(signDateSet, today)
				} else {
					const yesterday = this.shiftDateYmd(today, -1)
					this.signDays = this.countStreakFrom(signDateSet, yesterday)
				}
			},
			onSign() {
				if (!this.canSign) {
					uni.showToast({ title: '今日已签到', icon: 'none' })
					return
				}
				runWithAuth(this, (ok) => {
					if (!ok) return
					signIn().then(async (res) => {
						const payload = (res && res.data) || {}
						const gold = Number(payload.gold)
						const days = Number(payload.signDays)
						this.lastSignGold = !isNaN(gold) && gold > 0 ? gold : (this.signDayRewards[this.signDays] || 1)
						if (!isNaN(days) && days > 0) {
							this.signDays = days
						} else {
							this.signDays = Math.min(this.signDays + 1, this.signDayRewards.length)
						}
						this.canSign = false
						this.showSign = true
						await this.refreshUserInfo()
						await this.getSignInList()
						await this.getGoldRule()
					}).catch((e) => {
						uni.showToast({ title: (e && e.message) || '签到失败', icon: 'none' })
					})
				})
			},
			closeSign() {
				this.showSign = false
				this.getSignInList()
			},
			async refreshUserInfo() {
				try {
					const res = await getInfo()
					uni.setStorageSync('consultant', res.consultant)
					uni.setStorageSync('userCard', res.userCard)
					uni.setStorageSync('userInfo', res.data)
					uni.setStorageSync('userData', res.userInfo)
					this.userData = res.userInfo || {}
				} catch (e) {
					console.warn('refreshUserInfo', e)
				}
			},
			goServicePage() {
				uni.switchTab({
					url: '/pages/classify/classify'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'SignIn.scss';
</style>
