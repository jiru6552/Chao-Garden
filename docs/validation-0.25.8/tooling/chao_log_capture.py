"""Recover interrupted Gradle console forwarding from its own complete daemon log."""
from pathlib import Path
import hashlib

def complete_console(stage, started_ns):
    data=stage.read_bytes()
    if b'BUILD SUCCESSFUL' in data or b'BUILD FAILED' in data:
        return True
    candidates=sorted((p for p in Path('/workspace/tools/gradle-home/daemon/9.2.1').glob('daemon-*.out.log')
                       if p.stat().st_mtime_ns >= started_ns),key=lambda p:p.stat().st_mtime_ns)
    if not candidates:
        return False
    source=candidates[-1]
    raw=source.read_bytes()
    if b'BUILD SUCCESSFUL' not in raw and b'BUILD FAILED' not in raw:
        return False
    marker=(f'\nGRADLE_CONSOLE_RECOVERY source={source} sha256={hashlib.sha256(raw).hexdigest()}\n').encode()
    with stage.open('ab') as handle:
        handle.write(marker);handle.write(raw);handle.flush()
    return True

def coherent_console(data):
    """Return the one complete SHA-bound daemon segment after console recovery.

    Keep the original composite file intact for provenance; its early forwarded
    prefix may duplicate events from the authoritative recovered complete run.
    """
    import re
    marker=b'GRADLE_CONSOLE_RECOVERY '
    at=data.rfind(marker)
    if at<0:return data
    end=data.find(b'\n',at)
    if end<0:raise ValueError('Incomplete console recovery marker')
    header=data[at:end]
    expected=re.search(rb'sha256=([a-f0-9]{64})',header)
    segment=data[end+1:]
    if expected is None or hashlib.sha256(segment).hexdigest().encode()!=expected[1]:
        raise ValueError('Recovered daemon console digest does not match its exact segment')
    return segment
