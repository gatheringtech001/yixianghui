<template>
	<view class="page article">
		<view class="title" v-if="detailInfo">
			{{ detailInfo.pageName }}
		</view>
		<view class="detail" v-if="detailInfo">
			<u-parse :html="detailInfo.content" :autosetTitle="false" />
		</view>
	</view>
</template>

<script>
	import { getNoticeInfo } from '@/api/system/notice'
	import BaseUrl from '@/api/baseUrl'
	export default {
		data() {
			return {
				detailInfo: null
			}
		},
		onLoad(option) {
			const noticeId = option.id || option.noticeId || 10
			this.getNoticeDetail(noticeId)
		},
		methods: {
			async getNoticeDetail(noticeId) {
				const { data } = await getNoticeInfo(noticeId)
				if (!data) return
				let content = data.noticeContent || ''
				if (content) {
					content = content.replace(/src="\/api\//g, 'src="' + BaseUrl.publicUrl)
				}
				this.detailInfo = {
					pageName: data.noticeTitle,
					content
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'AboutUs.scss';
</style>
