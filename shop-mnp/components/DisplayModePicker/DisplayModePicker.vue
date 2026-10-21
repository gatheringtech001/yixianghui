<template>
	<view v-if="visible" class="mode-mask" @touchmove.stop.prevent>
		<scroll-view class="mode-sheet" scroll-y role="dialog" aria-label="选择显示模式" @tap.stop>
			<view class="mode-heading">选择适合您的显示方式</view>
			<text class="mode-description">内容和功能一样，选择您看得更清楚的方式。</text>
			<button class="mode-option" :class="{ selected: pendingMode === 'standard' }" @tap="pendingMode = 'standard'">
				<view class="mode-option-heading"><text>标准模式</text><text class="mode-check">{{ pendingMode === 'standard' ? '已选择' : '选择' }}</text></view>
				<text class="mode-sample standard-sample">自在旅居，乐享生活</text>
				<text class="mode-option-note">保持现在的字号和布局</text>
			</button>
			<button class="mode-option" :class="{ selected: pendingMode === 'senior' }" @tap="pendingMode = 'senior'">
				<view class="mode-option-heading"><text>长辈模式</text><text class="mode-check">{{ pendingMode === 'senior' ? '已选择' : '选择' }}</text></view>
				<text class="mode-sample senior-sample">自在旅居，乐享生活</text>
				<text class="mode-option-note">大字更清楚，按钮更好点</text>
			</button>
			<text class="mode-footnote">以后可在「我的 → 设置 → 显示模式」随时切换</text>
			<button class="mode-confirm" @tap="confirm">{{ firstChoice ? '选好了，继续' : '保存显示方式' }}</button>
			<button v-if="!firstChoice" class="mode-cancel" @tap="$emit('close')">暂不修改</button>
		</scroll-view>
	</view>
</template>

<script>
import { selectDisplayMode } from '@/utils/displayMode'
export default {
	name: 'DisplayModePicker',
	props: { visible: Boolean, firstChoice: Boolean },
	data() { return { pendingMode: this.isSeniorMode ? 'senior' : 'standard' } },
	watch: {
		visible(value) { if (value) this.pendingMode = this.isSeniorMode ? 'senior' : 'standard' }
	},
	methods: {
		confirm() {
			if (!selectDisplayMode(this.pendingMode)) {
				uni.showToast({ title: '保存失败，请重试', icon: 'none' })
				return
			}
			this.$emit('close')
		}
	}
}
</script>

<style scoped lang="scss">
/* Above the existing login overlay; only one deliberate display choice is shown. */
.mode-mask { position: fixed; inset: 0; z-index: 12000; background: rgba(35, 26, 23, .5); display: flex; align-items: center; justify-content: center; padding: 16px; box-sizing: border-box; }
.mode-sheet { width: 100%; max-width: 420px; height: calc(100vh - 64px); max-height: 640px; background: #f7f5f0; border: 1px solid #e8e1d8; border-radius: 20px; padding: 24px 18px; box-sizing: border-box; }
.mode-heading { font-size: 22px; line-height: 1.4; font-weight: 750; color: #222; }
.mode-description, .mode-footnote { display: block; font-size: 16px; line-height: 1.6; color: #555; margin: 12px 0; }
.mode-option { text-align: left; margin: 12px 0; padding: 16px; border-radius: 12px; border: 2px solid #d6cec5; background: #fff; color: #222; line-height: 1.5; }
.mode-option::after, .mode-confirm::after, .mode-cancel::after { border: 0; }
.mode-option.selected { border-color: #701018; background: #fcf3f0; }
.mode-option-heading { display: flex; justify-content: space-between; gap: 8px; font-size: 18px; font-weight: 700; }
.mode-check { font-size: 16px; color: #701018; flex-shrink: 0; }
.mode-sample, .mode-option-note { display: block; margin-top: 8px; }
.standard-sample { font-size: 16px; }
.senior-sample { font-size: 22px; font-weight: 650; }
.mode-option-note { font-size: 16px; color: #555; }
.mode-confirm, .mode-cancel { display: flex; align-items: center; justify-content: center; min-height: 52px; font-size: 18px; font-weight: 700; line-height: 1.5; border-radius: 12px; }
.mode-confirm { background: #701018; color: #fff; }
.mode-cancel { background: transparent; color: #701018; margin-top: 8px; }
</style>
