package com.chaogarden.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.WrittenBookContent;

/** Searchable chapters contain the journal's individual care sections. */
public final class AdventureGuideScreen extends AdventureScreen {
  private final List<Component> pages;
  private final Map<String,List<Integer>> chapters=new LinkedHashMap<>();
  private record Row(String title,String chapter,int page) {}
  private final List<Row> rows=new ArrayList<>();
  private int page,offset,scroll;
  private String chapter="",query="";
  private EditBox search;
  private static final int PAGE_SIZE=12;
  public AdventureGuideScreen(WrittenBookContent content){
    super(Component.literal("A Keeper's Journal"));pages=content.pages().stream().map(p->p.get(false)).toList();
    for(String c:List.of("Getting started","Chao care","Growth & family","Food & trees","Kindergarten","Toys & activities","Wardrobe","Gardens & travel","Black Market","Companions","Omochao","Settings & tools"))chapters.put(c,new ArrayList<>());
    for(int i=0;i<pages.size();i++)chapters.get(category(title(i))).add(i);
    chapters.values().removeIf(List::isEmpty);
  }
  private String title(int i){return pages.get(i).getString().split("\\R",2)[0];}
  private static String category(String title){
    return switch(title){
      case "A Keeper's Journal","First care","Handling eggs" -> "Getting started";
      case "Care supplies","The Analyzer","Choosing a gift","Curious hands","Personalities","Minecraft adaptations" -> "Chao care";
      case "Training","Learning to move","Learning to swim","Learning to fly","Growth and alignment","Alignment","Evolution","Adult forms","Breeding","Inheritance","Animal Genes","Reincarnation","Chaos Chao" -> "Growth & family";
      case "Fruit orchards","Learning gardening","Watering trees","Planted-tree life","Portable adventure fruit" -> "Food & trees";
      case "Meet the staff","Doctor care","Grade care","Classroom","Learned skills" -> "Kindergarten";
      case "Building a race","Races and karate","Garden friends","Contest awards","Birthday parties" -> "Toys & activities";
      case "Wardrobe" -> "Wardrobe";
      case "Your gardens","Garden registration","Garden boundaries","Safe transport","Kindergarten travel" -> "Gardens & travel";
      case "Black Market","Special eggs","Rare egg discovery","Gem eggs","Other market gifts","Expanded garden gifts" -> "Black Market";
      case "Chao companions","Companion flight and water","Nether companions","End companions","Companion recovery" -> "Companions";
      case "Robot legacy","A robot's next life" -> "Omochao";
      default -> "Settings & tools";
    };
  }
  private void list(){
    rows.clear();String q=query.strip().toLowerCase(Locale.ROOT);
    if(!q.isEmpty()){
      chapters.forEach((c,sections)->{
        boolean match=c.toLowerCase(Locale.ROOT).contains(q);
        if(match)rows.add(new Row(c,c,-1));
        for(int n:sections)if(match||title(n).toLowerCase(Locale.ROOT).contains(q))rows.add(new Row(title(n),c,n));
      });
    }else if(chapter.isEmpty())chapters.forEach((c,v)->rows.add(new Row(c,c,-1)));
    else for(int n:chapters.getOrDefault(chapter,List.of()))rows.add(new Row(title(n),chapter,n));
    offset=Math.max(0,Math.min(offset,Math.max(0,(rows.size()-1)/PAGE_SIZE*PAGE_SIZE)));
  }
  @Override protected void initAdventure(){
    list();search=addRenderableWidget(new EditBox(font,25,59,154,18,Component.literal("Search chapters and sections")));
    search.setMaxLength(60);search.setHint(Component.literal("Search journal"));search.setValue(query);
    search.setResponder(value->{if(value.equals(query))return;query=value;offset=0;refresh(true);});
    for(int i=offset;i<Math.min(rows.size(),offset+PAGE_SIZE);i++){
      Row row=rows.get(i);String label=(row.page<0?"> ":"")+row.title;
      addRenderableWidget(new AdventureButton(25,102+(i-offset)*15,154,14,label,0xffb7edf3,()->row.page==page,
        ()->{if(row.page<0){chapter=row.chapter;query="";offset=0;refresh(false);}else setPage(row.page);},true));
    }
    var back=addRenderableWidget(new AdventureButton(25,288,40,14,"<",0xff80deeb,()->false,()->{offset-=PAGE_SIZE;refresh(false);},true));back.active=offset>0;
    var more=addRenderableWidget(new AdventureButton(139,288,40,14,">",0xff80deeb,()->false,()->{offset+=PAGE_SIZE;refresh(false);},true));more.active=offset+PAGE_SIZE<rows.size();
    if(!chapter.isEmpty()||!query.isEmpty())addRenderableWidget(new AdventureButton(69,288,66,14,"Chapters",0xff80deeb,()->false,()->{chapter="";query="";offset=0;refresh(false);},true));
    var previous=addRenderableWidget(new AdventureButton(203,324,110,24,"< PREVIOUS",0xff80deeb,()->false,()->setPage(page-1)));previous.active=page>0;
    var next=addRenderableWidget(new AdventureButton(321,324,110,24,"NEXT >",0xff80deeb,()->false,()->setPage(page+1)));next.active=page+1<pages.size();
    addRenderableWidget(new AdventureButton(494,324,90,24,"CLOSE",0xffffc655,()->false,this::onClose));
  }
  private void refresh(boolean editing){int cursor=search==null?query.length():search.getCursorPosition();clearWidgets();initAdventure();if(editing){setFocused(search);search.setFocused(true);search.setCursorPosition(Math.min(cursor,query.length()));}}
  public void setPage(int value){page=Math.max(0,Math.min(pages.size()-1,value));scroll=0;if(query.isEmpty()){chapter=category(title(page));offset=Math.max(0,chapters.get(chapter).indexOf(page))/PAGE_SIZE*PAGE_SIZE;}refresh(false);}
  public int getPageCount(){return pages.size();}
  @Override public boolean keyPressed(int key,int scan,int modifiers){
    if(search!=null&&search.isFocused())return super.keyPressed(key,scan,modifiers);
    if(key==262||key==267){setPage(page+1);return true;}if(key==263||key==266){setPage(page-1);return true;}return super.keyPressed(key,scan,modifiers);
  }
  private List<net.minecraft.util.FormattedCharSequence> lines(){String[] parts=pages.get(page).getString().split("\\R",2);return font.split(Component.literal(parts.length>1?parts[1].strip():""),332);}
  @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
    if(localX(x)>=203){scroll=Math.max(0,Math.min(Math.max(0,lines().size()-12),scroll+(vertical<0?2:-2)));return true;}
    if(localY(y)>=100&&localY(y)<=285){offset+=vertical<0?PAGE_SIZE:-PAGE_SIZE;refresh(false);return true;}return false;
  }
  @Override public void render(GuiGraphics g,int mx,int my,float partial){
    begin(g,mx,my,partial,"guide");logo(g,"KEEPER'S JOURNAL");text(g,"A KEEPER'S JOURNAL",204,16,1.24f,0xff123d6b);
    text(g,"Small companions. A lifetime of adventures.",204,37,.79f,0xff234f79);
    right(g,String.format("%02d / %02d",page+1,pages.size()),574,18,0xffe4fcff);
    centered(g,!query.isEmpty()?"SEARCH RESULTS":chapter.isEmpty()?"CHAPTER SELECT":"SECTION SELECT",103,84,INK);
    round(g,25,99,156,204,8,0xfff0fbff);
    if(rows.isEmpty())text(g,"No matching sections",29,110,MUTED);
    text(g,category(title(page)).toUpperCase(Locale.ROOT),223,86,.8f,0xff78511d);
    text(g,title(page),222,116,Math.min(1.12f,332f/Math.max(1,font.width(title(page)))),INK);
    var body=lines();for(int i=scroll;i<Math.min(body.size(),scroll+12);i++)g.drawString(font,body.get(i),222,144+(i-scroll)*11,INK,false);
    if(body.size()>12)text(g,"Scroll to read • "+(scroll+1)+"–"+Math.min(body.size(),scroll+12)+" / "+body.size(),222,290,.8f,MUTED);
    end(g,mx,my,partial);
  }
}
