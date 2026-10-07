import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import pg from "pg";

const source = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const target = "cccccccc-cccc-cccc-cccc-cccccccccccc";
const db = new pg.Pool({
  connectionString:
    process.env.BANKLAB_TEST_DATABASE_URL ||
    "postgresql://m1_banklab:m1_banklab@127.0.0.1:5432/m1_banklab",
});
const balance = async (id) =>
  (await db.query("select balance::text from accounts where id=$1", [id]))
    .rows[0].balance;
const toCents = (value) => BigInt(value.replace(".", ""));
const count = async () =>
  Number((await db.query("select count(*) from transactions")).rows[0].count);

async function openDesk(page) {
  await page.goto("/");
  await expect(page.locator("#sync-time")).toContainText("Last read");
  await expect(page.locator("#load-status")).toBeHidden();
}

test.afterAll(async () => db.end());

test("real transfer reaches PostgreSQL once, updates balances, and exposes its evidence", async ({
  page,
}, testInfo) => {
  await openDesk(page);
  const beforeSource = toCents(await balance(source));
  const beforeTarget = toCents(await balance(target));
  const beforeCount = await count();
  let calls = 0;
  let release;
  let responseArrived;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  const arrived = new Promise((resolve) => {
    responseArrived = resolve;
  });
  await page.route("**/api/transfers", async (route) => {
    calls++;
    const response = await route.fetch(); // Real Spring request and real DB commit.
    responseArrived();
    await gate; // Only response delivery is delayed to exercise the busy guard.
    await route.fulfill({ response });
  });
  await page.getByLabel("Journal note").fill("Workshop supplies <fictional>");
  await page.getByRole("button", { name: "Post fictional transfer" }).click();
  await arrived;
  await expect(page.locator("#submit-transfer")).toBeDisabled();
  await page.evaluate(() => {
    document.querySelector("#transfer-form").requestSubmit();
    document.querySelector("#transfer-form").requestSubmit();
  });
  expect(calls).toBe(1);
  release();
  await expect(page.locator("#transfer-status")).toContainText(
    "Updated balances and journal are ready",
  );
  expect(await count()).toBe(beforeCount + 1);
  expect(toCents(await balance(source))).toBe(beforeSource - 25000n);
  expect(toCents(await balance(target))).toBe(beforeTarget + 25000n);
  const result = await db.query(
    "select id::text from transactions where description=$1 order by created_at desc limit 1",
    ["Workshop supplies <fictional>"],
  );
  const id = result.rows[0].id;
  expect(
    Number(
      (
        await db.query(
          "select count(*) from fraud_assessments where transaction_id=$1",
          [id],
        )
      ).rows[0].count,
    ),
  ).toBe(1);
  const audit = await db.query(
    "select event_type from audit_events where aggregate_id=$1",
    [id],
  );
  expect(audit.rows.map((row) => row.event_type)).toEqual(
    expect.arrayContaining(["FRAUD_ASSESSED", "TRANSFER_POSTED"]),
  );
  await expect(page.locator("#transaction-detail")).toContainText(id);
  await expect(page.locator("#transaction-detail")).toContainText(
    "Workshop supplies <fictional>",
  );
  await expect(page.locator("#transaction-detail")).toContainText(
    "TRANSFER_POSTED",
  );
  await expect(page.locator("#amount")).toHaveValue("");
  await expect(page.locator(`[data-account-id="${source}"]`)).toContainText(
    Number(await balance(source)).toLocaleString("en-US", {
      minimumFractionDigits: 2,
    }),
  );
  const accessibility = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
    .analyze();
  expect(accessibility.violations).toEqual([]);
  await page.screenshot({
    path: testInfo.outputPath("operations-desktop.png"),
    fullPage: true,
  });
  await testInfo.attach("Operations desk, real PostgreSQL", {
    path: testInfo.outputPath("operations-desktop.png"),
    contentType: "image/png",
  });
});

test("insufficient funds is a real 422 with unchanged balances and journal", async ({
  page,
}, testInfo) => {
  await openDesk(page);
  const balances = [await balance(source), await balance(target)];
  const beforeCount = await count();
  await page
    .getByRole("button", { name: "Insufficient funds", exact: true })
    .click();
  await expect(page.locator("#transfer-status")).toContainText(
    "Nothing has been sent",
  );
  const response = page.waitForResponse((response) =>
    response.url().endsWith("/api/transfers"),
  );
  await page.getByRole("button", { name: "Post fictional transfer" }).click();
  expect((await response).status()).toBe(422);
  await expect(page.locator("#transfer-status")).toContainText(
    "Insufficient funds for transfer.",
  );
  await expect(page.locator("#submit-transfer")).toBeEnabled();
  expect([await balance(source), await balance(target)]).toEqual(balances);
  expect(await count()).toBe(beforeCount);
  await page.screenshot({
    path: testInfo.outputPath("operations-rejected.png"),
    fullPage: true,
  });
});

test("invalid precision and same-account input never send a POST", async ({
  page,
}) => {
  await openDesk(page);
  let posts = 0;
  page.on("request", (request) => {
    if (request.method() === "POST") posts++;
  });
  await page.getByLabel("Amount", { exact: false }).fill("1.001");
  await page.getByRole("button", { name: "Post fictional transfer" }).click();
  expect(
    await page
      .locator("#amount")
      .evaluate((element) => element.validationMessage),
  ).toContain("2 decimal places");
  await page.locator("#amount").fill("1.00");
  await page.getByLabel("To account").selectOption(source);
  await page.getByRole("button", { name: "Post fictional transfer" }).click();
  expect(
    await page
      .locator("#target")
      .evaluate((element) => element.validationMessage),
  ).toContain("different destination");
  expect(posts).toBe(0);
});

