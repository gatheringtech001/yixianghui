#!/usr/bin/env node

import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const API_BASE = 'https://shzxj.lk01.cn/api/mnp/index'
const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.resolve(SCRIPT_DIR, '../..')
const SCHEMA_PATH = path.join(REPO_ROOT, 'sql/e2e-product-schema.sql')
const OUTPUT_PATH = path.join(REPO_ROOT, 'sql/e2e-production-products.sql')

async function requestJson(url, init) {
	const response = await fetch(url, init)
	if (!response.ok) {
		throw new Error(`${response.status} ${response.statusText}: ${url}`)
	}
	const body = await response.json()
	if (body.code !== 200) {
		throw new Error(`${body.code || 'unknown'} ${body.msg || 'API error'}: ${url}`)
	}
	return body.data
}

function sqlValue(value) {
	if (value === null || value === undefined) return 'NULL'
	if (typeof value === 'number') return Number.isFinite(value) ? String(value) : 'NULL'
	return `'${String(value)
		.replaceAll('\\', '\\\\')
		.replaceAll("'", "''")}'`
}

function insertRows(table, columns, rows) {
	if (!rows.length) return `-- ${table}: production returned no rows.\n`
	const values = rows.map(row => (
		`    (${columns.map(column => sqlValue(row[column])).join(', ')})`
	))
	return [
		`INSERT INTO ${table} (${columns.join(', ')}) VALUES`,
		`${values.join(',\n')};`,
		''
	].join('\n')
}

function maxId(rows, key) {
	return Math.max(0, ...rows.map(row => Number(row[key]) || 0)) + 1
}

function buildSql(schemaSql, { categories, goods, features, skus, options, skuData, educationExt }) {
	const lines = [
		'-- Production product snapshot for the local mini program E2E database.',
		`-- Source: ${API_BASE}`,
		`-- Captured: ${new Date().toISOString()}`,
		'-- This fixture intentionally replaces the legacy local product mocks.',
		'',
		schemaSql.trim(),
		''
	]

	lines.push(insertRows('app_goods_category', [
		'category_id', 'parent_id', 'parent_ids', 'category_name', 'category_icon',
		'is_hot', 'link_type', 'link_id', 'remark', 'order_num', 'status'
	], categories))
	lines.push(insertRows('app_goods', [
		'goods_id', 'category_id', 'category_ids', 'dept_id', 'goods_name',
		'goods_cover', 'goods_images', 'description', 'tags', 'price', 'vip_price',
		'unit', 'specifications', 'stock', 'goods_type', 'is_top', 'is_hot',
		'attr_ids', 'attr_values', 'is_sku', 'award_type', 'award_parent_ratio',
		'award_grand_parent_ratio', 'award_golden', 'content', 'express_fee',
		'weight', 'view_count', 'sale_count', 'create_time', 'update_time', 'status'
	], goods))
	lines.push(insertRows('app_goods_related', [
		'id', 'goods_id', 'section_id', 'section_name', 'content', 'sort_order',
		'min_content_length', 'create_time'
	], features))
	lines.push(insertRows('app_goods_education_ext', [
		'ext_id', 'goods_id', 'course_time', 'course_place', 'teacher_name',
		'lesson_count', 'class_size_max', 'class_size_min', 'start_date',
		'signup_start', 'signup_end', 'material_note', 'consult_phone',
		'create_time', 'update_time'
	], educationExt))
	lines.push(insertRows('app_goods_sku', [
		'sku_id', 'goods_id', 'sku_name', 'sku_type', 'sku_code', 'par_sku_id',
		'sort_order', 'status', 'valid_time', 'invalid_time', 'create_time',
		'stock', 'stock_unit', 'sale_num', 'price', 'sale_price'
	], skus))
	lines.push(insertRows('app_goods_sku_option', [
		'option_id', 'goods_id', 'sku_id', 'option_name', 'option_param',
		'create_time', 'status', 'option_type', 'option_value', 'option_value_unit',
		'option_sort', 'sku_seq_no'
	], options))
	lines.push(insertRows('app_goods_sku_data', [
		'data_id', 'goods_id', 'sku_ids', 'option_ids', 'data_values', 'data_price',
		'data_image', 'data_stock', 'remark', 'create_time', 'status'
	], skuData))
	lines.push(
		`ALTER TABLE app_goods_category AUTO_INCREMENT = ${maxId(categories, 'category_id')};`,
		`ALTER TABLE app_goods AUTO_INCREMENT = ${maxId(goods, 'goods_id')};`,
		`ALTER TABLE app_goods_related AUTO_INCREMENT = ${maxId(features, 'id')};`,
		`ALTER TABLE app_goods_education_ext AUTO_INCREMENT = ${maxId(educationExt, 'ext_id')};`,
		`ALTER TABLE app_goods_sku AUTO_INCREMENT = ${maxId(skus, 'sku_id')};`,
		`ALTER TABLE app_goods_sku_option AUTO_INCREMENT = ${maxId(options, 'option_id')};`,
		`ALTER TABLE app_goods_sku_data AUTO_INCREMENT = ${maxId(skuData, 'data_id')};`,
		''
	)
	return lines.join('\n')
}

