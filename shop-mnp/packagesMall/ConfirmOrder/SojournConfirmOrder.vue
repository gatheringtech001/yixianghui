<template>
	<view class="page_view">
		<view class="page_head_view">
		</view>
		<view class="page_body_view">
			<view class="body_base_view">
				<image :src="host + hotelData.goodsImages[0]" />
				<view class="base_view">
					<view class="base_title_view">{{hotelData.name}}</view>
					<view class="base_desc_view">{{hotelData.desc}}</view>
				</view>
			</view>
			<view class="body_date_view" @click="popupDate.show = true">
				<view class="date_start_view">
					<view class="text_view">入住日期</view>
					<view class="date_view">{{reserveData.checkInDate}}</view>
				</view>
				<view class="date_day_view">
					<view class="day_view">{{`${reserveData.day + 1}天${reserveData.day}晚`}}</view>
					<view class="day_line_view"></view>
				</view>
				<view class="date_end_view">
					<view class="text_view">退房日期</view>
					<view class="date_view">{{reserveData.checkOutDate}}</view>
				</view>
			</view>
			<view class="body_combo_view">
				<view class="combo_title_view">
					<view class="title_text_view">
						选择供餐套餐
					</view>
					<view class="title_price_view">
						<text>￥<text>{{comboList[comboIndex].price}}</text></text>/人/天
					</view>
				</view>
				<view class="combo_list_view">
					<view class="combo_item_view" :class="comboIndex == index ? 'combo_select' : ''"
						v-for="(item, index) in comboList" :key="index" @click="comboIndex = index">
						{{item.name}}
					</view>
					<view class="combo_hint_view" v-if="comboList.length === 1">
						默认供餐方案，无需额外选择
					</view>
				</view>
				<view class="combo_desc_view">
					供餐地址：<text>基地餐厅</text>
				</view>
				<view class="combo_text_view">
					共{{reserveData.day*reserveData.peopleNumber}}份早餐，
					{{comboIndex==2?reserveData.day*reserveData.peopleNumber:0}}份中餐，
					{{comboIndex!=0?reserveData.day*reserveData.peopleNumber:0}}份晚餐。
				</view>
			</view>
			<view class="body_combo_view">
				<view class="combo_title_view">
					<view class="title_text_view">
						预定信息
					</view>
				</view>
				<view class="combo_row_view">
					订几间房：<u-number-box v-model="reserveData.roomNumber" :min="1" disabled-input :input-width="200"
						:input-height="60" @change="onRoomNumberChange"></u-number-box>
					间
				</view>
				<view class="combo_row_view">
					几人用餐：<u-number-box v-model="reserveData.peopleNumber" :min="1" :max="maxPeopleNumber" disabled-input :input-width="200"
						:input-height="60"></u-number-box>
					人
				</view>
				<view class="combo_row_view">
					联系姓名：<u-input v-model="reserveData.name" type="text" border placeholder="请输入联系姓名" />
				</view>
				<view class="combo_row_view">
					联系电话：<u-input v-model="reserveData.phone" type="number" maxlength="11" border placeholder="请输入联系电话" />
				</view>
				<view class="combo_text_view">
					<text>逸享荟管家方便联系到您</text>
				</view>
				<view class="combo_text_view">
					<text>不占床位人员不记人数，1间房最多入住2人。</text>
				</view>
			</view>
			<view class="body_agreement_view" @click="isReadingAgreement = !isReadingAgreement">
				<view class="agreement-check" @click.stop>
					<u-checkbox v-model="isReadingAgreement"></u-checkbox>
				</view>
				<text class="agreement-text">我已阅读</text>
				<text class="agreement-link" @click.stop="openNotice">《预定及入住须知》</text>
			</view>
		</view>
		<view class="page_foot_view" v-if="!popupDate.show">
			<view class="foot_price_view">
				<view class="price_view">
					￥<text>{{ payableAmountText }}</text>
				</view>
				<view class="text_view">
					实付金额
				</view>
			</view>
			<view class="foot_button_view">
				<view class="text_button_view" @click="popupPrice.show = true">费用明细</view>
				<view class="pay_button_view" @click="createOrderFn">去付款</view>
			</view>
		</view>
		<u-popup
			v-model="popupDate.show"
			mode="bottom"
			border-radius="20"
			:closeable="true"
			height="85%"
			:safe-area-inset-bottom="true"
		>
			<view class="popup_view">
				<view class="popup_title_view">选择天数</view>
				<!-- 小程序 view 的 overflow 滚动不稳定，用 scroll-view 保证可滚 -->
				<scroll-view scroll-y="true" class="popup_calendar_view" :show-scrollbar="false">
					<Calendar ref="CalendarRef" :is-show="true" :isFixed="false" price="￥196/人"
						:startDate="reserveData.checkInDate" :endDate="reserveData.checkOutDate" :fixedEnd="true" :mode="2"
						themeColor="#701018" @callback="calendarCallBackFn" @calendarClick="calendarClickFn" />
				</scroll-view>
				<view class="popup_button_view">
					<view class="button_confirm_view" @click="popupDate.show = false">
						确认{{`${reserveData.day+1}天${reserveData.day}晚`}}
					</view>
				</view>
			</view>
		</u-popup>
		<u-popup v-model="popupPrice.show" @touchmove.stop.prevent mode="bottom" border-radius="20" :closeable="true">
			<view class="popup_price_view">
				<view class="price_title_view">费用明细</view>
				<view class="price_desc_view">
					<view class="desc_total_view">
						<view class="total_label_view">订单总额：</view>
						<view class="total_value_view">￥{{ payableAmountText }}</view>
					</view>
					<view class="desc_branch_view">
						<view class="branch_label_view">基础房费：</view>
						<view class="branch_value_view">￥{{ roomFeeText }}</view>
					</view>
					<view class="desc_branch_view">
						<view class="branch_label_view">基础餐费：</view>
						<view class="branch_value_view">￥{{ mealFeeText }}</view>
					</view>
					<view class="desc_total_view">
						<view class="total_label_view">全款：</view>
						<view class="total_value_view">￥{{ payableAmountText }}</view>
					</view>
				</view>
			</view>
		</u-popup>
		<u-popup v-model="popupNotice.show" @touchmove.stop.prevent mode="bottom" border-radius="20" :closeable="true">
			<view class="popup_notice_view">
				<view class="notice_title_view">{{ noticeInfo.noticeTitle || '预定及入住须知' }}</view>
				<scroll-view scroll-y class="notice_content_view">
					<u-parse :html="noticeInfo.noticeContent" />
				</scroll-view>
			</view>
		</u-popup>
	</view>
