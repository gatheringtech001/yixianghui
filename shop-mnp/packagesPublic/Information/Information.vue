<template>
	<view class="page">
		<scroll-view class="information-scroll" scroll-y :show-scrollbar="false">
			<view class="information-shell">
				<view class="profile-summary">
					<image class="summary-brand" src="/static/home-design/brand-logo-transparent.png" mode="aspectFit" />
					<image class="summary-avatar" :src="avatarDisplay" mode="aspectFill" />
					<view class="summary-copy">
						<text class="summary-name">{{ nickname || '逸享荟用户' }}</text>
						<text class="summary-description">完善资料，享受更贴心的旅居服务</text>
					</view>
				</view>

				<view class="section-heading">
					<text class="section-title">基本信息</text>
					<text class="section-caption">管理头像与联系方式</text>
				</view>
				<view class="information-card">
					<view class="information-row avatar-row">
						<text class="row-title">头像</text>
						<image class="row-avatar" :src="avatarDisplay" mode="aspectFill" />
					</view>
					<view class="information-row" @click="onNickname">
						<text class="row-title">昵称</text>
						<view class="row-value">
							<text class="row-text">{{ nickname || '未设置' }}</text>
							<u-icon name="arrow-right" color="#a49c93" size="26" />
						</view>
					</view>
					<view class="information-row" @click="onMobile">
						<text class="row-title">手机号</text>
						<view class="row-value">
							<text class="row-text">{{ mobile || '未绑定' }}</text>
							<u-icon name="arrow-right" color="#a49c93" size="26" />
						</view>
					</view>
				</view>

				<view class="section-heading">
					<text class="section-title">更多信息</text>
					<text class="section-caption">让服务更贴合您的需要</text>
				</view>
				<view class="information-card">
					<view class="information-row picker-row">
						<text class="row-title">性别</text>
						<view class="row-value">
							<text class="row-text">{{ sexText }}</text>
							<u-icon name="arrow-right" color="#a49c93" size="26" />
						</view>
						<picker class="row-picker" @change="sexPickerChange" :value="sexIndex" :range="sexArray">
							<view class="picker-target"></view>
						</picker>
					</view>
					<view class="information-row picker-row">
						<text class="row-title">出生日期</text>
						<view class="row-value">
							<text class="row-text">{{ birthday || '未设置' }}</text>
							<u-icon name="arrow-right" color="#a49c93" size="26" />
						</view>
						<picker class="row-picker" @change="birthdayPickerChange" mode="date" :value="birthdayDate" :start="startDate" :end="endDate">
							<view class="picker-target"></view>
						</picker>
					</view>
					<view class="information-row" @click="showRegionPicker = true">
						<text class="row-title">现在居住地址</text>
						<view class="row-value address-value">
							<text class="row-text">{{ liveAddress || '请选择' }}</text>
							<u-icon name="arrow-right" color="#a49c93" size="26" />
						</view>
					</view>
				</view>
			</view>
		</scroll-view>
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
				birthday: '',
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
			const sexIndex = Number(this.userInfo.sex)
			this.sexIndex = this.sexArray[sexIndex] ? sexIndex : 2
			this.sexText = this.sexArray[this.sexIndex]
			if (this.userInfo.birthday) {
				this.birthday = String(this.userInfo.birthday).slice(0, 10)
				this.birthdayDate = this.birthday
			}
			this.liveAddress = this.userInfo.liveAddress || ''
			this.syncLiveAddressRegion()
		},
		onLoad() {},
		computed: {
			avatarDisplay() {
				const avatar = this.userInfo && this.userInfo.avatar
				if (!avatar) return '/static/home-design/profile-avatar.png'
				if (avatar.startsWith('http') || avatar.startsWith('wxfile://')) return avatar
				if (avatar.startsWith('/')) return `${this.host}${avatar}`
				return avatar
			}
		},
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
				this.birthdayDate = e.detail.value;
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
