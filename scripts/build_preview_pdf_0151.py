#!/usr/bin/env python3
"""Build the three-page0.15.1 preview from whole actual Minecraft frames.

Run after package_chao0151.py --previews-only. Render and inspect every page
before publishing. The PDF operation marker is managed by the release owner.
"""
from pathlib import Path
import argparse, importlib.util, io, json
from PIL import Image
from pypdf import PdfReader
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

ROOT=Path(__file__).resolve().parents[1]
VERSION='0.15.1'
INK,MUTED,PAPER,EDGE=map(HexColor,('#173f4b','#47636e','#edf3f5','#afc8cf'))
PAGES=(
    ('dimension-eggs-front','Dimension eggs','Netherrack shell and the borderless live End portal',
     "The Nether egg has a finer netherrack pattern. The placed End egg uses Minecraft's actual animated End portal shader across its whole rounded shell. The ordinary egg remains alongside them for comparison."),
    ('portal-0','The End portal surface','A whole game frame from the captured motion study',
     'The old purple rim is gone. This static PDF shows one moment of the actual vanilla portal shader; the offline gallery includes an accelerated eight-frame review. Its matching inventory icon is a static portal motif.'),
    ('seed-catalog','Fruit-shaped seeds','Fourteen active fruit and seed pairs',
     'The first seven fruits occupy the top row with matching seeds directly below; the remaining seven fruits and seeds occupy the lower two rows. Seed silhouettes and color accents echo their fruit while keeping a dry seed-coat appearance.'),
)

def clean(value):
    for a,b in (('×','x'),('’',"'"),('‘',"'"),('—','-'),('–','-')):value=value.replace(a,b)
    return value

def wrap(value,width,size):
    lines=[];line=''
    for word in clean(value).split():
        candidate=(line+' '+word).strip()
        if line and pdfmetrics.stringWidth(candidate,'Gallery',size)>width:
            lines.append(line);line=word
        else:line=candidate
    if line:lines.append(line)
    return lines

def header(pdf,title,subtitle,page):
    width,height=842,595
    pdf.setFillColor(PAPER);pdf.rect(0,0,width,height,fill=1,stroke=0)
    pdf.setFillColor(INK);size=22
    while pdfmetrics.stringWidth(clean(title),'GalleryBold',size)>width-48:size-=.5
    assert size>=16,title
    pdf.setFont('GalleryBold',size);pdf.drawString(24,height-31,clean(title))
    pdf.setFillColor(MUTED);pdf.setFont('Gallery',10)
    assert pdfmetrics.stringWidth(clean(subtitle),'Gallery',10)<=width-48,subtitle
    pdf.drawString(24,height-49,clean(subtitle))
    pdf.setFont('Gallery',8);pdf.drawString(24,15,f'CHAO GARDEN {VERSION}  |  Minecraft Java 1.21.1 / NeoForge')
    pdf.drawRightString(width-24,15,f'{page:02d} / 03')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,default=ROOT.parent/'output/pdf/chao-garden-0.15.1-preview-gallery.pdf')
    args=parser.parse_args()
    spec=importlib.util.spec_from_file_location('chao_package_0151',ROOT/'scripts/package_chao0151.py')
    art=importlib.util.module_from_spec(spec);spec.loader.exec_module(art)
    manifest=json.loads((ROOT/'docs/release-capture-manifest-0.15.1.json').read_text())
    assert manifest['release']==VERSION and manifest['capture_count']==13
    expected={item['file']:item['sha256'] for item in manifest['captures']}
    shots=ROOT/'docs/screenshots/release-0.15.1'
    for key,*_ in PAGES:
        path=shots/f'chao0151-{key}.png'
        assert art.sha256(path)==expected[path.name], 'Frame differs from current manifest: '+key
    for name,path in (('Gallery','/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),('GalleryBold','/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')):
        pdfmetrics.registerFont(TTFont(name,path))
    buffer=io.BytesIO();pdf=canvas.Canvas(buffer,pagesize=(842,595),pageCompression=1)
    pdf.setTitle('Chao Garden0.15.1 - Dimension Eggs and Fruit-Shaped Seeds')
    pdf.setAuthor('Chao Garden');pdf.setSubject('Actual Minecraft views of dimension shells, the live End portal surface and redesigned seed icons')
    for number,(key,title,subtitle,text) in enumerate(PAGES,1):
        header(pdf,f'{number:02d}  {title}',subtitle,number)
        path=shots/f'chao0151-{key}.png'
        with Image.open(path) as picture:
            assert picture.size==(1280,720),picture.size
            picture.verify()
        width=794;height=width*720/1280;x,y=24,77
        pdf.setStrokeColor(EDGE);pdf.setLineWidth(.6);pdf.rect(x-.3,y-.3,width+.6,height+.6,fill=0,stroke=1)
        pdf.drawImage(ImageReader(str(path)),x,y,width=width,height=height,mask='auto')
        pdf.setFillColor(MUTED);pdf.setFont('Gallery',9.5)
        lines=wrap(text,794,9.5);assert len(lines)<=3,lines
        for row,line in enumerate(lines):pdf.drawString(24,57-row*13,line)
        pdf.bookmarkPage(key);pdf.addOutlineEntry(clean(title),key,level=0,closed=False);pdf.showPage()
    pdf.save();payload=buffer.getvalue();assert payload.startswith(b'%PDF-')
    reader=PdfReader(io.BytesIO(payload));assert len(reader.pages)==3 and len(reader.outline)==3
    assert all(page.extract_text().strip() for page in reader.pages)
    art.write_flushed(args.output,payload)
    art.write_flushed(ROOT/f'docs/chao-garden-{VERSION}-preview-gallery.pdf',payload)
    print(args.output,len(payload),'bytes;3 pages;source copy written')

if __name__=='__main__':main()
