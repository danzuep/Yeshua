# Web site

The `web` module builds a static reader (reading and switching translations) that is published to GitHub Pages by the `Web site` workflow (`.github/workflows/pages.yml`).

- **Reuses the app core.** USFM parsing, the eBible catalog, and the verse structure helpers live in `shared/src/commonMain` and are compiled for Android, the JVM and Kotlin/JS.
- **Static JSON.** eBible.org doesn't allow cross-origin downloads, so the build converts translations to `translations/<shortName>.json` plus `translations/index.json` (`web/src/jvmMain/.../Convert.kt`, format in `TranslationFiles.kt`).
- **UI.** `web/src/jsMain` is a small DOM reader. The last translation and chapter are kept in `localStorage`.

## Building locally

```
./gradlew :web:assembleSite -Ptranslations=engwebu,eng-kjv2006,eng-asv
python3 -m http.server --directory web/build/site
```

`translations` takes eBible ids from `app/src/main/assets/translations.csv` (redistributable ones only). The workflow's manual run takes the same list.

## Publishing

Pushes to `main` deploy the site. Enable it once under Settings > Pages > Source: GitHub Actions. Pull requests build the site and run `.github/scripts/web-smoke.mjs` (headless Chromium: John 1 renders, translation switching and the remembered position work) without deploying.
