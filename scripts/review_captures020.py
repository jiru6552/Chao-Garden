#!/usr/bin/env python3
"""Whole-frame contact sheets for manual review; never modifies assets/captures."""
from pathlib import Path
import argparse,hashlib,json,math
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'

def main():
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument('--source',type=Path,default=ROOT/'run/screenshots')
 p.add_argument('--output',type=Path,default=ROOT/'tmp/review020')
 p.add_argument('--category',nargs='*')
 p.add_argument('--batch-size',type=int,default=9)
 p.add_argument('--columns',type=int,default=3)
 p.add_argument('--allow-partial',action='store_true')
 p.add_argument('--pdf-pages',action='store_true')
 a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
 if a.pdf_pages:
  records=[(f.name,f) for f in sorted(a.source.glob('*.png'))]
 else:
  plan=json.loads((ROOT/'docs/release-scenes-0.20.0.json').read_text())
  records=[(f['key'],a.source/(plan['filename_prefix']+f['key']+'.png')) for f in plan['features'] if not a.category or f['category'] in a.category]
 missing=[name for name,path in records if not path.is_file()]
 if missing and not a.allow_partial:raise ValueError('Missing review frames: '+', '.join(missing))
 records=[r for r in records if r[1].is_file()]
 if not records:raise ValueError('No actual captures available')
 font=ImageFont.truetype(FONT,20);small=ImageFont.truetype(FONT,17)
 manifest=[]
 for start in range(0,len(records),a.batch_size):
  batch=records[start:start+a.batch_size];rows=math.ceil(len(batch)/a.columns)
  tilew,tileh,pad=640,405,12
  sheet=Image.new('RGB',(a.columns*tilew+(a.columns+1)*pad,rows*tileh+(rows+1)*pad+45),'#edf3f5');d=ImageDraw.Draw(sheet)
  d.text((pad,12),'0.20 actual '+('PDF pages' if a.pdf_pages else 'Minecraft frames')+' / '+str(start+1)+'-'+str(start+len(batch))+' of '+str(len(records)),font=font,fill='#173f4b')
  sources=[]
  for n,(name,path) in enumerate(batch):
   x=pad+n%a.columns*(tilew+pad);y=57+n//a.columns*(tileh+pad)
   with Image.open(path) as im:
    im.load();im=im.convert('RGB');im.thumbnail((tilew,360),Image.Resampling.LANCZOS)
    sheet.paste(im,(x+(tilew-im.width)//2,y+(360-im.height)//2))
   d.text((x,y+370),name,font=small,fill='#173f4b')
   sources.append(dict(file=str(path),sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
  tag='pdf' if a.pdf_pages else '-'.join(a.category or ['all'])
  output=a.output/f'{tag}-{start//a.batch_size+1:02d}.png';sheet.save(output)
  manifest.append(dict(contact_sheet=str(output),sources=sources))
 (a.output/('pdf-review-inputs.json' if a.pdf_pages else '-'.join(a.category or ['all'])+'-review-inputs.json')).write_text(json.dumps(dict(available=len(records),missing=missing,generated_sheets=manifest),indent=2)+'\n')
 print(str(len(records))+' real files / '+str(len(manifest))+' review sheets / missing '+str(len(missing)))
 for m in manifest:print(m['contact_sheet'])
if __name__=='__main__':main()
