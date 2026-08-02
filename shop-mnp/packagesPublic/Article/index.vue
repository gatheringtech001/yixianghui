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
	import BaseUrl from '@/api/baseUrl'
	export default {
		data() {
			return {
				detailInfo: null
			}
		},
		onLoad(option) {
			this.getArticleDetail(option.id)
		},
		methods: {
			async getArticleDetail(id) {
				let {
					data
				} = await getSingleInfo(id)
				this.detailInfo = data
				if (this.detailInfo.content) this.detailInfo.content = this.detailInfo.content.replace(/src="\/api\//g,
					'src="' + BaseUrl.publicUrl)
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'index.scss';
</style>