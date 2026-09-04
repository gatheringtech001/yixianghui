<template>
	<view class="page">
		<!-- 地址 -->
		<view class="address-data" v-if="goodsDetail && goodsDetail.goodsType == 'online'">
			<view class="address-list" v-if="address" @click="onSkip('address')">
				<view class="list">
					<text>{{address.provinceName}}{{address.cityName}}{{address.countyName}}{{address.streetName || ''}}</text>
				</view>
				<view class="list">
					<text class="address">{{address.addressDetail}}</text>
				</view>
				<view class="list">
					<text>{{address.linkPerson}}</text>
					<text>{{address.linkMobile}}</text>
				</view>
				<view class="list">
					<text class="tips">(如果快递不方便接收，您可以选择暂时寄存服务)</text>
				</view>
			</view>
			<view class="address-list" v-else @click="onSkip('address')">
				<view class="list">
					请选择您的收货地址！
				</view>
			</view>
			<view class="bar"></view>
		</view>
		<!-- 商品 -->
		<view class="goods-data">
			<view class="goods-title" v-if="goodsDetail && goodsDetail.goodsType == 'online'">
				<text>商品信息</text>
			</view>
			<view class="goods-list">
				<view class="list">
					<view class="thumb">
						<image :src="mediaUrl(goodsDetail.goodsCover)" mode="aspectFill"></image>
					</view>
					<view class="item">
						<view class="title">
							<text class="name one-omit">{{ goodsDetail.goodsName }}</text>
							<text class="attr" v-if="skuDataTitle">
								{{skuDataTitle}}
							</text>
						</view>
						<view class="price-number">
							<view class="price">
								<text class="min">￥</text>
								<text class="max">{{ goodsDetail.price }}</text>
								<!-- <text class="min">.00</text> -->
							</view>
							<view class="number">
								<u-number-box v-model="count" :step="1" :min="1" :max="goodsDetail.stock"></u-number-box>
							</view>
						</view>
						<!-- <view class="tag">
							<text>支持七天无理由退货</text>
						</view> -->
					</view>
				</view>
			</view>
			<view class="delivery">
				<div class="list" v-if="goodsDetail && goodsDetail.goodsType!='hotel'">
					<view class="title">配送</view>
					<view class="content">
						<text v-if="goodsDetail && goodsDetail.goodsType == 'online'">快递运输</text>
						<text v-else>无需配送</text>
						<!-- <text class="iconfont icon-more"></text> -->
					</view>
				</div>
				<!-- <div class="list">
					<view class="title">运费险</view>
					<view class="content">
						<text>￥10.00</text>
						<text class="iconfont icon-check"></text>
					</view>
				</div> -->
				<div class="list long">
					<view class="title">留言</view>
					<view class="content long">
						<u-input v-model="remark" type="textarea" border placeholder="选填,建议先和商家沟通确认" />
					</view>
				</div>
			</view>
		</view>
		<!--日历-->
		<view class="calendar-section" v-if="goodsDetail && goodsDetail.goodsType=='hotel'">
			<view class="calendar-header">
				<text class="section-title">选择入住和离店日期</text>
			</view>
			<Calendar ref="CalendarRef" :is-show="true" :isFixed="false" :endDate="calendarEndDate" :fixedEnd="true" :mode="2" themeColor="#C30D24"
				@callback="calendarCallBackFn" @calendarClick="calendarClickFn" />
		</view>
		<!-- 优惠 -->
		<view class="discounts-data" v-if="goodsDetail && goodsDetail.goodsType!='hotel'">
			<view class="discounts">
				<!-- <div class="list" @click="$refs['InvoiceInfo'].show()">
					<view class="title">发票</view>
					<view class="content">
						<text>不开发票</text>
						<text class="iconfont icon-more"></text>
					</view>
				</div> -->
				<div class="list" @click="chooseCoupon">
					<view class="title">优惠券</view>
					<view class="content">
						<text>无可用</text>
						<text class="iconfont icon-more"></text>
					</view>
				</div>
				<!-- <div class="list">
					<view class="title">金币</view>
					<view class="content">
						<text>共300，满1000可用</text>
					</view>
				</div> -->
			</view>
		</view>
		<!-- 订单金额 -->
		<view class="order-price" v-if="goodsDetail && goodsDetail.goodsType!='hotel'">
			<view class="price-list">
				<view class="list">
					<view class="title">
						<text>商品金额</text>
					</view>
					<view class="price">
						<text>￥{{getTotal()}}</text>
					</view>
				</view>
				<!-- <view class="list">
					<view class="title">
						<text>运费</text>
					</view>
					<view class="price">
						<text class="highlight">+￥0.00</text>
					</view>
				</view>
				<view class="list">
					<view class="title">
						<text>运费险</text>
					</view>
					<view class="price">
						<text class="highlight">+￥0.00</text>
					</view>
				</view> -->
			</view>
		</view>
		<!-- 地址提示 -->
		<view class="address-tips" :style="scrollTop >= 100 ? '':'display:none'"
			v-if="address && goodsDetail && goodsDetail.goodsType!='hotel'">
			<text>{{address.provinceName}}{{address.cityName}}{{address.countyName}}{{address.streetName || ''}}{{address.addressDetail}}</text>
		</view>
		<!-- 底部合计提交 -->
		<view class="footer-submit">
			<view class="price">
				<text class="min">￥</text>
				<text class="max">{{orderAmount}}</text>
			</view>
			<view class="submit" @click="onSubmit">
				<text>提交订单</text>
			</view>
		</view>
		<!-- 发票 -->
		<invoice-info ref="InvoiceInfo"></invoice-info>
		<!-- 优惠券 -->
		<use-coupon ref="UseCouponRef"></use-coupon>
	</view>
