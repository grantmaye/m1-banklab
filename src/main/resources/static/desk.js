import { cents, decimal, formatCents, money, parseApiJson } from "./money.js";

// Only the public fictional fixture IDs are selectable. Balances, names, and
// transaction evidence always come from the existing same-origin Spring APIs.
const accountIds = [
  "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "cccccccc-cccc-cccc-cccc-cccccccccccc",
];
const customerIds = [
  "11111111-1111-1111-1111-111111111111",
  "22222222-2222-2222-2222-222222222222",
];
const pendingKey = "banklab.pending-transfer";
const $ = (id) => document.getElementById(id);
const state = {
  accounts: [],
  customers: [],
  transactions: [],
  assessments: [],
  events: [],
  selected: null,
  busy: false,
  loading: false,
  fresh: false,
  uncertain: false,
  storageAvailable: true,
};
try {
  state.uncertain = sessionStorage.getItem(pendingKey) !== null;
} catch {
  state.storageAvailable = false;
}

const titleCase = (text) =>
  String(text)
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/\b\w/g, (char) => char.toUpperCase());
const timestamp = (value) =>
  new Date(value).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
function node(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}
function status(id, message, kind = "") {
  const element = $(id);
  element.textContent = message;
  element.className = `status ${kind}`;
  element.hidden = !message;
}
function customer(account) {
  return state.customers.find((item) => item.id === account.customerId);
}
function accountName(account) {
  const owner = customer(account);
  return owner ? `${owner.firstName} ${owner.lastName}` : "Fixture customer";
}
function label(account) {
  return `${accountName(account)} · ${titleCase(account.type)} ${account.accountNumber.slice(-4)}`;
}
function accountLabel(id) {
  const account = state.accounts.find((item) => item.id === id);
  return account ? label(account) : id || "—";
}
function controls() {
  $("transfer-fields").disabled =
    state.busy ||
    state.loading ||
    !state.fresh ||
    state.uncertain ||
    !state.storageAvailable;
  $("refresh").disabled = state.loading || state.busy;
  $("refresh").textContent = state.loading
    ? "Reading records…"
    : "Refresh records ↻";
  $("submit-transfer").textContent = state.busy
    ? "Posting transfer…"
    : "Post fictional transfer →";
  $("acknowledge").hidden = !state.uncertain;
  $("acknowledge").disabled = state.loading || state.busy || !state.fresh;
  $("transfer-form").setAttribute("aria-busy", String(state.busy));
}

async function api(path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(path, {
      ...options,
      signal: controller.signal,
      cache: "no-store",
      headers: {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
      },
    });
    const text = await response.text();
    let body;
    try {
      body = parseApiJson(text);
    } catch {
      throw new Error(
        `The service returned an unreadable response (HTTP ${response.status}).`,
      );
    }
    if (!response.ok) {
      const error = new Error(
        body.message || `The service returned HTTP ${response.status}.`,
      );
      error.status = response.status;
      error.fieldErrors = body.fieldErrors;
      throw error;
    }
    return body;
  } finally {
    clearTimeout(timeout);
  }
}

function renderAccounts() {
  $("total-balance").textContent = formatCents(
    state.accounts.reduce(
      (total, account) => total + cents(account.balance),
      0n,
    ),
  );
  $("account-list").replaceChildren(
    ...state.accounts.map((account) => {
      const row = node("div", "account-row");
      row.dataset.accountId = account.id;
      const avatar = node(
        "span",
        "account-avatar",
        accountName(account)
          .split(" ")
          .map((word) => word[0])
          .join("")
          .slice(0, 2),
      );
      avatar.setAttribute("aria-hidden", "true");
      const owner = node("div");
      owner.append(
        node("p", "account-name", accountName(account)),
        node(
          "p",
          "account-meta",
          `${titleCase(account.type)} · ${account.accountNumber}`,
        ),
      );
      const amount = node("div", "account-amount");
      amount.append(
        node("strong", "", money(account.balance)),
        node("p", "", account.status),
      );
      row.append(avatar, owner, amount);
      return row;
    }),
  );
  for (const id of ["source", "target", "journal-filter"]) {
    const select = $(id);
    const previous = select.value;
    const options = state.accounts.map(
      (account) => new Option(label(account), account.id),
    );
    if (id === "journal-filter")
      options.unshift(new Option("All seeded accounts", "all"));
    select.replaceChildren(...options);
    select.value = options.some((option) => option.value === previous)
      ? previous
      : id === "target"
        ? accountIds[2]
        : options[0].value;
  }
  updateSource();
}

