<template>
	<view class="page article">
		<!-- <view class="title" v-if="detailInfo">
			{{ detailInfo.pageName }}
		</view> -->
		<view class="detail" v-if="detailInfo">
			<u-parse :html="detailInfo.content" :autosetTitle="false" />
		</view>
		<view class="detail" v-else-if="errorMessage">{{ errorMessage }}</view>
		<view class="detail" v-else>正在加载...</view>
	</view>
</template>

<script>
	import {
		getSingleInfo
	} from '@/api/index'
	import { prepareRichTextHtml } from '@/utils/richText'
	import { parseInvitePageOptions } from '@/utils/invite'
	import sharePageMixin from '@/utils/sharePageMixin'
	export default {
		mixins: [sharePageMixin],
		data() {
			return {
				host: this.$host,
				detailInfo: null,
				articleId: null,
				errorMessage: ''
			}
		},
		onLoad(option) {
			parseInvitePageOptions(option)
			this.articleId = option.id
			this.getArticleDetail(option.id)
		},
		methods: {
			getShareConfig() {
				return {
					title: (this.detailInfo && this.detailInfo.pageName) || '逸享荟康养资讯',
					path: '/packagesPublic/Article/index',
					query: { id: this.articleId }
				}
			},
			async getArticleDetail(id) {
				this.errorMessage = ''
				try {
					const response = await getSingleInfo(id)
					if (!response.data || !response.data.content) {
						throw new Error('协议内容尚未配置')
					}
					this.detailInfo = response.data
					this.detailInfo.content = prepareRichTextHtml(this.detailInfo.content, this.host)
					uni.setNavigationBarTitle({ title: this.detailInfo.pageName || '协议详情' })
				} catch (error) {
					this.detailInfo = null
					this.errorMessage = (error && error.message) || '内容加载失败，请稍后重试'
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'index.scss';
</style>
