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

Fork `https://gitlab.com/fdroid/fdroiddata` on GitLab, then clone it locally (or reuse the existing `~/Projects/fdroiddata` clone).

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
Summary: Offline reader for the Jargon File, the original hacker culture dictionary
Description: |-
  Fully offline reader for the Jargon File — the hacker culture dictionary
  that gave the word "hacker" its original meaning. Over 2,300 entries,
  browse or search, tap cross-references between entries, favorite the
  ones you like, or hit random for a surprise.

  No accounts, no ads, no analytics, no network access of any kind —
  the dictionary ships inside the app.

  Dictionary content is CC BY-SA 4.0 from the Jargon File community
  edition. App source is GPL-3.0.

RepoType: git
Repo: https://github.com/AnalogGhost/jargon

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
```

Note: unlike c2k, this app has no `foss`/`play` product flavors — it only targets F-Droid for now, so the `Builds` entry has no `gradle:` key (builds the single default variant).

---

## 5. Validate locally (optional but saves round-trips)

If you have F-Droid server tools installed (`pip install fdroidserver`):

```bash
fdroid lint metadata/com.hackerapps.jargon.yml
fdroid readmeta
```

---

## 6. Submit the merge request

Push your branch to your fdroiddata fork and open an MR against `fdroid/fdroiddata` on GitLab.
Their bot runs a build check automatically. A maintainer will review — typically 1–4 weeks.

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
