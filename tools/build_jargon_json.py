#!/usr/bin/env python3
"""Parse the pinned Jargon File DocBook source into app/src/main/assets/jargon.json.

Source: jargon-source/jargon.xml, pinned to a specific commit of
https://github.com/agiacalone/jargonfile (see jargon-source/SOURCE.md).
Re-run this script manually after updating the pinned source; the app
build never fetches anything over the network.
"""
import html.entities
import json
import pathlib
import re
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).parent
SOURCE = ROOT / "jargon-source" / "jargon.xml"
OUTPUT = ROOT.parent / "app" / "src" / "main" / "assets" / "jargon.json"

# Entities the DocBook source defines itself (not in HTML's table), taken
# from the <!ENTITY ...> declarations at the top of jargon.xml.
CUSTOM_ENTITIES = {
    "ellipsis": "...",
    "endellipsis": "....",
    "version": "4.4.7",
    "date": "29 Dec 2003",
    "jargonmail": "esr@thyrsus.com",
    "esrhome": "http://www.catb.org/~esr",
    "jargonurl": "http://www.catb.org/~esr/jargon/",
    "schwa": "@",
}

# XML's five predefined entities are legal without a DTD declaration.
XML_BUILTIN_ENTITIES = {"amp", "lt", "gt", "quot", "apos"}

GLOSSENTRY_RE = re.compile(r"<glossentry\b.*?</glossentry>", re.DOTALL)
ENTITY_REF_RE = re.compile(r"&([a-zA-Z][a-zA-Z0-9]*);")


def resolve_entity_name(name: str) -> str:
    if name in CUSTOM_ENTITIES:
        return CUSTOM_ENTITIES[name]
    char = html.entities.html5.get(name + ";") or html.entities.html5.get(name)
    if char is None:
        raise ValueError(f"Unresolvable XML entity: &{name};")
    return char


def build_entity_doctype(entry_xml: str) -> str:
    names = {m.group(1) for m in ENTITY_REF_RE.finditer(entry_xml)}
    names -= XML_BUILTIN_ENTITIES
    declarations = []
    for name in sorted(names):
        value = resolve_entity_name(name)
        escaped = value.replace("&", "&amp;").replace("'", "&apos;")
        declarations.append(f"<!ENTITY {name} '{escaped}'>")
    return "\n".join(declarations)


def parse_entry_fragment(entry_xml: str) -> ET.Element:
    doctype = build_entity_doctype(entry_xml)
    wrapped = f"<?xml version='1.0'?>\n<!DOCTYPE root [\n{doctype}\n]>\n<root>{entry_xml}</root>"
    return ET.fromstring(wrapped)


def render_text(el: ET.Element, see_also: set[str]) -> str:
    parts: list[str] = []
    if el.text:
        parts.append(el.text)
    for child in el:
        if not isinstance(child.tag, str):
            # Comment or processing instruction (attribution notes) — skip.
            if child.tail:
                parts.append(child.tail)
            continue
        inner = render_text(child, see_also)
        if child.tag == "glossterm":
            see_also.add(" ".join(inner.split()))
            parts.append(inner)
        elif child.tag == "quote":
            parts.append("“" + inner + "”")
        else:
            parts.append(inner)
        if child.tail:
            parts.append(child.tail)
    return "".join(parts)


def normalize_paragraph(text: str) -> str:
    return " ".join(text.split())


def extract_glossdef_text(glossdef: ET.Element, see_also: set[str]) -> str:
    paragraphs = [
        normalize_paragraph(render_text(para, see_also))
        for para in glossdef.findall("para")
    ]
    return "\n\n".join(p for p in paragraphs if p)


def parse_entry(entry_xml: str) -> dict:
    root = parse_entry_fragment(entry_xml)
    entry = root.find("glossentry")

    entry_id = entry.get("id")
    term_el = entry.find("glossterm")
    term = normalize_paragraph(term_el.text or "")

    pronunciation = None
    part_of_speech = None
    abbrev = entry.find("abbrev")
    if abbrev is not None:
        for emphasis in abbrev.findall("emphasis"):
            role = emphasis.get("role")
            text = normalize_paragraph("".join(emphasis.itertext()))
            if role == "pronunciation":
                pronunciation = text or None
            elif role == "grammar":
                part_of_speech = text or None

    see_also: set[str] = set()
    definitions = []
    etymologies = []
    histories = []
    references = []
    for glossdef in entry.findall("glossdef"):
        role = glossdef.get("role")
        text = extract_glossdef_text(glossdef, see_also)
        if not text:
            continue
        if role == "etymology":
            etymologies.append(text)
        elif role == "history":
            histories.append(text)
        elif role == "references":
            references.append(text)
        else:
            definitions.append(text)

    see_also.discard(term)

    if not definitions and etymologies:
        # A few entries (e.g. MOTAS) only carry a role='etymology' block with
        # no separate un-roled glossdef — that block *is* the definition.
        definitions, etymologies = etymologies, []

    return {
        "id": entry_id,
        "term": term,
        "sortKey": re.sub(r"[^a-z0-9]", "", term.lower()) or entry_id.lower(),
        "pronunciation": pronunciation,
        "partOfSpeech": part_of_speech,
        "definition": "\n\n".join(definitions),
        "etymology": "\n\n".join(etymologies) or None,
        "history": "\n\n".join(histories + references) or None,
        "seeAlso": sorted(see_also),
    }


def main() -> None:
    raw = SOURCE.read_text(encoding="utf-8")
    matches = GLOSSENTRY_RE.findall(raw)
    if not matches:
        raise SystemExit(f"No <glossentry> elements found in {SOURCE}")

    entries = [parse_entry(m) for m in matches]
    entries.sort(key=lambda e: e["sortKey"])

    seen_ids = set()
    for e in entries:
        if e["id"] in seen_ids:
            raise SystemExit(f"Duplicate entry id: {e['id']}")
        seen_ids.add(e["id"])
        if not e["definition"]:
            raise SystemExit(f"Entry {e['id']} ({e['term']!r}) has no definition text")

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(
        json.dumps(entries, ensure_ascii=False, indent=None, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"Wrote {len(entries)} entries to {OUTPUT}")


if __name__ == "__main__":
    main()
