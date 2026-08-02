// utils/backPressManager.js
const backPressHandlers = [];

// 注册返回键处理函数
export function registerBackPressHandler(handler) {
  if (backPressHandlers.length === 0) {
    uni.onBackPress(globalBackPressHandler);
  }
  backPressHandlers.push(handler);
}

// 移除返回键处理函数
export function unregisterBackPressHandler(handler) {
  const index = backPressHandlers.indexOf(handler);
  if (index > -1) {
    backPressHandlers.splice(index, 1);
  }
  
  if (backPressHandlers.length === 0) {
    uni.offBackPress(globalBackPressHandler);
  }
}

// 全局返回键处理函数
function globalBackPressHandler() {
  // 从后往前遍历，最新注册的处理器优先处理
  for (let i = backPressHandlers.length - 1; i >= 0; i--) {
    const result = backPressHandlers[i]();
    if (result === true) {
      return true; // 拦截默认行为
    }
  }
  return false; // 允许默认行为
}