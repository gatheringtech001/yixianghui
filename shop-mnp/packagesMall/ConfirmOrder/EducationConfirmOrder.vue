<template>
	<view class="page_view">
		<scroll-view
			class="page_scroll"
			:style="{ height: pageScrollHeight + 'px' }"
			scroll-y
			:show-scrollbar="false"
		>
			<view v-if="goodsDetail">
				<view class="course-card">
					<image :src="coverUrl" mode="aspectFill" />
					<view class="course-body">
						<view class="course-name">{{ goodsDetail.goodsName }}</view>
						<view class="course-summary" v-if="courseSummary">{{ courseSummary }}</view>
					</view>
				</view>

				<view class="info-card" v-if="hasCourseInfo">
					<view class="card-title">{{ labels.courseInfo }}</view>
					<view class="info-grid">
						<view class="info-item" v-if="educationExt.courseTime">
							<text class="info-label">{{ labels.courseTime }}</text>
							<text class="info-value">{{ educationExt.courseTime }}</text>
						</view>
						<view class="info-item" v-if="educationExt.coursePlace">
							<text class="info-label">{{ labels.coursePlace }}</text>
							<text class="info-value">{{ educationExt.coursePlace }}</text>
						</view>
						<view class="info-item" v-if="educationExt.teacherName">
							<text class="info-label">{{ labels.courseTeacher }}</text>
							<text class="info-value">{{ educationExt.teacherName }}</text>
						</view>
						<view class="info-item" v-if="educationExt.lessonCount">
							<text class="info-label">{{ labels.lessonCount }}</text>
							<text class="info-value">{{ educationExt.lessonCount }}{{ labels.lessonUnit }}</text>
						</view>
						<view class="info-item" v-if="educationExt.startDate">
							<text class="info-label">{{ labels.startDate }}</text>
							<text class="info-value">{{ educationExt.startDate }}</text>
						</view>
						<view class="info-item" v-if="signupRangeText">
							<text class="info-label">{{ labels.signupRange }}</text>
							<text class="info-value">{{ signupRangeText }}</text>
						</view>
					</view>
				</view>

				<view class="form-card">
					<view class="card-title">{{ labels.signupInfo }}</view>
					<view class="form-row">
						<view class="row-label">{{ labels.contactName }}</view>
						<u-input v-model="contactName" type="text" border :placeholder="labels.contactNamePlaceholder" />
					</view>
					<view class="form-row">
						<view class="row-label">{{ labels.contactPhone }}</view>
						<u-input v-model="contactPhone" type="number" maxlength="11" border :placeholder="labels.contactPhonePlaceholder" />
						<view class="row-hint">{{ labels.contactHint }}</view>
					</view>
					<view class="form-row">
						<view class="row-label">{{ labels.remark }}</view>
						<u-input v-model="remark" type="textarea" border :placeholder="labels.remarkPlaceholder" />
					</view>
					<view class="agreement-row" @click="agreedNotice = !agreedNotice">
						<view class="agreement-check" @click.stop>
							<u-checkbox v-model="agreedNotice"></u-checkbox>
						</view>
						<view class="agreement-text">
							{{ labels.agreementPrefix }}
							<text class="agreement-link" @click.stop="openNoticePopup">{{ labels.signupNotice }}</text>
						</view>
					</view>
				</view>

				<view class="price-card">
					<view class="card-title">{{ labels.priceDetail }}</view>
					<view class="price-list">
						<view class="price-item">
							<text>{{ labels.listPrice }}</text>
							<text>{{ currencySymbol }}{{ listPrice }}</text>
						</view>
						<view class="price-item discount" v-if="memberDiscount > 0">
							<text>{{ labels.memberDiscount }}</text>
							<text>-{{ currencySymbol }}{{ memberDiscountText }}</text>
						</view>
						<view class="price-item total">
							<text>{{ labels.payAmount }}</text>
							<text class="value">{{ currencySymbol }}{{ payAmountText }}</text>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<view class="page_foot_view" v-if="goodsDetail">
			<view class="foot_price_view">
				<view class="price_view">
					{{ currencySymbol }}<text>{{ payAmountText }}</text>
				</view>
				<view class="text_view">{{ labels.payAmount }}</view>
			</view>
			<view class="pay_button_view" @click="submitOrder">{{ labels.goPay }}</view>
		</view>

		<u-popup v-model="noticePopup.show" @touchmove.stop.prevent mode="bottom" border-radius="20" :closeable="true">
			<view class="notice_popup_view">
				<view class="notice_title_view">{{ noticePopup.title }}</view>
				<scroll-view scroll-y class="notice_content_view">
					<u-parse :html="noticePopup.content" />
				</scroll-view>
			</view>
		</u-popup>
	</view>
</template>