test("lost response after real commit stays unconfirmed across reload without replay", async ({
  page,
}, testInfo) => {
  await openDesk(page);
  const beforeCount = await count();
  let posts = 0;
  await page.route("**/api/transfers", async (route) => {
    posts++;
    const response = await route.fetch();
    expect(response.status()).toBe(201);
    await route.abort("connectionreset"); // The write succeeded; only the response is lost.
  });
  await page.locator("#amount").fill("1.00");
  await page.getByRole("button", { name: "Post fictional transfer" }).click();
  await expect(page.locator("#transfer-status")).toContainText(
    "Outcome unconfirmed",
  );
  expect(await count()).toBe(beforeCount + 1);
  await expect(page.locator("#submit-transfer")).toBeDisabled();
  await expect(page.locator("#acknowledge")).toBeDisabled();
  await page.reload();
  await expect(page.locator("#transfer-status")).toContainText(
    "Outcome unconfirmed",
  );
  await expect(page.locator("#sync-time")).toContainText("Last read");
  await expect(page.locator("#submit-transfer")).toBeDisabled();
  await page.getByRole("button", { name: "Refresh records" }).click();
  await expect(page.locator("#acknowledge")).toBeEnabled();
  expect(posts).toBe(1);
  await page.screenshot({
    path: testInfo.outputPath("operations-unconfirmed.png"),
    fullPage: true,
  });
  await page.locator("#acknowledge").click();
  await expect(page.locator("#submit-transfer")).toBeEnabled();
  await expect(page.locator("#amount")).toHaveValue("");
  expect(await count()).toBe(beforeCount + 1);
});

test("read failure keeps old records marked stale and pauses transfers until recovery", async ({
  page,
}) => {
  await openDesk(page);
  const displayed = await page.locator("#total-balance").textContent();
  await page.route(`**/api/accounts/${source}`, (route) => route.abort());
  await page.getByRole("button", { name: "Refresh records" }).click();
  await expect(page.locator("#load-status")).toContainText(
    "Displayed records are from the last successful read",
  );
  await expect(page.locator("#total-balance")).toHaveText(displayed);
  await expect(page.locator("#submit-transfer")).toBeDisabled();
  await page.unroute(`**/api/accounts/${source}`);
  await page.getByRole("button", { name: "Refresh records" }).click();
  await expect(page.locator("#load-status")).toBeHidden();
  await expect(page.locator("#submit-transfer")).toBeEnabled();
});

test("confirmed write remains posted when its follow-up read fails", async ({
  page,
}) => {
  await openDesk(page);
  const beforeCount = await count();
  await page.route(`**/api/accounts/${source}`, (route) => route.abort());
  await page.locator("#amount").fill("2.00");
  await page.getByRole("button", { name: "Post fictional transfer" }).click();
  await expect(page.locator("#transfer-status")).toContainText(
    "Transfer posted: 2.00",
  );
  await expect(page.locator("#transfer-status")).toContainText(
    "do not repost this transfer",
  );
  await expect(page.locator("#submit-transfer")).toBeDisabled();
  expect(await count()).toBe(beforeCount + 1);
  await page.unroute(`**/api/accounts/${source}`);
  await page.getByRole("button", { name: "Refresh records" }).click();
  await expect(page.locator("#load-status")).toBeHidden();
  await expect(page.locator("#submit-transfer")).toBeEnabled();
  await expect(page.locator("#amount")).toHaveValue("");
  expect(await count()).toBe(beforeCount + 1);
});

test("mobile desk supports keyboard inspection, filtering, and accessible reflow", async ({
  page,
}, testInfo) => {
  await page.setViewportSize({ width: 375, height: 812 });
  await openDesk(page);
  await page.getByLabel("Show activity for").selectOption(source);
  const first = page.locator(".entry").first();
  await first.focus();
  await page.keyboard.press("Enter");
  await expect(first).toHaveAttribute("aria-pressed", "true");
  await expect(page.locator("#transaction-detail")).toContainText(
    "Transaction ID",
  );
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  const accessibility = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
    .analyze();
  expect(accessibility.violations).toEqual([]);
  await page.screenshot({
    path: testInfo.outputPath("operations-mobile.png"),
    fullPage: true,
  });
  await page.setViewportSize({ width: 1024, height: 900 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: testInfo.outputPath("operations-tablet.png"),
    fullPage: true,
  });
  await page.setViewportSize({ width: 320, height: 812 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
});

test("account register and search keep journal selection and evidence in sync", async ({
  page,
}) => {
  await openDesk(page);
  const entry = page.locator("#journal-list .entry").first();
  await expect(entry).toBeVisible();
  const id = await entry.getAttribute("data-transaction-id");
  await page.getByLabel("Search journal").fill(id);
  await expect(page.locator("#journal-list .entry")).toHaveCount(1);
  await expect(page.locator("#transaction-detail")).toContainText(id);
  await page.getByLabel("Search journal").fill("no-record-matches-this-query");
  await expect(page.locator("#journal-list")).toContainText(
    "No matching movements",
  );
  await expect(page.locator("#transaction-detail")).not.toContainText(id);
  await page.getByLabel("Search journal").fill("");
  const account = page.locator(`[data-account-id="${source}"]`);
  await account.click();
  await expect(account).toHaveAttribute("aria-pressed", "true");
  await expect(page.locator("#journal-filter")).toHaveValue(source);
  await page.getByRole("button", { name: "All accounts" }).click();
  await expect(account).toHaveAttribute("aria-pressed", "false");
  await expect(page.locator("#journal-filter")).toHaveValue("all");
});