async function main() {
	const schemaSql = await fs.readFile(SCHEMA_PATH, 'utf8')
	const categories = await requestJson(`${API_BASE}/get_goods_category?status=1`)
	const list = await requestJson(`${API_BASE}/queryGoodsList?pageNum=1&pageSize=500`, {
		method: 'POST',
		headers: { 'content-type': 'application/json' },
		body: '{}'
	})
	if (!Array.isArray(categories) || !categories.length) {
		throw new Error('Production returned no enabled product categories')
	}
	if (!Array.isArray(list) || !list.length) {
		throw new Error('Production returned no products')
	}

	const details = await Promise.all(list.map(item => (
		requestJson(`${API_BASE}/get_goods_info/${item.goodsId}`)
	)))
	const skuDataLists = await Promise.all(list.map(item => (
		requestJson(`${API_BASE}/goods_sku_data?goodsId=${item.goodsId}`)
	)))
	const goods = details.map(item => ({
		...item,
		goods_id: item.goodsId,
		category_id: item.categoryId,
		category_ids: item.categoryIds,
		dept_id: item.deptId,
		goods_name: item.goodsName,
		goods_cover: item.goodsCover,
		goods_images: item.goodsImages,
		description: item.description,
		tags: item.tags,
		price: item.price,
		vip_price: item.vipPrice,
		unit: item.unit,
		specifications: item.specifications,
		stock: item.stock,
		goods_type: item.goodsType,
		is_top: item.isTop,
		is_hot: item.isHot,
		attr_ids: item.attrIds,
		attr_values: item.attrValues,
		is_sku: item.isSku,
		award_type: item.awardType,
		award_parent_ratio: item.awardParentRatio,
		award_grand_parent_ratio: item.awardGrandParentRatio,
		award_golden: item.awardGolden,
		content: item.content,
		express_fee: item.expressFee,
		weight: item.weight,
		view_count: item.viewCount,
		sale_count: item.saleCount,
		create_time: item.createTime,
		update_time: item.updateTime,
		status: item.status
	}))
	const normalizedCategories = categories.map(item => ({
		category_id: item.categoryId,
		parent_id: item.parentId,
		parent_ids: item.parentIds,
		category_name: item.categoryName,
		category_icon: item.categoryIcon,
		is_hot: item.isHot,
		link_type: item.linkType,
		link_id: item.linkId,
		remark: item.remark,
		order_num: item.orderNum,
		status: item.status
	}))
	const features = details.flatMap(item => (item.features || []).map(feature => ({
		id: feature.id,
		goods_id: feature.goodsId,
		section_id: feature.sectionId,
		section_name: feature.sectionName,
		content: feature.content,
		sort_order: feature.sortOrder,
		min_content_length: feature.minContentLength,
		create_time: feature.createTime
	})))
	const skus = details.flatMap(item => (item.optionList || []).map(sku => ({
		sku_id: sku.skuId,
		goods_id: sku.goodsId,
		sku_name: sku.skuName,
		sku_type: sku.skuType,
		sku_code: sku.skuCode,
		par_sku_id: sku.parSkuId,
		sort_order: sku.sortOrder,
		status: sku.status,
		valid_time: sku.validTime,
		invalid_time: sku.invalidTime,
		create_time: sku.createTime,
		stock: sku.stock,
		stock_unit: sku.stockUnit,
		sale_num: sku.saleNum,
		price: sku.price,
		sale_price: sku.salePrice
	})))
	const options = details.flatMap(item => (item.optionList || []).flatMap(sku => (
		(sku.options || []).map(option => ({
			option_id: option.optionId,
			goods_id: option.goodsId,
			sku_id: option.skuId,
			option_name: option.optionName,
			option_param: option.optionParam,
			create_time: option.createTime,
			status: option.status,
			option_type: option.optionType,
			option_value: option.optionValue,
			option_value_unit: option.optionValueUnit,
			option_sort: option.optionSort,
			sku_seq_no: option.skuSeqNo
		}))
	)))
	const skuData = skuDataLists.flat().map(item => ({
		data_id: item.dataId,
		goods_id: item.goodsId,
		sku_ids: item.skuIds,
		option_ids: item.optionIds,
		data_values: item.dataValues,
		data_price: item.dataPrice,
		data_image: item.dataImage,
		data_stock: item.dataStock,
		remark: item.remark,
		create_time: item.createTime,
		status: item.status
	}))
	const educationExt = details.flatMap(item => item.educationExt ? [{
		ext_id: item.educationExt.extId,
		goods_id: item.educationExt.goodsId,
		course_time: item.educationExt.courseTime,
		course_place: item.educationExt.coursePlace,
		teacher_name: item.educationExt.teacherName,
		lesson_count: item.educationExt.lessonCount,
		class_size_max: item.educationExt.classSizeMax,
		class_size_min: item.educationExt.classSizeMin,
		start_date: item.educationExt.startDate,
		signup_start: item.educationExt.signupStart,
		signup_end: item.educationExt.signupEnd,
		material_note: item.educationExt.materialNote,
		consult_phone: item.educationExt.consultPhone,
		create_time: item.educationExt.createTime,
		update_time: item.educationExt.updateTime
	}] : [])

	await fs.writeFile(OUTPUT_PATH, buildSql(schemaSql, {
		categories: normalizedCategories,
		goods,
		features,
		skus,
		options,
		skuData,
		educationExt
	}), 'utf8')
	console.log(`Wrote ${OUTPUT_PATH}`)
	console.log(`Snapshot: ${goods.length} goods, ${features.length} features, ${skus.length} SKUs, ${options.length} options`)
}

main().catch(error => {
	console.error(error.message)
	process.exitCode = 1
})
