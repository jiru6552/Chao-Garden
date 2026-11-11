"""One opaque white texel for native runtime expression strokes; no source art edits."""
from pathlib import Path
import struct
import zlib

root = Path(__file__).resolve().parents[1]
target = root / "src/main/resources/assets/chao_garden/textures/entity/chao/expression_stroke025.png"


def chunk(kind, payload):
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))


png = b"\x89PNG\r\n\x1a\n"
png += chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0))
png += chunk(b"IDAT", zlib.compress(b"\x00\xff\xff\xff\xff"))
png += chunk(b"IEND", b"")
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(png)
print(target.relative_to(root))
