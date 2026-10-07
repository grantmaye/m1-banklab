import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests/browser',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 30000,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.BANKLAB_BASE_URL || 'http://127.0.0.1:8080',
    viewport: { width: 1440, height: 1080 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure'
  },
  webServer: {
    command: 'java -jar target/m1-banklab-0.1.0-SNAPSHOT.jar',
    url: `${process.env.BANKLAB_BASE_URL || 'http://127.0.0.1:8080'}/api/accounts/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`,
    reuseExistingServer: !process.env.CI,
    timeout: 120000
  }
});
