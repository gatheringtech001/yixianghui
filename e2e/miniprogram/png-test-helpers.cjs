const assert = require('node:assert/strict')
const zlib = require('node:zlib')

function getPngChunks(image) {
  const chunks = []
  let offset = 8

  while (offset + 12 <= image.length) {
    const length = image.readUInt32BE(offset)
    const type = image.toString('ascii', offset + 4, offset + 8)
    chunks.push({ type, data: image.subarray(offset + 8, offset + 8 + length) })
    offset += length + 12
  }

  return chunks
}

function getTopLeftPaletteAlpha(image) {
  const chunks = getPngChunks(image)
  const header = chunks.find(({ type }) => type === 'IHDR')?.data
  const transparency = chunks.find(({ type }) => type === 'tRNS')?.data
  const compressed = chunks
    .filter(({ type }) => type === 'IDAT')
    .map(({ data }) => data)

  assert.equal(header?.[8], 8, 'icon should use 8-bit color')
  assert.equal(header?.[9], 3, 'icon should use indexed color')
  assert.ok(transparency, 'icon should include palette transparency')

  const scanlines = zlib.inflateSync(Buffer.concat(compressed))
  const topLeftPaletteIndex = scanlines[1]
  return transparency[topLeftPaletteIndex] ?? 255
}

function paethPredictor(left, up, upperLeft) {
  const prediction = left + up - upperLeft
  const distanceToLeft = Math.abs(prediction - left)
  const distanceToUp = Math.abs(prediction - up)
  const distanceToUpperLeft = Math.abs(prediction - upperLeft)

  if (distanceToLeft <= distanceToUp && distanceToLeft <= distanceToUpperLeft) {
    return left
  }
  if (distanceToUp <= distanceToUpperLeft) {
    return up
  }
  return upperLeft
}

function getPaletteAlphaBounds(image, alphaThreshold = 16) {
  const chunks = getPngChunks(image)
  const header = chunks.find(({ type }) => type === 'IHDR')?.data
  const transparency = chunks.find(({ type }) => type === 'tRNS')?.data
  const compressed = chunks
    .filter(({ type }) => type === 'IDAT')
    .map(({ data }) => data)

  assert.equal(header?.[8], 8, 'icon should use 8-bit color')
  assert.equal(header?.[9], 3, 'icon should use indexed color')
  assert.ok(transparency, 'icon should include palette transparency')

  const width = header.readUInt32BE(0)
  const height = header.readUInt32BE(4)
  const scanlines = zlib.inflateSync(Buffer.concat(compressed))
  const rows = []
  let offset = 0

  for (let y = 0; y < height; y += 1) {
    const filter = scanlines[offset]
    const filtered = scanlines.subarray(offset + 1, offset + 1 + width)
    const row = Buffer.alloc(width)
    const previous = rows[y - 1]

    for (let x = 0; x < width; x += 1) {
      const left = x === 0 ? 0 : row[x - 1]
      const up = previous?.[x] ?? 0
      const upperLeft = x === 0 ? 0 : (previous?.[x - 1] ?? 0)
      const predictor = [
        0,
        left,
        up,
        Math.floor((left + up) / 2),
        paethPredictor(left, up, upperLeft)
      ][filter]

      assert.notEqual(predictor, undefined, `unsupported PNG filter ${filter}`)
      row[x] = (filtered[x] + predictor) & 0xff
    }

    rows.push(row)
    offset += width + 1
  }

  let minX = width
  let minY = height
  let maxX = -1
  let maxY = -1

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      if ((transparency[rows[y][x]] ?? 255) < alphaThreshold) {
        continue
      }
      minX = Math.min(minX, x)
      minY = Math.min(minY, y)
      maxX = Math.max(maxX, x)
      maxY = Math.max(maxY, y)
    }
  }

  assert.notEqual(maxX, -1, 'icon should contain visible pixels')
  return {
    x: minX,
    y: minY,
    width: maxX - minX + 1,
    height: maxY - minY + 1
  }
}

module.exports = { getPaletteAlphaBounds, getTopLeftPaletteAlpha }
