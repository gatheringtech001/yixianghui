<template>
	<view class="page">
		<view class="site-list" v-if="list.length > 0">
			<view class="list" v-for="(item, index) in list" :key="index" @click="onItemChange(item)">
				<view class="content">
					<view class="title">
						<text>{{ item.label }}</text>
					</view>
					<view class="identify-num" v-if="item.parentName && mode !== 'province'">
						<text>{{ item.parentName }}</text>
					</view>
				</view>
				<view class="edit">
					<text class="cuIcon-location" :class="[isActive(item) ? 'active' : '']"></text>
				</view>
			</view>
		</view>

		<view class="empty" v-else>
			<text class="empty-text">{{ emptyText }}</text>
		</view>
	</view>
</template>

<script>
	import {
		getSiteList,
		getProvinces,
		getCities
	} from '@/api/index'
	import {
		saveServiceFilter
	} from '@/utils/serviceFilter'
	import {
		formatProvinceName
	} from '@/utils/regionName'

	export default {
		data() {
			return {
				mode: 'city',
				provinceId: null,
				siteId: null,
				list: []
			};
		},
		computed: {
			emptyText() {
				return this.mode === 'province' ? '暂无省份' : '暂无城市'
			}
		},
		onLoad(option) {
			this.mode = option.mode || 'city'
			this.provinceId = option.provinceId || null
			this.siteId = option.id || null
			uni.setNavigationBarTitle({
				title: this.mode === 'province' ? '选择省份' : '选择城市'
			})
			this.loadList()
		},
		methods: {
			normalizeList(data) {
				return (data || []).map(item => ({
					...item,
					label: this.mode === 'province'
						? formatProvinceName(item.deptName)
						: item.deptName
				}))
			},
			async loadList() {
				try {
					if (this.mode === 'province') {
						const res = await getProvinces()
						this.list = this.normalizeList(res.data)
						return
					}
					if (this.provinceId) {
						const res = await getCities({
							provinceId: this.provinceId
						})
						this.list = this.normalizeList(res.data)
						return
					}
					const res = await getSiteList()
					this.list = this.normalizeList(res.rows)
				} catch (error) {
					console.error('loadList', error)
					this.list = []
				}
			},
			isActive(item) {
				if (this.mode === 'province') {
					return item.deptId == this.provinceId
				}
				return item.deptId == this.siteId
			},
			onItemChange(item) {
				if (this.mode === 'province') {
					saveServiceFilter({
						provinceId: item.deptId,
						provinceName: item.deptName,
						cityId: 0,
						cityName: '全部'
					})
					uni.navigateBack()
					return
				}
				const filter = uni.getStorageSync('serviceFilter') || {}
				saveServiceFilter({
					provinceId: filter.provinceId || item.parentId,
					provinceName: filter.provinceName || item.parentName || '',
					cityId: item.deptId,
					cityName: item.deptName
				})
				uni.setStorageSync('site', item)
				uni.navigateBack()
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'index.scss';
</style>
