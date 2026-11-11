"""Fetch the exact official dependency into a reconstructed Chao Garden source tree."""
from pathlib import Path
import hashlib,sys,urllib.request
if len(sys.argv)!=2:raise SystemExit('Usage: python fetch_lionfish.py CHAO_SOURCE')
expected='fdd90f19d0424c3a837111291d6ac1547d62cbcedd43b74d3a8946912f9c5212a5ea239a6969eccf678289bcb77cbb9a937db01fdaf60e57826f9a99893d19cb'
dest=Path(sys.argv[1])/'libs/lionfishapi-3.1.jar'
url='https://cdn.modrinth.com/data/FoVacERa/versions/fTRMVgyZ/lionfishapi-3.1.jar'
if dest.exists():data=dest.read_bytes()
else:
 with urllib.request.urlopen(url,timeout=60) as response:data=response.read()
if hashlib.sha512(data).hexdigest()!=expected:raise SystemExit('Checksum mismatch; no file written.')
if not dest.exists():dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
print('Verified Lionfish API 3.1:',dest)