</template>

<script>
	import Calendar from '@/components/mobile-calendar-simple/Calendar.vue'
	import BaseUrl from '@/api/baseUrl'
	import {
		getGoodsInfo
	} from '@/api/shop/index'
	import {
		createOrder
	} from '@/api/member/index'
	import {
		getNoticeInfo
	} from '@/api/system/notice'
	export default {
		components: {
			Calendar
		},
		data() {
			return {
				host: this.$host,
				hotelData: {
					id: undefined,
					goodsImages: [],
					name: '', // 名称
					desc: '', // 简介
				},
				reserveData: {
					day: 3,
					checkInDate: '', // 入住日期
					checkOutDate: '', // 离开日期
					roomNumber: 1, // 预定房间数
					peopleNumber: 2, // 入住人数
					name: '', // 联系姓名
					phone: '', // 联系电话
				}, // 预定信息
				popupDate: {
					show: false,
					calendarEndDate: '', // 日期组件初始化结束日期
				}, // 日历弹窗
				comboList: [{
						comboId: null,
						name: '含早餐',
						price: 0,
					},
					{
						comboId: null,
						name: '一早一正【晚餐】',
						price: 25,
					},
					{
						comboId: null,
						name: '一日三餐',
						price: 50,
					}
				], // 套餐数据集合
				comboIndex: 0, // 套餐选中的下标
				skuList: [{
					    skuId: null,
						skuSeqNo: null,
						day: 3,
						price: 312
					},
					{
						skuId: null,
						skuSeqNo: null,
						day: 6,
						price: 600
					},
					{
						skuId: null,
						skuSeqNo: null,
						day: 14,
						price: 1386
					},
					{
						skuId: null,
						skuSeqNo: null,
						day: 29,
						price: 2581
					}
				], // sku数据集合
				skuPrice: 0, // sku选中的价格（整段住宿房价，未乘房间数）
				nightPrice: 0, // 自选晚数：每晚单价
				selSkuId: undefined,
				selComboId: undefined,
				selSkuSeqNo: undefined,
				routeSkuId: null,
				routeSkuSeqNo: null,
				customNight: false, // 自选晚数下单
				standardSkuList: [], // skuType=200 标准房型
				isReadingAgreement: false, // 是否阅读协议
				popupPrice: {
					show: false,
				},
				popupNotice: {
					show: false,
				},
				noticeInfo: {
					noticeTitle: '',
					noticeContent: ''
				}
			}
		},
		computed: {
			currentCombo() {
				return this.comboList[this.comboIndex] || this.comboList[0] || { price: 0 }
			},
			mealUnitPrice() {
				return Number(this.currentCombo.price) || 0
			},
			roomFee() {
				const unit = Number(this.skuPrice) || 0
				const rooms = Number(this.reserveData.roomNumber) || 1
				return unit * rooms
			},
			mealFee() {
				const people = Number(this.reserveData.peopleNumber) || 0
				const days = Number(this.reserveData.day) || 0
				return people * this.mealUnitPrice * days
			},
			payableAmount() {
				return this.roomFee + this.mealFee
			},
			payableAmountText() {
				return this.payableAmount.toFixed(2)
			},
			roomFeeText() {
				return this.roomFee.toFixed(2)
			},
			mealFeeText() {
				return this.mealFee.toFixed(2)
			},
			maxPeopleNumber() {
				const rooms = Number(this.reserveData.roomNumber) || 1
				return Math.max(rooms * 2, 1)
			}
		},
		onLoad(e) {
			this.routeSkuId = e.skuId ? Number(e.skuId) : null
			this.routeSkuSeqNo = e.skuSeqNo !== undefined && e.skuSeqNo !== '' ? Number(e.skuSeqNo) : null
			this.customNight = e.customNight === '1' || e.customNight === 'true'
			if (e.comboIndex !== undefined && e.comboIndex !== '') {
				this.comboIndex = Number(e.comboIndex)
			}
			if (e.id) {
				this.hotelData.id = e.id
				this.getGoodsDetailFn(e.id)
			}
			if (e.day) {
				this.reserveData.day = Number(e.day)
			}
			if (e.checkInDate) {
				this.reserveData.checkInDate = e.checkInDate
			}
			if (e.checkOutDate) {
				this.reserveData.checkOutDate = e.checkOutDate
			}
		},
		methods: {
			onRoomNumberChange() {
				const maxPeople = this.maxPeopleNumber
				if (Number(this.reserveData.peopleNumber) > maxPeople) {
					this.reserveData.peopleNumber = maxPeople
				}
			},
			parseStayNights(skudays, skuName, optionValueUnit) {
				const name = skuName || ''
				if (name.indexOf('半月') !== -1) return 14
				if (name.indexOf('周') !== -1) return 6
				if (name.indexOf('月') !== -1) return 29
				const total = parseInt(skudays) || 0
				const unit = optionValueUnit || ''
				if (total > 0) {
					if (unit.indexOf('晚') !== -1 && unit.indexOf('天') === -1) {
						return total
					}
					return Math.max(total - 1, 0)
				}
				return 0
			},
			isSkuEnabled(sku) {
				if (!sku) return false
				const s = String(sku.status == null ? '' : sku.status).trim()
				if (!s) return true
				return !(s === '0' || s === '停用')
			},
			syncStayCalendar() {
				const nights = Number(this.reserveData.day) || 0
				if (!nights || !this.reserveData.checkInDate || !this.$refs.CalendarRef) return
				const end = new Date(this.reserveData.checkInDate.replace(/-/g, '/'))
				end.setDate(end.getDate() + nights)
				const checkOut = this.formatDate(end)
				this.reserveData.checkOutDate = checkOut
				this.$refs.CalendarRef.endDates = this.$refs.CalendarRef.resetTime(checkOut)
				this.syncSkuSelection()
			},
			syncSkuSelection() {
				const targetDay = parseInt(this.reserveData.day)
				// 自选晚数：标准房型售价 × 晚数
				if (this.customNight) {
					const enabledList = this.standardSkuList.filter(item => this.isSkuEnabled(item))
					const pool = enabledList.length ? enabledList : this.standardSkuList
					let std = null
					if (this.routeSkuId) {
						std = pool.find(item => Number(item.skuId) === Number(this.routeSkuId))
					}
					if (!std && pool.length === 1) {
						std = pool[0]
					}
					const perNight = std ? Number(std.price) : 0
					if (!std || !std.skuId || !Number.isFinite(perNight) || perNight <= 0 || !(targetDay > 0)) {
						this.skuPrice = 0
						this.nightPrice = 0
						this.selSkuId = undefined
						this.selSkuSeqNo = undefined
						return false
					}
					this.nightPrice = perNight
					this.skuPrice = perNight * targetDay
					this.selSkuId = std.skuId
					this.selSkuSeqNo = undefined
					return true
				}
				let matched = null
				if (this.routeSkuId && this.routeSkuSeqNo !== null) {
					matched = this.skuList.find(item =>
						Number(item.skuId) === Number(this.routeSkuId) &&
						parseInt(item.skuSeqNo) === this.routeSkuSeqNo &&
						parseInt(item.day) === targetDay
					)
				}
				if (!matched && this.routeSkuId) {
					matched = this.skuList.find(item =>
						Number(item.skuId) === Number(this.routeSkuId) &&
						parseInt(item.day) === targetDay
					)
				}
				if (!matched) {
					matched = this.skuList.find(item => parseInt(item.day) === targetDay)
				}
				if (matched) {
					this.skuPrice = matched.price
					this.nightPrice = 0
					this.selSkuId = matched.skuId
					this.selSkuSeqNo = matched.skuSeqNo
					return true
				}
				this.skuPrice = 0
				this.nightPrice = 0
				this.selSkuId = undefined
				this.selSkuSeqNo = undefined
				return false
			},
			// 获取商品详情
			getGoodsDetailFn(id) {
				getGoodsInfo(id).then(res => {
					this.hotelData.id = res.data.goodsId
					this.hotelData.name = res.data.goodsName
					this.hotelData.desc = res.data.description
					this.hotelData.goodsImages = res.data.goodsImages.split(',')
					this.comboList = []
					if(res.data.goodsImages){
						this.hotelData.goodsImages = res.data.goodsImages.split(",");
						this.hotelData.goodsImages.map(image => {
						      if (image.startsWith('http')) {
						        return image;
						      } else {
						        return this.host + image;
						      }
						    });
					}
					if (res.data.optionList) {
					  this.standardSkuList = res.data.optionList
					    .filter(option => option.skuType === '200' && this.isSkuEnabled(option))
					    .map(option => ({
					      skuId: option.skuId,
					      name: option.skuName || '',
					      price: Number(option.price) || 0,
					      status: option.status
					    }))
					  const filteredOptionList = res.data.optionList.filter(
					    option => option.skuType === '202' && this.isSkuEnabled(option)
					  );
					  this.skuList = filteredOptionList.flatMap(option => {
					   const groupedBySeqNo = {};
					   let skudays = '0';
					   let skudaysUnit = '';
					   if (option.options) {
					     option.options.forEach(item => {
					       if (item.skuSeqNo == 0) {
					         if (item.optionType == '303' && item.optionValue) {
					           skudays = item.optionValue;
					           skudaysUnit = item.optionValueUnit || '';
					         }
					       } else {
					         const seqNo = item.skuSeqNo;
					         if (!groupedBySeqNo[seqNo]) {
					           groupedBySeqNo[seqNo] = [];
					         }
					         groupedBySeqNo[seqNo].push(item);
					       }
					     });
					   }
					   const day = this.parseStayNights(skudays, option.skuName, skudaysUnit);
					   return Object.keys(groupedBySeqNo).map(seqNo => {
					     const items = groupedBySeqNo[seqNo];
					     const priceItem = items.find(item => item.optionType == '302');
					     return {
					       skuId: option.skuId,
					       skuSeqNo: parseInt(seqNo) || 0,
					       day,
					       price: priceItem ? parseFloat(priceItem.optionValue) || 0 : 0
					     };
					   });
					  });
					  this.syncSkuSelection();
					  if (this.reserveData.checkInDate) {
					    this.$nextTick(() => this.syncStayCalendar())
					  }
					  const filteredSelfOptionList = res.data.optionList.filter(
					    option => option.skuType === '201' && this.isSkuEnabled(option)
					  );
					  if(filteredSelfOptionList && filteredSelfOptionList.length>0){
					    this.comboList = filteredSelfOptionList.map(option => {
					      return {
					        comboId: option.skuId,
					        name: option.skuName || '',
					        price: option.price !== undefined ? option.price : 0
					      };
					    });
					  } else {
					    // 后台未配置供餐 SKU 时，默认含早餐、餐费为 0，仍可正常下单
					    this.comboList = [{
					      comboId: null,
					      name: '含早餐',
					      price: 0,
					    }];
					  }
					  if (this.comboList.length === 1) {
					    this.comboIndex = 0
					  }
						  
					}
				}).catch(err => {
					console.log('getGoodsInfo', err)
				})
			},
			// 日期组件回调方法
			calendarCallBackFn(date) {
				this.reserveData.checkInDate = date.startStr.dateStr
				if (date.endStr && date.endStr.dateStr) {
					this.reserveData.checkOutDate = date.endStr.dateStr
				}
			},
			calendarClickFn(data) {
				this.reserveData.checkInDate = data.startStr.dateStr
				this.syncStayCalendar()
			},
			// 格式化日期
			formatDate(date) {
				const year = date.getFullYear();
				const month = String(date.getMonth() + 1).padStart(2, '0');
				const day = String(date.getDate()).padStart(2, '0');
				return `${year}-${month}-${day}`;
			},
			normalizeDateStr(value) {
				if (value == null || value === '') return ''
				const text = String(value).trim()
				if (!text || text === 'null') return ''
				const parts = text.split('-')
				if (parts.length !== 3) return text
				return `${parts[0]}-${String(parts[1]).padStart(2, '0')}-${String(parts[2]).padStart(2, '0')}`
			},
			ensureStayDates() {
				this.reserveData.checkInDate = this.normalizeDateStr(this.reserveData.checkInDate)
				this.reserveData.checkOutDate = this.normalizeDateStr(this.reserveData.checkOutDate)
				const nights = Number(this.reserveData.day) || 0
				if (!this.reserveData.checkInDate || nights <= 0) return false
				if (!this.reserveData.checkOutDate) {
					const end = new Date(this.reserveData.checkInDate.replace(/-/g, '/'))
					end.setDate(end.getDate() + nights)
					this.reserveData.checkOutDate = this.formatDate(end)
				}
				return !!(this.reserveData.checkInDate && this.reserveData.checkOutDate)
			},
			// 选择日期方法
			selectDateFn() {
				this.popupDate.show = true
			},
			// 确认日期方法
			confirmFn() {
				this.popupDate.show = false
			},
			async openNotice() {
				if (this.noticeInfo.noticeContent) {
					this.popupNotice.show = true
					return
				}
				uni.showLoading({
					title: '加载中'
				})
				try {
					const res = await getNoticeInfo(1)
					if (res.code === 200 && res.data) {
						this.noticeInfo = res.data
						if (this.noticeInfo.noticeContent) {
							this.noticeInfo.noticeContent = this.noticeInfo.noticeContent.replace(/src="\/api\//g,
								'src="' + BaseUrl.publicUrl)
						}
						this.popupNotice.show = true
					} else {
						uni.showToast({
							icon: 'none',
							title: res.msg || '获取预定及入住须知失败'
						})
					}
				} catch (err) {
					console.log('getNoticeInfo', err)
				} finally {
					uni.hideLoading()
				}
			},
			// 创建订单方法
			createOrderFn() {
				if(!this.reserveData.name) {
					uni.showToast({
						icon: 'none',
						title:'请填写联系姓名'
					})
					return
				}
				if(!/^1\d{10}$/.test(String(this.reserveData.phone || '').trim())) {
					uni.showToast({
						icon: 'none',
						title:'请输入正确号码'
					})
					return
				}
				if(!this.isReadingAgreement) {
					uni.showToast({
						icon: 'none',
						title:'请阅读预定及入住须知'
					})
					return
				}
				if (!this.reserveData.checkInDate || !this.reserveData.checkOutDate) {
					uni.showToast({
						icon: 'none',
						title: '请选择入住和退房日期'
					})
					return
				}
				const nights = Math.round((new Date(this.reserveData.checkOutDate.replace(/-/g, '/')) - new Date(this.reserveData.checkInDate.replace(/-/g, '/'))) / (24 * 3600 * 1000))
				if (nights !== Number(this.reserveData.day)) {
					uni.showToast({
						icon: 'none',
						title: `套餐为${this.reserveData.day + 1}天${this.reserveData.day}晚，日期不匹配`
					})
					return
				}
				if (!this.syncSkuSelection() || !this.selSkuId) {
					uni.showToast({
						icon: 'none',
						title: this.customNight
							? '该房型未配置每晚单价，请返回重新选择'
							: '未匹配到有效套餐，请返回重新选择'
					})
					return
				}
				if (this.customNight && Number(this.reserveData.day) < 7) {
					uni.showToast({
						icon: 'none',
						title: '自选晚数不能少于7晚'
					})
					return
				}
				if (!this.ensureStayDates()) {
					uni.showToast({
						icon: 'none',
						title: '请选择入住和退房日期'
					})
					return
				}
				const combo = this.comboList[this.comboIndex] || this.comboList[0]
				if (!combo) {
					uni.showToast({
						icon: 'none',
						title: '暂无供餐套餐配置'
					})
					return
				}
				this.$u.throttle(() => {
					const selfSkuId = combo.comboId || null
					let params = {
						addressId: 0,
						goodsCount: this.reserveData.roomNumber,
						goodsId: this.hotelData.id,
						couponGotIds: '',
						checkInDate: this.reserveData.checkInDate,
						checkOutDate: this.reserveData.checkOutDate,
						skuDataId: null,
						selfSkuId: selfSkuId,
						skuId: this.selSkuId,
						selfGoodsCount: this.reserveData.peopleNumber,
						interCount: this.reserveData.day,
						contactName: this.reserveData.name,
						contactPhone: this.reserveData.phone
					}
					// 固定套餐传组合序号；自选晚数不传，后端按标准房型每晚单价计算
					if (!this.customNight && this.selSkuSeqNo !== null && this.selSkuSeqNo !== undefined) {
						params.skuSeqNo = this.selSkuSeqNo
					}
					createOrder(params).then(res => {
						if (!res || res.code !== 200 || !res.data) {
							uni.showToast({
								icon: 'none',
								title: (res && res.msg) || '订单创建失败'
							})
							return
						}
						const serverAmt = Number(res.data.moneyPayable)
						const localAmt = Number(this.payableAmount)
						const goCashier = () => {
							uni.redirectTo({
								url: `/packagesMall/CashierDesk/SojournCashierDesk?id=${this.hotelData.id}&price=${res.data.moneyPayable}&orderId=${res.data.orderId}&orderNo=${res.data.orderNo}&roomNumber=${this.reserveData.roomNumber}&peopleNumber=${this.reserveData.peopleNumber}&comboIndex=${this.comboIndex}&checkInDate=${this.reserveData.checkInDate}&checkOutDate=${this.reserveData.checkOutDate}`
							})
						}
						// 页面展示价与系统落库价不一致时，明确提示，防止误以为按展示价支付
						if (Number.isFinite(serverAmt) && Number.isFinite(localAmt)
							&& Math.abs(serverAmt - localAmt) > 0.009) {
							uni.showModal({
								title: '金额确认',
								content: `系统核算应付￥${serverAmt.toFixed(2)}元（当前页面展示￥${localAmt.toFixed(2)}元）。微信支付将按系统金额扣款。`,
								confirmText: '按系统金额支付',
								success: (modalRes) => {
									if (modalRes.confirm) {
										goCashier()
									}
								}
							})
							return
						}
						uni.showToast({
							icon: 'none',
							title: '订单创建成功！'
						})
						setTimeout(() => {
							goCashier()
						}, 800)
					}).catch(err => {
						console.log('createOrder', err)
						uni.showToast({
							icon: 'none',
							title: (err && (err.message || err.msg)) || '订单创建失败，请稍后重试'
						})
					})
				}, 500)
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'SojournConfirmOrder.scss';
</style>