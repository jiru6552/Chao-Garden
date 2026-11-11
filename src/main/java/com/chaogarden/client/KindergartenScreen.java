package com.chaogarden.client;

import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.core.ChaoPersonality;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenNetwork;
import com.chaogarden.kindergarten.KindergartenNpc;
import com.chaogarden.kindergarten.PrincipalTopics025;
import com.chaogarden.market.BlackMarketTrader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** In-scene Kindergarten conversations. Rooms remain visible behind every service overlay. */
public final class KindergartenScreen extends AdventureScreen {
  private KindergartenNetwork.Open snapshot;
  private ChaoState state;
  private int page,chart,ticks,topicPage;
  private String mode="home",boardPage="schedule";
  private boolean choosing;
  private EditBox name;
  private List<String[]> choices=List.of();
  private KindergartenScreen(KindergartenNetwork.Open value){super(Component.literal("Chao Kindergarten"));update(value);}
  public static void receive(KindergartenNetwork.Open value){
    var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||!KindergartenContent.inKindergarten(mc.level))return;
    var actor=mc.level.getEntity(value.npc());
    if(!(actor instanceof KindergartenNpc)&&!(value.role().equals("MARKET")&&actor instanceof BlackMarketTrader))return;
    if(actor==null||!actor.isAlive()||mc.player.distanceToSqr(actor)>64)return;
    if(mc.screen instanceof KindergartenScreen screen&&screen.snapshot.nonce().equals(value.nonce())){screen.update(value);screen.rebuildWidgets();}
    else mc.setScreen(new KindergartenScreen(value));
  }
  private void update(KindergartenNetwork.Open value){
    snapshot=value;state=value.state().isEmpty()?null:ChaoState.decode(value.state());
    var list=new ArrayList<String[]>();for(String line:value.options().split("\n")){int at=line.indexOf('|');if(at>0)list.add(new String[]{line.substring(0,at),line.substring(at+1)});}choices=list;
    page=Math.max(0,Math.min(page,Math.max(0,(choices.size()-1)/5)));
    if(value.role().equals("MARKET"))mode=value.proposal().split("\\|",-1)[0];
    if(value.role().equals("PRINCIPAL")&&!value.proposal().equals("principal|home"))mode="article";
  }
  public String serviceRole(){return snapshot.role();}
  public String selectedKey(){return snapshot.selected();}
  public int chartPage(){return chart;}
  public String viewMode(){return mode;}
  private void action(String action,String value){KindergartenNetwork.request(snapshot,snapshot.selected(),action,value);}
  private void view(String value){mode=value;choosing=false;rebuildWidgets();}
  private int accent(){return switch(snapshot.role()){case "TEACHER"->0xffd51d21;case "DOCTOR"->0xff3559bd;case "FORTUNE"->0xffad2acb;case "PRINCIPAL"->0xff169848;case "MARKET"->0xff9a3045;default->0xff4e9e8d;};}
  private SceneButton button(int x,int y,int w,int h,String label,Runnable action){var b=new SceneButton(x,y,w,h,label,()->false,action);addRenderableWidget(b);return b;}
  private SceneButton menu(int row,String label,Runnable action){return button(400,83+row*28,180,24,label,action);}
  private void selector(){
    if(state!=null&& !snapshot.role().equals("BOARD"))button(16,44,230,22,snapshot.name()+"   ▾",()->{choosing=!choosing;rebuildWidgets();});
    else if(!choices.isEmpty())button(16,44,230,22,"CHOOSE CHAO",()->{choosing=!choosing;rebuildWidgets();});
    if(!choosing)return;
    for(int row=0;row<5;row++){int index=page*5+row;if(index>=choices.size())break;var choice=choices.get(index);
      button(16,73+row*25,230,22,choice[1],()->{choosing=false;KindergartenNetwork.request(snapshot,choice[0],"select","");rebuildWidgets();});}
    if(choices.size()>5){button(16,203,48,20,"<",()->{page=Math.max(0,page-1);rebuildWidgets();});button(198,203,48,20,">",()->{page=Math.min((choices.size()-1)/5,page+1);rebuildWidgets();});}
  }
  @Override protected void initAdventure(){
    if(!snapshot.role().equals("MARKET"))selector();
    switch(snapshot.role()){
      case "DOCTOR"->{
        if(mode.equals("chart")){
          String[] tabs={"STATS","PROFILE","RECORDS"};for(int i=0;i<3;i++){final int tab=i;button(324+i*86,63,83,20,tabs[i],()->{chart=tab;rebuildWidgets();});}
          if(chart==0)for(int i=0;i<5;i++){final int stat=i;var b=button(549,122+i*23,30,17,"UP",()->action("grade",Integer.toString(stat)));b.active=state!=null&&state.canUpgradeGrade(i);if(state!=null)b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Grade care: "+((state.gradeUpgradeCostRings(i)+99)/100)+" emeralds; resets this ability's training.")));}
          button(400,263,180,24,"BACK",()->view("home"));
        }else{menu(0,"SEE THE DOCTOR",()->action("cure","" )).active=state!=null;menu(1,"MEDICAL CHART",()->view("chart"));menu(2,"CANCEL",this::onClose);}
      }
      case "TEACHER"->{
        if(mode.equals("enroll")){menu(4,"YES",()->{action("leave","");view("home");});menu(5,"NO",()->view("home"));}
        else{
          menu(4,"LEAVE CHAO",()->view("enroll")).active=state!=null&&!snapshot.selected().startsWith("c:")&&!snapshot.message().contains("Classroom: 4 / 4");
          menu(5,"TAKE CHAO",()->action("take","")).active=state!=null&&snapshot.selected().startsWith("c:")&&state.traitInt("lesson.remaining",0)==0;
          menu(6,"EXIT",this::onClose);
        }
      }
      case "FORTUNE"->{
        if(mode.equals("custom")){
          name=new EditBox(font,311,101,263,24,Component.literal("Name your Chao"));name.setMaxLength(48);name.setValue(snapshot.name().equals("No Chao selected")?"":snapshot.name());addRenderableWidget(name);setInitialFocus(name);
          button(311,139,128,24,"OK",()->{action("name",name.getValue());view("home");}).active=state!=null;button(446,139,128,24,"CANCEL",()->view("home"));
        }else if(mode.equals("reroll")){menu(0,"YES",()->{action("reroll","");view("home");});menu(1,"NO",()->view("home"));}
        else{menu(0,"ACCEPT NAME",()->action("accept","")).active=state!=null;menu(1,"ANOTHER NAME",()->view("reroll"));menu(2,"CUSTOM NAME",()->view("custom"));menu(3,"EXIT",this::onClose);}
      }
      case "PRINCIPAL"->{
        if(mode.equals("topics")){
          for(int row=0;row<6;row++){int index=topicPage*6+row;if(index>=PrincipalTopics025.TOPICS.size())break;var topic=PrincipalTopics025.TOPICS.get(index);menu(row,topic.title(),()->action("topic",topic.id()));}
          button(400,258,56,23,"<",()->{topicPage=Math.max(0,topicPage-1);rebuildWidgets();});button(462,258,56,23,">",()->{topicPage=Math.min((PrincipalTopics025.TOPICS.size()-1)/6,topicPage+1);rebuildWidgets();});button(524,258,56,23,"EXIT",this::onClose);
        }else if(mode.equals("article")){menu(4,"PREVIOUS",()->action("tip_prev",""));menu(5,"NEXT",()->action("tip_next",""));menu(6,"TOPICS",()->{action("tip_home","");view("topics");});}
        else{menu(0,"TIPS",()->view("topics"));menu(1,"EXIT",this::onClose);}
      }
      case "BOARD"->{menu(0,"SCHEDULE",()->{boardPage="schedule";rebuildWidgets();});menu(1,"CREDITS",()->{boardPage="credits";rebuildWidgets();});menu(2,"ABOUT CWE",()->{boardPage="cwe";rebuildWidgets();});menu(3,"EXIT",this::onClose);}
      case "MARKET"->{
        if(mode.equals("home")){menu(0,"SHOPPING",()->{action("market_shop","");minecraft.setScreen(null);});menu(1,"SELL ITEMS",()->action("market_sell",""));menu(2,"EXIT",this::onClose);}
        else if(mode.equals("quote")){menu(2,"CONFIRM SALE",()->{String[] parts=snapshot.proposal().split("\\|",-1);action("market_confirm",parts.length>1?parts[1]:"");});menu(3,"BACK",()->action("market_sell",""));}
        else{
          for(int row=0;row<5;row++){int index=page*5+row;if(index>=choices.size())break;var choice=choices.get(index);menu(row,choice[1],()->KindergartenNetwork.request(snapshot,choice[0],"select",""));}
          if(choices.size()>5){button(400,229,80,22,"<",()->{page=Math.max(0,page-1);rebuildWidgets();});button(500,229,80,22,">",()->{page=Math.min((choices.size()-1)/5,page+1);rebuildWidgets();});}
          menu(6,"BACK",()->action("market_back",""));
        }
      }
      default->menu(0,"EXIT",this::onClose);
    }
  }
  private void paragraph(GuiGraphics g,String value,int x,int y,int width,int maxLines,int color){int line=0;for(var text:font.split(Component.literal(value),width)){if(line>=maxLines)break;g.drawString(font,text,x,y+line++*12,color,false);}}
  private void panel(GuiGraphics g,int x,int y,int w,int h,int fill){round(g,x+2,y+3,w,h,5,0x66302025);round(g,x,y,w,h,5,fill);g.fill(x+6,y+4,x+w-6,y+5,0x99ffffff);}
  private void chart(GuiGraphics g){
    panel(g,315,52,272,242,0xf01665a2);g.fill(321,88,581,115,0xfff2d953);text(g,font.plainSubstrByWidth(snapshot.name(),246),328,98,0xff173d6c);
    if(state==null){paragraph(g,"Bring a Chao to see its medical chart.",329,131,244,6,0xffffffff);return;}
    if(chart==0){
      String[] labels={"SWIM","FLY","RUN","POWER","STAMINA"};for(int i=0;i<5;i++){int y=127+i*23;text(g,labels[i],327,y,.9f,0xffffffff);text(g,ChaoState.gradeName(state.grades[i]),388,y,0xffffe85e);text(g,"Lv "+state.levels[i],412,y,0xffffffff);right(g,Integer.toString(state.points[i]),539,y,0xffffee6e);g.fill(327,y+15,539,y+17,0xff427cae);}
      paragraph(g,state.alignmentName()+" / "+state.typeName(),328,245,244,1,0xffdcf2ff);
    }else if(chart==1){
      var personalities=new ArrayList<String>();for(int p:ChaoPersonality.profile(state))personalities.add(ChaoPreferences.PERSONALITIES[Math.max(0,Math.min(ChaoPreferences.PERSONALITIES.length-1,p))]);
      paragraph(g,"Personality: "+String.join(", ",personalities)+"\nFavorite shape: "+new String[]{"Round","Square","Triangle"}[Math.max(0,Math.min(2,state.favoriteFruit))]+"\nAge: "+((long)state.ageTicks+state.adultAgeTicks)/1200+" minutes\nReincarnations: "+state.reincarnations+"\nHealth: "+state.illnessName()+"\nHappiness: "+state.happiness+"\nFullness: "+state.hunger+" / 100\nEnergy: "+state.energy+" / 100",328,126,246,11,0xffffffff);
    }else{
      StringBuilder records=new StringBuilder("Earned medals\n");boolean found=false;for(String id:ChaoCareServices.MEDALS)if(state.traitInt("medal."+id,0)>0){records.append(id.replace('_',' ')).append('\n');found=true;}if(!found)records.append("No medals recorded yet.\n");records.append("\nKarate tournament wins: ").append(state.traitInt("contest.karate.wins",0));paragraph(g,records.toString(),328,126,246,10,0xffffffff);
    }
  }
  private String title(){return switch(snapshot.role()){case "TEACHER"->"CLASSROOM";case "DOCTOR"->"HEALTH CENTER";case "FORTUNE"->"FORTUNE-TELLING HOUSE";case "PRINCIPAL"->"PRINCIPAL'S OFFICE";case "MARKET"->"BLACK MARKET";default->"BULLETIN BOARD";};}
  private String dialogue(){
    if(snapshot.role().equals("FORTUNE")&&mode.equals("custom"))return "Choose a name for your Chao. You can type letters, numbers and spaces.";
    if(snapshot.role().equals("FORTUNE")&&mode.equals("reroll"))return "Shall I choose another lucky name for your Chao?";
    if(snapshot.role().equals("TEACHER")){
      if(mode.equals("enroll"))return "This class takes ten minutes at normal server speed. You cannot take your Chao home until the lesson finishes. Would you like to leave it here?";
      if(snapshot.message().startsWith("Your Chao")||snapshot.message().startsWith("That service"))return snapshot.message().split("\n\n",2)[0];
      return "Welcome! We can teach four Chao together. Choose Leave Chao to enrol your friend. Once class starts, please wait until the lesson is over.";
    }
    if(snapshot.role().equals("PRINCIPAL")&&mode.equals("article"))return "I hope this helps you enjoy raising your Chao. Choose Next to continue, or Topics to ask about something else.";
    if(snapshot.role().equals("BOARD"))return "Welcome to Chao Kindergarten. Select a notice to read it.";
    return snapshot.message();
  }
  @Override public void render(GuiGraphics g,int mx,int my,float partial){
    g.pose().pushPose();g.pose().translate(offsetX,offsetY,0);g.pose().scale(canvasScale,canvasScale,1);
    panel(g,12,12,576,25,accent());round(g,24,20,15,14,7,0xff52eced);g.fill(28,17,35,22,0xff52eced);round(g,30,13,4,4,2,0xff74ffff);text(g,title(),49,21,1.15f,0xffffffff);right(g,"CHAO KINDERGARTEN",575,23,0xfff8ead8);
    if(choosing)panel(g,12,69,238,159,0xde383750);
    if(snapshot.role().equals("DOCTOR")&&mode.equals("chart"))chart(g);
    if(snapshot.role().equals("FORTUNE")){
      if(mode.equals("custom")){panel(g,303,75,279,102,0xf4eab3d3);for(int y=0;y<8;y++)for(int x=0;x<24;x++)if((x+y)%2==0)g.fill(307+x*11,79+y*11,318+x*11,90+y*11,0x22ffffff);text(g,"NAME YOUR CHAO",314,85,0xff633e73);}
      else{panel(g,400,49,180,25,0xdd754880);centered(g,snapshot.proposal(),490,58,0xffffe9a1);}
    }
    if(snapshot.role().equals("TEACHER")){
      panel(g,365,76,223,116,0xecb13f34);text(g,choices.stream().anyMatch(c->c[0].startsWith("c:"))?"LESSON IN PROGRESS":"TODAY'S LESSON",377,87,0xffffedcf);
      String[] lines=snapshot.message().split("\n");int y=104;for(String line:lines)if(line.startsWith("Current class:")||line.startsWith("Next offer")||line.startsWith("Classroom:")){text(g,line,377,y,.9f,0xffffffff);y+=14;}
      int n=0;for(var choice:choices)if(choice[0].startsWith("c:")){if(n++>=4)break;String label=choice[1];text(g,font.plainSubstrByWidth(label,198),377,y,.9f,0xffc8ffbb);y+=13;}
    }
    if(snapshot.role().equals("PRINCIPAL")&&mode.equals("article")){
      panel(g,275,76,313,110,0xeaf6e5ba);var parts=snapshot.proposal().split("\\|");int index=parts.length>1?PrincipalTopics025.index(parts[1]):-1;String heading=index<0?"Chao Care":PrincipalTopics025.TOPICS.get(index).title();text(g,heading,288,87,0xff6c4824);paragraph(g,snapshot.message(),288,105,286,6,0xff513e27);
    }
    if(snapshot.role().equals("BOARD")){
      panel(g,22,84,352,180,0xeaf8edcf);String value=boardPage.equals("credits")?"CHAO KINDERGARTEN\n\nSonic Adventure 2 and its Chao world were created by SEGA and Sonic Team.\n\nThis Minecraft adaptation uses the owner's supplied gameplay videos as its room and interaction reference. Chao Island documents the original systems.":boardPage.equals("cwe")?"CHAO WORLD EXTENDED\n\nChao World Extended is a community expansion of the Sonic Adventure 2 Chao system. The supplied video shows its naming, school and market presentation.\n\nThis mod adapts those ideas for a persistent Minecraft garden; its recipes, emerald economy and companion abilities follow this mod's own rules.":snapshot.message();paragraph(g,value,36,99,323,12,0xff4f5949);
    }
    if(snapshot.role().equals("MARKET")&&mode.equals("quote")){panel(g,400,76,180,53,0xe6522929);paragraph(g,"One quarter of catalog value\nWhole inventory lot",410,88,159,3,0xffffe4c6);}
    panel(g,12,299,576,49,0xd9171c24);paragraph(g,dialogue().replace('\n',' '),25,309,551,3,0xffffffff);
    end(g,mx,my,partial);
  }
  @Override public void tick(){
    if(minecraft==null||minecraft.level==null||minecraft.player==null||!minecraft.player.isAlive()||!KindergartenContent.inKindergarten(minecraft.level)){onClose();return;}
    var actor=minecraft.level.getEntity(snapshot.npc());if(actor==null||!actor.isAlive()||minecraft.player.distanceToSqr(actor)>64){onClose();return;}
    if(snapshot.role().equals("TEACHER")&&++ticks%20==0)action("refresh","");
  }
  @Override public void onClose(){if(minecraft!=null&&minecraft.player!=null&&minecraft.getConnection()!=null)action("exit","");super.onClose();}
  private final class SceneButton extends AbstractButton {
    final Runnable action;final BooleanSupplier selected;
    SceneButton(int x,int y,int w,int h,String value,BooleanSupplier selected,Runnable action){super(x,y,w,h,Component.literal(value));this.selected=selected;this.action=action;}
    @Override public void onPress(){action.run();}
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){int color=active?(isHoveredOrFocused()||selected.getAsBoolean()?0xfff8e797:accent()):0xb84d525b;round(g,getX()+2,getY()+2,width,height,4,0x66201819);round(g,getX(),getY(),width,height,4,color);String value=font.plainSubstrByWidth(getMessage().getString(),width-12);g.drawString(font,value,getX()+(width-font.width(value))/2,getY()+(height-8)/2,active?(isHoveredOrFocused()?0xff4c4331:0xffffffff):0xffa7a6a0,false);if(isFocused())g.fill(getX()+6,getY()+height-2,getX()+width-6,getY()+height-1,0xffffffff);}
    @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
  }
}
