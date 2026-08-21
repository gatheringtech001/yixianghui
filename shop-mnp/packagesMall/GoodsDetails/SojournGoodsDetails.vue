<template>
	<view class="page_container">
	<u-navbar
		class="weapp-nav-box detail-nav"
		:is-back="true"
		title="旅居商品详情"
		title-color="#111111"
		:title-size="40"
		:title-bold="true"
		:title-width="520"
		:background="{ background: '#EFEBDF' }"
		:border-bottom="true"
	/>
	<scroll-view
		class="page_view"
		:style="{ height: pageScrollHeight + 'px' }"
		:scroll-y="true"
		:scroll-top="scrollTop"
		@scroll="scrollFn"
		scroll-with-animation
	>
		<!-- 轮播图 -->
		<swiper class="banner_swiper" indicator-dots="true" autoplay="true" interval="5000" duration="500">
			<swiper-item v-for="(item, index) in bannerImages" :key="index">
				<image :src="host + item" mode="aspectFill" class="banner-image"></image>
			</swiper-item>
		</swiper>
		<view class="page_body_view">
			<view class="body_base_view">
				<view class="base_type_view">
					{{hotelData.type}}
				</view>
				<view class="base_title_row">
					<view class="base_name_view">{{hotelData.name}}</view>
					<view
						class="btn-collect"
						:class="{ collected: !!collectId }"
						@click.stop="toggleCollect"
					>{{ collectId ? '已收藏' : '收藏' }}</view>
				</view>
				<view class="base_desc_view">
					{{ formatDescText(hotelData.desc) }}
				</view>
				<view class="base_tag_view">
					<view class="tag_view" v-for="(item, index) in hotelData.tagList" :key="index">
						{{ item }}
					</view>
				</view>
			</view>
			<view class="body_related_view">
				<view class="related_view" :class="relatedSelect == item.id ? 'related_select' : ''"
					v-for="(item, index) in hotelData.related" :key="index" @click="scrollToSectionFn(item.id)">
					{{item.name}}
				</view>
			</view>
      <view class="body_sku_view" id="id_skuList">
        <view class="sku_box_view" v-for="(skuGroup, groupIndex) in skuGroupList" :key="groupIndex">
          <view class="sku_info_view">
            <image :src="resolveCoverUrl(skuGroup.cover)" mode="aspectFill" />
            <view class="info_text_view">
              <view class="text_title_view">{{skuGroup.title}}</view>
              <view class="text_desc_view">{{ formatDescText(skuGroup.descOne) }}</view>
              <view class="text_label_view">{{ formatDescText(skuGroup.descTwo) }}</view>
            </view>
          </view>
          <view class="sku_option_view" v-if="skuGroup.skuDataList && skuGroup.skuDataList.length > 0">
            <view class="option_view" :class="skuSelect == groupIndex + '_' + index?'option_select':''"
                  v-for="(item, index) in skuGroup.skuDataList" :key="index" @click="skuSelect = groupIndex + '_' + index">
              {{ item.name }}
            </view>
          </view>
          <view class="sku_list_view" v-if="skuGroup.skuDataList && skuGroup.skuDataList.length > 0">
            <view class="sku_item_view" v-for="(item, index) in getCurrentCombinationList(groupIndex)" :key="index">
              <view class="item_title_view">
                <view class="title_icon_view">餐</view>
                <view class="title_view">{{ item.name }}</view>
              </view>
              <view class="item_price_view">
                <view class="price_view">
                  <text><text>￥ </text>{{ item.price }}</text> /人/起
                </view>
                <view class="price_average_view" v-if="item.average">
                  均￥{{ item.average }}/人/晚起
                </view>
              </view>
              <view class="item_button_view" @click="reserveFn(getCurrentSkuData(groupIndex), item, groupIndex)">订</view>
            </view>
          </view>
          <view class="sku_empty_view" v-else>
            <text>暂无可预订套餐</text>
          </view>
        </view>
      </view>

      <view class="body_relateds_view" v-for="(item, index) in  hotelData.related" :key="index" :id="item.id">
				<view class="relateds_box_view">
					<view class="relateds_title_view">
						{{item.name}}
					</view>
					<view class="relateds_content_view">
						<u-parse :html="item.content" />
					</view>
					<view class="relateds_button_view" v-if="item.showToggle">
						<view v-if="item.isExpand" class="button_view" @click="expandFn(item)">
							收起<u-icon name="arrow-up"></u-icon>
						</view>
						<view class="button_view" v-else @click="expandFn(item)">
							展开查看全部<u-icon name="arrow-down"></u-icon>
						</view>
					</view>
				</view>
			</view>
		</view>
	</scroll-view>
	<view class="page_foot_view">
		<button class="share-btn" open-type="share">
			<u-icon name="share-fill" color="#701018" size="32"></u-icon>
			<text class="btn-label">分享</text>
		</button>
		<view class="foot_secondary_view" @click="customerServiceData.show = true">
			<text>联系客服</text>
		</view>
		<view class="foot_primary_view" @tap="scrollToSectionFn('id_skuList')">
			<text>查看房源</text>
		</view>
	</view>
	<!-- 弹层必须放在页面 scroll-view 外，避免部分机型高度/安全区计算异常导致底部按钮被裁切 -->
	<u-popup
		v-model="popupDate.show"
		mode="bottom"
		border-radius="20"
		:closeable="true"
		height="85%"
		:safe-area-inset-bottom="false"
	>
		<view class="popup_view">
			<view class="popup_header_view">
				<view class="popup_title_view">选择天数</view>
				<view class="popup_tag_view">
					<view class="tag_view" :class="popupDate.skuSelect == index ? 'tag_select':''"
						v-for="(item, index) in popupSkuDataList" :key="index" @click="selectDayFn(index)">
						{{item.name}}
					</view>
					<view class="tag_customize_view" :class="popupDate.skuSelect == 'customize' ? 'tag_select':''"
						@click="selectDayFn('customize')">自选晚数</view>
				</view>
				<view class="popup_customize_view" v-if="popupDate.skuSelect == 'customize'">
					自选入住时长
					<u-number-box v-model="popupDate.day" :min="7" disabled-input :input-width="200"
						:input-height="60" @change="onCustomDayChange"></u-number-box>
					晚
				</view>
			</view>
			<!-- 小程序 view 的 overflow 滚动不稳定，用 scroll-view 保证可滚 -->
			<scroll-view scroll-y="true" class="popup_calendar_view" :show-scrollbar="false">
				<Calendar ref="CalendarRef" :is-show="true" :isFixed="false" :price="calendarPriceText"
					:startDate="popupDate.checkInDate" :endDate="popupDate.checkOutDate"
					:fixedEnd="true" :mode="2" themeColor="#FF7906" @callback="calendarCallBackFn"
					@calendarClick="calendarClickFn" />
			</scroll-view>
			<view class="popup_footer_view">
				<view class="popup_button_view">
					<view class="button_clear_view" @click="clearFn">清空</view>
					<view class="button_confirm_view" @click="confirmFn">确认{{ stayDurationLabel }}</view>
				</view>
			</view>
		</view>
	</u-popup>
	<u-popup class="butler-popup" v-model="customerServiceData.show" @touchmove.stop.prevent mode="bottom"
		border-radius="16" :closeable="true" :safe-area-inset-bottom="true">
		<view class="popup-title">添加逸享荟小管家</view>
		<view class="items">
			<view class="item">
				<u-icon name="server-man" size="40" color="#00C800" />
				<text>1对1专属服务</text>
			</view>
			<view class="item">
				<u-icon name="heart-fill" size="40" color="#00C800" />
				<text>全程管家式服务</text>
			</view>
			<view class="item">
				<u-icon name="file-text-fill" size="40" color="#00C800" />
				<text>优质路线推荐</text>
			</view>
			<view class="item">
				<u-icon name="bill-fill" size="40" color="#00C800" />
				<text>优惠活动不错过</text>
			</view>
		</view>
		<view class="steps" v-if="customerServiceData.content[0]">
			<view class="title"><text></text>第一步</view>
			<view class="content">
				<image
					:src="getAdImageUrl(customerServiceData.content[0].adImage)"
					mode="aspectFit"
					show-menu-by-longpress
					@tap="previewQrImage(customerServiceData.content[0].adImage)"
				/>
				<view class="tips">长按识别二维码添加</view>
			</view>
		</view>
		<view class="steps" v-if="customerServiceData.content[1]">
			<view class="title"><text></text>第二步</view>
			<view class="content else">
				<image :src="getAdImageUrl(customerServiceData.content[1].adImage)" mode="widthFix" />
			</view>
		</view>
		<view class="steps" v-if="!customerServiceData.content.length">
			<view class="content">
				<view class="tips">客服信息暂未配置</view>
			</view>
		</view>
	</u-popup>
	<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>
