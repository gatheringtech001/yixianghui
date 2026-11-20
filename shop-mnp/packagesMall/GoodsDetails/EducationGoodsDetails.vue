<template>
	<view :class="displayModeClass" class="page_container">
		<u-navbar
			class="weapp-nav-box detail-nav"
			:is-back="true"
			:title="labels.pageTitle"
			title-color="#111111"
			:title-size="40"
			:title-bold="true"
			:title-width="520"
			:background="{ background: '#EFEBDF' }"
			:border-bottom="true"
		/>
		<scroll-view
			class="page_scroll"
			:style="{ height: pageScrollHeight + 'px' }"
			scroll-y
			:show-scrollbar="false"
		>
			<view v-if="loading || loadError" class="course-state" role="status">
				<text class="state-title">{{ loading ? '正在加载课程详情…' : loadError }}</text>
				<text v-if="!loading" class="state-description">您可以重新加载，或返回课程列表选择其他课程。</text>
				<button v-if="!loading && courseId" class="state-primary" @tap="getCourseDetail(courseId)">重新加载</button>
				<button v-if="!loading" class="state-secondary" @tap="backToCourses">返回课程列表</button>
			</view>
			<view v-else-if="courseData" class="course-body">
				<view class="course-detail-hero">
					<image :src="resolveCoverUrl(courseData.cover)" mode="aspectFill" @tap.stop="previewDetailImages(resolveCoverUrl(courseData.cover))" />
					<view class="course-detail-copy">
						<text class="eyebrow">{{ labels.eyebrow }}</text>
						<view class="course-title-row">
							<text class="status-pill" :class="{ muted: !canApply }">{{ signupStatusText }}</text>
							<text class="course-name">{{ courseData.name }}</text>
							<view
								class="btn-collect"
								:class="{ collected: !!collectId }"
								:aria-label="collectId ? '取消收藏' : '收藏课程'"
								@click.stop="toggleCollect"
							><u-icon :name="collectId ? 'heart-fill' : 'heart'" color="#701018" size="32" /><text>{{ collectId ? '已收藏' : '收藏' }}</text></view>
						</view>
						<text class="course-desc" v-if="courseData.summary">{{ courseData.summary }}</text>
						<text class="course-sales" v-if="courseData.paidSignupCount !== null">已售 {{ courseData.paidSignupCount }} 人</text>
						<view class="detail-tags" v-if="courseData.tagList.length">
							<text class="outline-chip" v-for="(tag, index) in courseData.tagList" :key="index">{{ tag }}</text>
						</view>
					</view>
				</view>

				<view class="course-info-grid">
					<view class="info-item" v-if="signupRangeText">
						<text class="info-label">{{ labels.signupRange }}</text>
						<text class="info-value">{{ signupRangeText }}</text>
					</view>
					<view class="info-item" v-if="courseData.courseTime">
						<text class="info-label">{{ labels.courseTime }}</text>
						<text class="info-value">{{ courseData.courseTime }}</text>
					</view>
					<view class="info-item" v-if="courseData.coursePlace">
						<text class="info-label">{{ labels.coursePlace }}</text>
						<text class="info-value">{{ courseData.coursePlace }}</text>
					</view>
					<view class="info-item" v-if="courseData.courseTeacher">
						<text class="info-label">{{ labels.courseTeacher }}</text>
						<text class="info-value">{{ courseData.courseTeacher }}</text>
					</view>
					<view class="info-item">
						<text class="info-label">{{ labels.coursePrice }}</text>
						<text class="info-value price">{{ currencySymbol }}{{ courseData.price }}</text>
					</view>
					<view class="info-item" v-if="courseData.unit">
						<text class="info-label">{{ labels.courseUnit }}</text>
						<text class="info-value">{{ courseData.unit }}</text>
					</view>
					<view class="info-item" v-if="courseData.materialNote">
						<text class="info-label">{{ labels.materialNote }}</text>
						<text class="info-value">{{ courseData.materialNote }}</text>
					</view>
				</view>

				<view class="course-section" v-for="(section, index) in courseData.sections" :key="index">
					<view class="section-title">{{ section.name }}</view>
					<view class="section-content">
						<u-parse :html="displayRichText(section.content)" :preview="true" />
					</view>
				</view>
			</view>
		</scroll-view>
		<view class="bottom-action" v-if="courseData">
			<button class="share-btn" open-type="share">
				<u-icon name="share-fill" color="#701018" size="32"></u-icon>
				<text class="btn-label">分享</text>
			</button>
			<view class="btn secondary" @click="callPhone">{{ labels.consultPhone }}</view>
			<view class="btn primary" :class="{ disabled: !canApply }" @click="applyCourse">{{ applyButtonText }}</view>
		</view>
		<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>

