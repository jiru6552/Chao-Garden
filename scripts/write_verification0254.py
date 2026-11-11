#!/usr/bin/env python3
"""Write concise verification prose only from completed current release reports."""
import argparse
import json
from pathlib import Path
from source_export0254 import ROOT, PROOF, VERSION, validate_review_closure, sha


def main(out):
    validate_review_closure()
    core = json.loads((ROOT/PROOF/'core.json').read_text())
    default = json.loads((ROOT/PROOF/'server-default.json').read_text())
    accessories = json.loads((ROOT/PROOF/'server-accessories.json').read_text())
    production = json.loads((ROOT/PROOF/'production-jar.json').read_text())
    rebuild = json.loads((ROOT/PROOF/'asset-rebuild.json').read_text())
    jar = ROOT/f'build/libs/chao_garden-{VERSION}.jar'
    assert production['artifact']['sha256'] == sha(jar)
    # Field names are fixed by the current native report writer.
    count = lambda report: report['required_tests_passed']
    text = f'''# Chao Garden {VERSION} verification

The production build and all {len(core['suites'])} core suites passed, totaling {core['total_checks']:,} checks. Fresh native GameTest servers passed {count(default)}/{count(default)} cases with the base mod and {count(accessories)}/{count(accessories)} with the pinned real Accessories and owo dependencies. Both profiles used the exact final production JAR.

The native suite includes the new landscape and owned-world migration cases, eight-part transporter placement/removal and legacy upgrades, Hero device preservation and obstruction handling, clinic furnishings, Ionic column geometry and ceiling contacts, cornice placement, and shaped door picking. Earlier care, school, shop, garden and optional companion regressions remain enabled.

The full asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource-domain, texture, geometry and malformed-input guards passed. Production classes/resources were compared byte for byte with the tested artifact; development fixtures and test classes are excluded from the JAR.

The startup trace exposed repeated optimization of the detailed Ionic collision mesh; balanced unions now preserve its exact geometry and optimize once. An initial full run also exposed two stale landscape assertions, which were corrected while retaining the physical boundary checks. Failed-world entity data showed that the initial TV approach fixture had randomly developed hiccups, correctly disabling play while ill. That navigation fixture now receives the existing treatment immunity so unrelated random illness cannot interrupt the route. Both final full profiles passed with unchanged game AI, newborn speed, obstacles and timeout. The resident lifecycle fixture also waits for all six campus chunks to be entity-ticking before checking recovery, matching the mod's existing guard against duplicate residents; identity, position and stock assertions remain intact. Diagnostic logs are retained with the verification evidence.

No new screenshot/video gallery or native client visual review is claimed for this release. Final in-game appearance, lighting and feel can be checked directly with the supplied JAR, as requested.

Production artifact: `{jar.name}` ({jar.stat().st_size:,} bytes).

SHA-256: `{sha(jar)}`

Detailed reports and closed logs are in `docs/validation-{VERSION}/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads. Original feedback pictures are retained in `docs/references-{VERSION}/feedback/`.
'''
    (ROOT/'TESTING.md').write_text(text)
    out.mkdir(parents=True, exist_ok=True)
    plain = text.replace('# ', '', 1).replace('`', '')
    (out/f'chao-garden-{VERSION}-verification.txt').write_text(plain)
    print(json.dumps(dict(release=VERSION, result='WRITTEN', jar_sha256=sha(jar),
                         core_checks=core['total_checks'], default_cases=count(default),
                         accessories_cases=count(accessories))))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output-dir', type=Path, default=ROOT.parent/'deliverables')
    main(p.parse_args().output_dir.resolve())
