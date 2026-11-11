"""Closed, fsynced, atomic proof publication in the synchronized workspace."""
from pathlib import Path
import json,os

def _publish_bytes(target,payload):
 target=Path(target);target.parent.mkdir(parents=True,exist_ok=True)
 temporary=target.with_name(target.name+'.tmp')
 with temporary.open('wb') as handle:handle.write(payload);handle.flush();os.fsync(handle.fileno())
 os.replace(temporary,target)

def write_json_atomic(target,value):
 _publish_bytes(target,(json.dumps(value,indent=2)+'\n').encode())

def copy_atomic(source,target):
 source=Path(source);target=Path(target)
 if source.resolve()!=target.resolve():_publish_bytes(target,source.read_bytes())
