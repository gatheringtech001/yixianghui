<template>
  <div>
    <el-upload
      :action="uploadUrl"
      :before-upload="handleBeforeUpload"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      name="file"
      :show-file-list="false"
      :headers="headers"
      style="display: none"
      ref="upload"
      v-if="this.type == 'url'"
    >
    </el-upload>
    <div class="editor" ref="editor" :style="styles"></div>
  </div>
</template>

<script>
import Quill from "quill";
import "quill/dist/quill.core.css";
import "quill/dist/quill.snow.css";
import "quill/dist/quill.bubble.css";
import { getToken } from "@/utils/auth";

export default {
  name: "Editor",
  props: {
    /* 编辑器的内容 */
    value: {
      type: String,
      default: "",
    },
    /* 高度 */
    height: {
      type: Number,
      default: null,
    },
    /* 最小高度 */
    minHeight: {
      type: Number,
      default: null,
    },
    /* 只读 */
    readOnly: {
      type: Boolean,
      default: false,
    },
    /* 上传文件大小限制(MB) */
    fileSize: {
      type: Number,
      default: 5,
    },
    /* 类型（base64格式、url格式） */
    type: {
      type: String,
      default: "url",
    }
  },
  data() {
    return {
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload", // 上传的图片服务器地址
      headers: {
        Authorization: "Bearer " + getToken()
      },
      Quill: null,
      currentValue: "",
      options: {
        theme: "snow",
        bounds: document.body,
        debug: "warn",
        modules: {
          // 工具栏配置
          toolbar: [
            ["bold", "italic", "underline", "strike"],       // 加粗 斜体 下划线 删除线
            ["blockquote", "code-block"],                    // 引用  代码块
            [{ list: "ordered" }, { list: "bullet" }],       // 有序、无序列表
            [{ indent: "-1" }, { indent: "+1" }],            // 缩进
            [{ size: ["small", false, "large", "huge"] }],   // 字体大小
            [{ header: [1, 2, 3, 4, 5, 6, false] }],         // 标题
            [{ color: [] }, { background: [] }],             // 字体颜色、字体背景颜色
            [{ align: [] }],                                 // 对齐方式
            ["clean"],                                       // 清除文本格式
            ["link", "image", "video"]                       // 链接、图片、视频
          ],
        },
        placeholder: "请输入内容",
        readOnly: this.readOnly,
      },
    };
  },
  computed: {
    styles() {
      let style = {};
      if (this.minHeight) {
        style.minHeight = `${this.minHeight}px`;
      }
      if (this.height) {
        style.height = `${this.height}px`;
      }
      return style;
    },
  },
  watch: {
    value: {
      handler(val) {
        if (val !== this.currentValue) {
          this.currentValue = val === null ? "" : val;
          if (this.Quill) {
            this.Quill.clipboard.dangerouslyPasteHTML(this.currentValue);
          }
        }
      },
      immediate: true,
    },
  },
  mounted() {
    this.init();
  },
  beforeDestroy() {
    this.Quill = null;
  },
  methods: {
    init() {
      const editor = this.$refs.editor;
      this.Quill = new Quill(editor, this.options);
      // 如果设置了上传地址则自定义图片上传事件
      if (this.type == 'url') {
        let toolbar = this.Quill.getModule("toolbar");
        toolbar.addHandler("image", (value) => {
          if (value) {
            this.$refs.upload.$children[0].$refs.input.click();
          } else {
            this.quill.format("image", false);
          }
        });
      }
      this.Quill.clipboard.dangerouslyPasteHTML(this.currentValue);
      this.Quill.on("text-change", (delta, oldDelta, source) => {
        const html = this.$refs.editor.children[0].innerHTML;
        const text = this.Quill.getText();
        const quill = this.Quill;
        this.currentValue = html;
        this.$emit("input", html);
        this.$emit("on-change", { html, text, quill });
      });
      this.Quill.on("text-change", (delta, oldDelta, source) => {
        this.$emit("on-text-change", delta, oldDelta, source);
      });
      this.Quill.on("selection-change", (range, oldRange, source) => {
        this.$emit("on-selection-change", range, oldRange, source);
      });
      this.Quill.on("editor-change", (eventName, ...args) => {
        this.$emit("on-editor-change", eventName, ...args);
      });
    },
    // 上传前校检格式和大小
    async handleBeforeUpload(file) {
      const type = ["image/jpeg", "image/jpg", "image/png", "image/svg"];
      const isJPG = type.includes(file.type);
      // 检验文件格式
      if (!isJPG) {
        this.$message.error(`图片格式错误!`);
        return false;
      }

      // 校检文件大小，如果超出则自动压缩
      if (this.fileSize) {
        const isLt = file.size / 1024 / 1024 < this.fileSize;
        if (!isLt) {
          try {
            const compressionRatio = this.getCompressionRatio(file.size / 1024 / 1024);

            // 自动压缩图片
            let compressedFile = await this.compressImgSize(file, compressionRatio);

            // 检查压缩后的文件大小
            const compressedSize = compressedFile.size / 1024 / 1024;

            // 如果经过多次压缩仍然超标，给出明确提示
            if (compressedSize >= this.fileSize) {
              this.$message.error(`图片压缩后大小为${compressedSize.toFixed(2)}MB，仍超过限制${this.fileSize}MB，无法上传！`);
              return false;
            }

            this.$message.success(`压缩成功，从${(file.size / 1024 / 1024).toFixed(2)}MB压缩至${compressedSize.toFixed(2)}MB`);

            // 将压缩后的文件替换原文件进行上传
            // 注意：这里不能直接返回false，而是需要手动上传压缩后的文件
            this.uploadCompressedFile(compressedFile);
            return false; // 阻止默认上传行为
          } catch (error) {
            this.$message.error('图片压缩失败：' + (error.message || error.toString()));
            return false;
          }
        }
      }
      return true;
    },
    /** 获取文件大小倍数，生成质量比 */
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
    /** 压缩图片 */
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
      const targetWidth = image.width * sizeRatio;
      const targetHeight = image.height * sizeRatio;

      const canvas = document.createElement('canvas');
      const context = canvas.getContext('2d');
      canvas.width = targetWidth;
      canvas.height = targetHeight;
      context.drawImage(image, 0, 0, targetWidth, targetHeight);

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
    /** base64图片转file */
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
      // 将整形数组的每个元素填充为解码结果字符串对应位置字符的UTF-16 代码单元
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

    /** 手动上传压缩后的文件 */
    uploadCompressedFile(file) {
      const formData = new FormData();
      formData.append('file', file);

      // 显示加载提示
      const loading = this.$loading({
        lock: true,
        text: '正在上传图片...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      });

      // 使用fetch或axios上传
      fetch(this.uploadUrl, {
        method: 'POST',
        headers: this.headers,
        body: formData
      })
        .then(response => response.json())
        .then(res => {
          loading.close();
          if (res.code === 200) {
            // 上传成功，插入图片到编辑器
            let quill = this.Quill;
            let length = quill.getSelection().index;
            quill.insertEmbed(length, "image", process.env.VUE_APP_BASE_API + res.fileName);
            quill.setSelection(length + 1);
            this.$message.success("图片上传成功");
          } else {
            this.$message.error("图片上传失败：" + res.msg);
          }
        })
        .catch(error => {
          loading.close();
          this.$message.error("图片上传失败：" + error.message);
        });
    },
    handleUploadSuccess(res, file) {
      // 如果上传成功
      if (res.code == 200) {
        // 获取富文本组件实例
        let quill = this.Quill;
        // 获取光标所在位置
        let length = quill.getSelection().index;
        // 插入图片  res.url为服务器返回的图片地址
        quill.insertEmbed(length, "image", process.env.VUE_APP_BASE_API + res.fileName);
        // 调整光标到最后
        quill.setSelection(length + 1);
      } else {
        this.$message.error("图片插入失败");
      }
    },
    handleUploadError() {
      this.$message.error("图片插入失败");
    },
  },
};
</script>

<style>
.editor, .ql-toolbar {
  white-space: pre-wrap !important;
  line-height: normal !important;
}
.quill-img {
  display: none;
}
.ql-snow .ql-tooltip[data-mode="link"]::before {
  content: "请输入链接地址:";
}
.ql-snow .ql-tooltip.ql-editing a.ql-action::after {
  border-right: 0px;
  content: "保存";
  padding-right: 0px;
}
.ql-snow .ql-tooltip[data-mode="video"]::before {
  content: "请输入视频地址:";
}
.ql-snow .ql-picker.ql-size .ql-picker-label::before,
.ql-snow .ql-picker.ql-size .ql-picker-item::before {
  content: "14px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="small"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="small"]::before {
  content: "10px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="large"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="large"]::before {
  content: "18px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="huge"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="huge"]::before {
  content: "32px";
}
.ql-snow .ql-picker.ql-header .ql-picker-label::before,
.ql-snow .ql-picker.ql-header .ql-picker-item::before {
  content: "文本";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="1"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="1"]::before {
  content: "标题1";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="2"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="2"]::before {
  content: "标题2";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="3"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="3"]::before {
  content: "标题3";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="4"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="4"]::before {
  content: "标题4";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="5"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="5"]::before {
  content: "标题5";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="6"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="6"]::before {
  content: "标题6";
}
.ql-snow .ql-picker.ql-font .ql-picker-label::before,
.ql-snow .ql-picker.ql-font .ql-picker-item::before {
  content: "标准字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="serif"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="serif"]::before {
  content: "衬线字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="monospace"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="monospace"]::before {
  content: "等宽字体";
}
</style>