<script>
	import Calendar from '@/components/mobile-calendar-simple/Calendar.vue'
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	import {
		getGoodsInfo,
		getGoodsSkuInfo
	} from '@/api/shop/index'
	import { goodsCollect, deleteCollect, goodsCollectList } from '@/api/member/index'
	import { runWithAuth, bindPageAuthPopup } from '@/utils/login'
	import { prepareRichTextHtml } from '@/utils/richText'
	import { formatCalendarPrice, resolveCalendarUnitPrice } from '@/utils/travelPresentation'
	import { parseInvitePageOptions } from '@/utils/invite'
	import sharePageMixin from '@/utils/sharePageMixin'

	const DEFAULT_SKU_COVER = '/static/home-design/entry-stay.jpg'
	import {
		getBannerList
	} from '@/api/index'
	export default {
		mixins: [sharePageMixin],
		components: {
			Calendar,
			AuthProfilePopup
		},
		data() {
			return {
				host: this.$host,
        skuGroupList: [],
				//skuGroupCover: '@/static/img/showver.jpg',
				//skuGroupTitle: '豪华双床房【城景，住满7晚含接或送机站1次】',
				//skuGroupDescOne: '此房型无阳台，标间1.35米*2，大床1.8米，如需大床预定标间后客服留大床',
				//skuGroupDescTwo: '2人标间 | 35~40㎡ | 2张1.35m床',
				bannerImages: [],
				hotelData: {
					id: undefined,
					name: '', // 名称
					type: '', // 分类
					desc: '', // 简介
					tagList: [], // 标签数组
					related: [], // 相关介绍
				},
				relatedSelect: '', // 相关介绍选中下标
				skuDataList: [], // SKU
				skuSelect: '0_0', // 选中
				popupDate: {
					show: false,
					skuSelect: 0,
					day: 7,
					skuId: null,
					skuSeqNo: null,
					groupIndex: 0,
					customNight: false, // 自选晚数：使用标准房型(200)每晚单价
					checkInDate: '', // 入住日期
					checkOutDate: '', // 离店日期
				}, // 弹出窗口数据集合
				calendarEndDate: '', // 日期组件初始化结束日期
				scrollTop: 0, // 滚动条距离顶端位置
				anchorTop: [], // 锚点距离顶端高度
				anchorClickLock: false, // 点击锚点跳转中，暂时忽略滚动联动高亮
				anchorClickTimer: null,
				stickyOffset: 180, // 吸顶导航实际高度（动态测量）
				lastScrollTop: 0,
				scrollDirection: 'down',
				scrollSpyThrottleAt: 0,
				scrollSpySettleTimer: null,
				customerServiceData: {
					show: false,
					content: []
				}, // 客服数据集合
				pageScrollHeight: 0,
				defaultCustomNights: 7,
				collectId: null,
			};
		},
		onLoad(e) {
			parseInvitePageOptions(e)
			if (e.id) {
				this.getGoodsDetailFn(e.id)
			}
			this.getCustomerServiceDataFn()
		},
		onShow() {
			bindPageAuthPopup(this)
			this.loadCollectState()
		},
		onReady() {
			this.setPageScrollHeight()
		},
		onUnload() {
			this.clearAnchorTimers()
		},
		beforeDestroy() {
			this.clearAnchorTimers()
		},
		computed: {
			stayDurationLabel() {
				const nights = Number(this.popupDate.day) || 0
				return `${nights + 1}天${nights}晚`
			},
			calendarPriceText() {
				const group = this.skuGroupList[this.popupDate.groupIndex] || this.skuGroupList[0]
				if (!group) return ''
				if (this.popupDate.customNight) {
					return formatCalendarPrice(resolveCalendarUnitPrice({ nightPrice: group.price }))
				}
				const skuData = group.skuDataList && group.skuDataList[this.popupDate.skuSelect]
				const combinations = (skuData && skuData.combinationList) || []
				const selected = combinations.find(item => (
					Number(item.skuSeqNo) === Number(this.popupDate.skuSeqNo)
				)) || combinations[0]
				const unitPrice = resolveCalendarUnitPrice({
					average: selected && selected.average,
					total: selected && selected.price,
					nights: this.popupDate.day
				})
				return formatCalendarPrice(unitPrice)
			},
			popupSkuDataList() {
				const group = this.skuGroupList[this.popupDate.groupIndex] || this.skuGroupList[0]
				return (group && group.skuDataList) ? group.skuDataList : []
			}
		},
		methods: {
			getShareConfig() {
				const cover = this.bannerImages && this.bannerImages[0]
				return {
					title: this.hotelData.name || '逸享荟精选旅居',
					path: '/packagesMall/GoodsDetails/SojournGoodsDetails',
					query: { id: this.hotelData.id },
					imageUrl: cover ? this.resolveCoverUrl(cover) : ''
				}
			},
			clearAnchorTimers() {
				if (this.anchorClickTimer) {
					clearTimeout(this.anchorClickTimer)
					this.anchorClickTimer = null
				}
				if (this.scrollSpySettleTimer) {
					clearTimeout(this.scrollSpySettleTimer)
					this.scrollSpySettleTimer = null
				}
			},
			// 测量吸顶导航高度，替代写死 180
			measureStickyOffset() {
				return new Promise(resolve => {
					this.$nextTick(() => {
						const query = uni.createSelectorQuery().in(this)
						query.select('.body_related_view').boundingClientRect()
						query.exec(res => {
							const h = res && res[0] && res[0].height
							if (h > 0) {
								this.stickyOffset = Math.ceil(h) + 8
							}
							resolve(this.stickyOffset)
						})
					})
				})
			},
			getStickyOffset() {
				return this.stickyOffset > 0 ? this.stickyOffset : 180
			},
			parseStayNights(skudays, skuName, optionValueUnit) {
				const name = skuName || ''
				// 名称含周/半月/月时优先按套餐语义（半月必须在「月」之前），避免错误 303 把半月算成约 30 晚
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
			calcNightsBetween(checkIn, checkOut) {
				if (!checkIn || !checkOut) return -1
				const start = new Date(checkIn.replace(/-/g, '/'))
				const end = new Date(checkOut.replace(/-/g, '/'))
				return Math.round((end - start) / (24 * 3600 * 1000))
			},
			applyStayDuration() {
				const nights = Number(this.popupDate.day) || 0
				if (nights <= 0) return
				this.$nextTick(() => {
					const cal = this.$refs.CalendarRef
					if (!cal) return
					if (!this.popupDate.checkInDate) {
						this.popupDate.checkInDate = this.formatDate(new Date())
					}
					const startDate = this.popupDate.checkInDate
					cal.startDates = cal.resetTime(startDate)
					const end = new Date(startDate.replace(/-/g, '/'))
					end.setDate(end.getDate() + nights)
					const checkOut = this.formatDate(end)
					cal.endDates = cal.resetTime(checkOut)
					this.popupDate.checkOutDate = checkOut
					this.calendarEndDate = checkOut
				})
			},
			initPopupCalendar() {
				this.applyStayDuration()
			},
			setPageScrollHeight() {
				this.$nextTick(() => {
					const query = uni.createSelectorQuery().in(this)
					query.select('.weapp-nav-box').boundingClientRect()
					query.select('.page_foot_view').boundingClientRect()
					query.exec((res) => {
						const windowInfo = uni.getWindowInfo ? uni.getWindowInfo() : uni.getSystemInfoSync()
						const navH = (res[0] && res[0].height) || ((windowInfo.statusBarHeight || 0) + 44)
						const footH = (res[1] && res[1].height) || uni.upx2px(140)
						this.pageScrollHeight = windowInfo.windowHeight - navH - footH
					})
				})
			},
			formatDescText(text) {
				if (!text) return ''
				return String(text).replace(/^[·•]\s*/gm, '').replace(/[·•]/g, '')
			},
			async loadCollectState() {
				const token = uni.getStorageSync('token')
				const userInfo = uni.getStorageSync('userInfo')
				if (!token || !userInfo || !this.hotelData.id) {
					this.collectId = null
					return
				}
				try {
					const res = await goodsCollectList({ pageNum: 1, pageSize: 500 })
					const rows = (res && res.rows) || []
					const item = rows.find(v => {
						const type = v.collectType || 'goods'
						return type !== 'activity' && String(v.goodsId) === String(this.hotelData.id)
					})
					this.collectId = item ? item.collectId : null
				} catch (e) {
					console.warn('loadCollectState failed', e)
				}
			},
			toggleCollect() {
				if (!this.hotelData.id) return
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
						goodsId: this.hotelData.id
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
			resolveCoverUrl(url) {
				if (!url) return DEFAULT_SKU_COVER
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
			// 统一区块 DOM id，避免 sectionId 已含 id_ 前缀时重复拼接
			normalizeSectionDomId(sectionId) {
				if (!sectionId) return ''
				return sectionId.startsWith('id_') ? sectionId : 'id_' + sectionId
			},
			// 根据元素位置计算 scroll-view 目标 scrollTop
			calcScrollTopForRect(scrollViewRect, scrollOffset, targetRect, offset = 180) {
				if (!scrollViewRect || !scrollOffset || !targetRect) return null
				return Math.max(scrollOffset.scrollTop + targetRect.top - scrollViewRect.top - offset, 0)
			},
			setScrollTopValue(targetTop) {
				if (targetTop === null || targetTop === undefined) return
				if (this.scrollTop === targetTop) {
					this.scrollTop = targetTop > 0 ? targetTop - 1 : 1
					this.$nextTick(() => {
						this.scrollTop = targetTop
					})
				} else {
					this.scrollTop = targetTop
				}
			},
      getSkuSelectIndex(groupIndex) {
        if (!this.skuSelect || typeof this.skuSelect !== 'string') {
          return 0;
        }
        const selectKey = this.skuSelect.split('_');
        if (selectKey[0] == groupIndex) {
          const index = parseInt(selectKey[1]);
          return isNaN(index) ? 0 : index;
        }
        return 0;
      },
      // 获取当前套餐组的SKU数据
      getCurrentSkuData(groupIndex) {
        const group = this.skuGroupList[groupIndex];
        if (!group || !group.skuDataList || group.skuDataList.length === 0) {
          return null;
        }
        const index = this.getSkuSelectIndex(groupIndex);
        return group.skuDataList[index] || group.skuDataList[0];
      },
      // 获取当前套餐组的组合列表
      getCurrentCombinationList(groupIndex) {
        const skuData = this.getCurrentSkuData(groupIndex);
        if (!skuData || !skuData.combinationList) {
          return [];
        }
        return skuData.combinationList;
      },
			 // 为HTML内容中的URL添加host前缀并适配移动端图片展示
			  addHostPrefixToUrls(content, host) {
			    return prepareRichTextHtml(content, host)
			  },
			// 获取商品详情
			getGoodsDetailFn(id) {
				getGoodsInfo(id).then(res => {
					this.hotelData.id = res.data.goodsId
					this.hotelData.type = res.data.goodsType === 'hotel' ? '旅居基地' : ''
					this.hotelData.name = res.data.goodsName
					this.hotelData.desc = res.data.description
					this.loadCollectState()
					let thehost = this.host
					if(res.data.goodsImages){
						this.bannerImages = res.data.goodsImages.split(",");
						this.bannerImages.map(image => {
						      if (image.startsWith('http')) {
						        return image;
						      } else {
						        return this.host + image;
						      }
						    });
					}
					this.hotelData.tagList = res.data.tags ? String(res.data.tags).split('|').filter(Boolean) : []
					 // 处理 features 映射为 related
					    if (res.data.features) {
					      this.hotelData.related = res.data.features.map(feature => ({
					        id: this.normalizeSectionDomId(feature.sectionId),
					        name: feature.sectionName,
					        content: this.addHostPrefixToUrls(feature.content, this.host),
					        isExpand: true,
					        showToggle: false
					      }));
					      if (this.hotelData.related.length > 0) {
					        this.relatedSelect = this.hotelData.related[0].id
					      }
					    }
						 // 在 getGoodsDetailFn 中处理 optionList 映射
          if (res.data.optionList) {
            console.log('options', res.data.optionList);
            const filteredSkuGroupList = res.data.optionList
              .filter(option => option.skuType === '200' && this.isSkuEnabled(option));

            if (filteredSkuGroupList && filteredSkuGroupList.length > 0) {
              this.skuGroupList = filteredSkuGroupList.map((group, groupIndex) => {
                const skuGroupItem = {
                  title: group.skuName || '',
                  cover: DEFAULT_SKU_COVER,
                  descOne: '',
                  descTwo: '',
                  skuId: group.skuId,
                  // 标准规格售价 = 每晚单价（自选晚数用）
                  price: Number(group.price) || 0,
                  skuDataList: []
                };

                const skuGroupOptions = group.options;
                if (skuGroupOptions && skuGroupOptions.length > 0) {
                  skuGroupOptions.forEach(item => {
                    if (item.optionType == '305') {
                      skuGroupItem.cover = item.optionValue || DEFAULT_SKU_COVER;
                    }
                    if (item.optionType == '304') {
                      if (!skuGroupItem.descOne) {
                        skuGroupItem.descOne = item.optionValue || '';
                      } else if (!skuGroupItem.descTwo) {
                        skuGroupItem.descTwo = item.optionValue || '';
                      }
                    }
                  });
                }

                return skuGroupItem;
              });
            }

            const filteredOptionList = res.data.optionList.filter(
              option => option.skuType === '202' && this.isSkuEnabled(option)
            );
            if (filteredOptionList && filteredOptionList.length > 0) {
              this.skuGroupList.forEach((group, groupIndex) => {
                const groupSkuOptions = filteredOptionList.filter(option => option.parSkuId == group.skuId);

                if (groupSkuOptions && groupSkuOptions.length > 0) {
                  const skuDataList = groupSkuOptions.map(option => {
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

                    const thecombinationList = Object.keys(groupedBySeqNo).map(seqNo => {
                      const items = groupedBySeqNo[seqNo];
                      let optionAttrName = '';
                      let optionTotPrice = '';
                      let optionAvgPrice = '';
                      items.forEach(item => {
                        if(item.optionType == '302'){
                          optionTotPrice = item.optionValue;
                        }
                        if(item.optionType == '301'){
                          optionAvgPrice = item.optionValue;
                        }
                        if(item.optionType == '304'){
                          optionAttrName = item.optionValue;
                        }
                      });
                      return {
                        name: optionAttrName || '',
                        price: optionTotPrice || 0,
                        average: optionAvgPrice || 0,
                        skuSeqNo: parseInt(seqNo) || 0
                      };
                    });

                    return {
                      skuId: option.skuId,
                      name: option.skuName || '',
                      day: this.parseStayNights(skudays, option.skuName, skudaysUnit),
                      combinationList: thecombinationList
                    };
                  });

                  group.skuDataList = skuDataList;
                }
              });
            }
          }

					// 富文本图片异步加载会改变区块高度，多次重测避免锚点 top 偏小
					this.$nextTick(() => {
						;[300, 800, 1600].forEach(delay => {
							setTimeout(() => {
								this.getAnchorTopFn()
							}, delay)
						})
					})
        }).catch(err => {
					console.log('getGoodsInfo', err)
				})
			},
			// 获取客服数据
			getCustomerServiceDataFn() {
				getBannerList({
					positionId: 2
				}).then(res => {
					this.customerServiceData.content = Array.isArray(res.data) ? res.data : []
				}).catch(err => {
					console.log('getBannerList', err)
					this.customerServiceData.content = []
				})
			},
			getAdImageUrl(adImage) {
				if (!adImage) return ''
				return adImage.startsWith('http') ? adImage : this.host + adImage
			},
			previewQrImage(adImage) {
				const url = this.getAdImageUrl(adImage)
				if (!url) {
					uni.showToast({
						icon: 'none',
						title: '二维码未配置'
					})
					return
				}
				uni.previewImage({
					urls: [url],
					current: url
				})
			},
			// 相关介绍选中方法（点击时实时查询位置）
			scrollToSectionFn(id) {
				if (id != 'id_skuList') {
					this.relatedSelect = id
					// 锁定高亮，避免动画滚动过程中 scrollFn 用过期锚点把选中刷成「政策」
					this.anchorClickLock = true
					if (this.anchorClickTimer) {
						clearTimeout(this.anchorClickTimer)
					}
					this.anchorClickTimer = setTimeout(() => {
						this.anchorClickLock = false
						this.anchorClickTimer = null
						this.getAnchorTopFn()
					}, 600)
				}
				const offset = this.getStickyOffset()
				const query = uni.createSelectorQuery().in(this)
				query.select('.page_view').boundingClientRect()
				query.select('.page_view').scrollOffset()
				query.select(`#${id}`).boundingClientRect()
				query.exec((res) => {
					const targetTop = this.calcScrollTopForRect(res[0], res[1], res[2], offset)
					this.setScrollTopValue(targetTop)
				})
			},
			// 获取锚点 scrollTop 位置（用于滚动联动高亮）
			getAnchorTopFn() {
				const anchorIds = ['id_skuList', ...this.hotelData.related.map(item => item.id)]
				this.measureStickyOffset().then(offset => {
					const query = uni.createSelectorQuery().in(this)
					query.select('.page_view').boundingClientRect()
					query.select('.page_view').scrollOffset()
					anchorIds.forEach(id => query.select(`#${id}`).boundingClientRect())
					query.exec((res) => {
						const scrollViewRect = res[0]
						const scrollOffset = res[1]
						if (!scrollViewRect || !scrollOffset) return
						this.anchorTop = anchorIds.map((id, index) => {
							const rect = res[index + 2]
							if (!rect) return null
							return {
								id,
								top: this.calcScrollTopForRect(scrollViewRect, scrollOffset, rect, offset),
								height: rect.height
							}
						}).filter(Boolean)
					})
				})
			},
			/**
			 * 高亮判定（按体感）：
			 * - 下滑：提前量较大，接近下一段标题就切换（避免内容快没了才切）
			 * - 回滚：粘在当前段，明显滚回上一段后才切换（避免刚露出字就切）
			 */
			resolveActiveAnchorId(scrollTop) {
				const relatedAnchors = this.hotelData.related
					.map(item => this.anchorTop.find(a => a.id === item.id))
					.filter(Boolean)
				if (!relatedAnchors.length) return ''

				// 下滑提前量：越大越早切到下一段
				const DOWN_AHEAD = 120
				// 回滚粘滞：必须滚过当前段 top 这么多，才允许切回上一段
				const UP_STICK = 140

				const currentIndex = relatedAnchors.findIndex(a => a.id === this.relatedSelect)

				if (this.scrollDirection === 'up' && currentIndex > 0) {
					const keepUntil = relatedAnchors[currentIndex].top - UP_STICK
					// 还没明显离开当前段 → 继续高亮当前
					if (scrollTop >= keepUntil) {
						return relatedAnchors[currentIndex].id
					}
				}

				const ahead = this.scrollDirection === 'down' ? DOWN_AHEAD : 0
				let rawIndex = 0
				for (let i = 0; i < relatedAnchors.length; i++) {
					if (scrollTop >= relatedAnchors[i].top - ahead) {
						rawIndex = i
					}
				}
				return relatedAnchors[rawIndex].id
			},
			applyScrollSpy(scrollTop) {
				const activeId = this.resolveActiveAnchorId(scrollTop)
				if (activeId && activeId !== this.relatedSelect) {
					this.relatedSelect = activeId
				}
			},
			// 展开方法
			expandFn(data) {
				data.isExpand = !data.isExpand
				this.$nextTick(() => {
					setTimeout(() => {
						this.getAnchorTopFn()
					}, 100)
				})
			},
			// 预定方法
      reserveFn(skuData, combinationData, groupIndex) {
        if (!skuData) {
          uni.showToast({
            title: '套餐数据异常',
            icon: 'none'
          });
          return;
        }
        const group = this.skuGroupList[groupIndex]
        let dataIndex = 0
        if (group && group.skuDataList) {
          const idx = group.skuDataList.findIndex(item => item.skuId === skuData.skuId)
          if (idx >= 0) dataIndex = idx
        }
        this.popupDate.show = true
        this.popupDate.day = skuData.day
        this.popupDate.skuId = skuData.skuId
        this.popupDate.skuSeqNo = combinationData ? combinationData.skuSeqNo : null
        this.popupDate.groupIndex = groupIndex
        this.popupDate.skuSelect = dataIndex
        this.popupDate.customNight = false
        this.initPopupCalendar()
      },
      // 弹窗内当前房型（标准 200）
      getPopupSkuGroup() {
        return this.skuGroupList[this.popupDate.groupIndex] || this.skuGroupList[0] || null
      },
      // 获取当前选中的套餐组
      getCurrentSkuGroup() {
        if (this.skuGroupList && this.skuGroupList.length > 0) {
          const selectKey = this.skuSelect.toString().split('_');
          const groupIndex = parseInt(selectKey[0]) || 0;
          return this.skuGroupList[groupIndex] || this.skuGroupList[0];
        }
        return { skuDataList: [] };
      },
      // 选择天数方法
      selectDayFn(index) {
        if (index === 'customize') {
          const group = this.getPopupSkuGroup()
          const nightPrice = group ? Number(group.price) : 0
          if (!group || !group.skuId || !Number.isFinite(nightPrice) || nightPrice <= 0) {
            uni.showToast({
              title: '该房型未配置每晚单价，无法自选晚数',
              icon: 'none'
            })
            return
          }
          this.popupDate.skuSelect = 'customize'
          this.popupDate.day = this.defaultCustomNights
          // 自选晚数绑定标准房型 skuId(200)，不传组合序号
          this.popupDate.skuId = group.skuId
          this.popupDate.skuSeqNo = null
          this.popupDate.customNight = true
          this.applyStayDuration()
          return
        }
        this.popupDate.skuSelect = index
        this.popupDate.customNight = false
        const currentGroup = this.getPopupSkuGroup() || this.getCurrentSkuGroup();
        const skuData = currentGroup.skuDataList && currentGroup.skuDataList[index]
        if (skuData) {
          this.popupDate.day = skuData.day
          this.popupDate.skuId = skuData.skuId
          if (skuData.combinationList && skuData.combinationList.length) {
            const matchedCombo = skuData.combinationList.find(item => item.skuSeqNo === this.popupDate.skuSeqNo)
            this.popupDate.skuSeqNo = matchedCombo
              ? matchedCombo.skuSeqNo
              : skuData.combinationList[0].skuSeqNo
          }
        }
        this.applyStayDuration()
      },
      onCustomDayChange(e) {
        const val = typeof e === 'object' ? e.value : e
        this.popupDate.day = Math.max(Number(val) || 7, 7)
        this.applyStayDuration()
      },
			calendarCallBackFn(date) {
				if (date.startStr && date.startStr.dateStr) {
					this.popupDate.checkInDate = date.startStr.dateStr
				}
				if (date.endStr && date.endStr.dateStr) {
					this.popupDate.checkOutDate = date.endStr.dateStr
				}
			},
			calendarClickFn(data) {
				this.popupDate.checkInDate = data.startStr.dateStr
				this.applyStayDuration()
			},
			// 格式化日期
			formatDate(date) {
				const year = date.getFullYear();
				const month = String(date.getMonth() + 1).padStart(2, '0');
				const day = String(date.getDate()).padStart(2, '0');
				return `${year}-${month}-${day}`;
			},
			// 清除方法
			clearFn() {
				this.popupDate.checkInDate = ''
				this.popupDate.checkOutDate = ''
				if (this.$refs.CalendarRef) {
					this.$refs.CalendarRef.init()
				}
			},
			confirmFn() {
				if (!this.popupDate.checkInDate || !this.popupDate.checkOutDate) {
					uni.showToast({
						icon: 'none',
						title: '请选择入住和退房日期'
					})
					return
				}
				const nights = this.calcNightsBetween(this.popupDate.checkInDate, this.popupDate.checkOutDate)
				if (nights !== Number(this.popupDate.day)) {
					uni.showToast({
						icon: 'none',
						title: `当前套餐为${this.stayDurationLabel}，请重新选择日期`
					})
					this.applyStayDuration()
					return
				}
				if (this.popupDate.customNight) {
					if (nights < this.defaultCustomNights) {
						uni.showToast({
							icon: 'none',
							title: `自选晚数不能少于${this.defaultCustomNights}晚`
						})
						return
					}
					if (!this.popupDate.skuId) {
						uni.showToast({
							icon: 'none',
							title: '该房型未配置每晚单价，无法自选晚数'
						})
						return
					}
				}
				runWithAuth(this, (ok) => {
					if (!ok) return
					this.popupDate.show = false
					const query = [
						`id=${this.hotelData.id}`,
						`day=${this.popupDate.day}`,
						`checkInDate=${this.popupDate.checkInDate}`,
						`checkOutDate=${this.popupDate.checkOutDate}`
					]
					if (this.popupDate.customNight) {
						query.push('customNight=1')
					}
					if (this.popupDate.skuId) {
						query.push(`skuId=${this.popupDate.skuId}`)
					}
					if (!this.popupDate.customNight
						&& this.popupDate.skuSeqNo !== null
						&& this.popupDate.skuSeqNo !== undefined) {
						query.push(`skuSeqNo=${this.popupDate.skuSeqNo}`)
					}
					uni.navigateTo({
						url: `/packagesMall/ConfirmOrder/SojournConfirmOrder?${query.join('&')}`
					})
				}, {
					onNeedAuth: () => {
						this.popupDate.show = false
					}
				})
			},
			// 滚动条滚动事件：节流预览 + 停滚定稿 + 方向滞回
			scrollFn(e) {
				if (this.anchorClickLock) return
				const scrollTop = e.detail.scrollTop
				const delta = scrollTop - this.lastScrollTop
				// 忽略微小抖动，避免方向误判导致下滑变晚、回滚变早
				if (Math.abs(delta) >= 6) {
					this.scrollDirection = delta > 0 ? 'down' : 'up'
				}
				this.lastScrollTop = scrollTop

				const now = Date.now()
				if (now - this.scrollSpyThrottleAt >= 100) {
					this.scrollSpyThrottleAt = now
					this.applyScrollSpy(scrollTop)
				}

				if (this.scrollSpySettleTimer) {
					clearTimeout(this.scrollSpySettleTimer)
				}
				this.scrollSpySettleTimer = setTimeout(() => {
					this.scrollSpySettleTimer = null
					if (this.anchorClickLock) return
					this.applyScrollSpy(this.lastScrollTop)
				}, 120)
			}
		}
	};
</script>

<style scoped lang="scss">
	@import 'SojournGoodsDetails.scss';
</style>
