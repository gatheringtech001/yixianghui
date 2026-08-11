<template>
	<view class="page article">
		<!-- <view class="title" v-if="detailInfo">
			{{ detailInfo.pageName }}
		</view> -->
		<view class="detail" v-if="detailInfo">
			<u-parse :html="detailInfo.content" :autosetTitle="false" />
		</view>
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
				articleId: null
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
				let {
					data
				} = await getSingleInfo(id)
				this.detailInfo = data
				if (this.detailInfo.content) {
					this.detailInfo.content = prepareRichTextHtml(this.detailInfo.content, this.host)
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'index.scss';
</style>
