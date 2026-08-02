<template>
  <div class="component-upload-image">
    <el-upload
      multiple
      :action="uploadImgUrl"
      list-type="picture-card"
      :on-success="handleUploadSuccess"
      :before-upload="handleBeforeUpload"
      :limit="limit"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      ref="imageUpload"
      :on-remove="handleDelete"
      :show-file-list="true"
      :headers="headers"
      :file-list="fileList"
      :on-preview="handlePictureCardPreview"
      :class="{hide: this.fileList.length >= this.limit}"
    >
      <i class="el-icon-plus"></i>
    </el-upload>

    <!-- 上传提示 -->
    <div class="el-upload__tip" slot="tip" v-if="showTip">
      请上传
      <template v-if="fileSize"> 大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b> </template>
      <template v-if="fileType"> 格式为 <b style="color: #f56c6c">{{ fileType.join("/") }}</b> </template>
      的文件
    </div>

    <el-dialog
      :visible.sync="dialogVisible"
      title="预览"
      width="800"
      append-to-body
    >
      <img
        :src="dialogImageUrl"
        style="display: block; max-width: 100%; margin: 0 auto"
      />
    </el-dialog>
  </div>
</template>

<script>
import { getToken } from "@/utils/auth";
import { isExternal } from "@/utils/validate";

