<template>
	<view :class="displayModeClass" class="page_view">
		<view class="page_head_view">
		</view>
		<view v-if="!goodsLoaded" class="loading_quote_view">{{ loadError || '正在加载房型与套餐报价…' }}</view>
		<view class="page_body_view" v-if="goodsLoaded">
			<view class="body_base_view">
				<image :src="coverUrl" />
				<view class="base_view">
					<view class="base_title_view">{{hotelData.name}}</view>
					<view class="base_desc_view">{{hotelData.desc}}</view>
				</view>
			</view>
			<view class="body_date_view" @click="selectDateFn">
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
			<view class="combo_text_view">{{ selectedTravelPolicyText }}</view>
			<view v-if="selectionError && selectionError !== partyQuoteError" class="combo_text_view" style="color: #701018">{{ selectionError }}</view>
			<view class="body_combo_view">
				<view class="combo_title_view">
					<view class="title_text_view">
						选择供餐套餐
					</view>
					<view class="title_price_view" v-if="includedMeals">{{ metadataMealPresentation.title }}</view>
					<view class="title_price_view" v-else>
						<text>￥<text>{{currentCombo.price}}</text></text>/人/天
					</view>
				</view>
				<view class="combo_list_view">
					<view class="combo_item_view" :class="comboIndex == index ? 'combo_select' : ''"
						v-for="(item, index) in availableCombos" :key="index" @click="comboIndex = index">
						{{item.name}}
					</view>
					<view class="combo_hint_view" v-if="availableCombos.length === 1">
						默认供餐方案，无需额外选择
					</view>
				</view>
				<view class="combo_desc_view">
					供餐地址：<text>基地餐厅</text>
				</view>
				<view class="combo_text_view">
					{{ mealDescription }}
				</view>
			</view>
			<view class="body_combo_view">
				<view class="combo_title_view">
					<view class="title_text_view">
						预定信息
					</view>
				</view>
				<view class="booking_quote_view" v-if="!partyQuoteError && selectedQuote">
                    <view class="booking_quote_amount">约￥{{ selectedQuote.amount }}/{{ selectedQuote.unit }}/晚</view>
                    <view>套餐总价￥{{ selectedQuote.total }}/{{ selectedQuote.totalUnit }} · {{ reserveData.day + 1 }}天{{ reserveData.day }}晚</view>
                    <view>{{ reserveData.peopleNumber === 1 ? '单人包房' : selectedQuote.occupancyLabel }}</view>
                </view>
                <view class="combo_text_view" v-if="partyQuoteError" style="color: #701018">{{ partyQuoteError }}</view>
                <view v-if="reserveData.peopleNumber === 1 && soloRoomOffers.length" class="solo_room_offers">
                    <view>单人包房房型（点击明确选择，不自动替换原房型）</view>
                    <view v-for="offer in soloRoomOffers" :key="offer.skuId" class="solo_room_offer"
                        :class="!partyQuoteError && selSkuId === offer.skuId ? 'combo_select' : ''"
                        role="button" @click="selectSoloRoomOffer(offer.skuId)">
                        {{ offer.roomName }} · {{ offer.day + 1 }}天{{ offer.day }}晚 · {{ offer.meal }} · 总价￥{{ offer.total }}/间
                    </view>
                </view>
				<view class="combo_row_view">
					订几间房：<view class="booking_stepper">
                        <button role="button" aria-label="减少房间" :disabled="reserveData.roomNumber <= 1" @click="changeRoomNumber(-1)">－</button>
                        <text>{{ reserveData.roomNumber }}</text>
                        <button role="button" aria-label="增加房间" @click="changeRoomNumber(1)">＋</button>
                    </view>
					间
				</view>
				<view class="combo_row_view">
					入住人数：<view class="booking_stepper">
                        <button role="button" aria-label="减少入住人数" :disabled="reserveData.peopleNumber <= 1" @click="changePeopleNumber(-1)">－</button>
                        <text>{{ reserveData.peopleNumber }}</text>
                        <button role="button" aria-label="增加入住人数" :disabled="reserveData.peopleNumber >= maxPeopleNumber" @click="changePeopleNumber(1)">＋</button>
                    </view>
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
					<text>请按实际入住人数选择；1人入住仅使用已配置的单人包房报价。</text>
				</view>
			</view>
			<view v-if="eligibilityNotice" class="body_combo_view">
				<view class="combo_title_view">
					<view class="title_text_view">入住资格（自我声明）</view>
				</view>
				<view class="combo_text_view">{{ eligibilityNotice }}</view>
				<view class="body_agreement_view" @click="travelEligibilityConfirmed = !travelEligibilityConfirmed">
					<view class="agreement-check" @click.stop>
						<u-checkbox v-model="travelEligibilityConfirmed"></u-checkbox>
					</view>
					<text class="agreement-text" style="flex: 1">我确认本次入住符合上述资格条件（自我声明，非年龄核验）</text>
				</view>
			</view>
			<view class="body_agreement_view" @click="isReadingAgreement = !isReadingAgreement">
				<view class="agreement-check" @click.stop>
					<u-checkbox v-model="isReadingAgreement"></u-checkbox>
				</view>
				<text class="agreement-text">我已阅读</text>
				<text class="agreement-link" @click.stop="openNotice">《预订及入住须知》</text>
			</view>
		</view>
		<view class="page_foot_view" v-if="goodsLoaded && !popupDate.show">
			<view class="foot_price_view">
				<view class="price_view">
					<text>{{ partyQuoteError ? '报价待确认' : '￥' + payableAmountText }}</text>
				</view>
				<view class="text_view">
					实付金额
				</view>
			</view>
			<view class="foot_button_view">
				<view class="text_button_view" v-if="!partyQuoteError" @click="popupPrice.show = true">费用明细</view>
				<view class="pay_button_view" :class="{pay_disabled: !!selectionError}" :aria-disabled="!!selectionError" @click="createOrderFn">{{ selectionError ? '暂不可预订' : '去付款' }}</view>
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
				<view class="popup_title_view">选择入住日期 · {{reserveData.day}}晚</view>
				<!-- 小程序 view 的 overflow 滚动不稳定，用 scroll-view 保证可滚 -->
				<scroll-view scroll-y="true" class="popup_calendar_view" :show-scrollbar="false" :scroll-top="calendarScrollTop">
					<Calendar ref="CalendarRef" :is-show="true" :isFixed="false" :price="calendarPriceText"
						:focusMonthId="calendarFocusId" @monthFocus="onCalendarMonthFocus"
						:dailyPrices="customNight ? null : calendarRateData.prices" :initMonth="calendarRateData.count"
						:betweenStart="calendarRateData.first" :betweenEnd="calendarRateData.end"
						:startDate="reserveData.checkInDate" :endDate="reserveData.checkOutDate" :fixedEnd="true" :mode="2"
						themeColor="#701018" @callback="calendarCallBackFn" @calendarClick="calendarClickFn" />
				</scroll-view>
				<view class="popup_button_view">
					<view v-if="!selectionError">{{calendarPriceText}}/晚 · {{reserveData.day}}晚合计 ￥{{skuPrice}}</view>
					<view v-if="selectionError" style="color: #701018">{{ selectionError }}</view>
					<view class="button_confirm_view" @click="confirmFn">
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
				<view class="notice_title_view">{{ noticeInfo.noticeTitle || '预订及入住须知' }}</view>
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
	import { formatCalendarPrice, resolveCalendarUnitPrice, travelPriceUnit, sanitizeTravelCustomerText } from '@/utils/travelPresentation'
	import { groupTravelRates, resolveTravelRate, buildTravelRateCalendar } from '@/utils/travelRates'
	import { readTravelPackage, supportsCustomNights, travelCapacity, travelMealPresentation, travelPolicyText, validateTravelSelection } from '@/utils/travelPolicy'
	import { travelPartyVariants, selectTravelPartyRate, travelQuote } from '@/utils/travelBooking'
	import { detailImageUrl } from '@/utils/detailImagePreview'
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
				skuList: [],
				calendarFocusId: '',
				calendarScrollTop: 0,
				goodsLoaded: false,
                loadError: '',
				skuPrice: 0, // sku选中的价格（整段住宿房价，未乘房间数）
				nightPrice: 0, // 自选晚数：每晚单价
				selSkuId: undefined,
				selComboId: undefined,
				selSkuSeqNo: undefined,
				partyReference: null,
                soloRoomSkuId: null,
                partyQuoteError: '',
				routeSkuId: null,
				routeSkuSeqNo: null,
				customNight: false, // 自选晚数下单
				standardSkuList: [], // skuType=200 标准房型
				isReadingAgreement: false, // 是否阅读协议
				travelEligibilityConfirmed: false, // 独立的入住资格自我声明，不代表年龄核验
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
            coverUrl() { return detailImageUrl(this.hotelData.goodsImages[0], this.host) },
            partyVariants() { return this.partyReference ? travelPartyVariants(this.skuList, this.partyReference) : [] },
            soloRoomOffers() {
                if (Number(this.reserveData.peopleNumber) !== 1 || Number(this.reserveData.roomNumber) !== 1 || !this.partyReference) return []
                const ref = this.partyReference, meal = (ref.travelPolicy || {}).includedMeals
                return this.skuList.filter(s => Number((s.travelPolicy || {}).capacity) === 1
                    && Number(s.day) === Number(ref.day) && (s.travelPolicy || {}).includedMeals === meal
                    && travelQuote(s, (s.combinationList || [])[0])
                    && this.standardSkuList.some(r => Number(r.skuId) === Number(s.parSkuId))
                    && selectTravelPartyRate([s], 1, 1, this.reserveData.checkInDate, this.formatDate(new Date())))
                    .map(s => ({ skuId: s.skuId, day: Number(s.day), meal,
                        roomName: this.standardSkuList.find(r => Number(r.skuId) === Number(s.parSkuId)).name,
                        total: travelQuote(s, s.combinationList[0]).total }))
            },
            selectedQuote() { return travelQuote(this.selectedTravelPackage, (this.selectedTravelPackage.combinationList || [])[0]) },
			selectedRateFamily() {
				const selected = this.selectedTravelPackage
				const siblings = this.skuList.filter(s => Number(s.parSkuId) === Number(selected.parSkuId)
					&& Number(s.skuSeqNo) === Number(selected.skuSeqNo))
				return groupTravelRates(siblings).find(g => g.variants.some(s => s.skuId === selected.skuId)) || { variants: [] }
			},
			calendarRateData() {
				const today = this.formatDate(new Date())
				if (this.customNight) return { first: today, end: '', prices: {}, count: 6 }
				return buildTravelRateCalendar(this.selectedRateFamily.variants, this.reserveData.day, this.selSkuSeqNo, today)
			},
			selectedTravelPackage() {
				return this.skuList.find(item => Number(item.skuId) === Number(this.selSkuId)
					&& Number(item.skuSeqNo) === Number(this.selSkuSeqNo)) || {}
			},
			includedMeals() {
				return (this.selectedTravelPackage.travelPolicy || {}).includedMeals || ''
			},
			metadataMealPresentation() {
				return travelMealPresentation(this.includedMeals)
			},
			eligibilityNotice() {
				return (this.selectedTravelPackage.travelPolicy || {}).eligibilityNotice || ''
			},
			eligibilityConfirmationError() {
				return this.eligibilityNotice && this.travelEligibilityConfirmed !== true ? '请确认入住资格自我声明' : ''
			},
			availableCombos() {
				return this.includedMeals ? [{ comboId: null, name: this.includedMeals, price: 0 }] : this.comboList
			},
			currentCombo() {
				return this.availableCombos[this.comboIndex] || this.availableCombos[0] || { price: 0 }
			},
			roomCapacity() {
				return travelCapacity(this.selectedTravelPackage.travelPolicy)
			},
			selectedTravelPolicyText() {
				return travelPolicyText(this.selectedTravelPackage.travelPolicy)
			},
			selectionError() {
				if (!this.goodsLoaded) return '正在加载套餐，请稍候'
                if (this.partyQuoteError) return this.partyQuoteError
				if (!this.selSkuId) return this.customNight ? '该房型不支持自选晚数，请返回重新选择' : '未匹配到有效套餐，请返回重新选择'
				return validateTravelSelection({ ...this.selectedTravelPackage, ...this.reserveData, nights: this.reserveData.day })
			},
			mealDescription() {
				const count = this.reserveData.day * this.reserveData.peopleNumber
				if (this.includedMeals) return this.metadataMealPresentation.description
				return `共${count}份早餐，${this.comboIndex === 2 ? count : 0}份中餐，${this.comboIndex !== 0 ? count : 0}份晚餐。`
			},
			calendarPriceText() {
                if (!this.customNight && this.selectedQuote) return `约￥${this.selectedQuote.amount}/${this.selectedQuote.unit}`
                return formatCalendarPrice(resolveCalendarUnitPrice({ nightPrice: this.nightPrice, total: this.skuPrice, nights: this.reserveData.day }), '间')
			},
			mealUnitPrice() {
				return this.includedMeals ? 0 : Number(this.currentCombo.price) || 0
			},
			roomFee() {
				const unit = Number(this.skuPrice) || 0
				const rooms = Number(this.reserveData.roomNumber) || 1
				return Math.round(unit * 100) * rooms / 100
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
				return Math.max(rooms * Math.max(this.roomCapacity, ...this.partyVariants.map(s => Number((s.travelPolicy || {}).capacity) || 0)), 1)
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
			onCalendarMonthFocus(top) {
				this.calendarScrollTop = top + 1
				this.$nextTick(() => { this.calendarScrollTop = top })
			},
            changePeopleNumber(delta) {
                this.reserveData.peopleNumber = Math.max(1, Math.min(this.maxPeopleNumber, this.reserveData.peopleNumber + delta))
                if (this.reserveData.peopleNumber !== 1) this.soloRoomSkuId = null
                this.syncPartySelection()
            },
            selectSoloRoomOffer(skuId) {
                if (!this.soloRoomOffers.some(o => o.skuId === skuId)) return
                this.soloRoomSkuId = skuId
                this.syncPartySelection()
            },
            changeRoomNumber(delta) {
                this.reserveData.roomNumber = Math.max(1, this.reserveData.roomNumber + delta)
                this.reserveData.peopleNumber = Math.min(this.reserveData.peopleNumber, this.maxPeopleNumber)
                this.syncPartySelection()
            },
            syncPartySelection() {
                if (this.customNight || !this.partyReference || !(this.partyReference.travelPolicy || {}).capacity) return
                const explicitSolo = Number(this.reserveData.peopleNumber) === 1 && this.soloRoomOffers.some(o => o.skuId === this.soloRoomSkuId)
                const variants = explicitSolo ? this.skuList.filter(s => s.skuId === this.soloRoomSkuId) : this.partyVariants
                const sku = selectTravelPartyRate(variants, Number(this.reserveData.peopleNumber), Number(this.reserveData.roomNumber), this.reserveData.checkInDate, this.formatDate(new Date()))
                if (!sku) {
                    this.partyQuoteError = Number(this.reserveData.peopleNumber) === 1
                        ? '该房型、餐制和日期暂无可用单人包房报价，请联系客服确认或选择多人入住。'
                        : '当前人数、房间数或日期暂无匹配套餐，请调整选择。'
                    this.skuPrice = 0
                    return
                }
                const changed = this.routeSkuId !== sku.skuId || this.routeSkuSeqNo !== sku.skuSeqNo
                this.partyQuoteError = ''
                this.routeSkuId = sku.skuId
                this.routeSkuSeqNo = sku.skuSeqNo
                this.syncSkuSelection()
                if (changed) this.travelEligibilityConfirmed = false
            },
			onRoomNumberChange() {
				const maxPeople = this.maxPeopleNumber
				if (Number(this.reserveData.peopleNumber) > maxPeople) {
					this.reserveData.peopleNumber = maxPeople
				}
                this.$nextTick(() => { this.syncPartySelection() })
			},

			isSkuEnabled(sku) {
				if (!sku) return false
				const s = String(sku.status == null ? '' : sku.status).trim()
				if (!s) return true
				return !(s === '0' || s === '停用')
			},
			syncStayCalendar() {
				const nights = Number(this.reserveData.day) || 0
				if (!nights || !this.reserveData.checkInDate) return
				const end = new Date(this.reserveData.checkInDate.replace(/-/g, '/'))
				end.setDate(end.getDate() + nights)
				const checkOut = this.formatDate(end)
				this.reserveData.checkOutDate = checkOut
				if (this.$refs.CalendarRef) this.$refs.CalendarRef.endDates = this.$refs.CalendarRef.resetTime(checkOut)
				this.syncSkuSelection()
			},
			syncSkuSelection() {
				const targetDay = Number(this.reserveData.day)
				// 自选晚数：标准房型售价 × 晚数
				if (this.customNight) {
					const enabledList = this.standardSkuList.filter(item => this.isSkuEnabled(item))
					const pool = enabledList
					let std = null
					if (this.routeSkuId) {
						std = pool.find(item => Number(item.skuId) === Number(this.routeSkuId))
					}
					if (!std && !this.routeSkuId && pool.length === 1) {
						std = pool[0]
					}
					const perNight = std ? Number(std.price) : 0
					if (!supportsCustomNights(std) || !Number.isSafeInteger(targetDay) || targetDay < 7) {
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
				if (!matched && this.routeSkuId && this.routeSkuSeqNo === null) {
					matched = this.skuList.find(item =>
						Number(item.skuId) === Number(this.routeSkuId) &&
						parseInt(item.day) === targetDay
					)
				}
				if (!matched && !this.routeSkuId) {
					matched = this.skuList.find(item => parseInt(item.day) === targetDay)
				}
				if (matched && Number.isFinite(Number(matched.price)) && Number(matched.price) > 0) {
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
					this.hotelData.goodsImages = String(res.data.goodsImages || '').split(',').filter(Boolean)
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
					      && this.standardSkuList.some(room => Number(room.skuId) === Number(option.parSkuId))
					  );
					  this.skuList = filteredOptionList.flatMap(option => {
					   const groupedBySeqNo = {};
					   const packagePolicy = readTravelPackage(option);
					   if (option.options) {
					     option.options.forEach(item => {
					       if (item.skuSeqNo == 0) {
					         // Public 303 is parsed by readTravelPackage.
					       } else {
					         const seqNo = item.skuSeqNo;
					         if (!groupedBySeqNo[seqNo]) {
					           groupedBySeqNo[seqNo] = [];
					         }
					         groupedBySeqNo[seqNo].push(item);
					       }
					     });
					   }

					   return Object.keys(groupedBySeqNo).map(seqNo => {
					     const items = groupedBySeqNo[seqNo];
					     const priceItem = items.find(item => item.optionType == '302');
					     const nameItem = items.find(item => item.optionType == '304');
					     const averageItem = items.find(item => item.optionType == '301');
					     const priceUnit = travelPriceUnit(priceItem && priceItem.optionValueUnit);
					     const combo = {
					       skuSeqNo: parseInt(seqNo),
					       name: (packagePolicy.travelPolicy || {}).includedMeals || sanitizeTravelCustomerText(nameItem && nameItem.optionValue),
					       price: priceItem ? Number(priceItem.optionValue) : 0,
					       priceUnit,
					       average: priceUnit === '间' ? 0 : Number(averageItem && averageItem.optionValue) || 0
					     };
					     return {
					       skuId: option.skuId,
					       parSkuId: option.parSkuId,
					       name: option.skuName,
					       combinationList: [combo],
					       skuSeqNo: parseInt(seqNo) || 0,
					       ...packagePolicy,
					       price: priceItem ? Number(priceItem.optionValue) : 0,
					       priceUnit: priceItem ? priceItem.optionValueUnit : '人'
					     };
					   });
					  });
					  this.syncSkuSelection();
					  this.reserveData.peopleNumber = this.roomCapacity
                      this.partyReference = this.selectedTravelPackage
					  const filteredSelfOptionList = res.data.optionList.filter(
					    option => option.skuType === '201' && this.isSkuEnabled(option)
					  );
					  if (this.includedMeals) {
					    this.comboList = [{ comboId: null, name: this.includedMeals, price: 0 }]
					  } else if(filteredSelfOptionList && filteredSelfOptionList.length>0){
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
					  if (this.availableCombos.length === 1) {
					    this.comboIndex = 0
					  }
						  
					}
					this.goodsLoaded = true
				}).catch(err => {
					this.loadError = '套餐加载失败，请返回重试'
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
				const date = data.startStr.dateStr
				if (!this.customNight) {
					const sku = resolveTravelRate(this.selectedRateFamily.variants, date, this.reserveData.day, this.selSkuSeqNo, this.formatDate(new Date()))
					if (!sku) return
					this.routeSkuId = sku.skuId
					this.routeSkuSeqNo = sku.skuSeqNo
				}
				this.reserveData.checkInDate = date
				this.syncStayCalendar()
                this.syncPartySelection()
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
				this.calendarFocusId = ''
				this.$nextTick(() => { this.calendarFocusId = `travel-month-${(this.reserveData.checkInDate || this.calendarRateData.first).slice(0, 7)}` })
			},
			// 确认日期方法
			confirmFn() {
				if (this.selectionError) {
					uni.showToast({ icon: 'none', title: this.selectionError })
					return
				}
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
							title: res.msg || '获取预订及入住须知失败'
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
                this.syncPartySelection()
                if (!this.partyQuoteError) this.syncSkuSelection()
				const error = this.selectionError || this.eligibilityConfirmationError
				if (error) {
					uni.showToast({ icon: 'none', title: error })
					return
				}
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
						title:'请阅读预订及入住须知'
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
				const combo = this.currentCombo
				if (!combo) {
					uni.showToast({
						icon: 'none',
						title: '暂无供餐套餐配置'
					})
					return
				}
				this.$u.throttle(() => {
					const error = this.selectionError || this.eligibilityConfirmationError
					if (error) {
						uni.showToast({ icon: 'none', title: error })
						return
					}
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
					if (this.eligibilityNotice) {
						params.params = { travelEligibilityConfirmed: true }
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
