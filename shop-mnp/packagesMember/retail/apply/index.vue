<template>
	<view class="talent-join-page">
		<view class="join-hero">
			<image class="hero-image" src="/packagesMember/static/talent-join-hero.jpg" mode="aspectFill" />
			<view class="hero-copy">
				<view class="hero-title"><view>把热爱变成连接</view><view>和我们一起出发</view></view>
				<view class="hero-description"><text>分享旅居体验</text><text>陪伴美好生活</text></view>
			</view>
		</view>
		<view class="role-intro">
			<view class="role-item"><text class="role-title">旅居达人</text><text class="role-description">分享真实旅居体验</text></view>
			<view class="role-item"><text class="role-title">康养顾问</text><text class="role-description">提供贴心康养服务</text></view>
		</view>
		<view class="join-card" v-if="pageMode === 'form'">
			<view class="card-title">申请加入我们</view>
			<label class="form-field"><text class="form-label">姓名</text><input class="join-input" v-model="form.name" maxlength="50" placeholder="请输入您的真实姓名" placeholder-class="input-placeholder" /></label>
			<label class="form-field"><text class="form-label">手机号</text><input class="join-input" v-model="form.mobile" type="number" maxlength="11" placeholder="请输入可联系的手机号" placeholder-class="input-placeholder" /></label>
			<view class="terms-toggle" role="button" :aria-expanded="showTerms" @click="toggleTerms">
				<text>达人加入条款</text><view class="terms-toggle-label"><text>{{ showTerms ? '收起' : '阅读' }}</text><u-icon :name="showTerms ? 'arrow-up' : 'arrow-down'" size="26" color="#701018" /></view>
			</view>
			<view class="terms-content" v-if="showTerms">
				<text class="term">1. 请提供本人真实姓名和可联系的手机号，用于申请登记、联系服务及合作对接。</text>
				<text class="term">2. 分享真实体验，尊重他人隐私与知识产权；不夸大服务效果，不承诺保本或固定收益。</text>
				<text class="term">3. 康养服务介绍不替代医疗诊断、治疗或专业医疗建议。对外服务内容与合作规则需如实说明。</text>
				<text class="term">4. 申请通过仅代表完成加入登记。具体业务身份、服务范围和经营数据权限以达人中心的规则及确认结果为准。</text>
				<text class="term">5. 姓名和手机号仅用于上述申请及合作目的。您可通过小程序客服咨询资料更正或退出合作。</text>
				<text class="terms-version">条款版本：2026-09-v1</text>
			</view>
			<view class="agreement-row" @click="checked = !checked">
				<view class="consent-check" :class="{ 'is-checked': checked }" role="checkbox" :aria-checked="checked"><u-icon v-if="checked" name="checkmark" size="24" color="#ffffff" /></view>
				<view class="agreement-copy">我已阅读并同意<text class="policy-link" @click.stop="toggleTerms">《达人加入条款》</text>及<text class="policy-link" @click.stop="openPrivacy">《隐私政策》</text></view>
			</view>
			<view class="join-error" v-if="error" role="alert">{{ error }}</view>
			<button class="primary-button" :disabled="submitting || checking" :loading="submitting" @click="submit">{{ submitting ? '正在提交…' : (checking ? '正在确认状态…' : '同意条款，提交申请') }}</button>
		</view>
		<view class="join-card approved-card" v-else>
			<view class="card-title">申请已通过</view><text class="join-note">欢迎加入我们！继续前往达人中心完善注册。</text>
			<view class="join-error" v-if="error" role="alert">{{ error }}</view>
			<button class="primary-button" @click="openCenter">进入达人中心</button>
		</view>
		<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>

<script>
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	import { getInfo } from '@/api/public'
	import { applyConsultant } from '@/api/member/index'
	import { isAuthorizedUser, runWithAuth, bindPageAuthPopup, syncConsultantStorage } from '@/utils/login'

	export default {
		components: { AuthProfilePopup },
		data() {
			return { form: { name: '', mobile: '' }, checked: false, showTerms: false, submitting: false,
				checking: false, pageMode: 'form', error: '' }
		},
		onShow() {
			bindPageAuthPopup(this)
			if (!this.submitting) this.checkConsultantStatus()
		},
		methods: {
			toggleTerms() { this.showTerms = !this.showTerms },
			async checkConsultantStatus() {
				if (this.checking || !isAuthorizedUser()) return
				this.checking = true
				this.error = ''
				try {
					const res = await getInfo()
					if (res.code !== 200) throw new Error(res.msg || '申请状态获取失败，请重试')
					const consultant = res.consultant
					syncConsultantStorage(consultant)
					if (consultant && consultant.consultantId && consultant.status === '01') {
						this.pageMode = 'approved'
						await this.openCenter()
					} else if (consultant) {
						if (!this.form.name) this.form.name = consultant.consultantName || ''
						if (!this.form.mobile) this.form.mobile = consultant.mobile || ''
					}
				} catch (error) {
					this.error = error.message || '申请状态获取失败，请稍后重试'
				} finally { this.checking = false }
			},
			openPrivacy() { uni.navigateTo({ url: '/packagesPublic/Article/index?id=5' }) },
			async openCenter() {
				this.error = ''
				try {
					await new Promise((resolve, reject) => uni.redirectTo({
						url: '/packagesPublic/TalentCenter/index', success: resolve, fail: reject
					}))
				} catch (error) { this.error = '申请已通过，页面打开失败，请点击“进入达人中心”重试' }
			},
			async submit() {
				if (this.submitting || this.checking) return
				this.error = ''
				const name = String(this.form.name || '').trim()
				const mobile = String(this.form.mobile || '').trim()
				if (!this.checked) { this.error = '请先阅读并同意达人加入条款及隐私政策'; return }
				if (!name || name.length > 50) { this.error = '请填写不超过50个字的真实姓名'; return }
				if (!/^1[3-9][0-9]{9}$/.test(mobile)) { this.error = '请输入正确的11位手机号'; return }
				this.submitting = true
				try {
					const loggedIn = await new Promise(resolve => runWithAuth(this, resolve))
					if (!loggedIn) return
					const res = await applyConsultant({
						consultantName: name, mobile, acceptedTerms: true, termsVersion: '2026-09-v1'
					})
					if (res.code !== 200) throw new Error(res.msg || '申请失败，请重试')
					const info = await getInfo()
					if (info.code !== 200 || !info.consultant || !info.consultant.consultantId || info.consultant.status !== '01') {
						throw new Error('申请已提交，但通过状态尚未确认，请稍后重试或联系客服')
					}
					syncConsultantStorage(info.consultant)
					this.pageMode = 'approved'
					await this.openCenter()
				} catch (error) { this.error = error.message || '提交失败，请稍后重试' }
				finally { this.submitting = false }
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'index.scss';
</style>
