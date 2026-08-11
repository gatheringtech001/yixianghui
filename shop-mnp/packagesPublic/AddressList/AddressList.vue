<template>
	<view class="page">
		<!-- 地址列表 -->
		<view class="address-list" v-if="addressList.length > 0">
			<view class="list" v-for="(item, index) in addressList" :key="index" @click="chooseAddress(item)" @longpress="onAddressLongPress(item)">
				<view class="name-phone">
					<view class="name">
						<text class="one-omit">{{item.linkPerson}}</text>
					</view>
					<view class="phone">
						<text>{{item.linkMobile}}</text>
						<text class="tag" v-if="Number(item.isDefault) === 1">默认</text>
						<!-- <text class="tag blue">公司</text> -->
					</view>
				</view>
				<view class="address-edit">
					<view class="address">
						<text>{{item.provinceName}}{{item.cityName}}{{item.countyName}}{{item.streetName || ''}}{{item.addressDetail}}</text>
					</view>
					<view class="edit" @click.stop="onAddressEdit(1, item)">
						<text class="iconfont icon-edit1"></text>
					</view>
				</view>
			</view>
		</view>
		
		<view class="address-list empty" v-else>
			<u-empty text="暂无收货地址" mode="list"></u-empty>
		</view>
		<!-- 添加地址 -->
		<view class="add-address">
			<view class="btn" @click="onAddressEdit(2)">
				<text>新建收货地址</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getAddressList, deleteAddress } from '@/api/member/index'
	export default {
		data() {
			return {
				addressList: [],
				prev: ''
			};
		},
		onLoad(option) {
			if(option.type && option.type == 'creatOrder'){
				this.prev = option.type
			}
		},
		onShow() {
			this.getAddress()
		},
		methods:{
			async getAddress() {
				let { rows } = await getAddressList()
				this.addressList = rows
			},
			chooseAddress(item) {
				if(this.prev && this.prev == 'creatOrder') {
					let pages = getCurrentPages()
					let prevPage = pages[pages.length - 2]
					prevPage.$vm.getAddressDetail(item.addressId)
					uni.navigateBack({
						delta: 1
					})
				}else{
					uni.navigateTo({
						url: `/packagesPublic/AddressEdit/AddressEdit?type=1&id=${item.addressId}`,
					})
				}
			},
			/**
			 * 编辑地址点击
			 */
			onAddressEdit(type, item){
				this.$u.throttle(() => {
					if(type == 1) {
						var path = `/packagesPublic/AddressEdit/AddressEdit?type=${type}&id=${item.addressId}`
					}else {
						var path = `/packagesPublic/AddressEdit/AddressEdit?type=${type}`
					}
					uni.navigateTo({
						url: path,
					})
				}, 500)
			},
			onAddressLongPress(item) {
				uni.showActionSheet({
					itemList: ['复制地址', '删除'],
					success: ({ tapIndex }) => {
						if (tapIndex === 0) {
							this.copyAddress(item)
						} else if (tapIndex === 1) {
							this.confirmDeleteAddress(item)
						}
					}
				})
			},
			getAddressText(item) {
				const region = `${item.provinceName || ''}${item.cityName || ''}${item.countyName || ''}${item.streetName || ''}${item.addressDetail || ''}`
				return `${item.linkPerson || ''} ${item.linkMobile || ''}\n${region}`
			},
			copyAddress(item) {
				uni.setClipboardData({
					data: this.getAddressText(item),
					success: () => {
						uni.showToast({ title: '地址已复制', icon: 'none' })
					}
				})
			},
			confirmDeleteAddress(item) {
				uni.showModal({
					title: '提示',
					content: '确定删除该收货地址吗？',
					success: async ({ confirm }) => {
						if (!confirm) return
						await deleteAddress(item.addressId)
						uni.showToast({ title: '删除成功', icon: 'none' })
						this.getAddress()
					}
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'AddressList.scss';
</style>
