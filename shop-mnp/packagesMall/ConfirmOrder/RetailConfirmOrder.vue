<template>
	<view class="retail-confirm">
		<view class="card address" @click="chooseAddress">
			<view><text class="title">收货地址</text><u-icon name="arrow-right" color="#777" size="26" /></view>
			<template v-if="address">
				<text>{{ address.linkPerson }}　{{ address.linkMobile }}</text>
				<text>{{ address.provinceName }}{{ address.cityName }}{{ address.countyName }}{{ address.streetName || '' }}{{ address.addressDetail }}</text>
			</template>
			<text v-else class="muted">请选择收货地址</text>
		</view>
		<view v-if="error" class="card error" @click="refreshQuote">{{ error }}　点击重试</view>
		<view v-if="quote" class="card">
			<text class="title">{{ quote.supplierName }} · 商品清单</text>
			<view v-for="(item, index) in quote.items" :key="index" class="line">
				<image :src="mediaUrl(item.goodsCover)" mode="aspectFill" />
				<view class="line-body"><text class="name">{{ item.goodsName }}</text><text class="muted">{{ item.specifications }}</text>
					<view class="row"><text class="price">￥{{ money(item.price) }}</text><text>× {{ item.count }}</text></view>
				</view>
			</view>
			<view v-if="selection.goodsId" class="row"><text>购买数量</text><u-number-box v-model="selection.count" :min="1" :max="9999" @change="refreshQuote" /></view>
			<view class="row"><text>配送方式</text><text>快递配送</text></view>
			<view class="remark"><text>订单留言</text><u-input v-model="remark" type="textarea" :maxlength="500" placeholder="选填，请填写配送要求" /></view>
		</view>
		<view v-if="quote" class="card totals">
			<picker :range="couponOptions" range-key="label" @change="chooseCoupon"><view class="row"><text>优惠券</text><text>{{ couponLabel }} ›</text></view></picker>
			<view class="row"><text>商品金额</text><text>￥{{ money(quote.moneyTotal) }}</text></view>
			<view class="row"><text>运费</text><text>￥{{ money(quote.moneyExpress) }}</text></view>
			<view class="row"><text>优惠</text><text class="price">−￥{{ money(quote.moneyDiscount) }}</text></view>
			<text class="muted">金额按后台当前售价、商品运费及可用优惠计算</text>
		</view>
		<view class="footer"><view><text class="muted">合计</text><text class="total">￥{{ quote ? money(quote.moneyPayable) : '—' }}</text></view>
			<button :disabled="!quote || loading || submitting || !!error" @click="submit">{{ submitting ? '提交中…' : '提交订单' }}</button>
		</view>
	</view>