<script>
	import { getGoodsInfo } from '@/api/shop/index'
	import { createOrder } from '@/api/member/index'
	import { prepareRichTextHtml } from '@/utils/richText'
	import { parseCourseMeta } from '@/utils/courseMeta'
	import { getCourseSignupPhase } from '@/utils/courseSignup'

	export default {
		data() {
			return {
				host: this.$host,
				pageScrollHeight: 0,
				goodsId: null,
				goodsDetail: null,
				educationExt: {},
				courseSummary: '',
				contactName: '',
				contactPhone: '',
				remark: '',
				agreedNotice: false,
				signupNoticeHtml: '',
				currencySymbol: '\uFFE5',
				noticePopup: {
					show: false,
					title: '',
					content: ''
				},
				labels: {
					courseInfo: '\u8bfe\u7a0b\u4fe1\u606f',
					courseTime: '\u4e0a\u8bfe\u65f6\u95f4',
					coursePlace: '\u6388\u8bfe\u5730\u70b9',
					courseTeacher: '\u6388\u8bfe\u8001\u5e08',
					lessonCount: '\u8bfe\u6b21',
					lessonUnit: '\u6b21',
					startDate: '\u5f00\u8bfe\u65e5\u671f',
					signupRange: '\u62a5\u540d\u65f6\u95f4',
					signupInfo: '\u62a5\u540d\u4fe1\u606f',
					contactName: '\u8054\u7cfb\u59d3\u540d',
					contactNamePlaceholder: '\u8bf7\u8f93\u5165\u62a5\u540d\u59d3\u540d',
					contactPhone: '\u8054\u7cfb\u7535\u8bdd',
					contactPhonePlaceholder: '\u8bf7\u8f93\u5165\u624b\u673a\u53f7',
					contactHint: '\u65b9\u4fbf\u8001\u5e74\u5b66\u6821\u4e0e\u60a8\u8054\u7cfb\u786e\u8ba4\u62a5\u540d\u4fe1\u606f',
					remark: '\u7559\u8a00',
					remarkPlaceholder: '\u9009\u586b\uff0c\u5982\u6709\u7279\u6b8a\u60c5\u51b5\u53ef\u7559\u8a00',
					agreementPrefix: '\u6211\u5df2\u9605\u8bfb',
					signupNotice: '\u300a\u62a5\u540d\u987b\u77e5\u300b',
					priceDetail: '\u8d39\u7528\u660e\u7ec6',
					listPrice: '\u8bfe\u7a0b\u5b66\u8d39',
					memberDiscount: '\u4f1a\u5458\u4f18\u60e0',
					payAmount: '\u5b9e\u4ed8\u91d1\u989d',
					goPay: '\u63d0\u4ea4\u62a5\u540d'
				}
			}
		},
		computed: {
			coverUrl() {
				if (!this.goodsDetail) return ''
				const cover = this.goodsDetail.goodsCover
					|| (this.goodsDetail.goodsImages && String(this.goodsDetail.goodsImages).split(',')[0])
					|| ''
				if (!cover) return '/static/home-design/entry-stay.jpg'
				if (cover.startsWith('http') || cover.startsWith('/static/')) return cover.startsWith('/') ? cover : cover
				return this.host + cover
			},
			hasCourseInfo() {
				const ext = this.educationExt || {}
				return !!(ext.courseTime || ext.coursePlace || ext.teacherName || ext.lessonCount || ext.startDate || this.signupRangeText)
			},
			signupRangeText() {
				const ext = this.educationExt || {}
				if (ext.signupStart && ext.signupEnd) {
					return `${ext.signupStart} ~ ${ext.signupEnd}`
				}
				if (ext.signupStart) return ext.signupStart
				if (ext.signupEnd) return ext.signupEnd
				return ''
			},
			listPrice() {
				return this.formatMoney(this.goodsDetail && this.goodsDetail.price)
			},
			payAmount() {
				if (!this.goodsDetail) return 0
				const vipPrice = Number(this.goodsDetail.vipPrice)
				const price = Number(this.goodsDetail.price)
				if (vipPrice > 0) return vipPrice
				return price
			},
			payAmountText() {
				return this.formatMoney(this.payAmount)
			},
			memberDiscount() {
				if (!this.goodsDetail) return 0
				const price = Number(this.goodsDetail.price) || 0
				const vipPrice = Number(this.goodsDetail.vipPrice) || 0
				if (vipPrice > 0 && price > vipPrice) {
					return price - vipPrice
				}
				return 0
			},
			memberDiscountText() {
				return this.formatMoney(this.memberDiscount)
			}
		},
		onLoad(options) {
			const userInfo = uni.getStorageSync('userInfo')
			if (!userInfo || userInfo === '' || userInfo === undefined) {
				uni.showToast({
					icon: 'none',
					title: '\u62a5\u540d\u8bf7\u5148\u767b\u5f55'
				})
				setTimeout(() => {
					uni.navigateTo({
						url: '/packagesPublic/login/login'
					})
				}, 1500)
				return
			}
			if (!options.id) return
			this.goodsId = options.id
			this.prefillContactInfo()
			this.loadGoodsDetail(options.id)
		},
		onReady() {
			this.setPageScrollHeight()
		},
		methods: {
			setPageScrollHeight() {
				this.$nextTick(() => {
					const windowInfo = uni.getWindowInfo ? uni.getWindowInfo() : uni.getSystemInfoSync()
					const footH = uni.upx2px(160) + (windowInfo.safeAreaInsets ? windowInfo.safeAreaInsets.bottom : 0)
					this.pageScrollHeight = windowInfo.windowHeight - footH
				})
			},
			prefillContactInfo() {
				const userInfo = uni.getStorageSync('userInfo')
				if (!userInfo) return
				this.contactName = userInfo.nickName || ''
				this.contactPhone = userInfo.phonenumber || userInfo.mobile || ''
			},
			formatMoney(value) {
				const num = Number(value)
				if (!num && num !== 0) return '0.00'
				return num.toFixed(2)
			},
			loadGoodsDetail(id) {
				getGoodsInfo(id).then(res => {
					const data = res.data || {}
					if (data.goodsType !== 'education') {
						uni.redirectTo({
							url: `/packagesMall/ConfirmOrder/ConfirmOrder?id=${id}&dataId=0`
						})
						return
					}
					this.goodsDetail = data
					this.educationExt = data.educationExt || {}
					this.courseSummary = parseCourseMeta(data.description).summary
					this.signupNoticeHtml = this.resolveSignupNotice(data.features || [])
					this.$nextTick(() => {
						this.setPageScrollHeight()
					})
				}).catch(() => {
					uni.showToast({
						title: '\u8bfe\u7a0b\u4fe1\u606f\u52a0\u8f7d\u5931\u8d25',
						icon: 'none'
					})
				})
			},
			resolveSignupNotice(features) {
				const noticeFeature = features.find(item => item.sectionId === 'signup_notice')
					|| features.find(item => String(item.sectionName || '').indexOf('\u62a5\u540d\u987b\u77e5') > -1)
				if (!noticeFeature || !noticeFeature.content) {
					return `<p>${'\u8bf7\u5728\u540e\u53f0\u5546\u54c1\u7279\u8272\u4e2d\u914d\u7f6e\u300a\u62a5\u540d\u987b\u77e5\u300b\u5185\u5bb9\u3002'}</p>`
				}
				return prepareRichTextHtml(noticeFeature.content, this.host)
			},
			getSignupPeriodError() {
				const phase = getCourseSignupPhase(this.educationExt || {})
				if (phase === 'not_start') return '报名尚未开始'
				if (phase === 'closed') return '报名时间已截止'
				return ''
			},
			openNoticePopup() {
				this.noticePopup.title = this.labels.signupNotice
				this.noticePopup.content = this.signupNoticeHtml
				this.noticePopup.show = true
			},
			validateForm() {
				if (!this.goodsDetail) {
					return '\u8bfe\u7a0b\u4fe1\u606f\u672a\u52a0\u8f7d'
				}
				if (String(this.goodsDetail.status) !== '1') {
					return '\u8bfe\u7a0b\u672a\u4e0a\u67b6\uff0c\u6682\u4e0d\u53ef\u62a5\u540d'
				}
				const signupError = this.getSignupPeriodError()
				if (signupError) {
					return signupError
				}
				const stock = Number(this.goodsDetail.stock)
				if (stock <= 0) {
					return '\u62a5\u540d\u540d\u989d\u5df2\u6ee1'
				}
				if (!this.contactName.trim()) {
					return this.labels.contactNamePlaceholder
				}
				if (!/^1\d{10}$/.test(String(this.contactPhone).trim())) {
					return '\u8bf7\u8f93\u5165\u6b63\u786e\u53f7\u7801'
				}
				if (!this.agreedNotice) {
					return '\u8bf7\u9605\u8bfb\u5e76\u540c\u610f\u62a5\u540d\u987b\u77e5'
				}
				return ''
			},
			submitOrder() {
				const errorText = this.validateForm()
				if (errorText) {
					uni.showToast({
						title: errorText,
						icon: 'none'
					})
					return
				}
				this.$u.throttle(() => {
					const params = {
						addressId: '0',
						goodsCount: 1,
						goodsId: this.goodsDetail.goodsId,
						couponGotIds: '',
						checkInDate: '',
						checkOutDate: '',
						skuDataId: '',
						contactName: this.contactName.trim(),
						contactPhone: String(this.contactPhone).trim(),
						remark: this.remark.trim()
					}
					createOrder(params).then(res => {
						if (!res || res.code !== 200 || !res.data || !res.data.orderId) {
							uni.showToast({
								title: (res && res.msg) || (typeof res.data === 'string' ? res.data : '\u8ba2\u5355\u521b\u5efa\u5931\u8d25'),
								icon: 'none'
							})
							return
						}
						uni.showToast({
							icon: 'none',
							title: '\u62a5\u540d\u8ba2\u5355\u521b\u5efa\u6210\u529f'
						})
						setTimeout(() => {
							uni.redirectTo({
								url: `/packagesMall/CashierDesk/CashierDesk?orderAmount=${res.data.moneyPayable}&orderId=${res.data.orderId}&orderNo=${res.data.orderNo}`
							})
						}, 1200)
					}).catch((err) => {
						uni.showToast({
							title: (err && err.message) || '\u8ba2\u5355\u521b\u5efa\u5931\u8d25',
							icon: 'none'
						})
					})
				}, 500)
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'EducationConfirmOrder.scss';
</style>
