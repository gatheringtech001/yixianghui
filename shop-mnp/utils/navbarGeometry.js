// All inputs and outputs are CSS pixels, not rpx.
export function navbarTitleGeometry(windowWidth, requestedWidth, capsuleLeft, backWidth) {
  const viewport = Math.max(0, Number(windowWidth) || 0)
  const leftEdge = Math.min(viewport, Math.max(8, Number(backWidth) || 8))
  const rightEdge = Math.max(leftEdge, Math.min(viewport - 8, capsuleLeft - 8))
  const width = Math.max(0, Math.min(Number(requestedWidth) || 0, rightEdge - leftEdge))
  const left = Math.max(leftEdge, Math.min((viewport - width) / 2, rightEdge - width))
  return { left: left + 'px', width: width + 'px' }
}
