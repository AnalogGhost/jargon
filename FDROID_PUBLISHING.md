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

Add `metadata/com.hackerapps.jargon.yml` to your fdroiddata fork:

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

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    gradle:
      - yes

CurrentVersion: 1.0.0
CurrentVersionCode: 1

AutoUpdateMode: Version
UpdateCheckMode: Tags
```

Notes, learned the hard way from c2k's actual submission history:

- `gradle: [yes]` is required even with no product flavors — omitting it or using `[release]` both get rejected/fixed during review.
- `AutoUpdateMode: Version` — not `Version v%v`. The `v%v` form was c2k's original (wrong) submission; F-Droid's schema check corrected it.
- No `Summary`/`Description` fields — current fdroiddata convention pulls these from the app repo's own `fastlane/metadata/android/en-US/{short_description,full_description}.txt` instead of duplicating them here.
- `CurrentVersion`/`CurrentVersionCode` are required at submission time, not something added later — c2k's first submission was missing them and needed a follow-up fix commit.

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

The push option opens the MR directly against `fdroid/fdroiddata` (GitLab's fork relationship handles the target project automatically). Their bot runs a build check automatically. A maintainer will review — typically 1–4 weeks.

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