</template>

<script>
	import InvoiceInfo from '@/components/InvoiceInfo/InvoiceInfo.vue';
	import UseCoupon from '@/components/UseCoupon/UseCoupon.vue'
	// import uniCalendar from '@/uni_modules/uni-calendar/components/uni-calendar/uni-calendar.vue' // 引入日历组件
	import Calendar from '@/components/mobile-calendar-simple/Calendar.vue'
	import {
		getGoodsInfo,
		getGoodsSkuInfoById
	} from '@/api/shop/index'
	import {
		getAddressList,
		getAddressInfo,
		createOrder,
		deleteCart
	} from '@/api/member/index'
	export default {
		components: {
			// 发票
			InvoiceInfo,
			// 优惠券
			UseCoupon,
			// uniCalendar, // 注册日历组件
			Calendar
		},
		data() {
			return {
				scrollTop: 0,
				host: this.$host,
				address: null,
				goodsDetail: null,
				count: 1,
				orderAmount: 0,
				remark: '',
				checkInDate: '',
				checkOutDate: '',
				skuDataId: '', // sku数据ID
				calendarEndDate: '', // 日期组件结束日期
				fixedEndDay: 0, // 固定结束天数
				skuDataTitle: '', // sku标题
				cartId: null,
			};
		},
		onLoad(option) {
			/* if (option.dataId) {
				this.skuDataId = option.dataId
			} else {
				this.skuDataId = ''
			} */
			this.skuDataId = ''
			this.skuDataTitle = ''
			this.count = Math.max(1, Number(option.count) || 1)
			this.cartId = option.cartId ? Number(option.cartId) : null
			this.getGoodsDetail(option.id)
		},
		onPageScroll(e) {
			this.scrollTop = e.scrollTop;
		},
		onShow() {
			this.getAddress()
		},
		methods: {
			mediaUrl(path) {
				if (!path) return ''
				return /^https?:\/\//.test(path) ? path : this.host + path
			},
			async getAddress() {
				let {
					rows
				} = await getAddressList()
				this.address = rows.find(v => v.isDefault == 1)
			},
			// 获取地址详情
			async getAddressDetail(id) {
				let {
					data
				} = await getAddressInfo(id)
				this.address = data
			},
			// 获取商品详情
			async getGoodsDetail(id) {
				let {
					data
				} = await getGoodsInfo(id)
				if (data && data.goodsType === 'education') {
					uni.redirectTo({
						url: `/packagesMall/ConfirmOrder/EducationConfirmOrder?id=${id}`
					})
					return
				}
				if (data && data.goodsType === 'hotel') {
					const query = [`id=${id}`]
					if (this.checkInDate) query.push(`checkInDate=${this.checkInDate}`)
					if (this.checkOutDate) query.push(`checkOutDate=${this.checkOutDate}`)
					uni.redirectTo({
						url: `/packagesMall/ConfirmOrder/SojournConfirmOrder?${query.join('&')}`
					})
					return
				}
				this.goodsDetail = data
				this.goodsDetail.goodsImages = this.goodsDetail.goodsImages.split(',')
				// 如果传入SKUid则查询
				if (this.skuDataId) this.getGoodsSkuInfoByIdFn(this.skuDataId)
			},
			getTotal() {
				const total = Number(this.goodsDetail.price || 0) * this.count
				this.orderAmount = total
				return total.toFixed(2)
			},
			// 选择优惠券
			chooseCoupon() {
				this.$refs['UseCouponRef'].show(this.goodsDetail.goodsId)
			},
			/**
			 * 提交订单
			 */
			onSubmit() {
				this.$u.throttle(() => {
					if (this.goodsDetail.goodsType === 'online' && !this.address) {
						uni.showToast({ icon: 'none', title: '请先选择收货地址' })
						return
					}
					let params = {
						addressId: this.goodsDetail.goodsType == 'online' ? this.address.addressId : '0',
						goodsCount: this.count,
						goodsId: this.goodsDetail.goodsId,
						couponGotIds: '',
						remark: this.remark.trim(),
						checkInDate: this.checkInDate,
						checkOutDate: this.checkOutDate,
						skuDataId: this.skuDataId // SKU数据主键
					}
					createOrder(params).then(async res => {
						if (this.cartId) {
							try {
								await deleteCart(this.cartId)
							} catch (error) {
								console.warn('订单已创建，但购物车清理失败', error)
							}
						}
						uni.showToast({
							icon: 'none',
							title: '订单创建成功！'
						})
						setTimeout(() => {
							uni.redirectTo({
								url: `/packagesMall/CashierDesk/CashierDesk?orderAmount=${res.data.moneyPayable}&orderId=${res.data.orderId}&orderNo=${res.data.orderNo}`,
							})
						}, 1500)
					})
				}, 500)
			},
			formatDate(date) {
				const year = date.getFullYear();
				const month = String(date.getMonth() + 1).padStart(2, '0');
				const day = String(date.getDate()).padStart(2, '0');
				return `${year}-${month}-${day}`;
			},
			onSkip(type) {
				switch (type) {
					case 'address':
						uni.navigateTo({
							url: '/packagesPublic/AddressList/AddressList?type=creatOrder',
						})
						break;
				}
			},
			// 根据id查询sku信息
			getGoodsSkuInfoByIdFn(id) {
				getGoodsSkuInfoById(id).then(res => {
					if(res.code != 200) return
					if(res.data.remark) {
						this.fixedEndDay = parseInt(res.data.remark)
						const date = new Date();
						date.setDate(date.getDate() + parseInt(res.data.remark) - 1);
						this.calendarEndDate =  this.formatDate(date);
						this.checkInDate = this.formatDate(new Date())
						this.checkOutDate = this.formatDate(date)
					} else {
						// 如果没有维护则默认为两天
						this.fixedEndDay = 2
						const date = new Date();
						date.setDate(date.getDate() + 1);
						this.calendarEndDate =  this.formatDate(date);
						this.checkInDate = this.formatDate(new Date())
						this.checkOutDate = this.formatDate(date)
					}
					this.goodsDetail.price = res.data.dataPrice.toFixed(2)
					this.orderAmount = res.data.dataPrice.toFixed(2)
					this.skuDataTitle = res.data.dataValues
				}).catch(err => {
					console.log('getGoodsSkuInfoById', err)
				})
			},
			// 日期组件回调方法
			calendarCallBackFn(date) {
				this.checkInDate = date.startStr.dateStr
				this.checkOutDate = date.endStr.dateStr
			},
			// 日期组件点击时触发的方法s
			calendarClickFn(data) {
				const date = new Date(data.startStr.dateStr);
				let fixedEndDay = this.fixedEndDay - 1
				date.setDate(date.getDate() + fixedEndDay);
				this.$refs.CalendarRef.endDates = this.$refs.CalendarRef.resetTime(this.formatDate(date))
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'ConfirmOrder.scss';
</style>
