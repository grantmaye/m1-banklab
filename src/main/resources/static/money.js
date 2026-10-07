// Preserve JSON numeric tokens before parsing: API BigDecimal values can exceed
// JavaScript's safe integer range. Quoted strings are matched first and untouched.
export function parseApiJson(text) {
  return JSON.parse(text.replace(/"(?:\\.|[^"\\])*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g,
    token => token.startsWith('"') ? token : JSON.stringify(token)));
}

export function cents(value) {
  if (!/^\d{1,17}(?:\.\d{1,2})?$/.test(String(value))) throw new Error('Unsupported monetary value.');
  const [whole, fraction = ''] = String(value).split('.');
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'));
}

export function decimal(value) {
  return `${value / 100n}.${(value % 100n).toString().padStart(2, '0')}`;
}

export function formatCents(value) {
  const [whole, fraction] = decimal(value).split('.');
  return `${whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}.${fraction}`;
}

export function money(value) { return formatCents(cents(value)); }
