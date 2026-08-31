<template>
	<view class="page_container">
		<u-navbar
			class="weapp-nav-box detail-nav"
			:is-back="true"
			title="搜索"
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
			<view class="search-head">
				<view class="search-bar">
					<u-icon name="search" color="#8a8a8a" size="32" />
					<input
						class="search-input"
						type="text"
						v-model="keyword"
						placeholder="搜索旅居、活动和课程"
						confirm-type="search"
						@input="onKeywordInput"
						@confirm="onSearch"
					/>
					<text class="search-action" @click="onSearch">搜索</text>
				</view>
			</view>
			<view class="search-empty" v-if="searchError">
				<text>{{ searchError }}</text>
			</view>
			<view class="search-empty" v-else-if="showSearchEmpty">
				<text>您搜索的<text class="search-keyword">{{ emptySearchKeyword }}</text>找不到</text>
			</view>
			<view class="search-result" v-if="showSearchResult">
				<view class="result-section" v-for="group in resultGroups" :key="group.type">
					<view class="result-heading">
						<text>{{ group.label }}</text>
						<text class="result-count">{{ group.items.length }}项</text>
					</view>
					<view
						class="result-card"
						v-for="item in group.items"
						:key="item.id"
						@tap.stop="openResult(item)"
					>
						<image v-if="item.image" :src="imageUrl(item.image)" mode="aspectFill" />
						<view class="item_content_view">
							<view class="result-type">{{ group.label }}</view>
							<view class="content_title_view">{{ item.title }}</view>
							<view class="content_desc_view" v-if="item.description">{{ item.description }}</view>
							<view class="content_meta_view" v-if="item.meta">{{ item.meta }}</view>
							<view class="content_tag_view" v-if="item.tags.length">
								<view class="tag_view" v-for="tag in item.tags.slice(0, 3)" :key="tag">{{ tag }}</view>
							</view>
							<view class="content_price_view">
								<view class="price_view">{{ item.priceText }}</view>
								<view class="button_view">查看详情</view>
							</view>
						</view>
					</view>
				</view>
			</view>
			<view class="search-record" v-if="!searching && !searchError && !showSearchEmpty && !showSearchResult">
				<view class="search-title">
					<view class="title">搜索历史</view>
					<view class="iconfont icon-laji" @click="clearSearch"></view>
				</view>
				<view class="record-list">
					<view
						class="list"
						v-for="(item, index) in SearchRecordArr"
						:key="index"
						@click="onRecord(item)"
					>
						<text>{{ item }}</text>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { getGoodsList } from '@/api/shop/index'
	import { getActivityList } from '@/api/activity/index'

	export default {
		data() {
			return {
				host: this.$host,
				SearchRecordArr: [],
				keyword: '',
				searching: false,
				showSearchEmpty: false,
				showSearchResult: false,
				emptySearchKeyword: '',
				searchError: '',
				resultGroups: [],
				pageScrollHeight: 0
			};
		},
		onLoad() {
			if (uni.getStorageSync('SearchRecordArr')) {
				this.SearchRecordArr = JSON.parse(uni.getStorageSync('SearchRecordArr'));
			}
		},
		onReady() {
			this.setPageScrollHeight()
		},
		methods: {
			setPageScrollHeight() {
				this.$nextTick(() => {
					const query = uni.createSelectorQuery().in(this)
					query.select('.weapp-nav-box').boundingClientRect()
					query.exec((res) => {
						const sys = uni.getSystemInfoSync()
						const navH = (res[0] && res[0].height) || (sys.statusBarHeight + 44)
						this.pageScrollHeight = sys.windowHeight - navH
					})
				})
			},
			parseTags(tags) {
				return String(tags || '').split(/[,|，]/).map(v => v.trim()).filter(Boolean)
			},
			imageUrl(path) {
				if (!path || /^https?:\/\//.test(path)) return path
				return this.host + path
			},
			searchScore(item, keyword) {
				const value = keyword.toLowerCase()
				const title = String(item.title || '').toLowerCase()
				if (title === value) return 0
				if (title.startsWith(value)) return 1
				if (title.includes(value)) return 2
				if (item.tags.join(' ').toLowerCase().includes(value)) return 3
				return 4
			},
			sortResults(items, keyword) {
				return items.sort((left, right) => (
					this.searchScore(left, keyword) - this.searchScore(right, keyword)
					|| Number(right.id) - Number(left.id)
				))
			},
			buildGoodsResult(item) {
				const image = item.goodsCover || String(item.goodsImages || '').split(',')[0]
				const typeByGoodsType = {
					hotel: 'travel',
					education: 'education'
				}
				return {
					id: item.goodsId,
					type: typeByGoodsType[item.goodsType] || '',
					title: item.goodsName,
					description: item.description || '',
					meta: '',
					tags: this.parseTags(item.tags),
					image,
					priceText: `￥${item.vipPrice || item.price || 0}`
				}
			},
			buildActivityResult(item) {
				const isFree = item.isFree === 1 || item.isFree === '1' || item.isFree == null
				return {
					id: item.activityId,
					type: 'activity',
					title: item.activityName,
					description: item.description || '',
					meta: [item.activityTime, item.address].filter(Boolean).join(' · '),
					tags: this.parseTags(item.tags),
					image: item.activityCover,
					priceText: isFree ? '免费' : `￥${item.vipPrice || item.price || 0}`
				}
			},
			buildResultGroups(goods, activities, keyword) {
				const rows = (goods || []).map(item => this.buildGoodsResult(item)).filter(item => item.type)
				const definitions = [
					{ type: 'travel', label: '全国旅居', items: rows.filter(v => v.type === 'travel') },
					{ type: 'activity', label: '聚会活动', items: (activities || []).map(item => this.buildActivityResult(item)) },
					{ type: 'education', label: '老年教育', items: rows.filter(v => v.type === 'education') }
				]
				return definitions.map(group => ({
					...group,
					items: this.sortResults(group.items, keyword)
				})).filter(group => group.items.length)
			},
			onSearch() {
				const keyword = String(this.keyword || '').trim()
				if (!keyword) {
					uni.showToast({
						title: '请输入搜索内容',
						icon: 'none',
					})
					return;
				}
				this.saveSearchRecord(keyword)
				this.doSearch(keyword)
			},
			saveSearchRecord(keyword) {
				const arr = this.SearchRecordArr.filter(item => item !== keyword)
				arr.unshift(keyword)
				this.SearchRecordArr = arr.slice(0, 10)
				uni.setStorageSync('SearchRecordArr', JSON.stringify(this.SearchRecordArr))
			},
			async doSearch(keyword) {
				if (this.searching) return
				this.showSearchEmpty = false
				this.showSearchResult = false
				this.searchError = ''
				this.resultGroups = []
				this.searching = true
				uni.showLoading({ title: '搜索中...', mask: true })
				try {
					let goodsFailed = false
					let activityFailed = false
					const [goodsResponse, activityResponse] = await Promise.all([
						getGoodsList({ goodsName: keyword, ignoreSite: true }).catch(() => {
							goodsFailed = true
							return { data: [] }
						}),
						getActivityList({ activityName: keyword, signFilter: 'active' }).catch(() => {
							activityFailed = true
							return { rows: [] }
						})
					])
					if (goodsFailed && activityFailed) {
						this.searchError = '搜索服务暂不可用，请稍后重试'
						return
					}
					this.resultGroups = this.buildResultGroups(
						goodsResponse.data,
						activityResponse.rows,
						keyword
					)
					this.showSearchResult = this.resultGroups.length > 0
					this.showSearchEmpty = !this.showSearchResult
					this.emptySearchKeyword = keyword
					if (goodsFailed || activityFailed) {
						uni.showToast({ title: '部分结果加载失败', icon: 'none' })
					}
				} catch (e) {
					this.searchError = '搜索服务暂不可用，请稍后重试'
				} finally {
					this.searching = false
					uni.hideLoading({ noConflict: true })
				}
			},
			onKeywordInput() {
				this.showSearchEmpty = false
				this.showSearchResult = false
				this.searchError = ''
				this.resultGroups = []
			},
			openResult(item) {
				const paths = {
					travel: `/packagesMall/GoodsDetails/SojournGoodsDetails?id=${item.id}`,
					education: `/packagesMall/GoodsDetails/EducationGoodsDetails?id=${item.id}`,
					activity: `/packagesMall/Activity/detail/index?id=${item.id}`
				}
				if (paths[item.type]) uni.navigateTo({ url: paths[item.type] })
			},
			clearSearch() {
				uni.removeStorageSync('SearchRecordArr')
				this.SearchRecordArr = []
			},
			onRecord(val) {
				this.keyword = val
				this.saveSearchRecord(val)
				this.doSearch(val)
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'search.scss';
</style>
