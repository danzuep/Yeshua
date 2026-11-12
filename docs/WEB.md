# Web site

The `web` module builds a static reader (reading and switching translations) that is published to GitHub Pages by the `Web site` workflow (`.github/workflows/pages.yml`).

- **Reuses the app core.** USFM parsing, the eBible catalog, and the verse structure helpers live in `shared/src/commonMain` and are compiled for Android, the JVM and Kotlin/JS.
- **Static JSON.** eBible.org doesn't allow cross-origin downloads, so the build converts translations to `translations/<shortName>.json` plus `translations/index.json` (`web/src/jvmMain/.../Convert.kt`, format in `TranslationFiles.kt`).
- **UI.** `web/src/jsMain` is a small DOM reader. Chapters load continuously as you scroll (infinite scroll, in both directions) and the toolbar follows the chapter under it. There is a night mode (auto, dark, light) and text size buttons. The last translation, chapter, theme and text size are kept in `localStorage`.

## Building locally

```
./gradlew :web:assembleSite -Ptranslations=engwebu,cmn-ncvt
python3 -m http.server --directory web/build/site
```

`translations` takes eBible ids from `app/src/main/assets/translations.csv` (redistributable ones only). The workflow's manual run takes the same list.

## Publishing

Pushes to `main` deploy the site. Enable it once under Settings > Pages > Source: GitHub Actions. Pull requests build the site and run `.github/scripts/web-smoke.mjs` (headless Chromium: John 1 renders, translation switching and the remembered position work) without deploying.

A translation can read its zip from another URL with `id=<zip url>`, e.g. `-Ptranslations=engwebu,cmn-ncvt=https://github.com/<owner>/<repo>/releases/download/usfm-ruby/cmn-ncvt_zh-HK.zip` for ruby USFM built by a separate workflow. The manual workflow run has `ruby_zip` and `ruby_id` inputs for this.

## Ruby

The workflow annotates the `ruby_id` text (default `cmn-ncvt`) with [PhoneticAnnotator](https://github.com/danzuep/PhoneticAnnotator) v0.2.0 before building the site: it downloads the eBible USFM zip, adds `\rb base|reading\rb*` ruby with the Unihan readings of `ruby_language` (default `zh-HK`, Jyutping; the first reading of each character, not chosen from context) and the build reads the annotated zip. Set `ruby_zip` to skip this and use a zip you made yourself. The **Ruby** checkbox is only shown for translations that have ruby.
