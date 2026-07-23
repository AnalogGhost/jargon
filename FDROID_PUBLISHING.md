# F-Droid Publishing Guide

Package ID: `com.hackerapps.jargon`

---

## 1. Push to GitHub

Create a repo at `https://github.com/AnalogGhost/jargon`, then:

```bash
git remote add origin https://github.com/AnalogGhost/jargon.git
git push -u origin main
git tag v1.0.0
git push origin v1.0.0
```

---

## 2. Check fastlane metadata

F-Droid reads from `fastlane/metadata/android/en-US/`. Verify these are accurate before submitting:

- `title.txt` — `Jargon`
- `short_description.txt` — one line, ≤80 chars
- `full_description.txt` — paragraph description
- `changelogs/1.txt` — changelog for versionCode 1

---

## 3. Fork fdroiddata

Fork `https://gitlab.com/fdroid/fdroiddata` on GitLab, then clone it locally (or reuse the existing `~/Projects/fdroiddata` clone, which already has `origin` pointing at the fork). Sync your fork's `master` to upstream before branching — it drifts fast, this repo gets thousands of commits.

---

## 4. Create the app metadata file

Add `metadata/com.hackerapps.jargon.yml` to your fdroiddata fork. This is the version that actually passed CI (`fdroid build`, `fdroid lint`, `fdroid rewritemeta`) for the v1.0.0 submission:

```yaml
Categories:
  - Reading
  - Science & Education
License: GPL-3.0-only
AuthorName: Matt Brown
AuthorEmail: jargon@hackerapps.com
AuthorWebSite: https://hackerapps.com
SourceCode: https://github.com/AnalogGhost/jargon
IssueTracker: https://github.com/AnalogGhost/jargon/issues

AutoName: Jargon

RepoType: git
Repo: https://github.com/AnalogGhost/jargon
Binaries: https://github.com/AnalogGhost/jargon/releases/download/v%v/app-release.apk

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: <full 40-char commit SHA the release tag points to>
    subdir: app
    gradle:
      - yes

AllowedAPKSigningKeys: <SHA-256 fingerprint of the release keystore cert, colons stripped, lowercase>

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

Notes, learned the hard way from real CI failures on this exact submission (MR !43683) plus c2k's history:

- `commit:` must be the **full commit hash**, not a tag or branch name — a maintainer flagged this directly in review. Get it with `git rev-parse v1.0.0` — but note that resolves an *annotated* tag to its own tag-object hash, not the commit; use `git log -1 v1.0.0 --format=%H` to get the actual commit SHA.
- `subdir: app` is required since the Gradle module lives in `app/`, not repo root. Without it, `fdroid build` succeeds but then fails with "Failed to find any output apks" — fdroidserver looks for the output in the wrong directory.
- `gradle: [yes]` is required even with no product flavors — omitting it or using `[release]` both get rejected/fixed during review.
- `AutoUpdateMode: Version` — not `Version v%v`. The `v%v` form was c2k's original (wrong) submission; F-Droid's schema check corrected it.
- No `Summary`/`Description` fields — current fdroiddata convention pulls these from the app repo's own `fastlane/metadata/android/en-US/{short_description,full_description}.txt` instead of duplicating them here.
- `CurrentVersion`/`CurrentVersionCode` go **after** `AutoUpdateMode`/`UpdateCheckMode`, not before — `fdroid rewritemeta` enforces exact field order and fails CI otherwise.
- `Binaries:`/`AllowedAPKSigningKeys` are optional but enable F-Droid's Reproducible Builds verification (their build must byte-match this URL's asset). Only add them once a real signed GitHub release exists — get the fingerprint with `keytool -list -v -keystore ~/jargon-release.jks -storepass "$PASS"`, take the `SHA256:` line, strip colons, lowercase. If F-Droid's build doesn't byte-match later, the fix historically has been a `postbuild` zipalign step using `reproducible-apk-tools` (see `com.hackerapps.c2k.yml`'s history) — don't add that preemptively, only if CI actually flags a mismatch.

---

## 5. Validate locally (optional but saves round-trips)

If you have F-Droid server tools installed (`pip install fdroidserver`):

```bash
fdroid lint metadata/com.hackerapps.jargon.yml
fdroid readmeta
```

---

## 6. Submit the merge request

```bash
cd ~/Projects/fdroiddata
git checkout -b add-com.hackerapps.jargon master
# add metadata/com.hackerapps.jargon.yml
git add metadata/com.hackerapps.jargon.yml
git commit -m "Add com.hackerapps.jargon"
git push -o merge_request.create -o merge_request.target=master origin add-com.hackerapps.jargon
```

The push option opens the MR directly against `fdroid/fdroiddata` (GitLab's fork relationship handles the target project automatically), but it does **not** apply the repo's MR template — a maintainer flagged this on !43683. Immediately after creating it: edit the MR, use the "Choose a template" dropdown to select **App inclusion**, delete the instructional header (down through "Please remove above lines!"), and fill in the checklist honestly rather than leaving it default-unchecked. Also rename the title to `New app: <AppName>` format per the template's own instruction — the push-option title doesn't follow that format automatically.

Their bot runs a build check automatically (`fdroid build`, `fdroid lint`, `fdroid rewritemeta`, `schema validation`). Check the pipeline status before assuming it's fine — a merge request can sit "open" while its pipeline is actually failing. A maintainer will review after CI is green — typically 1–4 weeks.

---

## Releasing future versions

After the initial merge, new releases are automatic. Just bump `versionCode` and `versionName`
in `app/build.gradle.kts`, then tag and push:

```bash
git tag v1.x.x
git push origin v1.x.x
```

`UpdateCheckMode: Tags` picks up new tags and queues an F-Droid build with no further MR needed.

To pick up a newer upstream Jargon File snapshot: refresh `tools/jargon-source/jargon.xml`
per `tools/jargon-source/SOURCE.md`, re-run `tools/build_jargon_json.py`, review the diff in
`app/src/main/assets/jargon.json`, then release as normal.

---

## Version checklist

Before tagging a release:

- [ ] Increment `versionCode` (integer, always increasing)
- [ ] Update `versionName` (e.g. `1.1.0`)
- [ ] Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
- [ ] Run `./gradlew test` — all tests pass
- [ ] Run `./gradlew assembleRelease` — release APK builds clean
- [ ] Commit, tag, push
