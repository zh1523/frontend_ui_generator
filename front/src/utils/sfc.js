export function parseSfcSections(vueCode = "") {
  const source = stripFence(vueCode);
  return {
    template: findBlock(source, /<template[\s\S]*?<\/template>/i),
    script: findBlock(source, /<script[\s\S]*?<\/script>/i),
    style: findBlock(source, /<style[\s\S]*?<\/style>/i)
  };
}

export function extractTemplateBody(templateTag = "") {
  return templateTag
    .replace(/<template[^>]*>/i, "")
    .replace(/<\/template>/i, "")
    .trim();
}

export function extractStyleBody(styleTag = "") {
  return styleTag
    .replace(/<style[^>]*>/i, "")
    .replace(/<\/style>/i, "")
    .trim();
}

export function detectUnsafeCode(vueCode = "") {
  const rules = [
    /import\s+[^;]*from\s*['"]https?:\/\//i,
    /import\s*\(\s*['"]https?:\/\//i,
    /<script\s+[^>]*src=/i,
    /\beval\s*\(/i,
    /new\s+Function\s*\(/i,
    /\b(fetch|XMLHttpRequest|WebSocket)\b/
  ];
  return rules.some((rule) => rule.test(vueCode));
}

export function extractSimpleState(scriptTag = "") {
  const body = scriptTag
    .replace(/<script[^>]*>/i, "")
    .replace(/<\/script>/i, "");
  const state = {};
  const refPattern = /const\s+([A-Za-z_]\w*)\s*=\s*ref\(([^)]+)\)/g;
  for (const match of body.matchAll(refPattern)) {
    const key = match[1];
    const value = parseLiteral(match[2]);
    if (value !== undefined) {
      state[key] = value;
    }
  }
  const constPattern = /const\s+([A-Za-z_]\w*)\s*=\s*([^;]+);/g;
  for (const match of body.matchAll(constPattern)) {
    const key = match[1];
    if (Object.hasOwn(state, key)) {
      continue;
    }
    const value = parseLiteral(match[2]);
    if (value !== undefined) {
      state[key] = value;
    }
  }
  return state;
}

function stripFence(text) {
  const match = text.match(/```(?:vue|html)?\s*([\s\S]*?)```/i);
  return match ? match[1] : text;
}

function findBlock(source, regex) {
  const match = source.match(regex);
  return match ? match[0] : "";
}

function parseLiteral(raw) {
  const text = raw.trim();
  if (/^(true|false)$/.test(text)) {
    return text === "true";
  }
  if (/^-?\d+(\.\d+)?$/.test(text)) {
    return Number(text);
  }
  if ((text.startsWith('"') && text.endsWith('"')) || (text.startsWith("'") && text.endsWith("'")) || (text.startsWith("`") && text.endsWith("`"))) {
    return text.slice(1, -1);
  }
  if (text.startsWith("[") || text.startsWith("{")) {
    try {
      return JSON.parse(text.replaceAll("'", '"'));
    } catch (_) {
      return undefined;
    }
  }
  if (text === "null") {
    return null;
  }
  return undefined;
}
