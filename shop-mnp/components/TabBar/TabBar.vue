<template>
	<view class="page-total" :class="displayModeClass">
		<view class="tab-list">
			<view
				class="tab-item"
				v-for="(item, index) in TabBarList"
				:key="index"
				:class="{ active: tabBarShow === index }"
				@click="onTabBar(item, index)"
			>
				<image class="tab-icon" :src="item.icon" mode="aspectFit" />
				<text class="tab-label">{{ item.name }}</text>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				TabBarList: [
					{
						index: 0,
						name: '首页',
						icon: '/static/navigation-icons/nav-home.png'
					},
					{
						index: 1,
						name: '服务',
						icon: '/static/navigation-icons/nav-service.png'
					},
					{
						index: 2,
						name: '领金币',
						icon: '/static/navigation-icons/nav-coins.png'
					},
					{
						index: 3,
						name: '客服',
						icon: '/static/navigation-icons/nav-support.png'
					},
					{
						index: 4,
						name: '我的',
						icon: '/static/navigation-icons/nav-profile.png'
					}
				]
			}
		},
		props: {
			tabBarShow: {
				type: Number,
				default: 0
			}
		},
		methods: {
			onTabBar(item, index) {
				if (this.tabBarShow === index) return
				switch (index) {
					case 0:
						wx.switchTab({ url: '/pages/home/home' })
						break
					case 1:
						wx.switchTab({ url: '/pages/classify/classify' })
						break
					case 2:
						wx.switchTab({ url: '/pages/SignIn/SignIn' })
						break
					case 3:
						wx.switchTab({ url: '/pages/MembersOpened/MembersOpened' })
						break
					case 4:
						wx.switchTab({ url: '/pages/my/my' })
						break
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'TabBar.scss';
	.senior-mode {
		.tab-list { min-height: 64px; }
		.tab-item { min-height: 64px; }
		.tab-icon { width: 26px; height: 26px; opacity: 1; }
		.tab-label { font-size: 16px; color: #555; line-height: 1.4; }
		.tab-item.active .tab-label { color: #701018; }
	}
</style>