</template>
<script>
import { getAddressList, getAddressInfo, quoteRetailOrder, submitRetailOrder } from '@/api/member/index'
export default {
	data() { return { selection: {}, quote: null, address: null, remark: '', couponGotId: null, loading: false, submitting: false, error: '', checkoutKey: '', quoteSequence: 0 } },
	computed: {
		couponOptions() { return [{id: null, label: '自动使用渠道优惠'}, {id: 0, label: '不使用优惠券'}, ...((this.quote && this.quote.coupons) || []).map(c => ({id: c.gotId, label: `${c.name}（减${this.money(c.discount)}元）`}))] },
		couponLabel() { if (!this.quote || !this.quote.couponGotId) return '未使用'; const c = this.quote.coupons.find(c => c.gotId === this.quote.couponGotId); return c ? c.name : '已使用' }
	},
	onLoad(options) {
		this.selection = options.cartIds ? {cartIds: options.cartIds.split(',').map(Number)} : {goodsId: Number(options.id), count: Number(options.count) || 1}
		this.newKey(); this.refreshQuote()
	},
	onShow() { this.loadAddress() },
	methods: {
		money(value) { return Number(value || 0).toFixed(2) },
		mediaUrl(path) { return /^https?:\/\//.test(path || '') ? path : this.$host + path },
		newKey() { this.checkoutKey = `retail-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}` },
		async loadAddress() { if (this.address) return; try { const res = await getAddressList(); this.address = (res.rows || []).find(a => a.isDefault == 1) || null } catch (e) { this.toast(e.message || '地址加载失败') } },
		async getAddressDetail(id) { try { const res = await getAddressInfo(id); this.address = res.data; this.newKey() } catch (e) { this.toast(e.message || '地址加载失败') } },
		chooseAddress() { uni.navigateTo({url: '/packagesPublic/AddressList/AddressList?type=creatOrder'}) },
		chooseCoupon(event) { this.couponGotId = this.couponOptions[Number(event.detail.value)].id; this.refreshQuote() },
		async refreshQuote() {
			const sequence = ++this.quoteSequence; this.loading = true; this.error = ''
			try {
				const res = await quoteRetailOrder({...this.selection, couponGotId: this.couponGotId})
				if (sequence !== this.quoteSequence) return
				if (this.quote && this.quote.fingerprint !== res.data.fingerprint) this.newKey()
				this.quote = res.data
			} catch (e) { if (sequence === this.quoteSequence) this.error = e.message || '结算加载失败' }
			finally { if (sequence === this.quoteSequence) this.loading = false }
		},
		async submit() {
			if (this.submitting || this.loading || !this.quote || this.error) return
			if (!this.address) return this.toast('请选择收货地址')
			this.submitting = true
			try {
				const res = await submitRetailOrder({...this.selection, addressId: this.address.addressId, couponGotId: this.couponGotId, remark: this.remark.trim(), checkoutKey: this.checkoutKey, fingerprint: this.quote.fingerprint})
				const order = res.data
				const url = order.payStatus === '1' ? `/packagesMall/OrderDetails/OrderDetails?orderId=${order.orderId}` : `/packagesMall/CashierDesk/CashierDesk?orderId=${order.orderId}&orderNo=${order.orderNo}&orderAmount=${order.moneyPayable}`
				uni.redirectTo({url})
			} catch (e) { this.toast(e.message || '提交失败，请重试') }
			finally { this.submitting = false }
		},
		toast(title) { uni.showToast({title, icon: 'none'}) }
	}
}
</script>
<style scoped lang="scss">
.retail-confirm { min-height: 100vh; background: #f7f7f5; padding: 24rpx 24rpx 156rpx; color: #333; }
.card { margin-bottom: 22rpx; padding: 28rpx; background: #fff; border: 1rpx solid #e8e1d8; border-radius: 20rpx; }
.title { display: block; font-size: 32rpx; font-weight: 700; color: #111; }
.address > view, .row { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.address > text { display: block; margin-top: 14rpx; line-height: 1.5; font-size: 28rpx; }
.muted { font-size: 24rpx; color: #777; line-height: 1.5; }
.line { display: flex; gap: 22rpx; padding: 26rpx 0; border-bottom: 1rpx solid #eee; }
.line image { width: 150rpx; height: 150rpx; border-radius: 12rpx; flex-shrink: 0; }
.line-body { flex: 1; min-width: 0; }.line-body > text { display: block; }.name { font-size: 30rpx; margin-bottom: 8rpx; }
.row { padding: 16rpx 0; font-size: 28rpx; }.price { color: #701018; font-weight: 700; }.remark { padding-top: 20rpx; font-size: 28rpx; }
.footer { position: fixed; bottom: 0; left: 0; right: 0; background: #fff; border-top: 1rpx solid #e8e1d8; padding: 18rpx 28rpx 28rpx; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; }
.total { font-size: 38rpx; font-weight: 700; color: #701018; margin-left: 12rpx; }.footer button { margin: 0; background: #701018; color: #fff; font-size: 30rpx; border-radius: 12rpx; min-width: 230rpx; }.footer button[disabled] { background: #ccc; }.error { color: #701018; }
</style>
