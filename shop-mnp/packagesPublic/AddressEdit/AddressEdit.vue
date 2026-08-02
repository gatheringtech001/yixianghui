<template>
	<view class="page">
		<view class="address-input">
			<view class="list-input">
				<view class="title">
					<text>收货人</text>
				</view>
				<view class="content">
					<input v-model="addressInfo.linkPerson" type="text" placeholder="请填写收货人姓名">
				</view>
			</view>
			<view class="list-input">
				<view class="title">
					<text>手机号</text>
				</view>
				<view class="content">
					<input
						v-model="addressInfo.linkMobile"
						type="number"
						maxlength="11"
						placeholder="请填写收货人手机号">
				</view>
			</view>
			<view class="list-input">
				<view class="title">
					<text>所在地区</text>
				</view>
				<view class="content">
					<text v-if="addressInfo.provinceName" @click="show = true">{{addressInfo.provinceName}}{{addressInfo.cityName}}{{addressInfo.countyName}}</text>
					<input v-else type="text" disabled placeholder="省市区县/乡镇等" @click="show = true">
					<u-picker mode="region" v-model="show" :area-code="defaultCode"  @confirm="changeAddress"></u-picker>
				</view>
			</view>
			<view class="list-textarea">
				<view class="title">
					<text>详细地址</text>
				</view>
				<view class="content">
					<textarea type="text" v-model="addressInfo.addressDetail" placeholder="请输入详细地址" />
				</view>
			</view>
		</view>
		<view class="tag-default">
			<view class="default-address">
				<view class="title">
					<text>默认地址</text>
				</view>
				<view class="switch-default">
					<switch class="red sm" color="#701018" :checked="Number(addressInfo.isDefault) === 1" @change="changeDefault" />
				</view>
			</view>
		</view>
		<view class="footer-btn">
			<view class="btn" @click="$u.throttle(submitAddress, 500)">
				<text>保存</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getAddressInfo, addAddress, updateAddress, getAddressList } from '@/api/member/index'
	export default {
		data() {
			return {
				show: false,
				addressType: '2',
				addressInfo: {
					linkPerson: '',
					linkMobile: '',
					provinceCode: '',
					provinceName: '',
					cityCode: '',
					cityName: '',
					countyCode: '',
					countyName: '',
					addressDetail: '',
					isDefault: 0
				},
				defaultCode:  ['', '', '']
			};
		},
		onLoad(option) {
			this.addressType = option.type || '2'
			uni.setNavigationBarTitle({
				title: this.addressType === '1' ? '编辑收货地址':'新建收货地址'
			})
			if(option.id){
				this.getDetail(option.id)
			}
		},
		methods: {
			normalizeAddressInfo(data) {
				return {
					...data,
					isDefault: Number(data.isDefault) === 1 ? 1 : 0
				}
			},
			// 获取地址详情
			async getDetail(id) {
				let {data} = await getAddressInfo(id)
				this.addressInfo = this.normalizeAddressInfo(data)
        // 添加验证确保省市区代码有效
        const provinceCode = data.provinceCode || '';
        const cityCode = data.cityCode || '';
        const countyCode = data.countyCode || '';
				this.defaultCode = [provinceCode, cityCode, countyCode];
			},
			async clearOtherDefaultAddresses(currentAddressId) {
				const { rows } = await getAddressList()
				const others = rows.filter(item => Number(item.isDefault) === 1 && item.addressId != currentAddressId)
				await Promise.all(others.map(item => updateAddress({
					...item,
					isDefault: 0
				})))
			},
			
			changeAddress(e) {
				let {province, city, area} = e
				this.defaultCode = [province.code, city.code, area.code]
				this.addressInfo.provinceCode = province.code
				this.addressInfo.provinceName = province.name
				this.addressInfo.cityCode = city.code
				this.addressInfo.cityName = city.name
				this.addressInfo.countyCode = area.code
				this.addressInfo.countyName = area.name
			},
			changeDefault({detail}) {
				this.addressInfo.isDefault = detail.value ? 1 : 0
			},
			async submitAddress() {
				const mobile = String(this.addressInfo.linkMobile || '').trim()
				if (!this.addressInfo.linkPerson ||
					!this.addressInfo.provinceName ||
					!this.addressInfo.addressDetail) {
					uni.showToast({
						icon: 'none',
						title: '请完善收货地址信息'
					})
					return
				}
				if (!/^1\d{10}$/.test(mobile)) {
					uni.showToast({
						icon: 'none',
						title: '请输入正确的11位号码'
					})
					return
				}
				this.addressInfo.linkMobile = mobile
				const payload = this.normalizeAddressInfo(this.addressInfo)
				if (payload.isDefault === 1) {
					await this.clearOtherDefaultAddresses(payload.addressId || null)
				}
				if(this.addressType == '2') {
					await addAddress(payload)
					uni.showToast({
						icon: 'none',
						title: '收货地址新增成功！'
					})
					uni.navigateBack()
				}
				if(this.addressType == '1') {
					await updateAddress(payload)
					uni.showToast({
						icon: 'none',
						title: '收货地址修改成功！'
					})
					uni.navigateBack()
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'AddressEdit.scss';
</style>
