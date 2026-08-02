<template>
	<view class="page">
		<view class="head-banner">
			<image :src='host + banner' mode="widthFix"></image>
		</view>
		<view class="reject-tip" v-if="pageMode === 'form' && rejectedTip">
			<u-icon name="info-circle-fill" color="#ff6b00" size="32"></u-icon>
			<text>{{ rejectedTip }}</text>
		</view>
		<view class="apply" v-if="pageMode === 'form'">
			<view class="title">欢迎加入<text>逸享荟康养顾问中心</text>，请填写申请信息</view>
			<view class="form-box">
				<u-form :model="form" ref="uForm" label-position="left" label-width='160'>
					<u-form-item label="邀请人">
						<text>{{ inviterName }}</text>
					</u-form-item>
					<u-form-item label="姓名" required prop="name">
						<u-input v-model="form.name" />
					</u-form-item>
					<u-form-item label="手机号" required prop="mobile">
						<u-input v-model="form.mobile" maxlength='11' type="number" />
					</u-form-item>
				</u-form>
			</view>
			<view class="tips">
				<u-checkbox v-model="checked" shape="circle">我已经阅读并同意</u-checkbox>
				<text class="article" @click="goToArticle">【顾问申请协议】</text>
			</view>
		</view>
		<view class="apply status-box" v-else>
			<u-empty :text="statusText" mode="list"></u-empty>
		</view>
		<view class="submit" v-if="pageMode === 'form'">
			<u-button type="error" shape="circle" @click="submit">申请成为康养顾问</u-button>
		</view>
	</view>
</template>

<script>
	import './index.scss'
	import { getBannerList } from '@/api/index'
	import { getInfo } from '@/api/public'
	import { applyConsultant, getConsultantParent } from '@/api/member/index'
	import { syncConsultantStorage } from '@/utils/login'
	
	export default {
		data() {
			return {
				host: this.$host,
				banner: '',
				inviterName: '逸享荟',
				pageMode: 'form',
				statusText: '',
				rejectedTip: '',
				form: {
					name: '',
					mobile: null
				},
				rules: {
					name: [
						{ required: true, message: '请输入姓名', trigger: ['change','blur'] },
					],
					mobile: [
						{ required: true, message: '请输入手机号', trigger: ['change','blur'] },
						{
							validator: (rule, value, callback) => {
								return this.$u.test.mobile(value);
							},
							message: '手机号码不正确',
							trigger: ['change','blur']
						}
					]
				},
				checked: false
			};
		},
		onReady() {
			if (this.pageMode === 'form' && this.$refs.uForm) {
				this.$refs.uForm.setRules(this.rules);
			}
		},
		onLoad() {
			this.initPage()
		},
		methods:{
			async initPage() {
				await Promise.all([this.getAdList(), this.loadInviter(), this.checkConsultantStatus()])
			},
			async checkConsultantStatus() {
				try {
					const res = await getInfo()
					const consultant = res.consultant
					if (!consultant) return
					syncConsultantStorage(consultant)
					if (consultant.status === '00') {
						this.pageMode = 'pending'
						this.statusText = '顾问申请审核中，请耐心等待'
					} else if (consultant.status === '02') {
						this.pageMode = 'form'
						this.rejectedTip = consultant.remark
							? `上次申请未通过：${consultant.remark}，可重新提交`
							: '上次申请未通过，请修改信息后重新提交'
						if (consultant.consultantName) this.form.name = consultant.consultantName
						if (consultant.mobile) this.form.mobile = consultant.mobile
					} else if (consultant.status === '01') {
						uni.redirectTo({
							url: '/packagesMember/retail/index'
						})
					}
				} catch (e) {}
			},
			async loadInviter() {
				try {
					const res = await getConsultantParent()
					if (res.code === 200 && res.data) {
						this.inviterName = res.data.nickName || res.data.userName || res.data.consultantName || '逸享荟'
					}
				} catch (e) {}
			},
			async getAdList() {
				let params = {
					positionId: 4
				}
				let { data } = await getBannerList(params)
				if (data && data.length > 0) {
					this.banner = data[0].adImage
				}
			},
			goToArticle() {
				uni.navigateTo({
					url: '/packagesPublic/Article/index?id=2'
				})
			},
			submit() {
				if(!this.checked) {
					uni.showToast({
						icon: 'none',
						title: '请阅读并勾选《顾问申请协议》'
					})
					return
				}
				this.$refs.uForm.validate(valid => {
					if (valid) {
						let params = {
							consultantName: this.form.name,
							mobile: this.form.mobile
						}
						applyConsultant(params).then(async res => {
							if (res.code !== 200) {
								uni.showToast({
									icon: 'none',
									title: res.msg || '申请失败'
								})
								return
							}
							const info = await getInfo()
							syncConsultantStorage(info.consultant)
							uni.showToast({
								icon: 'none',
								title: '申请成功，等待审核'
							})
							this.pageMode = 'pending'
							this.statusText = '顾问申请审核中，请耐心等待'
						})
					}
				});
			}
		},
	}
</script>