function updateSource() {
  const source = state.accounts.find(
    (account) => account.id === $("source").value,
  );
  $("source-balance").textContent = source
    ? `Last read balance: ${money(source.balance)} demo units`
    : "Choose a source account.";
  $("target").setCustomValidity(
    $("target").value === $("source").value
      ? "Choose a different destination account."
      : "",
  );
}

function selectTransaction(id) {
  state.selected = id;
  for (const entry of $("journal-list").querySelectorAll("button"))
    entry.setAttribute(
      "aria-pressed",
      String(entry.dataset.transactionId === id),
    );
  renderDetail();
}

function renderJournal() {
  const filter = $("journal-filter").value;
  const transactions = state.transactions.filter(
    (item) =>
      filter === "all" ||
      item.sourceAccountId === filter ||
      item.targetAccountId === filter,
  );
  $("journal-count").textContent =
    `${transactions.length} ${transactions.length === 1 ? "entry" : "entries"}`;
  if (!transactions.some((item) => item.id === state.selected))
    state.selected = transactions[0]?.id || null;
  $("journal-list").replaceChildren(
    ...transactions.map((transaction) => {
      const button = node("button", "entry");
      button.type = "button";
      button.dataset.transactionId = transaction.id;
      button.setAttribute(
        "aria-pressed",
        String(transaction.id === state.selected),
      );
      const symbol = node(
        "span",
        "entry-symbol",
        transaction.type === "TRANSFER"
          ? "↗"
          : transaction.type === "DEPOSIT"
            ? "+"
            : "−",
      );
      symbol.setAttribute("aria-hidden", "true");
      const copy = node("span", "entry-copy");
      copy.append(
        node(
          "strong",
          "",
          transaction.description || titleCase(transaction.type),
        ),
        node(
          "p",
          "",
          `${titleCase(transaction.type)} · ${timestamp(transaction.createdAt)}`,
        ),
      );
      const amount = node("span", "entry-amount", money(transaction.amount));
      amount.append(
        node(
          "span",
          transaction.status === "FAILED" ? "failed" : "",
          transaction.status,
        ),
      );
      button.append(symbol, copy, amount);
      button.addEventListener("click", () => selectTransaction(transaction.id));
      return button;
    }),
  );
  if (!transactions.length)
    $("journal-list").append(
      node(
        "p",
        "empty",
        "No movements yet. Opening balances are not journal entries. Post a fictional transfer to begin.",
      ),
    );
  renderDetail();
}

