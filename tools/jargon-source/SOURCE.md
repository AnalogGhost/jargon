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
