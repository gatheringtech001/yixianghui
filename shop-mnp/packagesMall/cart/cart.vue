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
					<view class="radio" :class="{ checked: selectedId === item.cartId }"></view>
				</view>
				<image class="cover" :src="mediaUrl(item.goodsInfo && item.goodsInfo.goodsCover)" mode="aspectFill" @click="openGoods(item)" />
				<view class="body">
					<text class="name" @click="openGoods(item)">{{ item.goodsInfo ? item.goodsInfo.goodsName : '商品已失效' }}</text>
					<text v-if="item.dataValues" class="spec">{{ item.dataValues }}</text>
					<text v-if="!isAvailable(item)" class="unavailable">商品已下架或库存不足</text>
					<view class="row">
						<text class="price">￥{{ displayPrice(item) }}</text>
						<view class="counter">
							<button @click="changeCount(item, -1)">−</button>
							<text>{{ item.goodsCount }}</text>
							<button @click="changeCount(item, 1)">＋</button>
						</view>
					</view>
					<text class="delete" @click="removeItem(item)">删除</text>
				</view>
			</view>
		</view>
		<view v-if="items.length" class="footer">
			<view>
				<text class="hint">每次结算 1 件商品</text>
				<text class="total">合计 ￥{{ selectedTotal }}</text>
			</view>
			<button :disabled="!selectedItem" @click="checkout">去结算</button>
		</view>
	</view>
</template>

<script>
	import { clearCart, deleteCart, getCartList, updateCart } from '@/api/member/index'

	export default {
		data() {
			return { host: this.$host, items: [], loading: false, selectedId: null }
		},
		computed: {
			selectedItem() {
				return this.items.find(item => item.cartId === this.selectedId && this.isAvailable(item)) || null
			},
			selectedTotal() {
				if (!this.selectedItem) return '0.00'
				return (Number(this.displayPrice(this.selectedItem)) * this.selectedItem.goodsCount).toFixed(2)
			}
		},
		onShow() { this.loadCart() },
		methods: {
			async loadCart() {
				this.loading = true
				try {
					const result = await getCartList({ pageNum: 1, pageSize: 200 })
					this.items = result.rows || []
					if (!this.selectedItem) this.selectedId = null
				} finally { this.loading = false }
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
				return Number(goods.vipPrice || goods.price || 0).toFixed(2)
			},
			selectItem(item) {
				if (!this.isAvailable(item)) return
				this.selectedId = this.selectedId === item.cartId ? null : item.cartId
			},
			async changeCount(item, delta) {
				const next = Number(item.goodsCount) + delta
				if (next < 1) return this.removeItem(item)
				try {
					await updateCart({ cartId: item.cartId, goodsCount: next })
					item.goodsCount = next
				} catch (error) { this.toast(error.message || '数量修改失败') }
			},
			removeItem(item) {
				uni.showModal({ title: '删除商品', content: '确认从购物车移除？', success: async result => {
					if (!result.confirm) return
					await deleteCart(item.cartId)
					this.items = this.items.filter(row => row.cartId !== item.cartId)
					if (this.selectedId === item.cartId) this.selectedId = null
				} })
			},
			confirmClear() {
				uni.showModal({ title: '清空购物车', content: '确认移除全部商品？', success: async result => {
					if (!result.confirm) return
					await clearCart()
					this.items = []
					this.selectedId = null
				} })
			},
			checkout() {
				if (!this.selectedItem) return
				const item = this.selectedItem
				uni.navigateTo({ url: `/packagesMall/ConfirmOrder/ConfirmOrder?id=${item.goodsId}&dataId=${item.dataId || 0}&count=${item.goodsCount}&cartId=${item.cartId}` })
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