function renderDetail() {
  const transaction = state.transactions.find(
    (item) => item.id === state.selected,
  );
  if (!transaction) {
    const empty = node("div", "empty detail-empty");
    empty.append(
      node("span", "", "≡"),
      node("h3", "", "Every movement has a story."),
      node(
        "p",
        "",
        "Post a fictional transfer, then select its journal entry to trace the account movement, risk assessment, and audit events.",
      ),
    );
    $("transaction-detail").replaceChildren(empty);
    return;
  }
  const detail = node("div", "detail-body");
  const top = node("div", "detail-top");
  top.append(
    node("span", "detail-value", money(transaction.amount)),
    node(
      "span",
      `badge ${transaction.status === "FAILED" ? "failed" : ""}`,
      transaction.status,
    ),
  );
  const fields = node("dl");
  for (const [key, value] of [
    ["Movement", titleCase(transaction.type)],
    ["From", accountLabel(transaction.sourceAccountId)],
    ["To", accountLabel(transaction.targetAccountId)],
    ["Recorded", timestamp(transaction.createdAt)],
    ["Journal note", transaction.description || "—"],
    ["Transaction ID", transaction.id],
    ...(transaction.failureReason
      ? [["Failure reason", transaction.failureReason]]
      : []),
  ]) {
    const row = node("div");
    row.append(
      node("dt", "", key),
      node("dd", key === "Transaction ID" ? "record-id" : "", value),
    );
    fields.append(row);
  }
  const assessment = state.assessments.find(
    (item) => item.transactionId === transaction.id,
  );
  const risk = node("div", "evidence");
  risk.append(node("h3", "", "RISK ASSESSMENT"));
  if (assessment)
    risk.append(
      node(
        "p",
        "",
        `${titleCase(assessment.riskLevel)} risk · score ${assessment.score}`,
      ),
      node("p", "", assessment.reasons || "No rules matched."),
    );
  else
    risk.append(
      node("p", "", "No matching assessment in the latest 100 records."),
    );
  risk.append(
    node(
      "p",
      "muted",
      "Assessment records risk after posting; it does not block a transfer.",
    ),
  );
  const audit = node("div", "evidence");
  audit.append(node("h3", "", "AUDIT EVIDENCE"));
  const events = state.events.filter(
    (event) =>
      event.aggregateType === "TRANSACTION" &&
      event.aggregateId === transaction.id,
  );
  if (events.length) {
    const list = node("ul");
    for (const event of events) {
      const item = node("li");
      item.append(
        node("code", "", event.eventType),
        node("p", "", event.details || "—"),
        node("p", "muted", `${event.actor} · ${timestamp(event.createdAt)}`),
      );
      list.append(item);
    }
    audit.append(list);
  } else
    audit.append(
      node(
        "p",
        "",
        "No transaction-linked event in the latest 100 records. Failed-withdrawal events are linked to the account.",
      ),
    );
  detail.append(top, fields, risk, audit);
  $("transaction-detail").replaceChildren(detail);
}

async function refresh() {
  if (state.loading) return;
  state.loading = true;
  state.fresh = false;
  controls();
  status("load-status", "Reading accounts, journal, and evidence…");
  try {
    const [accounts, customers, journals, assessments, events] =
      await Promise.all([
        Promise.all(accountIds.map((id) => api(`/api/accounts/${id}`))),
        Promise.all(customerIds.map((id) => api(`/api/customers/${id}`))),
        Promise.all(
          accountIds.map((id) => api(`/api/accounts/${id}/transactions`)),
        ),
        api("/api/fraud/assessments"),
        api("/api/audit/events"),
      ]);
    // Validate before publishing a complete new snapshot; don't show partial reads.
    accounts.forEach((account) => cents(account.balance));
    const transactions = [
      ...new Map(journals.flat().map((item) => [item.id, item])).values(),
    ].sort(
      (a, b) =>
        b.createdAt.localeCompare(a.createdAt) || a.id.localeCompare(b.id),
    );
    transactions.forEach((transaction) => cents(transaction.amount));
    Object.assign(state, {
      accounts,
      customers,
      transactions,
      assessments,
      events,
    });
    renderAccounts();
    renderJournal();
    state.fresh = true;
    $("sync-time").textContent = `Last read ${new Date().toLocaleTimeString()}`;
    status(
      "load-status",
      state.storageAvailable
        ? ""
        : "Tab storage is unavailable. Reading works, but transfers are disabled so an interrupted request cannot lose its local warning.",
      "warning",
    );
  } catch (error) {
    status(
      "load-status",
      `Records could not be refreshed. ${error.message} ${state.accounts.length ? "Displayed records are from the last successful read." : "Start the Spring service and its seeded PostgreSQL database."} Transfers are paused; use Refresh records to try reading again.`,
      "error",
    );
  } finally {
    state.loading = false;
    controls();
  }
}

function unknownOutcome() {
  state.uncertain = true;
  status(
    "transfer-status",
    "Outcome unconfirmed. The request may have committed. Do not repeat it automatically. Refresh and inspect balances and the journal before deciding whether to start a new transfer. A read cannot prove that an in-flight request has finished.",
    "warning",
  );
}

