# New app: Yeshua

## Abstract for new app inclusion

### Functions and usage

Yeshua is a free and open source Bible reader for Android 6.0 and later, licensed under Apache-2.0. It provides a minimalist reading interface without advertisements, tracking, or analytics.

Users download translations from eBible.org once and then read them offline. They can compare multiple translations side by side, search verses, save bookmarks and highlights, write notes, and copy or share passages. The reader supports adjustable font sizes and light and dark themes. The interface is available in multiple languages.

Translations are distributed as USFM files. The public domain World English Bible (WEB) is the default translation; permissions for other translations depend on their individual licences. GitHub releases offer both a plain APK and a separate APK with WEB bundled for offline use from the first launch. The current F-Droid metadata builds the plain app without bundled assets.

Source and releases: https://github.com/danzuep/Yeshua

Issue tracker and maintainer contact: https://github.com/danzuep/Yeshua/issues

Application ID: `com.github.danzuep.yeshua.android`

The English store listing, icon, and a changelog are provided upstream in `fastlane/metadata/android/en-US/`. Screenshots still need to be attached to this MR and added upstream under `fastlane/metadata/android/en-US/images/phoneScreenshots/`.

### Background and distinguishing features

Yeshua is maintained by `danzuep` (Daniel Collingwood) and is a fork of [Joshua by Xizhi Zhu](https://github.com/xizzhu/Joshua). The fork continued development after the original project appeared inactive, updated the build toolchain, and restored the unit tests. The project was developed to provide a FOSS reader for the public domain Bible translations maintained by Michael Johnson at eBible.org.

Its focus is a small, offline-capable reader combining side-by-side translation comparison, full-text search, personal annotations, and access to eBible.org's multilingual USFM translations. Rather than claiming features that no other F-Droid Bible app offers, this submission highlights that combination in a lightweight, minimalist application.

## Checklist

### Policy

* [ ] The app complies with the [inclusion policy](https://f-droid.org/docs/Inclusion_Policy). _Pay special attention to Quality point 6._
* [ ] The original app author has been notified (and does not oppose the inclusion). If you are not the author, please paste the link of the reply from the author.
* [x] The upstream app source code repo contains the app metadata in a [Fastlane](https://gitlab.com/snippets/1895688) or [Triple-T](https://gitlab.com/snippets/1901490) folder structure. The summary and description must be included and images, icon, and changelog should also be provided for better user experience. The `en-US` locale must be included. Summary, description, icon, and a changelog are present; screenshots and a changelog for the submitted version remain to be added.

This is an inclusion request for the Yeshua fork, not the original Joshua app. Author-notification confirmation has not been provided with this draft; leave that item unchecked until confirmed. Full inclusion-policy compliance also remains subject to review, including translation licensing and Quality point 6.

### Docs

* [ ] Please read [the guide](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md) first if this is your first contribution.
* [ ] Please make sure your metadata follows the best practice in [our templates](https://gitlab.com/fdroid/fdroiddata/tree/master/templates).
* [ ] Please read the [Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/) and make sure your metadata is valid.
* [ ] Please read the [Quick Start Guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/).

### Merge Request Setup

* [ ] The title of this merge request should follow "New app: app name" format.
* [ ] Please make sure your fdroiddata fork is public and your branch is not protected. See <https://docs.gitlab.com/user/project/repository/branches/protected/>.
* [ ] Please read [our Git guide](https://gitlab.com/fdroid/wiki/-/wikis/Tips-for-fdroiddata-contributors/Git-Usage) if you don't know how to rebase your branch. Don't rebase your branch if there is no conflict.
* [ ] All related [fdroiddata](https://gitlab.com/fdroid/fdroiddata/issues) and [RFP issues](https://gitlab.com/fdroid/rfp/issues) have been referenced in this merge request
* [x] Please only submit one app in one MR.

### Metadata

* [ ] Metadata must be put in `metadata/com.github.danzuep.yeshua.android.yml`. The upstream draft is in `docs/com.github.danzuep.yeshua.android.yml`; verify the final fdroiddata MR path.
* [ ] Metadata must be a valid YAML file.
* [ ] Metadata must use LF as line ending.
* [ ] Don't add summary/description/changelog/images or anything that should be provided in upstream repo. Please check the Changes tab to make sure there is no other unrelated files added in the MR.
* [x] Releases are tagged and auto update is enabled unless there is a special reason. The draft uses `AutoUpdateMode: Version` and `UpdateCheckMode: Tags ^v[0-9.]+$`.
* [ ] There is an issue tracker and contact info of the author so that we can report bugs and contact the author.
* [x] An AuthorName must be added. It doesn't need to be the real name. The draft sets `AuthorName: danzuep`.
* [ ] External repos are added as git submodules instead of srclibs. You can update git submodules without opening an MR in this repo and the submodule is covered by our scanner.
* [ ] Enable [Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds). We'll use your signature for improved security/reliability, also allowing users to switch between different channels. Do note that if you don't enable reproducible build then the apk will be signed with our key so you can't enable it later. If you can't enable this, please add the reasons here.
* [ ] Setup abi split if the APK is large and the splitted ones can be much smaller.
* [ ] Only the latest versions should be kept in the metadata before it's merged. If you update the metadata, please replace the old versions with the new ones.
* [x] Don't add any disabled versions in the metadata. The draft contains no disabled builds.
* [ ] The `commit` field should be the full hash. Please don't use tag or branch in commit.

The draft currently lists version `0.33.2` (`versionCode: 3302`) and uses `commit: v0.33.2`. Confirm that this is the latest published release and replace the tag with its full commit hash before submission.

Reproducible builds are not yet configured or verified: the current metadata has no upstream APK reference or signing-certificate allowlist, and byte-for-byte reproduction of the GitHub APK has not been demonstrated. Keep this item open until the release build is verified and the appropriate metadata is added, or the maintainers agree to F-Droid signing with its own key. Never publish the private keystore or its password; reproducible-build setup uses public certificates and signed APKs.

### Pipeline

* [ ] All pipelines should pass.
* [ ] All warnings and errors in the Reports tab should be fixed or explained.
* [ ] F-Droid CI runners are under GitLab's FOSS program, so there's no need for you to pay for any CI time. If Gitlab starts asking for phone numbers or credit cards don't submit anything, just leave a note in the MR so we know we need to trigger the CI.

---------------------

Related RFP: [fdroid/rfp#4730](https://gitlab.com/fdroid/rfp/-/work_items/4730).

Closes rfp#4730

/label ~"New App"
