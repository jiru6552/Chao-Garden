#!/usr/bin/env python3
"""Isolated proof-binding controls; this never creates visual PASS evidence."""
from pathlib import Path
import copy
import hashlib
import importlib.util
import json
import tempfile

ROOT=Path(__file__).resolve().parents[1]


def main():
    spec=importlib.util.spec_from_file_location('release022_fixture',ROOT/'scripts/package_chao022.py')
    package=importlib.util.module_from_spec(spec);spec.loader.exec_module(package)
    rejected=[]
    with tempfile.TemporaryDirectory(prefix='chao-release022-proof-') as directory:
        root=Path(directory);out=root/'out';out.mkdir();evidence=root/'docs/validation-0.22';evidence.mkdir(parents=True)
        def write(path,value):
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(json.dumps(value,indent=2)+'\n')
        def edit(path,change):
            value=json.loads(path.read_text());change(value);write(path,value)
        plan=dict(features=[dict(key=k,title=k,subtitle='fixture',caption='fixture') for k in ('a','b')],
                  motions=[dict(key='walk',frames=['walk-00','walk-01'])],pdf=['a','b'])
        captures=[dict(key=k,file=k+'.png',sha256=hashlib.sha256(k.encode()).hexdigest()) for k in ('a','b','walk-00','walk-01')]
        manifest=root/'manifest.json';write(manifest,dict(captures=captures))
        # A binary payload is sufficient for this hash-binding unit fixture;
        # actual PDF parsing/rendering is independently required by packaging.
        pdf=root/'reviewed-payload.bin';pdf.write_bytes(b'isolated evidence fixture')
        sources=dict(pages=[dict(page=i+1,key=r['key'],sha256=r['sha256']) for i,r in enumerate(captures[:2])])
        records=[dict(page=i+1,source_key=r['key'],source_capture_sha256=r['sha256'],render_sha256=hashlib.sha256(('render'+r['key']).encode()).hexdigest(),reviewer='fixture') for i,r in enumerate(captures[:2])]
        delegated=evidence/'delegated.json'
        write(delegated,dict(result='PASS',pdf_sha256=package.sha256(pdf),pages=[dict(page=r['page'],render_sha256=r['render_sha256'],result='PASS') for r in records]))
        overview=evidence/'pdf-review.json'
        write(overview,dict(release='0.22.0',result='PASS',pdf_sha256=package.sha256(pdf),pages=2,every_page_individually_inspected=True,
            selected_page_metadata_sha256=package.pdf_page_metadata_sha256(plan),reviewed_page_records=records,
            review_reports=[dict(file=str(delegated.relative_to(root)),sha256=package.sha256(delegated),pages=[1,2])]))
        write(evidence/'static-review.json',dict(release='0.22.0',result='PASS',source_capture_manifest_sha256=package.sha256(manifest),
            frames=[dict(**r,result='PASS') for r in captures[:2]]))
        write(evidence/'motion-review.json',dict(release='0.22.0',result='PASS',raw_motion_frames_inspected=2,source_capture_manifest_sha256=package.sha256(manifest),
            sequences=[dict(sequence='walk',frames=[dict(file=r['file'],sha256=r['sha256'],result='PASS',used_in_gif=True) for r in captures[2:]])]))
        relative=root/'docs/release-gallery-0.22.0.html';relative.write_text('fixture relative HTML')
        standalone=out/'chao-garden-0.22.0-gallery.html';standalone.write_text('fixture standalone HTML')
        for name,path in [('gallery.json',relative),('gallery-standalone.json',standalone)]:
            write(evidence/name,dict(release='0.22.0',result='PASS',html_sha256=package.sha256(path),static_cards=2,motion_cards=1))
        package.validate_review_evidence(root,plan,pdf,sources,captures,manifest,out)
        baseline={p:p.read_bytes() for p in root.rglob('*') if p.is_file()}
        def reject(name,mutation):
            current=copy.deepcopy(plan)
            mutation(current)
            try:
                package.validate_review_evidence(root,current,pdf,sources,captures,manifest,out)
            except (AssertionError,RuntimeError):
                rejected.append(name)
            else:
                raise AssertionError('Release gate accepted corruption: '+name)
            finally:
                for path,data in baseline.items():path.write_bytes(data)
        reject('stale PDF bytes',lambda p:pdf.write_bytes(b'changed'))
        reject('missing page approval',lambda p:edit(overview,lambda d:d['reviewed_page_records'].pop()))
        reject('changed selected caption',lambda p:p['features'][0].update(caption='changed'))
        def bad_render(p):
            edit(delegated,lambda d:d['pages'][0].update(render_sha256='0'*64))
            edit(overview,lambda d:d['review_reports'][0].update(sha256=package.sha256(delegated)))
        reject('delegated render differs from aggregate',bad_render)
        reject('missing static approval',lambda p:edit(evidence/'static-review.json',lambda d:d['frames'].pop()))
        reject('omitted motion review frame',lambda p:edit(evidence/'motion-review.json',lambda d:d['sequences'][0]['frames'][1].update(used_in_gif=False)))
        reject('stale relative gallery',lambda p:relative.write_text('changed'))
        reject('changed capture manifest',lambda p:manifest.write_text('changed'))
    report=dict(release='0.22.0',result='PASS',checks=1+len(rejected),valid_fixture_accepted=True,rejected_corruptions=rejected,
                scope='Synthetic proof-binding controls only. These data are never final PDF, gallery or capture approvals.')
    path=ROOT/'docs/research/RELEASE_GATE_022_FIXTURE.json';path.parent.mkdir(parents=True,exist_ok=True);write(path,report)
    print(json.dumps(report,indent=2))


if __name__=='__main__':main()
