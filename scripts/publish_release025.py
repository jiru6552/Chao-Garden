"""Publish immutable verified bytes atomically, away from live archive writers."""
from pathlib import Path
import hashlib
import os
import shutil
import uuid


def digest(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def publish(source, target):
    source, target = Path(source), Path(target)
    expected = digest(source)
    target.parent.mkdir(parents=True, exist_ok=True)
    # The Work environment excludes this directory from synchronization.
    # Neither a partial copy nor an active archive writer uses the final path.
    staging = target.parent / '.rsync-tmp'
    staging.mkdir(exist_ok=True)
    temporary = staging / (uuid.uuid4().hex + '.tmp')
    with source.open('rb') as incoming, temporary.open('xb') as outgoing:
        shutil.copyfileobj(incoming, outgoing, 1024 * 1024)
        outgoing.flush()
        os.fsync(outgoing.fileno())
    if digest(temporary) != expected or digest(source) != expected:
        raise RuntimeError('Publication input/copy changed: ' + str(target))
    os.replace(temporary, target)
    os.utime(target, None)
    with target.open('rb') as stream:
        os.fsync(stream.fileno())
    handle = os.open(target.parent, os.O_DIRECTORY)
    try:
        os.fsync(handle)
    finally:
        os.close(handle)
    if digest(target) != expected:
        raise RuntimeError('Published bytes differ: ' + str(target))
    return expected
