#!/usr/bin/env python3
"""Build the eight-page 0.15 release preview from selected whole game frames.

Requires current package_chao015.py --previews-only output, Pillow, ReportLab and
pypdf. Render every page and inspect it visually before publishing this PDF.
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

ROOT = Path(__file__).resolve().parents[1]
VERSION = '0.15.0'
ORDER = ('main-forms','magic-themes','genes-chaos','transporter-trader','eggs-rear','natural-orchard','fruit-catalog')
INK,MUTED,PAPER,EDGE = map(HexColor,('#173f4b','#47636e','#edf3f5','#afc8cf'))


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


def header(pdf,width,height,title,subtitle,page):
    pdf.setPageSize((width,height));pdf.setFillColor(PAPER);pdf.rect(0,0,width,height,fill=1,stroke=0)
    pdf.setFillColor(INK);size=22
    while pdfmetrics.stringWidth(clean(title),'GalleryBold',size)>width-48:size-=.5
    assert size>=16,title
    pdf.setFont('GalleryBold',size);pdf.drawString(24,height-31,clean(title))
    pdf.setFillColor(MUTED);pdf.setFont('Gallery',10)
    assert pdfmetrics.stringWidth(clean(subtitle),'Gallery',10)<=width-48,subtitle
    pdf.drawString(24,height-49,clean(subtitle))
    pdf.setFont('Gallery',8);pdf.drawString(24,15,f'CHAO GARDEN {VERSION}  |  Minecraft Java 1.21.1 / NeoForge')
    pdf.drawRightString(width-24,15,f'{page:02d} / 08')


def caption(pdf,text,width=794,x=24,y=57,size=9.5,max_lines=3):
    pdf.setFillColor(MUTED);pdf.setFont('Gallery',size)
    lines=wrap(text,width,size);assert len(lines)<=max_lines,lines
    for row,line in enumerate(lines):pdf.drawString(x,y-row*13,line)


def whole_frame(pdf,path,x,y,width):
    with Image.open(path) as picture:
        assert picture.size==(1280,720),picture.size
        picture.verify()
    height=width*720/1280
    pdf.setStrokeColor(EDGE);pdf.setLineWidth(.6)
    pdf.rect(x-.3,y-.3,width+.6,height+.6,fill=0,stroke=1)
    pdf.drawImage(ImageReader(str(path)),x,y,width=width,height=height,mask='auto')
    return height


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,default=ROOT.parent/'output/pdf/chao-garden-0.15.0-preview-gallery.pdf')
    args=parser.parse_args()
    spec=importlib.util.spec_from_file_location('chao_package_015',ROOT/'scripts/package_chao015.py')
    art=importlib.util.module_from_spec(spec);spec.loader.exec_module(art)
    manifest=json.loads((ROOT/'docs/release-capture-manifest-0.15.json').read_text())
    assert manifest['release']==VERSION and manifest['capture_count']==23
    expected={item['file']:item['sha256'] for item in manifest['captures']}
    shots=ROOT/'docs/screenshots/release-0.15'
    for key in (*ORDER,'held-chao','music-disc'):
        path=shots/f'chao015-{key}.png'
        assert art.sha256(path)==expected[path.name],'Frame differs from current capture manifest: '+key
    for name,path in (('Gallery','/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),('GalleryBold','/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')):
        pdfmetrics.registerFont(TTFont(name,path))
    buffer=io.BytesIO();pdf=canvas.Canvas(buffer,pagesize=(842,595),pageCompression=1)
    pdf.setTitle('Chao Garden 0.15.0 - Official Forms and Garden Refinements')
    pdf.setAuthor('Chao Garden')
    pdf.setSubject('Selected actual Minecraft release views: forms, themes, genes, eggs, fruit, transporter, held Chao and CD')
    for page,key in enumerate(ORDER,1):
        title,subtitle,text=art.SHOTS[key]
        if key=='magic-themes':
            text+=' Optional themes are enabled with development identity fixtures; this does not test complete third-party mods.'
        header(pdf,842,595,f'{page:02d}  {title}',subtitle,page)
        whole_frame(pdf,shots/f'chao015-{key}.png',24,77,794)
        caption(pdf,text)
        pdf.bookmarkPage(key);pdf.addOutlineEntry(clean(title),key,level=0,closed=False);pdf.showPage()
    header(pdf,595,842,'08  Held Chao and garden music','Animated carried Chao and the vanilla jukebox CD',8)
    # Both source frames remain complete, stacked on one portrait page.
    whole_frame(pdf,shots/'chao015-held-chao.png',24,440,547)
    caption(pdf,'Shift-right-click pickup creates the filled carrier directly. Holding it shows the saved Chao with animation.',width=547,y=426,size=9,max_lines=2)
    whole_frame(pdf,shots/'chao015-music-disc.png',24,99,547)
    caption(pdf,'The Chao Garden CD plays the Neutral Garden introduction and one complete loop: about 115.81 seconds.',width=547,y=85,size=9,max_lines=2)
    pdf.bookmarkPage('held-and-music');pdf.addOutlineEntry('Held Chao and garden music','held-and-music',level=0,closed=False);pdf.showPage()
    pdf.save();payload=buffer.getvalue();assert payload.startswith(b'%PDF-')
    reader=PdfReader(io.BytesIO(payload));assert len(reader.pages)==8 and len(reader.outline)==8
    assert all(page.extract_text().strip() for page in reader.pages)
    art.write_flushed(args.output,payload);art.write_flushed(ROOT/f'docs/chao-garden-{VERSION}-preview-gallery.pdf',payload)
    print(args.output,len(payload),'bytes; 8 pages; source copy written')


if __name__=='__main__':main()
