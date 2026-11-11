#!/usr/bin/env python3
"""Focused export policy regressions: primary union, current logs and unsafe files."""
from pathlib import Path
import tempfile
import unittest
from source_export0253 import helpers, source_files


class SourceExport0253Test(unittest.TestCase):
    def setUp(self):
        self.directory=tempfile.TemporaryDirectory();self.addCleanup(self.directory.cleanup);self.root=Path(self.directory.name)
        for relative in helpers.IMPORTED_ALLOWLIST:self.add('art/imported_chao/'+relative)

    def add(self,relative,payload=b'fixture'):
        path=self.root/relative;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(payload);return path

    def exported(self):return {relative for _,relative in source_files(self.root)}

    def test_primary_input_union_and_current_log_suffix_survive(self):
        primary=['src/main/java/Garden.java','src/main/resources/native.png','art/blockbench/plant.bbmodel','gradle/wrapper/gradle-wrapper.jar']
        primary+=['docs/references-'+version+'/original.png' for version in ('0.25','0.25.1','0.25.2','0.25.3')]
        current=['docs/validation-0.25.3/production-build.txt','docs/validation-0.25.3/native.log',
                 'docs/validation-0.25.3/tooling/capture.py','docs/screenshots-0.25.3/actual.png',
                 'docs/motion-0.25.3/chao0253-native-raw.ffmpeg.log','docs/motion-0.25.3/actual.mp4']
        old=['docs/screenshots-0.25.2/old.png','docs/motion-0.25.2/old.mp4',
             'docs/validation-0.25.2/old.json','docs/release-gallery-0.25.2.html']
        excluded=['build/generated.class','run-visual0253-v1/options.txt','runtime.log','cached.zip']
        for relative in primary+current+old+excluded:self.add(relative)
        exported=self.exported()
        self.assertTrue(set(primary+current)<=exported)
        self.assertTrue(set(old+excluded).isdisjoint(exported))
        self.assertEqual(len([file for file in exported if file.startswith('art/imported_chao/')]),41)
        rows=list(source_files(self.root));self.assertEqual([relative for _,relative in rows],sorted(exported))

    def test_primary_input_cannot_silently_disappear_through_suffix_filter(self):
        self.add('src/required.log')
        with self.assertRaisesRegex(RuntimeError,'Primary source/reference inventory omitted'):self.exported()

    def test_symlink_is_rejected(self):
        target=self.add('src/real.java');(self.root/'src/alias.java').symlink_to(target)
        with self.assertRaisesRegex(RuntimeError,'Unsafe source path'):self.exported()

    def test_unknown_import_is_rejected(self):
        self.add('art/imported_chao/unrecorded.png')
        with self.assertRaisesRegex(RuntimeError,'Unknown imported Chao input'):self.exported()


if __name__=='__main__':unittest.main()
