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
						placeholder="搜索商品"
						confirm-type="search"
						@input="onKeywordInput"
						@confirm="onSearch"
					/>
					<text class="search-action" @click="onSearch">搜索</text>
				</view>
			</view>
			<view class="search-empty" v-if="showSearchEmpty">
				<text>您搜索的<text class="search-keyword">{{ emptySearchKeyword }}</text>找不到</text>
			</view>
			<view class="search-result" v-if="showSearchResult">
				<view
					class="goods_item_view"
					v-for="(item, index) in goodsList"
					:key="item.goodsId || index"
					:data-goods-id="item.goodsId"
					:data-goods-type="item.goodsType || ''"
					@tap.stop="goodsFn"
				>
					<image :src="host + item.image" mode="aspectFill" />
					<view class="item_content_view">
						<view class="content_title_view">{{ item.goodsName }}</view>
						<view class="content_desc_view">{{ item.description }}</view>
						<view class="content_tag_view" v-if="item.tagList && item.tagList.length">
							<view class="tag_view" v-for="(tag, tagIndex) in item.tagList" :key="tagIndex">
								{{ tag }}
							</view>
						</view>
						<view class="content_price_view">
							<view class="price_view">
								<text>￥<text>{{ item.price }}</text></text>
							</view>
							<view class="button_view">查看详情</view>
						</view>
					</view>
				</view>
			</view>
			<view class="search-record" v-if="!showSearchEmpty && !showSearchResult">
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
				goodsList: [],
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
			getServiceCategoryId() {
				const navList = (uni.getStorageSync('cls') || []).filter(v => v.parentId == 0)
				const travelNav = navList.find(v => v.categoryName === '全国旅居')
				if (travelNav) {
					return travelNav.categoryId
				}
				return navList.length ? navList[0].categoryId : null
			},
			parseGoodsList(data) {
				const list = []
				;(data || []).forEach((item) => {
					let tagList = []
					let image = ''
					if (item.tags) {
						tagList = item.tags.split(/[,|，]/)
					}
					if (item.goodsImages) {
						image = item.goodsImages.split(',')[0]
					}
					list.push({
						goodsId: item.goodsId,
						goodsName: item.goodsName,
						tagList,
						image,
						price: item.price,
						unit: item.unit,
						goodsType: item.goodsType,
						description: item.description
					})
				})
				return list
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
				this.SearchRecordArr = arr
				uni.setStorageSync('SearchRecordArr', JSON.stringify(arr))
			},
			async doSearch(keyword) {
				if (this.searching) return
				this.showSearchEmpty = false
				this.showSearchResult = false
				this.goodsList = []
				const categoryId = this.getServiceCategoryId()
				if (!categoryId) {
					uni.showToast({
						title: '分类数据未加载，请先进入服务页',
						icon: 'none'
					})
					return
				}
				this.searching = true
				uni.showLoading({ title: '搜索中...', mask: true })
				try {
					const res = await getGoodsList({
						categoryId,
						goodsName: keyword,
						ignoreSite: true
					})
					const list = this.parseGoodsList(res.data)
					if (list.length > 0) {
						this.goodsList = list
						this.showSearchResult = true
					} else {
						this.emptySearchKeyword = keyword
						this.showSearchEmpty = true
					}
				} catch (e) {
					this.emptySearchKeyword = keyword
					this.showSearchEmpty = true
				} finally {
					this.searching = false
					uni.hideLoading({ noConflict: true })
				}
			},
			onKeywordInput() {
				this.showSearchEmpty = false
				this.showSearchResult = false
				this.goodsList = []
			},
			goodsFn(e) {
				let goodsId = null
				let goodsType = ''
				if (e && e.currentTarget && e.currentTarget.dataset) {
					const ds = e.currentTarget.dataset
					goodsId = ds.goodsId
					goodsType = ds.goodsType || ''
				} else if (e && e.goodsId) {
					goodsId = e.goodsId
					goodsType = e.goodsType || ''
				}
				if (!goodsId) {
					uni.showToast({
						title: '商品信息无效',
						icon: 'none'
					})
					return
				}
				if (goodsType === 'hotel') {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/SojournGoodsDetails?id=${goodsId}`
					})
				} else {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/GoodsDetails?id=${goodsId}`
					})
				}
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
