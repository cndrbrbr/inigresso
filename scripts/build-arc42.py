#!/usr/bin/env python3
"""Erstellt aus architektur/*.md und adr/*.md ein arc42-PDF samt Mermaid."""

import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ARCH = ROOT / 'architektur'
BUILD = ROOT / 'build'
DIAGRAMS = BUILD / 'diagrams'
MERMAID = re.compile(r'^```mermaid\s*\n(.*?)^```\s*$', re.MULTILINE | re.DOTALL)


def main():
    DIAGRAMS.mkdir(parents=True, exist_ok=True)
    chapters = sorted(ARCH.glob('[0-9][0-9]-*.md'))
    adrs = sorted(p for p in (ARCH / 'adr').glob('*.md') if not p.name.startswith('0000-'))
    if not chapters:
        raise RuntimeError('Keine arc42-Kapitel gefunden')

    (BUILD / 'puppeteer.json').write_text('{"args": ["--no-sandbox"]}', encoding='utf-8')
    content = []
    number = 0
    for source in [*chapters, *adrs]:
        if source in adrs and source == adrs[0]:
            content.append('\\newpage\n\n# Anhang: Architekturentscheidungen (ADR)\n')
        text = source.read_text(encoding='utf-8')

        def render(match):
            nonlocal number
            number += 1
            input_file = DIAGRAMS / f'diagram-{number:03}.mmd'
            png_file = DIAGRAMS / f'diagram-{number:03}.png'
            input_file.write_text(match.group(1), encoding='utf-8')
            subprocess.run([
                'mmdc', '-i', str(input_file), '-o', str(png_file),
                '-b', 'white', '-s', '2', '-p', str(BUILD / 'puppeteer.json')
            ], check=True, cwd=ROOT)
            return f'\n![Architekturdiagramm](diagrams/{png_file.name})\n'

        text = MERMAID.sub(render, text)
        # ADR-Links und Kapitel-Links nach wie vor als Referenzen sichtbar.
        content.append(text + '\n\n')

    source_md = BUILD / 'arc42-kombiniert.md'
    source_md.write_text('\n\\newpage\n\n'.join(content), encoding='utf-8')
    subprocess.run([
        'pandoc', str(source_md), '-o', str(BUILD / 'inigresso-arc42.pdf'),
        '--from=markdown+raw_tex', '--pdf-engine=xelatex',
        '--resource-path', os.pathsep.join([str(BUILD), str(ARCH), str(ARCH / 'adr'), str(ROOT)]),
        '--toc', '--toc-depth=3', '-V', 'lang=de-DE',
        '-V', 'geometry:margin=23mm',
        '-V', 'colorlinks=true',
        '-M', 'title=Inigresso – Softwarearchitektur (arc42)',
    ], check=True, cwd=ROOT)
    print(f'PDF erstellt: {BUILD / "inigresso-arc42.pdf"}')


if __name__ == '__main__':
    main()
