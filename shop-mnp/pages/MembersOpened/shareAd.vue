<template>
	<view class="share-content">
		<view class="page-ad">
			<image src="/static/img/logo.jpg" mode="widthFix"></image>
		</view>
		<view class="info-content">
			<view class="main">
				<view class="user">
					<view class="img">
						<image src="/static/img/user_pic.jpg" mode=""></image>
					</view>
					<text>此处是用户名称</text>
				</view>
				<view class="tips">
					邀请您开通逸享荟会员，尊享多项权益，12000元权益等你来拿！
				</view>
			</view>
			<view class="code">
				<image src="/static/code.png" mode=""></image>
			</view>
		</view>
	</view>
</template>

<script>
</script>

<style lang="scss" scoped>
	.share-content{
		position: fixed;
		left: 24rpx;
		top: 100rpx;
		z-index: 99999;
		width: calc(100% - 48rpx);
		height: auto;
		background: #fff;
		border-radius: 16rpx;
		.page-ad{
			width: 100%;
			height: auto;
			border-radius: 16rpx 16rpx 0 0;
			overflow: hidden;
			image{
				width: 100%;
			}
		}
		.info-content{
			display: flex;
			align-items: center;
			margin: 12rpx 0 20rpx 0;
			.code{
				width: 200rpx;
				height: auto;
				margin: 0 24rpx;
				image{
					width: 200rpx;
					height: 200rpx;
				}
			}
			.main{
				flex: 1;
				margin-left: 24rpx;
				.user{
					display: flex;
					align-items: center;
					.img{
						width: 100rpx;
						height: 100rpx;
						overflow: hidden;
						border-radius: 50%;
						margin-right: 12rpx;
						image{
							width: 100rpx;
							height: 100rpx;
						}
					}
					text{
						font-size: 32rpx;
						font-weight: 600;
					}
				}
				.tips{
					font-size: 30rpx;
					line-height: 1.6;
					color: #666;
				}
			}
		}
	}
	
</style>