export default {
  props: {
    value: [String, Object, Array],
    // 图片数量限制
    limit: {
      type: Number,
      default: 5,
    },
    // 大小限制(MB)
    fileSize: {
       type: Number,
      default: 3,
    },
    // 文件类型, 例如['png', 'jpg', 'jpeg']
    fileType: {
      type: Array,
      default: () => ["png", "jpg", "jpeg"],
    },
    // 是否显示提示
    isShowTip: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      number: 0,
      uploadList: [],
      dialogImageUrl: "",
      dialogVisible: false,
      hideUpload: false,
      baseUrl: process.env.VUE_APP_BASE_API,
      uploadImgUrl: process.env.VUE_APP_BASE_API + "/common/upload", // 上传的图片服务器地址
      headers: {
        Authorization: "Bearer " + getToken(),
      },
      fileList: [],
      upfile: null
    };
  },
  watch: {
    value: {
      handler(val) {
        if (val) {
          // 首先将值转为数组
          const list = Array.isArray(val) ? val : this.value.split(',');
          // 然后将数组转为对象数组
          this.fileList = list.map(item => {
            if (typeof item === "string") {
              if (item.indexOf(this.baseUrl) === -1 && !isExternal(item)) {
                  item = { name: this.baseUrl + item, url: this.baseUrl + item };
              } else {
                  item = { name: item, url: item };
              }
            }
            return item;
          });
        } else {
          this.fileList = [];
          return [];
        }
      },
      deep: true,
      immediate: true
    }
  },
  computed: {
    // 是否显示提示
    showTip() {
      return this.isShowTip && (this.fileType || this.fileSize);
    },
  },
  methods: {
    // 上传前loading加载
    async handleBeforeUpload(file) {
      let isImg = false;
      if (this.fileType.length) {
        let fileExtension = "";
        if (file.name.lastIndexOf(".") > -1) {
          fileExtension = file.name.slice(file.name.lastIndexOf(".") + 1);
        }
        isImg = this.fileType.some(type => {
          if (file.type.indexOf(type) > -1) return true;
          if (fileExtension && fileExtension.indexOf(type) > -1) return true;
          return false;
        });
      } else {
        isImg = file.type.indexOf("image") > -1;
      }

      if (!isImg) {
        this.$modal.msgError(`文件格式不正确，请上传${this.fileType.join("/")}图片格式文件!`);
        return false;
      }
      if (file.name.includes(',')) {
        this.$modal.msgError('文件名不正确，不能包含英文逗号!');
        return false;
      }
      if (this.fileSize) {
        const isLt = file.size / 1024 / 1024 < this.fileSize;
        if (!isLt) {
          const compressionRatio = this.getCompressionRatio(file.size / 1024 / 1024);

          // 等待压缩完成
          try {
            // 超出限制大小自动压缩图片
            let compressedFile = await this.compressImgSize(file, compressionRatio);

            // 检查压缩后的文件大小
            const compressedSize = compressedFile.size / 1024 / 1024;

            // 如果经过多次压缩仍然超标，给出明确提示
            if (compressedSize >= this.fileSize) {
              this.$modal.msgError(`图片压缩后大小为${compressedSize.toFixed(2)}MB，仍超过限制${this.fileSize}MB，无法上传！`);
              return false;
            }

            this.$modal.msgSuccess(`压缩成功，从${(file.size / 1024 / 1024).toFixed(2)}MB压缩至${compressedSize.toFixed(2)}MB`);

            // 将压缩后的文件作为新的上传文件
            file = compressedFile;
          } catch (error) {
            this.$modal.msgError('图片压缩失败：' + (error.message || error.toString()));
            return false;
          }
        }
      }

      this.$modal.loading("正在上传图片，请稍候...");
      this.number++;
    },
    /** 压缩图片*/
    compressImgSize(file, compressratio) {
      const fileUrl = URL.createObjectURL(file);
      return new Promise((resolve, reject) => {
        const image = new Image();
        image.src = fileUrl;
        image.onload = async () => {
          try {
            if (image.width > 0 && image.height > 0) {
              let compressedFile = await this.compressWithQuality(
                image,
                file,
                compressratio
              );

              // 检查压缩后是否仍然超标，如果超标则继续压缩
              let attempts = 0;
              const maxAttempts = 5;
              let currentRatio = compressratio;

              while (compressedFile.size / 1024 / 1024 >= this.fileSize && attempts < maxAttempts) {
                attempts++;
                // 逐步降低压缩比例和质量
                currentRatio *= 0.8;
                const quality = Math.max(0.3, 0.92 - (attempts * 0.15));

                compressedFile = await this.compressWithQuality(
                  image,
                  file,
                  currentRatio,
                  quality
                );
              }

              resolve(compressedFile);
            }
          } catch (error) {
            reject(error);
          }
        };

        image.onerror = (e) => {
          reject(new Error('图片加载失败'));
          console.error('图片加载失败详情:', {
            fileName: file.name,
            fileSize: file.size,
            fileType: file.type,
            fileUrl: fileUrl,
            error: e
          });
        };

        image.onloadend = () => {
          URL.revokeObjectURL(fileUrl);
        };
      });
    },
    /** 按指定质量和尺寸压缩图片 */
    async compressWithQuality(image, originalFile, sizeRatio, quality = 0.92) {
      // 计算16:9比例下的目标尺寸
      const targetAspectRatio = 16 / 9;
      let targetWidth, targetHeight;

      // 根据原始图片的宽高比决定如何裁剪到16:9
      if (image.width / image.height > targetAspectRatio) {
        // 原始图片更宽，以高度为基准计算宽度
        targetHeight = image.height * sizeRatio;
        targetWidth = targetHeight * targetAspectRatio;
      } else {
        // 原始图片更高，以宽度为基准计算高度
        targetWidth = image.width * sizeRatio;
        targetHeight = targetWidth / targetAspectRatio;
      }

      const canvas = document.createElement('canvas');
      const context = canvas.getContext('2d');
      canvas.width = targetWidth;
      canvas.height = targetHeight;

      // 使用白色背景填充（可选，也可以透明）
      context.fillStyle = '#FFFFFF';
      context.fillRect(0, 0, targetWidth, targetHeight);

      // 计算居中裁剪的位置
      const sourceX = Math.max(0, (image.width - (image.height * targetAspectRatio)) / 2);
      const sourceY = Math.max(0, (image.height - (image.width / targetAspectRatio)) / 2);
      const sourceWidth = Math.min(image.width, image.height * targetAspectRatio);
      const sourceHeight = Math.min(image.height, image.width / targetAspectRatio);

      // 绘制裁剪并缩放后的图片
      context.drawImage(image, sourceX, sourceY, sourceWidth, sourceHeight, 0, 0, targetWidth, targetHeight);

      // 使用原文件的MIME类型
      const mimeType = originalFile.type || 'image/jpeg';
      const imageBase64 = canvas.toDataURL(mimeType, quality);

      const randomName = `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      const fileExtension = originalFile.name.slice(originalFile.name.lastIndexOf('.'));
      const newFileName = `${randomName}${fileExtension}`;

      const newFile = this.base64ToFile(imageBase64, newFileName);

      const compressedFile = new File([newFile], newFileName, {
        type: mimeType
      });

      return compressedFile;
    },
    // base64图片转file的方法（base64图片, 设置生成file的文件名）
    base64ToFile(base64, fileName) {
      // 将base64按照 , 进行分割 将前缀  与后续内容分隔开
      let data = base64.split(',');
      // 利用正则表达式 从前缀中获取图片的类型信息（image/png、image/jpeg、image/webp等）
      let type = data[0].match(/:(.*?);/)[1];
      // 从图片的类型信息中 获取具体的文件格式后缀（png、jpeg、webp）
      let suffix = type.split('/')[1];
      // 使用atob()对base64数据进行解码  结果是一个文件数据流 以字符串的格式输出
      const bstr = window.atob(data[1]);
      // 获取解码结果字符串的长度
      let n = bstr.length
      // 根据解码结果字符串的长度创建一个等长的整形数字数组
      // 但在创建时 所有元素初始值都为 0
      const u8arr = new Uint8Array(n)
      // 将整形数组的每个元素填充为解码结果字符串对应位置字符的UTF-16 编码单元
      while (n--) {
        // charCodeAt()：获取给定索引处字符对应的 UTF-16 代码单元
        u8arr[n] = bstr.charCodeAt(n)
      }
      // 利用构造函数创建File文件对象
      // new File(bits, name, options)
      const file = new File([u8arr], `${fileName}.${suffix}`, {
        type: type
      })
      // 将File文件对象返回给方法的调用者
      return file;
    },
    // 文件个数超出
    handleExceed() {
      this.$modal.msgError(`上传文件数量不能超过 ${this.limit} 个!`);
    },
    // 上传成功回调
    handleUploadSuccess(res, file) {
      if (res.code === 200) {
        this.uploadList.push({ name: res.fileName, url: res.fileName });
        this.uploadedSuccessfully();
      } else {
        this.number--;
        this.$modal.closeLoading();
        this.$modal.msgError(res.msg);
        this.$refs.imageUpload.handleRemove(file);
        this.uploadedSuccessfully();
      }
    },
    /**获取文件大小倍数，生成质量比*/
    getCompressionRatio(fileSize) {
      const multiple = (fileSize / this.fileSize).toFixed(2);
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
    // 删除图片
    handleDelete(file) {
      const findex = this.fileList.map(f => f.name).indexOf(file.name);
      if (findex > -1) {
        this.fileList.splice(findex, 1);
        this.$emit("input", this.listToString(this.fileList));
      }
    },
    // 上传失败
    handleUploadError() {
      this.$modal.msgError("上传图片失败，请重试");
      this.$modal.closeLoading();
    },
    // 上传结束处理
    uploadedSuccessfully() {
      if (this.number > 0 && this.uploadList.length === this.number) {
        this.fileList = this.fileList.concat(this.uploadList);
        this.uploadList = [];
        this.number = 0;
        this.$emit("input", this.listToString(this.fileList));
        this.$modal.closeLoading();
      }
    },
    // 预览
    handlePictureCardPreview(file) {
      this.dialogImageUrl = file.url;
      this.dialogVisible = true;
    },
    // 对象转成指定字符串分隔
    listToString(list, separator) {
      let strs = "";
      separator = separator || ",";
      for (let i in list) {
        if (list[i].url) {
          strs += list[i].url.replace(this.baseUrl, "") + separator;
        }
      }
      return strs != '' ? strs.substr(0, strs.length - 1) : '';
    }
  }
};
</script>
<style scoped lang="scss">
// .el-upload--picture-card 控制加号部分
::v-deep.hide .el-upload--picture-card {
    display: none;
}
// 去掉动画效果
::v-deep .el-list-enter-active,
::v-deep .el-list-leave-active {
    transition: all 0s;
}

::v-deep .el-list-enter, .el-list-leave-active {
  opacity: 0;
  transform: translateY(0);
}
</style>

