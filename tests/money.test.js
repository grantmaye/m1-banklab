import test from "node:test";
import assert from "node:assert/strict";
import {
  cents,
  decimal,
  formatCents,
  money,
  parseApiJson,
} from "../src/main/resources/static/money.js";

test("database-size decimals survive API JSON, totals, display, and request serialization", () => {
  const account = parseApiJson(
    '{"balance":99999999999999999.99,"score":35,"note":"balance: 123.45; \\\"amount\\\":250.00"}',
  );
  assert.equal(account.balance, "99999999999999999.99");
  assert.equal(money(account.balance), "99,999,999,999,999,999.99");
  assert.equal(decimal(cents(account.balance)), account.balance);
  assert.equal(
    formatCents(cents(account.balance) * 3n),
    "299,999,999,999,999,999.97",
  );
  assert.equal(account.note, 'balance: 123.45; "amount":250.00');
  assert.equal(account.score, "35");
});

test("cent parser rejects fractional cents, exponent notation, signs and oversized inputs", () => {
  for (const input of [
    "1.001",
    "1e3",
    "-1",
    "+1",
    "NaN",
    "Infinity",
    "",
    "100000000000000000",
    "1,000.00",
  ]) {
    assert.throws(() => cents(input), input);
  }
  assert.equal(cents("0.01"), 1n);
  assert.equal(cents("250.5"), 25050n);
  assert.equal(cents("0"), 0n);
});
