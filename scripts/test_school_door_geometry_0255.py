#!/usr/bin/env python3
"""Compile the real dependency-free native geometry and compare every owned slice."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
from validate_school_doors_0255 import ROOT,expected_boxes,require

def main():
 javac=shutil.which('javac')
 if not javac and os.environ.get('JAVA_HOME'):javac=str(Path(os.environ['JAVA_HOME'])/'bin/javac')
 if not javac:
  available=sorted(Path('/workspace/tools').glob('jdk-*/bin/javac'))
  if available:javac=str(available[-1])
 require(javac is not None,'A JDK is required to test actual SchoolDoorGeometry0255')
 java=str(Path(javac).with_name('java'))
 runner='''import com.chaogarden.core.SchoolDoorGeometry0255;
 public class DoorProbe {
  public static void main(String[] args) {
   for(boolean left:new boolean[]{true,false})for(boolean open:new boolean[]{false,true})for(int section=0;section<4;section++)for(int segment=0;segment<3;segment++) {
    StringBuilder s=new StringBuilder("[");
    for(var b:SchoolDoorGeometry0255.boxes(left,open,section,segment)) {
     if(s.length()>1)s.append(",");
     s.append("[["+b.x0()+","+b.y0()+","+b.z0()+"],["+b.x1()+","+b.y1()+","+b.z1()+"],"+b.left()+"]");
    }
    System.out.println(s+"]");
   }
  }
 }'''
 with tempfile.TemporaryDirectory(prefix='chao-door0255-core-') as temp:
  directory=Path(temp);probe=directory/'DoorProbe.java';probe.write_text(runner)
  subprocess.run([javac,'-d',temp,str(ROOT/'src/main/java/com/chaogarden/core/SchoolDoorGeometry0255.java'),str(probe)],check=True,capture_output=True,text=True)
  rows=subprocess.run([java,'-cp',temp,'DoorProbe'],check=True,capture_output=True,text=True).stdout.splitlines()
 expected=[expected_boxes(left,opened,section,part) for left in (True,False) for opened in (False,True) for section in ('leaf','center','tip','far_tip') for part in range(3)]
 require(len(rows)==len(expected)==48,'Native core omitted state combinations')
 for index,(row,wanted) in enumerate(zip(rows,expected)):
  require(json.loads(row)==json.loads(json.dumps(wanted)),f'Compiled actual geometry differs from rendered cut-leaf outline at state{index}')
 print('School doors0.25.5 compiled native core: all48 closed/open/hinged/section/height shapes match the independently derived physical silhouette. PASS')

if __name__=='__main__':main()