<script>
	import { getGoodsInfo, getGoodsCatrgorys } from '@/api/shop/index'
	import { goodsCollect, deleteCollect, goodsCollectList } from '@/api/member/index'
	import { prepareRichTextHtml } from '@/utils/richText'
	import { parseCourseMeta } from '@/utils/courseMeta'
	import {
		getCourseSignupPhase,
		getCourseSignupPhaseText,
		getCourseApplyButtonText
	} from '@/utils/courseSignup'
	import { runWithAuth, bindPageAuthPopup } from '@/utils/login'
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	import { parseInvitePageOptions } from '@/utils/invite'
	import sharePageMixin from '@/utils/sharePageMixin'
	import { previewDetailImages } from '@/utils/detailImagePreview'

	const DEFAULT_COVER = '/static/home-design/entry-stay.jpg'
	const SECTION_CONTENT = '\u8bfe\u7a0b\u5185\u5bb9'

	export default {
		mixins: [sharePageMixin],
		components: {
			AuthProfilePopup
		},
		data() {
			return {
				host: this.$host,
				pageScrollHeight: 0,
				consultPhone: '13764363947',
				courseData: null,
				courseId: '',
				loading: false,
				loadError: '',
				signupPhase: 'open',
				currencySymbol: '\uFFE5',
				collectId: null,
				labels: {
					pageTitle: '\u8bfe\u7a0b\u8be6\u60c5',
					eyebrow: '芳华学院',
					courseTime: '\u4e0a\u8bfe\u65f6\u95f4',
					coursePlace: '\u6388\u8bfe\u5730\u70b9',
					courseTeacher: '\u6388\u8bfe\u8001\u5e08',
					coursePrice: '\u8bfe\u7a0b\u5b66\u8d39',
					courseUnit: '\u8bfe\u7a0b\u89c4\u683c',
					materialNote: '\u6750\u6599\u5907\u6ce8',
					signupRange: '\u62a5\u540d\u65f6\u95f4',
					consultPhone: '\u54a8\u8be2\u7535\u8bdd',
					applyNow: '\u7acb\u5373\u62a5\u540d'
				}
			}
		},
		computed: {
			canApply() {
				return this.signupPhase === 'open'
			},
			signupStatusText() {
				return getCourseSignupPhaseText(this.signupPhase)
			},
			applyButtonText() {
				return getCourseApplyButtonText(this.signupPhase)
			},
			signupRangeText() {
				if (!this.courseData) return ''
				const start = this.courseData.signupStart
				const end = this.courseData.signupEnd || this.courseData.startDate
				if (start && end) return `${start} ~ ${end}`
				if (start) return start
				if (end) return end
				return ''
			}
		},
		onLoad(options = {}) {
			parseInvitePageOptions(options)
			return this.getCourseDetail(options.id || options.goodsId)
		},
		onShow() {
			bindPageAuthPopup(this)
			this.loadCollectState()
			if (this.courseData) {
				this.refreshSignupPhase()
			}
		},
		onResize() {
			this.setPageScrollHeight()
		},
		onReady() {
			this.setPageScrollHeight()
		},
		methods: {
			previewDetailImages(current, images) {
				return previewDetailImages.call(this, current, images)
			},
			getShareConfig() {
				const course = this.courseData || {}
				return {
					title: course.name || '逸享荟芳华学院课程',
					path: course.id ? '/packagesMall/GoodsDetails/EducationGoodsDetails' : '/pages/classify/classify',
					query: course.id ? { id: course.id } : {},
					imageUrl: course.cover ? this.resolveCoverUrl(course.cover) : ''
				}
			},
			refreshSignupPhase() {
				if (!this.courseData) {
					this.signupPhase = 'open'
					return
				}
				this.signupPhase = getCourseSignupPhase({
					signupStart: this.courseData.signupStart,
					signupEnd: this.courseData.signupEnd,
					startDate: this.courseData.startDate
				})
			},
			setPageScrollHeight() {
				this.$nextTick(() => {
					const query = uni.createSelectorQuery().in(this)
					query.select('.page_scroll').boundingClientRect()
					query.select('.bottom-action').boundingClientRect()
					query.exec((res) => {
						const windowInfo = uni.getWindowInfo ? uni.getWindowInfo() : uni.getSystemInfoSync()
						// Use viewport coordinates, not component-host heights: navbar placeholders
						// and safe areas must not be subtracted a second time on real phones.
						const scrollTop = res[0] && Number.isFinite(res[0].top)
							? res[0].top : (windowInfo.statusBarHeight || 0) + 44
						const bottom = res[1] && Number.isFinite(res[1].top)
							? res[1].top : windowInfo.windowHeight - (this.courseData ? uni.upx2px(128) + ((windowInfo.safeAreaInsets || {}).bottom || 0) : 0)
						this.pageScrollHeight = Math.max(0, bottom - scrollTop)
					})
				})
			},
			resolveCoverUrl(url) {
				if (!url) return DEFAULT_COVER
				const normalized = String(url).replace(/^@\//, '/')
				if (normalized.startsWith('data:image') || normalized.startsWith('http')) {
					return normalized
				}
				if (normalized.startsWith('/static/')) {
					return normalized
				}
				if (normalized.startsWith('/')) {
					return this.host + normalized
				}
				return `${this.host}/${normalized}`
			},
			async loadCollectState() {
				const token = uni.getStorageSync('token')
				const userInfo = uni.getStorageSync('userInfo')
				const goodsId = this.courseData && this.courseData.id
				if (!token || !userInfo || !goodsId) {
					this.collectId = null
					return
				}
				try {
					const res = await goodsCollectList({ pageNum: 1, pageSize: 500 })
					const rows = (res && res.rows) || []
					const item = rows.find(v => {
						const type = v.collectType || 'goods'
						return type !== 'activity' && String(v.goodsId) === String(goodsId)
					})
					this.collectId = item ? item.collectId : null
				} catch (e) {
					console.warn('loadCollectState failed', e)
				}
			},
			toggleCollect() {
				if (!this.courseData || !this.courseData.id) return
				runWithAuth(this, (ok) => {
					if (!ok) return
					if (this.collectId) {
						deleteCollect({ collectId: this.collectId }).then(() => {
							this.collectId = null
							uni.showToast({ title: '已取消收藏', icon: 'none' })
						}).catch(err => {
							uni.showToast({ title: (err && err.message) || '取消失败', icon: 'none' })
						})
						return
					}
					goodsCollect({
						collectType: 'goods',
						goodsId: this.courseData.id
					}).then(res => {
						const data = (res && res.data) || {}
						if (data.collectId) {
							this.collectId = data.collectId
						} else {
							this.loadCollectState()
						}
						uni.showToast({ title: '收藏成功', icon: 'none' })
					}).catch(err => {
						uni.showToast({ title: (err && err.message) || '收藏失败', icon: 'none' })
					})
				})
			},
			async backToCourses() {
				let categories = uni.getStorageSync('cls') || []
				if (!Array.isArray(categories) || !categories.length) {
					try {
						const result = await getGoodsCatrgorys({})
						categories = Array.isArray(result && result.data) ? result.data : []
						if (categories.length) uni.setStorageSync('cls', categories)
					} catch (error) {
						// The service page also provides category retry when the network is unavailable.
						categories = []
					}
				}
				const education = Array.isArray(categories) && categories.find(item => (
					item && item.parentId == 0 && ['老年教育', '芳华学院'].includes(String(item.categoryName || '').trim())
				))
				if (education) uni.setStorageSync('currentCls', education.categoryId)
				uni.switchTab({ url: '/pages/classify/classify' })
			},
			getCourseDetail(id) {
				if (this.loading) return
				this.courseId = /^\d+$/.test(String(id || '')) && Number(id) > 0 ? String(id) : ''
				this.courseData = null
				this.collectId = null
				this.loadError = ''
				if (!this.courseId) {
					this.loadError = '未找到课程信息，请从课程列表重新进入'
					return
				}
				this.loading = true
				return getGoodsInfo(this.courseId).then(res => {
					const data = res && res.data
					if (!data || !data.goodsId || !data.goodsName) {
						this.loadError = '该课程暂不可查看，请选择其他课程'
						return
					}
					const ext = data.educationExt || {}
					const courseMeta = parseCourseMeta(data.description, { includeNote: true })
					let cover = ''
					if (data.goodsCover) {
						cover = data.goodsCover
					} else if (data.goodsImages) {
						cover = String(data.goodsImages).split(',')[0]
					}
					const tagList = data.tags ? String(data.tags).split(/[,，|]/).filter(Boolean) : []
					const sections = (Array.isArray(data.features) ? data.features : []).filter(Boolean).map(feature => ({
						name: feature.sectionName,
						content: prepareRichTextHtml(feature.content, this.host)
					}))
					if (!sections.length && data.content) {
						sections.push({
							name: SECTION_CONTENT,
							content: prepareRichTextHtml(data.content, this.host)
						})
					}
					this.courseData = {
						id: data.goodsId,
						name: data.goodsName,
						summary: courseMeta.summary,
						courseTime: ext.courseTime || courseMeta.time,
						coursePlace: ext.coursePlace || courseMeta.place,
						courseTeacher: ext.teacherName || courseMeta.teacher,
						materialNote: ext.materialNote || courseMeta.note,
						signupStart: ext.signupStart || '',
						signupEnd: ext.signupEnd || '',
						startDate: ext.startDate || '',
						price: data.vipPrice || data.price,
						paidSignupCount: data.paidSignupCount != null && String(data.paidSignupCount).trim() !== '' && Number.isFinite(Number(data.paidSignupCount))
							? Math.max(0, Math.floor(Number(data.paidSignupCount))) : null,
						unit: data.unit,
						cover,
						tagList,
						sections
					}
					this.refreshSignupPhase()
					this.loadCollectState()
					if (ext.consultPhone) {
						this.consultPhone = ext.consultPhone
					}
					this.$nextTick(() => {
						this.setPageScrollHeight()
					})
				}).catch(() => {
					this.courseData = null
					this.loadError = '课程详情加载失败，请检查网络后重试'
				}).finally(() => {
					this.loading = false
					this.setPageScrollHeight()
				})
			},
			callPhone() {
				uni.makePhoneCall({
					phoneNumber: this.consultPhone
				})
			},
			applyCourse() {
				if (!this.canApply) {
					uni.showToast({
						icon: 'none',
						title: this.signupStatusText
					})
					return
				}
				runWithAuth(this, (ok) => {
					if (!ok || !this.canApply || !this.courseData || !this.courseData.id) return
					uni.navigateTo({
						url: `/packagesMall/ConfirmOrder/EducationConfirmOrder?id=${this.courseData.id}`
					})
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'EducationGoodsDetails.scss';
</style>
