// Reuse the publication dictionary and category IDs; never infer type from payment/fulfilment.
export function goodsOrderQuery(query, scope = 'all') {
  return { ...query, businessType: null, goodsType: scope === 'all' ? null : scope }
}

// Clear hidden fields too: changing tabs must not leave the previous type's filters active.
export function resetGoodsOrderQuery(query) {
  return Object.fromEntries(Object.keys(query).map(key => [key,
    key === 'pageSize' ? query.pageSize : key === 'pageNum' ? 1 : null]))
}
