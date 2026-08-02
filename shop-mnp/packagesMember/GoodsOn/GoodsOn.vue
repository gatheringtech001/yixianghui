<template>
	<view class="page">
		<u-navbar
			class="weapp-nav-box detail-nav"
			:is-back="true"
			title="我的收藏"
			title-color="#111111"
			:title-size="40"
			:title-bold="true"
			:title-width="400"
			:background="{ background: '#EFEBDF' }"
			:border-bottom="true"
			:custom-back="onNavBack"
		/>
		<scroll-view
			class="list-scroll"
			scroll-y
			:show-scrollbar="false"
			:style="{ height: scrollHeight + 'px' }"
		>
			<view v-if="displayList.length" class="card-list">
				<view
					class="collect-card"
					v-for="item in displayList"
					:key="item.collectId"
					@click.stop="goDetail(item)"
				>
					<image class="cover" :src="item.coverUrl" mode="aspectFill" />
					<view class="body">
						<view class="title-row">
							<text class="type-tag" v-if="item.typeLabel">{{ item.typeLabel }}</text>
							<text class="title">{{ item.title }}</text>
						</view>
						<view class="meta" v-if="item.meta">{{ item.meta }}</view>
						<view class="foot">
							<text class="price">{{ item.priceText }}</text>
							<view class="btn-cancel" @click.stop="cancelGoodsCollect(item)">取消收藏</view>
						</view>
					</view>
				</view>
			</view>
			<view class="empty" v-else-if="!loading">
				<u-empty text="暂无收藏，去服务页逛逛吧" mode="list"></u-empty>
				<view class="empty-action" @click.stop="goServicePage">去看看</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { goodsCollectList, deleteCollect } from '@/api/member/index'
	export default {
		data() {
			return {
				host: this.$host || '',
				rawList: [],
				loading: false,
				scrollHeight: 500
			};
		},
		computed: {
			displayList() {
				return (this.rawList || []).map(item => this.normalizeItem(item)).filter(Boolean)
			}
		},
		onReady() {
			this.calcScrollHeight()
		},
		onShow() {
			this.getCollects()
			this.$nextTick(() => this.calcScrollHeight())
		},
		methods: {
			calcScrollHeight() {
				this.$nextTick(() => {
					const query = uni.createSelectorQuery().in(this)
					query.select('.weapp-nav-box').boundingClientRect()
					query.exec((res) => {
						const nav = res && res[0]
						const windowInfo = uni.getWindowInfo ? uni.getWindowInfo() : uni.getSystemInfoSync()
						const windowHeight = Number(windowInfo.windowHeight) || 0
						const navBottom = nav && Number.isFinite(nav.bottom) ? nav.bottom : uni.upx2px(88)
						this.scrollHeight = Math.max(Math.floor(windowHeight - navBottom), 240)
					})
				})
			},
			onNavBack() {
				const pages = getCurrentPages()
				if (pages && pages.length > 1) {
					uni.navigateBack({ delta: 1 })
					return
				}
				uni.switchTab({ url: '/pages/my/my' })
			},
			async getCollects() {
				this.loading = true
				try {
					const data = await goodsCollectList({ pageNum: 1, pageSize: 200 })
					this.rawList = (data && data.rows) || []
				} catch (e) {
					this.rawList = []
					uni.showToast({
						title: (e && e.message) || '加载失败',
						icon: 'none'
					})
				} finally {
					this.loading = false
				}
			},
			isActivityCollect(item) {
				return !!(item && (item.collectType === 'activity' || (item.activityId && !item.goodsId)))
			},
			resolveMediaUrl(path) {
				if (!path) return ''
				let raw = String(path).trim()
				if (!raw) return ''
				if (raw.indexOf(',') > -1) {
					raw = raw.split(',')[0].trim()
				}
				if (!raw) return ''
				if (raw.startsWith('http://') || raw.startsWith('https://')) {
					return encodeURI(raw)
				}
				if (raw.startsWith('/static/')) {
					return raw
				}
				const base = String(this.host || '').replace(/\/$/, '')
				const rel = raw.startsWith('/') ? raw : `/${raw}`
				return encodeURI(base + rel)
			},
			normalizeItem(item) {
				if (!item || !item.collectId) return null
				if (this.isActivityCollect(item)) {
					const info = item.activityInfo || {}
					const title = info.activityName || '活动'
					const cover = this.resolveMediaUrl(info.activityCover)
					const isFree = info.isFree === 1 || info.isFree === '1' || info.isFree == null
					return {
						...item,
						kind: 'activity',
						typeLabel: '活动',
						title,
						meta: info.address || info.activityTime || '',
						priceText: isFree ? '免费' : `¥${info.vipPrice || info.price || 0}`,
						coverUrl: cover || '/static/home-design/entry-stay.jpg'
					}
				}
				const goods = item.goodsInfo || {}
				if (!item.goodsId && !goods.goodsId) return null
				const cover = this.resolveMediaUrl(
					goods.goodsCover || (goods.goodsImages && String(goods.goodsImages).split(',')[0]) || ''
				)
				const type = goods.goodsType
				let typeLabel = '商品'
				if (type === 'hotel') typeLabel = '旅居'
				else if (type === 'education') typeLabel = '课程'
				const price = goods.vipPrice != null && goods.vipPrice !== '' ? goods.vipPrice : goods.price
				return {
					...item,
					kind: 'goods',
					goodsType: type,
					typeLabel,
					title: goods.goodsName || '商品',
					meta: '',
					priceText: `¥${price != null ? price : 0}`,
					coverUrl: cover || '/static/home-design/entry-stay.jpg',
					vipPrice: goods.vipPrice
				}
			},
			goDetail(item) {
				if (!item) return
				if (item.kind === 'activity') {
					const id = item.activityId || (item.activityInfo && item.activityInfo.activityId)
					if (!id) return
					uni.navigateTo({
						url: `/packagesMall/Activity/detail/index?id=${id}`
					})
					return
				}
				const goodsId = item.goodsId || (item.goodsInfo && item.goodsInfo.goodsId)
				if (!goodsId) return
				if (item.goodsType === 'hotel') {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/SojournGoodsDetails?id=${goodsId}`
					})
				} else if (item.goodsType === 'education') {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/EducationGoodsDetails?id=${goodsId}`
					})
				} else {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/GoodsDetails?id=${goodsId}`
					})
				}
			},
			goServicePage() {
				// reLaunch 直接打开服务 Tab，避免先闪回「我的」
				uni.reLaunch({
					url: '/pages/classify/classify'
				})
			},
			cancelGoodsCollect(item) {
				if (!item || !item.collectId) return
				deleteCollect({ collectId: item.collectId }).then(() => {
					uni.showToast({
						title: '取消成功',
						icon: 'none'
					})
					this.getCollects()
				}).catch(err => {
					uni.showToast({
						title: (err && err.message) || '取消失败',
						icon: 'none'
					})
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'GoodsOn.scss';
</style>
