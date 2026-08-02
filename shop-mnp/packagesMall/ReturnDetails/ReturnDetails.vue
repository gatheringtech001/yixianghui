<template>
	<view class="page">
		<!-- 商品 -->
		<view class="goods-card" v-if="orderDetail">
			<view class="goods-row" v-for="(item, index) in orderDetail.goodsList" :key="index">
				<image class="thumb" :src="host + item.goodsCover" mode="aspectFill"></image>
				<view class="goods-info">
					<text class="goods-name one-omit">{{ item.goodsName }}</text>
					<text class="goods-meta">数量：{{ orderDetail.goodsCount }}</text>
					<text class="goods-price">￥{{ formatMoney(displayOrderAmount) }}</text>
				</view>
			</view>
		</view>

		<!-- 申请表单 -->
		<view class="form-card">
			<view class="form-row" @click="isApplyCause = true">
				<text class="label">申请原因</text>
				<view class="value">
					<text :class="{ placeholder: refundReason < 0 }">{{ refundReason < 0 ? '请选择' : appReason }}</text>
					<text class="iconfont icon-more1 arrow"></text>
				</view>
			</view>
			<view class="form-row">
				<text class="label">申请金额</text>
				<view class="value amount">
					<input
						class="amount-input"
						type="digit"
						v-model="form.appRefundMoney"
						placeholder="可不填，默认实付金额"
					/>
					<text class="unit">元</text>
				</view>
			</view>
			<view class="form-block">
				<text class="label">问题描述</text>
				<textarea
					class="desc-input"
					placeholder="请具体描述申请原因（选填）"
					v-model="form.reasonDescription"
					maxlength="200"
				/>
			</view>
			<view class="form-block voucher-block">
				<text class="label">上传凭证<span class="optional">（选填）</span></text>
				<u-upload
					:action="action"
					:file-list="fileList"
					:autoUpload="true"
					:before-upload="beforeUpload"
					:showProgress="true"
					:source-type="uploadSourceType"
					:size-type="sizeType"
					:limitType="limitType"
					:camera="cameraPosition"
					@afterRead="afterRead"
					@on-success="handleChange"
					:maxSize="maxSize"
					:maxCount="5"
					:previewFullImage="true"
				></u-upload>
			</view>
		</view>

		<!-- 提交 -->
		<view class="footer-btn">
			<view class="btn" @click="returnPament()">提交</view>
		</view>

		<!-- 申请原因弹窗 -->
		<view class="apply-cause-win cu-modal bottom-modal" :class="{ show: isApplyCause }" @click="isApplyCause = false">
			<view class="cu-dialog" @click.stop>
				<view class="sheet-handle"></view>
				<view class="title">申请原因</view>
				<view class="cause-list">
					<view
						class="list"
						v-for="item in causeOptions"
						:key="item.value"
						:class="{ active: refundReason === item.value }"
						@click="selectCause(item)"
					>
						<text class="cause">{{ item.label }}</text>
						<text
							class="iconfont check-icon"
							:class="refundReason === item.value ? 'icon-checked action' : 'icon-check'"
						></text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getOrderDetail,refundOrder } from '@/api/member/index'
	export default {
		data() {
			return {
				isApplyCause: false,
				host: this.$host,
				orderDetail: null,
				limitType: ['png', 'jpg', 'jpeg'],
        cameraPosition: 'back',
        uploadSourceType: ['camera','album'],
        sizeType: ['original', 'compressed'],
		fileMaxSize: 1 * 1024 * 1024,
						maxSize:2 * 1024 * 1024,
						fileMinSize: 5 * 1024,
				appReason: '申请原因',
				refundReason: -1,
				causeOptions: [
					{ label: '质量问题', value: 0 },
					{ label: '拍错/多拍/不想要', value: 1 },
					{ label: '协商一致退款', value: 2 },
					{ label: '缺货', value: 3 },
					{ label: '未按约定时间发货', value: 4 },
					{ label: '其他', value: 5 }
				],
				action: this.$host+"/common/upload",
				fileList: [],
        fileObjs: [],
        refundData: {},
        form:{
          refundReason: null,
          reasonDescription: '',
          appRefundMoney: ''
        },
		submitting: false
			};
		},
		computed: {
			displayOrderAmount() {
				if (!this.orderDetail) return 0
				const paid = Number(this.orderDetail.payMoney)
				if (Number.isFinite(paid) && paid > 0) return paid
				const payable = Number(this.orderDetail.moneyPayable)
				return Number.isFinite(payable) ? payable : 0
			}
		},
		onLoad(option) {
			this.getOrderInfo(option.orderId)
		},
		methods:{
			formatMoney(value) {
				const num = Number(value)
				return Number.isFinite(num) ? num.toFixed(2) : '0.00'
			},
			selectCause(item) {
				this.appReason = item.label
				this.refundReason = item.value
				this.isApplyCause = false
			},
			getOrderInfo(orderId) {
				getOrderDetail({ orderId: orderId }).then(res => {
					this.orderDetail = res.data
					const amount = this.displayOrderAmount
					this.form.appRefundMoney = this.formatMoney(amount)
				})
			},
      beforeUpload(index, list) {
        const fileName = list[index].url
        const suffix = fileName.slice(fileName.lastIndexOf('.') + 1).toLowerCase()
        const validTypes = ['jpg', 'jpeg', 'png']
        if (!validTypes.includes(suffix)) {
          uni.showToast({
            title: '只能上传 JPG/PNG 格式的图片!',
            icon: 'none',
            duration: 2000
          })
          return false
        }
        return true
      },
	  			getCompressionRatio(fileSize) {
	  				const multiple = (fileSize / this.fileMaxSize).toFixed(2);
	  				let compressionRatio = 1;
	  				if (multiple > 5) {
	  					compressionRatio = 0.5
	  				} else if (multiple > 4) {
	  					compressionRatio = 0.6
	  				} else if (multiple > 3) {
	  					compressionRatio = 0.7
	  				} else if (multiple > 2) {
	  					compressionRatio = 0.8
	  				} else if (multiple > 1) {
	  					compressionRatio = 0.9
	  				} else {
	  					compressionRatio = 2
	  				}
	  				return compressionRatio;
	  			},
							async afterRead(event) {
								let lists = [].concat(event.file);
								let fileListLen = this[`fileList${event.name}`].length;
								for (let index in lists) {
									const item = lists[index];
									const fileSize = item.size;
									const fileName = item.name ?? '';										
									if(!this.isAssetTypeAnImage(item.name)) {
										this.$.msg('不允许该文件类型上传');						
										return false
									}
									if (fileSize > this.fileMaxSize) {
										const compressionRatio = this.getCompressionRatio(fileSize);						
										if (compressionRatio > 1) {
											this.$.msg('文件' + fileName + '大于10M');
											return false
										}
										await this.compressImg(item, compressionRatio)
										if (item.size > this.maxSize) {
											this.$.msg('文件' + fileName + '压缩后超出2M')
											return false
										}
									}
									if (item.size < this.fileMinSize) {
										this.$.msg('文件' + fileName + '不能小于5KB');
										return false
									}					
									this[`fileList${event.name}`].push({
										...item,
										status: 'uploading',
										message: '上传中'
									})
								}
								for (let i = 0; i < lists.length; i++) {
									const result = await this.uploadFilePromise(lists[i].url);
									if (!result.Success) {
										uni.$showMsg(result.Message);
										const index = this.fileList1.findIndex(event => event.name === lists[i].name)
										if (index !== -1) return this.fileList1.splice(index, 1);
									}
									let item = this[`fileList${event.name}`][fileListLen]
									this[`fileList${event.name}`].splice(fileListLen, 1, Object.assign(item, {
										status: 'success',
										message: '',
										url: lists[i].url,
										res: result
									}))
									fileListLen++
								}
								this.imgList(this.fileList1);
							},
							compressImg(source, compressionRatio) {
								let that = this;
								return new Promise((resolve, reject) => {
									that.$.compressImg(source.url, compressionRatio, source.type, compressRes => {
										resolve(compressRes);
									})
								}).then((res) => {
									source.size = res.size
									source.url = res.source
									source.thumb = res.source					
									return source
								}).catch(err => {
									console.log('图片压缩失败', err)
								})
							},
			clearImgList() {
				this.fileList1 = []
			},
			imgList(list) {
				let arr = []
				let newArr = []
				list.map(item => {
					arr.push(item.res)
				})
				arr.map(x => {
					newArr.push(x.Data[0])
				})
				this.$emit('imgList', Array.from(new Set(newArr)))
			},
			isAssetTypeAnImage(ext) {
				const str = ext.split('.')[1];
				return this.limitType.indexOf(str.toLowerCase()) !== -1;			    
			},
      handleChange(data,dataindex, list,listindex) {
        uni.showToast({
          title: '上传成功!',
          icon: 'success',
          duration: 2000
        })
        this.fileObjs.push(data)
      },
			goOrderList() {
				// 回退售后类型页 + 申请页，直接回到订单列表
				uni.navigateBack({
					delta: 2,
					fail: () => {
						uni.redirectTo({
							url: '/packagesMall/MyOrderList/MyOrderList?type=4'
						})
					}
				})
			},
			returnPament(){
				if (this.submitting) return
				if(this.refundReason==-1){
					uni.showToast({
					  title: '请选择申请原因',
					  icon: 'none',
					  duration: 2000
					})
					return
				}
				const moneyInput = this.form.appRefundMoney
				const hasMoneyInput = moneyInput !== '' && moneyInput !== null && moneyInput !== undefined
				let refundAmount = Number(this.displayOrderAmount)
				if (hasMoneyInput) {
					refundAmount = Number(moneyInput)
					if(!Number.isFinite(refundAmount) || refundAmount <= 0){
						uni.showToast({
						  title: '退款金额不正确',
						  icon: 'none',
						  duration: 2000
						})
						return
					}
					if (refundAmount > Number(this.displayOrderAmount) + 0.0001) {
						uni.showToast({
							title: '退款金额不能大于实付金额',
							icon: 'none'
						})
						return
					}
				}
				const doSubmit = () => {
					this.submitting = true
					this.refundData = {
						afterType:  '2',
						userId: this.orderDetail.userId,
						orderId: this.orderDetail.orderId,
						outOrderNo: this.orderDetail.orderNo,
						orderMoney: this.displayOrderAmount,
						goodsId: this.orderDetail.goodsId,
						goodsCount: this.orderDetail.goodsCount,
						goodsMoney: this.displayOrderAmount,
						refundReason: this.refundReason,
						reasonDescription: String(this.form.reasonDescription || '').trim(),
						appRefundMoney: refundAmount,
						fileList: this.fileObjs
					}
					refundOrder(this.refundData).then(res => {
						if(res.code == 200) {
							uni.showToast({
								title: '申请成功',
								icon: 'success',
								duration: 1500
							})
							setTimeout(() => {
								this.goOrderList()
							}, 1500)
						}else{
							this.submitting = false
							uni.showToast({
								title: res.msg || '申请失败',
								icon: 'none',
								duration: 2000
							})
						}
					}).catch(err => {
						this.submitting = false
						uni.showToast({
							title: (err && (err.message || err.msg)) || '申请失败',
							icon: 'none'
						})
					})
				}

				doSubmit()
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'ReturnDetails.scss';
</style>
