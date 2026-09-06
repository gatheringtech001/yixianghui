function boundary(question) {
  const match = question.match(/(?:正好|恰好)(?:连住|住满|住|满)?\s*([\d一二两三四五六七八九十]+)\s*(周岁|岁|天)/);
  if (!match) return null;
  const digits = "零一二三四五六七八九";
  const raw = match[1].replaceAll("两", "二");
  const number = /^\d+$/.test(raw) ? Number(raw) : raw.includes("十")
    ? (digits.indexOf(raw.split("十")[0]) || 1) * 10 + (digits.indexOf(raw.split("十")[1]) || 0)
    : digits.indexOf(raw);
  return Number.isSafeInteger(number) && number > 0 ? { number, unit: match[2] === "天" ? "天" : "岁" } : null;
}

export function policyAnswer(question, candidates) {
  const point = boundary(question);
  if (point && /取消|退|费|餐|价格|钱/.test(question)) {
    const n = point.number;
    const patterns = point.unit === "天"
      ? [new RegExp(`${n}\\s*天以内[^。；;\\n]*`), new RegExp(`${n}\\s*天以上[^。；;\\n]*`)]
      : [new RegExp(`\\d+\\s*[-—–~至到]\\s*${n}\\s*岁[^，,。；;\\n]*`), new RegExp(`${n}\\s*岁以上[^，,。；;\\n]*`)];
    for (const item of candidates) {
      const evidence = patterns.map((pattern) => item.content.match(pattern)?.[0]);
      if (evidence.some((value) => !value || value.length > 350 || /不含|不包括|不包含/.test(value))) continue;
      return { grounded: true, reason: "ambiguous_policy_boundary",
        answer: `资料在“${n}${point.unit}”边界存在交叠：一处写“${evidence[0]}”，另一处写“${evidence[1]}”。因此不能确定该边界适用哪一档，不能承诺无损取消或具体费用；请向业务方确认包含关系和实际执行规则。[${item.id}]`,
        sources: [{ ...item.source, id: item.id, quote: item.content, evidence }] };
    }
  }
  if (/宠物/.test(question) && !candidates.some((item) => /宠物|可带.{0,6}[猫狗]/.test(item.content))) {
    return { grounded: false, reason: "pet_policy_not_retrieved", sources: [],
      answer: "本次检索未找到该基地的宠物准入或宠物专项清洁费依据，不能用日常保洁费替代。请向基地确认。" };
  }
  return null;
}
