# Upstream source pin

`jargon.xml` is pinned from:

- Repo: https://github.com/agiacalone/jargonfile
- Path: `template/jargon.xml`
- Commit: `d7c9d6924caa34b16c411c96b147dca41757af1d` (2025-09-27)
- License: CC-BY-SA-4.0 (`UPSTREAM-LICENSE.txt`, this repo's `LICENSE.txt`)

To refresh: download the same path at a newer commit, review the diff,
replace this file, then re-run `tools/build_jargon_json.py` and re-check
`app/src/main/assets/jargon.json` into the repo. The app build never
fetches this over the network.

The content itself has been essentially frozen since Eric Raymond's last
substantive revision (version 4.4.7, 29 Dec 2003) — the pinned commit above
is a syntax-only bugfix, the first non-trivial commit to this file since the
community-edition repo was created in 2021. There's no routine cadence to
follow here; check back occasionally (a quick glance at the repo's commit
history is enough), not on a schedule.
