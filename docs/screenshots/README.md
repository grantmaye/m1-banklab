# Operations desk screenshots

These unedited Chromium screenshots were produced by [CI run 37576443583](https://github.com/grantmaye/m1-banklab/actions/runs/37576443583) on October 7, 2026, from source commit [`bdf5d31d5fd8d7b500559305ccb6a322ceb4fa26`](https://github.com/grantmaye/m1-banklab/commit/bdf5d31d5fd8d7b500559305ccb6a322ceb4fa26).

The browser loaded the packaged Spring application connected to the workflow's disposable PostgreSQL 16 service. Account balances, transactions, assessments, and audit events came from the actual APIs. The tests independently queried PostgreSQL to verify writes. No successful response or displayed balance was fabricated for these captures.

| Image | Captured state |
| --- | --- |
| [Desktop](operations-desktop.png) | A 250.00-unit transfer committed; checking balances are 2250.00 and 1250.00, with transaction-linked risk/audit evidence |
| [Mobile](operations-mobile.png) | 375-pixel viewport, after the suite's three committed transfers; keyboard-focused journal entry and evidence inspection |
| [Rejected transfer](operations-rejected.png) | Actual insufficient-funds HTTP 422; SQL assertions confirmed unchanged balances and transaction count |
| [Unconfirmed outcome](operations-unconfirmed.png) | A real transfer committed, then the test deliberately aborted response delivery; the desk retains the uncertainty warning after reload and refresh |

The same run passed all 10 Java tests with zero skips, two exact-decimal tests, and seven browser scenarios. Desktop/mobile axe scans found no violations for the selected WCAG rule tags. Screenshots demonstrate those moments, not general accessibility certification or financial correctness beyond the tested behavior.

The original `banking-desk-browser-evidence` artifact includes the Playwright report and is retained by Actions for 14 days. These four copied images remain in the repository after that artifact expires. Later runs generate new IDs, timestamps, and screenshots; the checked-in images retain their original provenance. All people, account identifiers, and money in the lab are fictional.
