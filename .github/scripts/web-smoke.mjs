// Opens the built site in headless Chromium: checks that John 1 renders and that switching translation works.
// Usage: node web-smoke.mjs <site url> [screenshot path]
import { chromium } from 'playwright';
import assert from 'node:assert/strict';

const [url, screenshot = 'web-smoke.png'] = process.argv.slice(2);
const browser = await chromium.launch();
const page = await browser.newPage();
const errors = [];
page.on('pageerror', (e) => errors.push(e.message));
page.on('console', (m) => m.type() === 'error' && errors.push(m.text()));

try {
  await page.goto(url);
  await page.waitForSelector('#text .verse');

  assert.equal(await page.locator('#book option:checked').textContent(), 'John');
  assert.equal(await page.inputValue('#chapter'), '0');
  const john = page.locator('#text section[data-book="42"][data-chapter="0"]');
  const first = await john.locator('.verse').first().textContent();
  assert.match(first, /beginning/i, `unexpected first verse: ${first}`);
  assert.ok((await john.locator('.verse').count()) >= 40, 'John 1 has 51 verses');

  assert.ok(await page.locator('#ruby-label').isHidden(), 'no ruby checkbox without ruby text');

  const translations = await page.locator('#translation option').evaluateAll((o) => o.map((e) => e.value));
  assert.ok(translations.length >= 2, `expected several translations, got ${translations}`);
  await page.selectOption('#translation', translations[1]);
  await page.waitForFunction((t) => document.querySelector('#translation').value === t && document.querySelector('#status').textContent === '', translations[1]);
  await page.waitForSelector('#text .verse');
  assert.equal(await page.inputValue('#book'), '42', 'still on John after switching translation');

  if (translations[1] === 'cmn-ncvt') {
    assert.ok(await page.locator('#ruby-label').isVisible(), 'ruby checkbox shown for the annotated translation');
    assert.ok((await page.locator('#text ruby rt').count()) > 0, 'Jyutping is drawn above the Chinese text');
    await page.uncheck('#ruby');
    assert.equal(await page.locator('#text ruby').count(), 0, 'ruby is removed when unchecked');
    await page.check('#ruby');
  }

  await page.selectOption('#chapter', { value: '1' });
  await page.waitForFunction(() => document.title.endsWith('2 · Yeshua'));
  await page.evaluate(() => document.querySelector('#text section[data-book="42"][data-chapter="2"]').scrollIntoView());
  await page.waitForFunction(() => document.title.endsWith('3 · Yeshua'));
  await page.waitForFunction(() => document.title.endsWith('3 · Yeshua'));

  assert.ok((await page.locator('#text section').count()) >= 3, 'neighbouring chapters are loaded for infinite scroll');

  await page.click('#theme');
  assert.equal(await page.getAttribute('html', 'data-theme'), 'dark', 'night mode toggles');

  await page.reload();
  await page.waitForSelector('#text .verse');
  assert.equal(await page.inputValue('#translation'), translations[1], 'translation is remembered');
  assert.ok((await page.title()).endsWith('3 · Yeshua'), 'position is remembered');

  assert.deepEqual(errors, [], 'no console errors');
  console.log('web smoke test passed');
} finally {
  await page.screenshot({ path: screenshot, fullPage: true });
  await browser.close();
}
