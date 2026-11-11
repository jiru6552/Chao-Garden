"""Compatibility entry point for the current cube-art mesh and texture pipeline."""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name('build_adventure_assets.py')),run_name='__main__')
