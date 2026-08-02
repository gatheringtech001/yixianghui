<template>
	<view class="page">
		<!-- 用户信息列表 -->
		<view class="user-list">
			<view class="list" style="height: 160rpx;">
				<view class="title">
					<text>头像</text>
				</view>
				<view class="more-content">
					<image :src="userInfo && userInfo.avatar?host+userInfo.avatar : '/static/img/logo.jpg'"></image>
					<text class="iconfont icon-more more"></text>
				</view>
			</view>
			<view class="list" @click="onNickname">
				<view class="title">
					<text>昵称</text>
				</view>
				<view class="more-content">
					<text class="content">{{nickname}}</text>
					<text class="iconfont icon-more more"></text>
				</view>
			</view>
			<view class="list" @click="onMobile">
				<view class="title">
					<text>手机号</text>
				</view>
				<view class="more-content">
					<text class="content">{{mobile}}</text>
					<text class="iconfont icon-more more"></text>
				</view>
			</view>
			<view class="list">
				<view class="title">
					<text>性别</text>
				</view>
				<view class="more-content">
					<text class="content">{{sexText}}</text>
					<text class="iconfont icon-more more"></text>
				</view>
				<view class="picker">
					<picker @change="sexPickerChange" :value="sexIndex" :range="sexArray">
						<view class="uni-input" style="height: 100rpx;">{{sexText}}</view>
					</picker>
				</view>
			</view>
			<view class="list">
				<view class="title">
					<text>出生日期</text>
				</view>
				<view class="more-content">
					<text class="content">{{birthday}}</text>
					<text class="iconfont icon-more more"></text>
				</view>
				<view class="picker">
					<picker @change="birthdayPickerChange" mode="date" :value="birthdayDate" :start="startDate" :end="endDate">
						<view class="uni-input" style="height: 100rpx;">{{birthdayDate}}</view>
					</picker>
				</view>
			</view>
			<view class="list" @click="showRegionPicker = true">
				<view class="title">
					<text>现在居住地址</text>
				</view>
				<view class="more-content">
					<text class="content address-content">{{ liveAddress || '请选择' }}</text>
					<text class="iconfont icon-more more"></text>
				</view>
			</view>
		</view>
		<u-picker
			mode="region"
			v-model="showRegionPicker"
			title="选择居住地址"
			:area-code="defaultRegionCode"
			:default-region="defaultRegion"
			@confirm="onLiveAddressConfirm"
		></u-picker>
		<!-- 提示框 -->
		<DialogBox ref="DialogBox"></DialogBox>
	</view>
</template>

<script>
	import { updateInfo, getInfo } from '@/api/public'
	import { formatRegionAddress, parseRegionFromAddress } from '@/utils/region'

	export default {
		data() {
			const currentDate = this.getDate({
				format: true
			})
			return {
				// 性别
				sexArray: ['男','女','保密'],
				sexIndex: 0,
				sexText: '保密',
				// 生日
				birthdayDate: currentDate,
				startDate: this.getDate('start'),
				endDate: this.getDate('end'),
				birthday: '2020-02-02',
				DialogBox: {},
				// 昵称
				nickname: '',
				mobile: '',
				liveAddress: '',
				showRegionPicker: false,
				defaultRegion: [],
				defaultRegionCode: ['', '', ''],
				host: this.$host,
				userInfo: null
			};
		},
		
		onShow() {
			this.userInfo = uni.getStorageSync('userInfo')
			if (!this.userInfo) return
			this.nickname = this.userInfo.nickName
			this.mobile = this.userInfo.phonenumber || this.userInfo.mobile || ''
			this.sexIndex = Number(this.userInfo.sex)
			this.liveAddress = this.userInfo.liveAddress || ''
			this.syncLiveAddressRegion()
		},
		onLoad() {},
		methods:{
			/**
			 * 性别
			 * @param {Object} e
			 */
			sexPickerChange(e){
				this.sexIndex = e.detail.value;
				this.sexText = this.sexArray[this.sexIndex];
			},
			/**
			 * 生日
			 * @param {Object} e
			 */
			birthdayPickerChange(e){
				this.birthday = e.detail.value;
			},
			/**
			 * 获取日期
			 * @param {Object} type
			 */
			getDate(type) {
				const date = new Date();
				let year = date.getFullYear();
				let month = date.getMonth() + 1;
				let day = date.getDate();

				if (type === 'start') {
						year = year - 60;
				} else if (type === 'end') {
						year = year + 2;
				}
				month = month > 9 ? month : '0' + month;;
				day = day > 9 ? day : '0' + day;
				return `${year}-${month}-${day}`;
			},
			/**
			 * 昵称点击
			 */
			onNickname(){
				this.$refs['DialogBox'].confirm({
					title: '更改昵称',
					placeholder: '请输入修改的昵称',
					value: this.nickname,
					DialogType: 'input',
					animation: 0
				}).then((res)=>{
					this.nickname = res.value;
				})
			},
			onMobile(){
				this.$refs['DialogBox'].confirm({
					title: '更改手机号',
					placeholder: '请输入修改的手机号',
					value: this.mobile,
					DialogType: 'input',
					animation: 0
				}).then((res)=>{
					this.mobile = res.value;
				})
			},
			onLiveAddressConfirm(e) {
				const { province, city, area } = e || {}
				if (!province || !city || !area) return
				const liveAddress = formatRegionAddress(province, city, area)
				this.defaultRegionCode = [province.code, city.code, area.code]
				this.defaultRegion = [province.name, city.name, area.name]
				this.saveLiveAddress(liveAddress)
			},
			syncLiveAddressRegion() {
				const parsed = parseRegionFromAddress(this.liveAddress)
				if (parsed) {
					this.defaultRegionCode = parsed.codes
					this.defaultRegion = parsed.names
					return
				}
				this.defaultRegionCode = ['', '', '']
				this.defaultRegion = []
			},
			async saveLiveAddress(liveAddress) {
				try {
					uni.showLoading({ title: '保存中...', mask: true })
					const updateRes = await updateInfo({ liveAddress })
					if (updateRes && updateRes.code && updateRes.code !== 200) {
						throw new Error(updateRes.msg || '保存失败')
					}
					this.liveAddress = liveAddress
					const infoRes = await getInfo()
					const userInfo = infoRes.data || {}
					if (infoRes.liveAddress != null) {
						userInfo.liveAddress = infoRes.liveAddress
					}
					uni.setStorageSync('userInfo', userInfo)
					uni.showToast({ title: '保存成功', icon: 'success' })
				} catch (e) {
					uni.showToast({
						title: (e && e.message) || '保存失败',
						icon: 'none'
					})
				} finally {
					uni.hideLoading({ noConflict: true })
				}
			},
		}
	}
</script>

<style scoped lang="scss">
	@import 'Information.scss';
</style>
