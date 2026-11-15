#!/usr/bin/env python3
"""Regenerate only the five jack-in-the-box stages; shared full pipeline uses the same builder."""
from pathlib import Path
import importlib.util
import json

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('jack_source080',ROOT/'art/build_toy_blocks_018.py')
source=importlib.util.module_from_spec(spec);spec.loader.exec_module(source)
for stage,active,frame in [('idle',False,0),*((f'play_{i}',True,i) for i in range(4))]:
    path=source.ASSETS/'models/block'/f'chao_jack_in_box_{stage}.json'
    path.write_text(json.dumps(source.model('chao_jack_in_box',active,frame),indent=2)+'\n')
print('Regenerated only five jack-in-the-box models; item binding, registry and acquisition retained.')