$("transfer-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  if (
    state.busy ||
    state.loading ||
    !state.fresh ||
    state.uncertain ||
    !state.storageAvailable
  )
    return;
  const input = $("amount");
  input.setCustomValidity("");
  let amount;
  try {
    amount = cents(input.value.trim());
    if (amount < 1n) throw new Error("Amount must be positive.");
  } catch {
    input.setCustomValidity(
      "Enter a positive amount with up to 17 whole digits and 2 decimal places.",
    );
  }
  updateSource();
  if (!$("transfer-form").reportValidity()) return;
  const request = {
    sourceAccountId: $("source").value,
    targetAccountId: $("target").value,
    amount: decimal(amount),
    description: $("description").value.trim() || null,
  };
  // Write the marker before POST. It preserves an interrupted-request warning
  // across reloads in this tab; it is deliberately not an idempotency claim.
  try {
    sessionStorage.setItem(pendingKey, "pending");
  } catch {
    state.storageAvailable = false;
    status(
      "transfer-status",
      "The tab could not retain its pending-request warning. No request was sent. Transfers are paused.",
      "error",
    );
    controls();
    return;
  }
  state.busy = true;
  controls();
  status(
    "transfer-status",
    "Posting the fictional transfer. Waiting for the service…",
  );
  try {
    const transaction = await api("/api/transfers", {
      method: "POST",
      body: JSON.stringify(request),
    });
    if (
      !transaction.id ||
      transaction.status !== "POSTED" ||
      transaction.type !== "TRANSFER"
    )
      throw new Error("Unconfirmed transfer response.");
    sessionStorage.removeItem(pendingKey);
    state.selected = transaction.id;
    $("journal-filter").value = "all";
    input.value = "";
    $("description").value = "";
    status(
      "transfer-status",
      `Transfer posted: ${money(transaction.amount)} demo units. Reference ${transaction.id}. Refreshing the records…`,
      "success",
    );
    await refresh();
    status(
      "transfer-status",
      `Transfer posted: ${money(transaction.amount)} demo units. Reference ${transaction.id}. ${state.fresh ? "Updated balances and journal are ready to inspect." : "The follow-up read failed. Use Refresh records; do not repost this transfer."}`,
      "success",
    );
  } catch (error) {
    if ([400, 404, 409, 422].includes(error.status)) {
      sessionStorage.removeItem(pendingKey);
      status(
        "transfer-status",
        `Transfer rejected (HTTP ${error.status}). ${error.message} ${Object.entries(
          error.fieldErrors || {},
        )
          .map(([field, message]) => `${field}: ${message}`)
          .join(" ")}`,
        "error",
      );
      await refresh();
    } else unknownOutcome();
  } finally {
    state.busy = false;
    controls();
    $("transfer-status").focus();
  }
});

$("refresh").addEventListener("click", refresh);
$("source").addEventListener("change", updateSource);
$("target").addEventListener("change", updateSource);
$("amount").addEventListener("input", () => $("amount").setCustomValidity(""));
$("journal-filter").addEventListener("change", renderJournal);
$("scenario-transfer").addEventListener("click", () => {
  $("amount").value = "250.00";
  $("amount").setCustomValidity("");
  $("description").value = "Workshop supplies";
  $("amount").focus();
});
$("scenario-insufficient").addEventListener("click", () => {
  const source = state.accounts.find(
    (account) => account.id === $("source").value,
  );
  const attempted = cents(source.balance) + 100n;
  if (attempted > 9999999999999999999n) {
    status(
      "transfer-status",
      "This balance is too large to prepare a valid overdraw scenario. Choose another seeded account.",
      "warning",
    );
    return;
  }
  $("amount").value = decimal(attempted);
  $("amount").setCustomValidity("");
  $("description").value = "Insufficient-funds exercise";
  status(
    "transfer-status",
    "Prepared an amount 1.00 above the last read balance. Nothing has been sent. Post the transfer to ask the server to evaluate it. Other writes may change that balance.",
    "warning",
  );
  $("amount").focus();
});
$("acknowledge").addEventListener("click", () => {
  if (!state.fresh || state.busy || state.loading) return;
  try {
    sessionStorage.removeItem(pendingKey);
  } catch {
    state.storageAvailable = false;
  }
  state.uncertain = false;
  $("amount").value = "";
  $("description").value = "";
  status(
    "transfer-status",
    "A new transfer form is ready. The previous outcome remains your responsibility to reconcile; a new request is a separate movement.",
    "warning",
  );
  controls();
  $("amount").focus();
});
if (state.uncertain) unknownOutcome();
refresh();
