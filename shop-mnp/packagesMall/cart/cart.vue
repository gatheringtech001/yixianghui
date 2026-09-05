<template>
	<view class="cart-page">
		<view class="cart-head">
			<text class="title">购物车</text>
			<text v-if="items.length" class="clear" @click="confirmClear">清空</text>
		</view>
		<view v-if="loading" class="state">加载中...</view>
		<view v-else-if="!items.length" class="state empty">
			<text>购物车还是空的</text>
			<button @click="goShopping">去逛云南好物</button>
		</view>
		<view v-else class="cart-list">
			<view v-for="item in items" :key="item.cartId" class="cart-item" :class="{ disabled: !isAvailable(item) }">
				<view class="selector" @click="selectItem(item)">
					<view class="radio" :class="{ checked: selectedIds.includes(item.cartId) }"></view>
				</view>
				<image class="cover" :src="mediaUrl(item.goodsInfo && item.goodsInfo.goodsCover)" mode="aspectFill" @click="openGoods(item)" />
				<view class="body">
					<text class="name" @click="openGoods(item)">{{ item.goodsInfo ? item.goodsInfo.goodsName : '商品已失效' }}</text>
					<text v-if="item.dataValues" class="spec">{{ item.dataValues }}</text>
					<text v-if="!isAvailable(item)" class="unavailable">商品已下架或库存不足</text>
					<view class="row">
						<text class="price">￥{{ displayPrice(item) }}</text>
						<view class="counter">
							<button :disabled="item.goodsCount <= 1 || updating.includes(item.cartId)" @click="changeCount(item, -1)">−</button>
							<text>{{ item.goodsCount }}</text>
							<button :disabled="updating.includes(item.cartId) || !item.goodsInfo || item.goodsCount >= item.goodsInfo.stock" @click="changeCount(item, 1)">＋</button>
						</view>
					</view>
					<text class="delete" @click="removeItem(item)">删除</text>
				</view>
			</view>
		</view>
		<view v-if="items.length" class="footer">
			<view class="select-all" @click="selectAll"><view class="radio" :class="{checked: allSelected}"></view><text>全选</text></view>
			<view>
				<text class="hint">运费优惠结算时确认</text>
				<text class="total">合计 ￥{{ selectedTotal }}</text>
			</view>
			<button :disabled="!selectedItems.length || updating.length > 0" @click="checkout">结算({{ selectedItems.length }})</button>
		</view>
	</view>
</template>

<script>
	import { clearCart, deleteCart, getCartList, updateCart } from '@/api/member/index'

	export default {
		data() {
			return { host: this.$host, items: [], loading: false, selectedIds: [], initialized: false, updating: [] }
		},
		computed: {
			selectedItems() {
				return this.items.filter(item => this.selectedIds.includes(item.cartId) && this.isAvailable(item))
			},
			allSelected() {
				const available = this.items.filter(this.isAvailable)
				return available.length > 0 && available.every(item => this.selectedIds.includes(item.cartId))
			},
			selectedTotal() {
				const cents = this.selectedItems.reduce((sum, item) => sum + Math.round(Number(this.displayPrice(item)) * 100) * item.goodsCount, 0)
				return (cents / 100).toFixed(2)
			}
		},
		onShow() { this.loadCart() },
		methods: {
			async loadCart() {
				this.loading = true
				try {
					const result = await getCartList({ pageNum: 1, pageSize: 200 })
					this.items = result.rows || []
					const available = this.items.filter(this.isAvailable).map(item => item.cartId)
					this.selectedIds = this.initialized ? this.selectedIds.filter(id => available.includes(id)) : available
					this.initialized = true
				} catch (error) { this.toast(error.message || '购物车加载失败') }
				finally { this.loading = false }
			},
			isAvailable(item) {
				const goods = item && item.goodsInfo
				return !!goods && goods.status === '1' && goods.goodsType === 'online'
					&& Number(goods.stock) >= Number(item.goodsCount)
			},
			mediaUrl(path) {
				if (!path) return '/static/image/car.png'
				return /^https?:\/\//.test(path) ? path : this.host + path
			},
			displayPrice(item) {
				const goods = item.goodsInfo || {}
				return Number(goods.price || 0).toFixed(2)
			},
			selectItem(item) {
				if (!this.isAvailable(item)) return
				this.selectedIds = this.selectedIds.includes(item.cartId) ? this.selectedIds.filter(id => id !== item.cartId) : [...this.selectedIds, item.cartId]
			},
			selectAll() {
				this.selectedIds = this.allSelected ? [] : this.items.filter(this.isAvailable).map(item => item.cartId)
			},
			async changeCount(item, delta) {
				const next = Number(item.goodsCount) + delta
				if (next < 1 || this.updating.includes(item.cartId)) return
				this.updating.push(item.cartId)
				try {
					await updateCart({ cartId: item.cartId, goodsCount: next })
					item.goodsCount = next
				} catch (error) { this.toast(error.message || '数量修改失败') }
				finally { this.updating = this.updating.filter(id => id !== item.cartId) }
			},
			removeItem(item) {
				uni.showModal({ title: '删除商品', content: '确认从购物车移除？', success: async result => {
					if (!result.confirm) return
					try {
						await deleteCart(item.cartId)
						this.items = this.items.filter(row => row.cartId !== item.cartId)
						this.selectedIds = this.selectedIds.filter(id => id !== item.cartId)
					} catch (error) { this.toast(error.message || '删除失败') }
				} })
			},
			confirmClear() {
				uni.showModal({ title: '清空购物车', content: '确认移除全部商品？', success: async result => {
					if (!result.confirm) return
					try {
						await clearCart()
						this.items = []
						this.selectedIds = []
					} catch (error) { this.toast(error.message || '清空失败') }
				} })
			},
			checkout() {
				if (!this.selectedItems.length || this.updating.length) return
				const ids = this.selectedItems.map(item => item.cartId).join(',')
				uni.navigateTo({ url: `/packagesMall/ConfirmOrder/RetailConfirmOrder?cartIds=${ids}` })
			},
			openGoods(item) {
				if (item.goodsInfo) uni.navigateTo({ url: `/packagesMall/GoodsDetails/GoodsDetails?id=${item.goodsId}` })
			},
			goShopping() {
				uni.setStorageSync('currentClsName', '云南好物')
				uni.switchTab({ url: '/pages/classify/classify' })
			},
			toast(title) { uni.showToast({ title, icon: 'none' }) }
		}
	}
</script>

<style scoped lang="scss">
	@import 'cart.scss';
</style>
