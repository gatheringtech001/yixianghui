<template>
	<view class="page">
		<!-- 头部背景 -->
		<view class="head-bg">
			<view class="integral-balance">
				<view class="integral">
					<text>当前可用金币</text>
					<text class="number">{{userData.golden || 0}}</text>
				</view>
			</view>
			<view class="bg">
				<image src="/static/integral_bg1.png" mode=""></image>
			</view>
		</view>
		<!-- tab -->
		<view class="integral-tab">
			<view class="tab" :class="[currentTap == 0?'action': '']" @click="changeType(0)">
				<text>全部</text>
			</view>
			<view class="tab" :class="[currentTap == 1?'action': '']" @click="changeType(1)">
				<text>收入</text>
			</view>
			<view class="tab" :class="[currentTap == 2?'action': '']" @click="changeType(2)">
				<text>支出</text>
			</view>
		</view>
		<!-- 记录列表 -->
		<view class="record-list" v-if="filters.length > 0">
			<view class="list" v-for="(item,index) in filters" :key="index">
				<view class="title-date">
					<view class="title">
						<text>{{ item.tradeTitle || item.businessType }}</text>
					</view>
					<view class="date">
						<text>{{ item.createTime }}</text>
					</view>
				</view>
				<view class="integral">
					<text class="add" v-if="item.tradeType == 1">+{{item.gold}}</text>
					<text v-else>-{{item.gold}}</text>
				</view>
			</view>
		</view>
		<view class="record-list" v-else>
			<u-empty text="暂无记录" mode="list"></u-empty>
		</view>
	</view>
</template>

<script>
	import { signInList } from '@/api/signIn/index'
	export default {
		data() {
			return {
				userData: null,
				currentTap: 0,
				list: [],
				filters: []
			};
		},
		onShow() {
			this.userData = uni.getStorageSync('userData')
			this.getSignInList()
		},
		methods: {
			changeType(index) {
				this.currentTap = index
				if(index == 0) this.filters = this.list
				if(index == 1) this.filters = this.list.filter(v => v.tradeType == 1)
				if(index == 2) this.filters = this.list.filter(v => v.tradeType != 1)
			},
			// 获取签到历史
			async getSignInList() {
				let params = {}
				let { data } = await signInList(params)
				this.list = data
				this.filters = data
			},
		}
	}
</script>

<style scoped lang="scss">
	@import 'IntegralRecord.scss';
</style>
