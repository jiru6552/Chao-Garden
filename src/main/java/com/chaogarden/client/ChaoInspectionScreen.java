package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.core.NeutralMorphology056;
import com.chaogarden.core.HeroNormalMorphology057;
import com.chaogarden.core.NeutralSwimMorphology059;
import com.chaogarden.core.DarkNormalMorphology060;
import com.chaogarden.core.NeutralFlyMorphology061;
import com.chaogarden.core.NeutralRunMorphology062;
import com.chaogarden.core.NeutralPowerMorphology063;
import com.chaogarden.core.HeroSwimMorphology064;
import com.chaogarden.core.OmochaoRules;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoPersonality;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.companion.CompanionRules;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.care.ChaoCareNetwork;
import com.chaogarden.care.ChaoSchoolBlock;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeItem;
import java.util.*;
import com.chaogarden.entity.ChaoEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A live Adventure-inspired medical chart, reading the server's authoritative Chao. */
public final class ChaoInspectionScreen extends AdventureScreen {
  private static final int[] STAT_COLORS = {
    0xff23add3, 0xffad83df, 0xff75ba54, 0xffec8863, 0xffe5b536
  };
  private static final int[] PAGE_COLORS = {0xff6ad9f4, 0xffffd563, 0xffffafd6,0xffbcb1f4,0xff94e3b0};
  private static final String[] THEMES = {"status", "genetics", "animals","status","status"};
  private final ChaoEntity chao;
  private int page;
  private int liveTicks, mindTab, careTab, inventoryPage, animalTab, geneTab, dnaPage;
  private static final int DNA_PER_PAGE=21;
  private boolean missingOnly;
  private boolean alignmentDragging;
  private Integer alignmentDragValue,alignmentSentValue;
  private int alignmentSendTick=-100;
  private boolean maturityDragging,maturityDragAdult;
  private Integer maturityDragValue,maturitySentValue;
  private int maturitySendTick=-100;
  private String editorNotice="";
  private int editorNoticeUntil;
  private final long doctorVisit;
  private static final LinkedHashMap<UUID,Long> VISITS=new LinkedHashMap<>();
  private final List<AdventureButton> mindButtons=new ArrayList<>(),careButtons=new ArrayList<>(),geneButtons=new ArrayList<>();
  private final List<AdventureButton> upgradeButtons=new ArrayList<>(),removeButtons=new ArrayList<>(),dyeButtons=new ArrayList<>();
  private final List<AdventureItemButton> inventoryButtons=new ArrayList<>();
  private AdventureButton cureButton,previousItems,nextItems,medalButton,claimButton,animalButton,dnaFilterButton,chaosDnaButton,previousDna,nextDna;
  private boolean clinicNearby;
  private final Map<String,String> observedAncestorNames=new HashMap<>();

  public ChaoInspectionScreen(ChaoEntity chao) { this(chao,0); }
  public ChaoInspectionScreen(ChaoEntity chao,int initialPage) {
    super(Component.literal("Chao Analyzer"));this.chao=chao;page=Math.floorMod(initialPage,5);
    doctorVisit=VISITS.merge(chao.getUUID(),1L,Long::sum)-1;
    if(VISITS.size()>256)VISITS.remove(VISITS.keySet().iterator().next());
  }
  public void showAnimals(int value) {cancelEditorDrags();page=2;animalTab=Math.floorMod(value,2);dnaPage=0;}
  public void showGenes(int value) {cancelEditorDrags();page=1;geneTab=Math.floorMod(value,3);}
  public void showMind(int value) {cancelEditorDrags();page=3;mindTab=Math.floorMod(value,4);}
  public void showCare(int value) {cancelEditorDrags();page=4;careTab=Math.floorMod(value,3);}

  @Override
  protected void initAdventure() {
    mindButtons.clear();careButtons.clear();geneButtons.clear();upgradeButtons.clear();removeButtons.clear();dyeButtons.clear();inventoryButtons.clear();
    String[] names = {"STATS", "GENES", "PARTS", "MIND", "CARE"};
    for (int i = 0; i < 5; i++) {
      final int selected = i;
      addRenderableWidget(
          new AdventureButton(
              16 + i * 94,
              324,
              88,
              24,
              names[i],
              PAGE_COLORS[i],
              () -> page == selected,
              () -> showPage(selected)));
    }
    addRenderableWidget(
        new AdventureButton(494, 324, 90, 24, "CLOSE", 0xffffc655, () -> false, this::onClose));
    String[] mindNames={"PROFILE","FEELINGS","HEALTH","FAMILY"},careNames={"DOCTOR","LESSONS","WARDROBE"};
    for(int i=0;i<4;i++) {
      final int tab=i;var mind=new AdventureButton(218+i*87,84,82,18,mindNames[i],0xffc3b6ef,()->mindTab==tab,()->mindTab=tab);
      mindButtons.add(mind);addRenderableWidget(mind);
    }
    for(int i=0;i<3;i++) {
      final int tab=i;var care=new AdventureButton(218+i*116,84,110,18,careNames[i],0xff9fe3b8,()->careTab==tab,()->careTab=tab);
      careButtons.add(care);addRenderableWidget(care);
    }
    String[] geneNames={"APPEARANCE","SKILL GENES","GROWTH"};
    for(int i=0;i<3;i++){final int tab=i;var button=new AdventureButton(218+i*116,84,110,18,geneNames[i],0xffffd563,()->geneTab==tab,()->geneTab=tab);geneButtons.add(button);addRenderableWidget(button);}
    animalButton=addRenderableWidget(new AdventureButton(465,81,100,20,"BODY PARTS",0xffffafd6,()->false,()->animalTab=0));
    dnaFilterButton=addRenderableWidget(new AdventureButton(447,104,116,18,"MISSING ONLY",0xffffafd6,()->missingOnly,()->{missingOnly=!missingOnly;dnaPage=0;}));
    chaosDnaButton=addRenderableWidget(new AdventureButton(307,273,88,17,"ANIMAL DNA",0xffffafd6,()->false,()->{animalTab=1;missingOnly=false;dnaPage=0;}));
    previousDna=addRenderableWidget(new AdventureButton(220,263,54,17,"PREV",0xffffafd6,()->false,()->dnaPage=Math.max(0,dnaPage-1)));
    nextDna=addRenderableWidget(new AdventureButton(509,263,54,17,"NEXT",0xffffafd6,()->false,()->dnaPage++));
    cureButton=addRenderableWidget(new AdventureButton(220,195,166,20,"USE MEDICINE",0xffa4e3b8,()->false,()->sendCare("cure","")));
    claimButton=addRenderableWidget(new AdventureButton(405,166,158,18,"CLAIM INSTRUMENTS",0xffffd563,()->false,()->sendCare("claim","")));
    for(int i=0;i<5;i++) {
      final int stat=i;var upgrade=new AdventureButton(512,231+i*14,50,13,"RAISE",0xffffd563,()->false,()->sendCare("upgrade",Integer.toString(stat)));
      upgradeButtons.add(upgrade);addRenderableWidget(upgrade);
      var slot=ChaoCareServices.WARDROBE[i];
      var remove=new AdventureButton(465,112+i*28,59,18,"REMOVE",0xffefb6a1,()->false,()->sendCare("remove",slot.name()));
      var dye=new AdventureButton(530,112+i*28,34,18,"DYE",0xffdbc0ee,()->false,()->sendCare("dye",slot.name()));
      removeButtons.add(remove);dyeButtons.add(dye);addRenderableWidget(remove);addRenderableWidget(dye);
    }
    for(int i=0;i<8;i++) {final int choice=i;var button=new AdventureItemButton(222+i*40,276,
        ()->inventoryChoice(choice),()->{int slot=inventoryChoiceSlot(choice);if(slot>=0)sendCare("equip",Integer.toString(slot));});
      inventoryButtons.add(button);addRenderableWidget(button);}
    previousItems=addRenderableWidget(new AdventureButton(202,278,17,20,"<",0xffa4e3b8,()->false,()->inventoryPage=Math.max(0,inventoryPage-1)));
    nextItems=addRenderableWidget(new AdventureButton(548,278,17,20,">",0xffa4e3b8,()->false,()->inventoryPage++));
    medalButton=addRenderableWidget(new AdventureButton(28,253,150,20,"MEDAL",0xffffd563,()->false,this::nextMedal));
    updateCareButtons();
  }

  void showPage(int value) {
    cancelEditorDrags();
    editorNotice="";
    page = Math.floorMod(value, 5);
  }

  @Override
  public void tick() {
    liveTicks++;
    if(alignmentDragging) {
      if(!alignmentProfile()||!alignmentEditable())cancelAlignmentDrag();
      else sendAlignment(false);
    } else if(alignmentDragValue!=null&&(!alignmentProfile()||!alignmentEditable()
        ||AnalyzerAlignmentBar053.milli(chao.state().lockedAlignment())==alignmentDragValue
        ||liveTicks-alignmentSendTick>20))cancelAlignmentDrag();
    if(maturityDragging) {
      if(page!=0||!maturityEditable()||maturityDragAdult!=chao.state().adult)cancelMaturityDrag();
      else sendMaturity(false);
    } else if(maturityDragValue!=null&&(page!=0||!maturityEditable()||maturityDragAdult!=chao.state().adult
        ||AnalyzerMaturityBar056.milli(chao.state().evolutionStrength(),chao.state().adult)==maturityDragValue
        ||liveTicks-maturitySendTick>20))cancelMaturityDrag();
    if (chao.isRemoved()) onClose();
    if(liveTicks%20==1 && minecraft!=null && minecraft.player!=null) {
      clinicNearby=ChaoCareServices.nearbyStation(minecraft.player,chao,ChaoSchoolBlock.Kind.DOCTOR).isPresent();
      observedAncestorNames.clear();
      if(minecraft.level!=null)for(var relative:minecraft.level.getEntitiesOfClass(ChaoEntity.class,chao.getBoundingBox().inflate(128)))
        observedAncestorNames.put(relative.state().lineageId(),relative.getName().getString());
    }
    updateCareButtons();
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (key == 262) {
      showPage(page + 1);
      return true;
    }
    if (key == 263) {
      showPage(page - 1);
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
    begin(g, mouseX, mouseY, partial, THEMES[page]);
    ChaoState s = chao.state();
    logo(g, "CHAO ANALYZER");
    String name = font.plainSubstrByWidth(chao.getName().getString(), 246);
    text(g, name, 204, 15, 1.25f, 0xff123d6b);
    text(
        g,
        s.alignmentName() + "  /  " + s.maturityName() + "  /  " + s.typeName(),
        204,
        36,
        .82f,
        0xff234f79);
    right(g, String.format("FILE %02d", Math.floorMod(chao.getId(), 100)), 574, 18, 0xffd2faff);
    right(g, "LIVE  " + (page + 1) + " / 5", 574, 35, 0xffb6eeff);
    profile(g, s, localX(mouseX), localY(mouseY), partial);
    if (page == 0) status(g, s);
    else if (page == 1) genes(g, s);
    else if(page==2) animals(g,s);
    else if(page==3) mind(g,s);
    else care(g,s);
    if(liveTicks<editorNoticeUntil&&!editorNotice.isEmpty())text(g,editorNotice,220,313,.56f,0xff815289);
    updateCareButtons();
    end(g, mouseX, mouseY, partial);
    if (page == 0) movementTooltip(g, s, mouseX, mouseY);
    if(page==1&&geneTab==0)geneTooltip(g,s,mouseX,mouseY);
    if(page==3 && mindTab==3)familyTooltip(g,s,mouseX,mouseY);
    if(page==3 && mindTab==1)emotionTooltip(g,s,mouseX,mouseY);
    if(alignmentProfile())alignmentTooltip(g,s,mouseX,mouseY);
    if(page==4 && careTab==2)inventoryTooltip(g,mouseX,mouseY);
  }

  private void profile(GuiGraphics g, ChaoState s, int mx, int my, float partial) {
    centered(g, "CHAO PROFILE", 103, 82, 0xff123d6b);
    portrait(g, mx, my, partial);
    int alignment = s.align >= .5f ? 0xff53abc5 : s.align <= -.5f ? 0xff796bb6 : 0xff67b9b0;
    round(g, 28, 234, 150, 15, 6, alignment);
    centered(
        g,
        s.alignmentName() + "  " + s.maturityName(),
        103,
        238,
        s.align <= -.5f ? 0xffffffff : 0xff123d6b);
    String theme = com.chaogarden.ChaoVariants.activeId(s);
    String appearance = !s.immortal && !theme.equals("normal")
        ? com.chaogarden.ChaoVariants.name(theme) + " Chao" : s.colorName();
    var lines = font.split(Component.literal(appearance), 150);
    if(!(page==4 && careTab==2))for (int i = 0; i < Math.min(2, lines.size()); i++)
      g.drawString(font, lines.get(i), 28, 257 + i * 10, INK, false);
    String fruit = new String[] {"Round", "Square", "Triangle"}[s.favoriteFruit];
    text(g, "Favorite Fruit: " + fruit,28,278,.8f,MUTED);
    text(g, "Doctor: " + s.doctorPersonality(doctorVisit+liveTicks/100),28,289,.65f,INK);
    text(g, "Life " + (s.reincarnations + 1) + " / " + activeAge(s.ageTicks),28,300,.65f,INK);
    if (s.immortal) {
      symbol(g, 4, 158, 104, 16);
    } else if (s.shiny) {
      symbol(g, 4, 158, 104, 14);
    }
  }

  private void portrait(GuiGraphics g, int mx, int my, float partial) {
    g.enableScissor(screenX(26), screenY(100), screenX(180), screenY(229));
    float body=chao.yBodyRot,oldBody=chao.yBodyRotO,yaw=chao.getYRot(),oldYaw=chao.yRotO,
        pitch=chao.getXRot(),oldPitch=chao.xRotO,head=chao.yHeadRot,oldHead=chao.yHeadRotO;
    boolean previousNames=ChaoRenderer.suppressNames;
    try {
      ChaoRenderer.suppressNames=true;
      boolean hover=mx>=26&&mx<=180&&my>=100&&my<=229;
      float turn=hover?(float)Math.toDegrees(Math.atan((103-mx)/95f)):(float)Math.sin((liveTicks+partial)*.022f)*10f;
      float tilt=hover?(float)Math.toDegrees(Math.atan((my-155)/110f)):0;
      // LivingEntityRenderer interpolates these pairs and passes head minus body yaw to the rig.
      chao.yBodyRot=chao.yBodyRotO=180+turn*.4f;
      chao.setYRot(180+turn*.4f);chao.yRotO=chao.getYRot();
      chao.yHeadRot=chao.yHeadRotO=180+turn;
      chao.setXRot(tilt);chao.xRotO=tilt;
      Quaternionf view=new Quaternionf().rotateZ((float)Math.PI);
      InventoryScreen.renderEntityInInventory(g,103,218,chao.state().immortal?116:chao.state().adult||chao.isCocooned()?128:160,new Vector3f(),view,new Quaternionf(),chao);
    } finally {
      ChaoRenderer.suppressNames=previousNames;
      chao.yBodyRot=body;chao.yBodyRotO=oldBody;chao.setYRot(yaw);chao.yRotO=oldYaw;
      chao.setXRot(pitch);chao.xRotO=oldPitch;chao.yHeadRot=head;chao.yHeadRotO=oldHead;g.disableScissor();
    }
  }

  private static String activeAge(int ticks) {
    int mins = Math.max(0, ticks / 1200);
    return (mins / 60) + "h " + String.format("%02d", mins % 60) + "m active";
  }

  private void chartTitle(GuiGraphics g, String title, String detail) {
    text(g, title, 223, 86, 0xff78511d);
    right(g, detail, 560, 86, 0xff906126);
  }

  private boolean creativeEditor(){return minecraft!=null&&minecraft.player!=null&&minecraft.player.isCreative()&&!minecraft.player.isSpectator();}
  @Override public boolean mouseClicked(double x,double y,int button){
    if(button==0&&page==0&&AnalyzerMaturityBar056.hit(x,y,canvasScale,offsetX,offsetY)) {
      cancelEditorDrags();
      if(maturityEditable()) {
        maturityDragging=true;maturityDragAdult=chao.state().adult;
        maturityDragValue=AnalyzerMaturityBar056.value(x,canvasScale,offsetX,maturityDragAdult);
        sendMaturity(true);
      } else editorNotice("Maturity is read-only: "+editorReadOnlyReason(false));
      return true;
    }
    if(page==0&&button==0&&creativeEditor()){
      var edit=com.chaogarden.core.AnalyzerStatRules.clickScaled(x,y,hasShiftDown(),hasControlDown(),canvasScale,offsetX,offsetY);
      if(edit!=null){com.chaogarden.care.ChaoCareNetwork.requestStat(chao,edit);return true;}
    }
    if(button==0&&alignmentProfile()&&AnalyzerAlignmentBar053.hit(x,y,canvasScale,offsetX,offsetY)) {
      cancelEditorDrags();
      if(alignmentEditable()) {
        alignmentDragging=true;alignmentSentValue=null;
        alignmentDragValue=AnalyzerAlignmentBar053.value(x,canvasScale,offsetX);sendAlignment(true);
      } else editorNotice("Alignment is read-only: "+editorReadOnlyReason(true));
      return true;
    }
    cancelEditorDrags();
    return super.mouseClicked(x,y,button);
  }

  private boolean alignmentProfile(){return page==3&&mindTab==0;}
  private boolean alignmentEditable(){
    return !chao.state().adult&&maturityEditable();
  }
  private boolean maturityEditable(){
    var s=chao.state();
    if(!creativeEditor()||s.dead||s.immortal||OmochaoRules.isOmochao(s)||chao.isCocooned()||chao.isRemoved())return false;
    var player=minecraft.player;
    return player.getMainHandItem().is(ChaoGarden.item("chao_analyzer"))
        ||player.getOffhandItem().is(ChaoGarden.item("chao_analyzer"));
  }
  private void sendAlignment(boolean finalValue){
    if(!alignmentProfile()||!alignmentEditable()||alignmentDragValue==null)return;
    if(!finalValue&&(Objects.equals(alignmentSentValue,alignmentDragValue)||liveTicks-alignmentSendTick<2))return;
    // Only the preview thumb is local; care, identity and saved alignment await the server.
    ChaoCareNetwork.requestAlignment(chao,alignmentDragValue);
    alignmentSentValue=alignmentDragValue;alignmentSendTick=liveTicks;
  }
  private void cancelAlignmentDrag(){alignmentDragging=false;alignmentDragValue=null;alignmentSentValue=null;}
  private void cancelMaturityDrag(){maturityDragging=false;maturityDragValue=null;maturitySentValue=null;}
  private void cancelEditorDrags(){cancelAlignmentDrag();cancelMaturityDrag();}
  private void editorNotice(String message){editorNotice=message;editorNoticeUntil=liveTicks+100;}
  private String editorReadOnlyReason(boolean alignment){
    var s=chao.state();
    if(alignment&&s.adult)return "adult identity locked at first evolution.";
    if(!creativeEditor())return "Creative mode required.";
    if(OmochaoRules.isOmochao(s))return "Omochao has a fixed mechanical form.";
    if(s.dead||s.immortal||chao.isCocooned()||chao.isRemoved())return "unavailable during this life stage or activity.";
    return "hold a Chao Analyzer and stay nearby.";
  }
  private void sendMaturity(boolean finalValue){
    if(page!=0||!maturityEditable()||maturityDragValue==null)return;
    if(!finalValue&&(Objects.equals(maturitySentValue,maturityDragValue)||liveTicks-maturitySendTick<2))return;
    ChaoCareNetwork.requestMaturity(chao,maturityDragValue);
    maturitySentValue=maturityDragValue;maturitySendTick=liveTicks;
  }
  @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
    if(button==0&&maturityDragging){
      if(page!=0||!maturityEditable()||maturityDragAdult!=chao.state().adult){cancelMaturityDrag();return true;}
      var value=AnalyzerMaturityBar056.value(x,canvasScale,offsetX,maturityDragAdult);
      if(value!=null)maturityDragValue=value;
      sendMaturity(false);return true;
    }
    if(button==0&&alignmentDragging){
      if(!alignmentProfile()||!alignmentEditable()){cancelAlignmentDrag();return true;}
      var value=AnalyzerAlignmentBar053.value(x,canvasScale,offsetX);
      if(value!=null)alignmentDragValue=value;
      sendAlignment(false);return true;
    }
    return super.mouseDragged(x,y,button,dx,dy);
  }
  @Override public boolean mouseReleased(double x,double y,int button){
    if(button==0&&maturityDragging){
      if(page==0&&maturityEditable()&&maturityDragAdult==chao.state().adult){
        var value=AnalyzerMaturityBar056.value(x,canvasScale,offsetX,maturityDragAdult);
        if(value!=null)maturityDragValue=value;
        sendMaturity(true);
      }
      maturityDragging=false;return true;
    }
    if(button==0&&alignmentDragging){
      if(alignmentProfile()&&alignmentEditable()){
        var value=AnalyzerAlignmentBar053.value(x,canvasScale,offsetX);
        if(value!=null)alignmentDragValue=value;
        sendAlignment(true);
      }
      alignmentDragging=false;return true;
    }
    return super.mouseReleased(x,y,button);
  }
  @Override public void onClose(){cancelEditorDrags();super.onClose();}
  @Override public void removed(){cancelEditorDrags();super.removed();}

  private void status(GuiGraphics g, ChaoState s) {
    chartTitle(g, "ABILITY DATA", "LEVEL / POINTS / GRADE");
    for (int i = 0; i < 5; i++) {
      int y = 109 + i * 26;
      symbol(g, i, 216, y - 1, 19);
      text(g, ChaoState.STATS[i], 242, y, INK);
      text(g, "Lv. " + String.format("%02d", s.levels[i]), 322, y, INK);
      right(g, String.format("%04d", s.points[i]) + " pts", 484, y, MUTED);
      round(g, 549, y - 4, 19, 19, 9, gradeColor(s.grades[i]));
      centered(g, ChaoState.gradeName(s.grades[i]), 559, y + 1, s.grades[i]>=5?INK:0xffffffff);
      segmented(g, 242, y + 13, 257, 6, s.progress[i] / 100f, STAT_COLORS[i], 20);
      right(g, s.progress[i] + "/100", 539, y + 12, MUTED);
    }
    String[] labels = {"HAPPINESS", "BOND", "FULLNESS", "ENERGY"};
    int[] values = {s.happiness, s.bond, s.hunger, s.energy};
    int[] colors = {0xffec88a5, 0xff72bfa4, 0xffe2a645, 0xff57acd1};
    for (int i = 0; i < 4; i++) {
      int x = 220 + i * 87;
      text(g, labels[i], x, 255, .68f, MUTED);
      text(g, Integer.toString(values[i]), x, 267, .95f, INK);
      float value = (i == 0 ? (s.happiness + 100) / 200f : values[i] / 100f);
      segmented(g, x, 281, 76, 5, value, colors[i], 8);
    }
    int savedMaturity=AnalyzerMaturityBar056.milli(s.evolutionStrength(),s.adult);
    int shownMaturity=maturityDragValue!=null?maturityDragValue:savedMaturity;
    boolean fixedForm=s.immortal||OmochaoRules.isOmochao(s);
    String growth=fixedForm ? "FIXED CHARACTER FORM" : "MATURITY "+AnalyzerMaturityBar056.percent(shownMaturity);
    text(g,growth,220,292,.65f,MUTED);
    segmented(g,318,293,244,5,fixedForm?1:shownMaturity/1000f,0xff67bed0,22);
    if(!fixedForm){
      int shown=AnalyzerMaturityBar056.marker(shownMaturity),saved=AnalyzerMaturityBar056.marker(savedMaturity);
      if(shown!=saved)g.fill(saved,291,saved+1,300,INK);
      round(g,shown-2,290,5,11,2,maturityEditable()?0xff297f9c:0xff647f90);
      g.fill(shown-1,292,shown+2,299,0xfff7ffff);
    }
    if(creativeEditor()&&!fixedForm)text(g,s.adult?"Drag maturity: current adult stage; lifetime stays unchanged.":"Drag maturity: baby cap 99%; first evolution stays separate.",220,305,.55f,MUTED);
    text(g, creativeEditor()?"Edit: grade / bar / level. Shift: −; Ctrl: ×10":"Hover a skill for garden and companion abilities", 220, 237, .65f, MUTED);
    String action = chao.getItemAction() == ChaoEntity.ItemAction.NONE
        ? chao.getBehavior()!=ChaoEntity.Behavior.NONE ? chao.getBehavior().name().toLowerCase(java.util.Locale.ROOT).replace('_',' ')
            : chao.toyPlayingKind().isEmpty() ? "Resting / exploring" : "Playing"
        : chao.getItemAction().name().toLowerCase(java.util.Locale.ROOT);
    text(g, "Boredom " + s.boredom + "   /   " + action, 220, 246, .65f, MUTED);
  }

  private void movementTooltip(GuiGraphics g, ChaoState s, int mouseX, int mouseY) {
    int x=localX(mouseX),y=localY(mouseY);if(x<214||x>571)return;
    var lines=new ArrayList<Component>();
    if(y>=287&&y<=313) {
      // Keep the live percentage and thumb visible while scrubbing. Ordinary
      // hover help returns on release; skill tooltips keep their own behavior.
      if(maturityDragging)return;
      lines.add(Component.literal((s.adult?"Biological maturity: ":"First evolution: ")+String.format(Locale.ROOT,s.adult?"%.2f / 1.20":"%.2f / 1.00",s.evolutionStrength())));
      if(maturityDragValue!=null)lines.add(Component.literal("Requested maturity: "+AnalyzerMaturityBar056.percent(maturityDragValue)+"; server confirms the saved value."));
      if(maturityEditable()) {
        lines.add(Component.literal("Creative: tap or drag the maturity bar to test this life stage."));
        lines.add(Component.literal(s.adult?"Adult strength: 0.00 to 1.20. First type/alignment stay locked.":"Baby strength: 0.00 to 0.99. Stops short of first evolution."));
        lines.add(Component.literal("Does not edit lifetime or trigger death/reincarnation. Natural aging resumes."));
      } else lines.add(Component.literal("Read-only: "+editorReadOnlyReason(false)));
      if(!s.adult)lines.add(Component.literal("Ordinary first type now: "+ChaoState.TYPES[s.prospectivePrimaryType()]+" (Stamina excluded)"));
      lines.add(Component.literal("Alignment and first adult type lock at first evolution."));
      var neutral=NeutralMorphology056.sample(s);
      var heroNormal=HeroNormalMorphology057.sample(s);
      var neutralSwim=NeutralSwimMorphology059.sample(s);
      var darkNormal=DarkNormalMorphology060.sample(s);
      var neutralFly=NeutralFlyMorphology061.sample(s);
      var neutralRun=NeutralRunMorphology062.sample(s);
      var neutralPower=NeutralPowerMorphology063.sample(s);
      var heroSwim=HeroSwimMorphology064.sample(s);
      var morph=HeroRunMorphology052.sample(s);
      if(heroSwim.enabled()) {
        lines.add(Component.literal(String.format(Locale.ROOT,"Hero Swim training: Run(-) / Power(+) %+.0f%%; Swim(-) / Fly(+) %+.0f%%.",heroSwim.runPower()*100,heroSwim.swimFly()*100)));
        lines.add(Component.literal("Maturity blends the fresh Hero Swim form into these targets: "+Math.round(heroSwim.maturity()*100)+"%."));
        lines.add(Component.literal("Provisional full axis: "+Math.round(HeroSwimMorphology064.SPECIALIZATION_PROGRESS)+" net training progress; compatible axes combine."));
        lines.add(Component.literal("Fly can cross balance toward its later form; the first Swim type and Hero alignment stay locked."));
        lines.add(Component.literal("Reductions change specialization, not biological maturity or first identity."));
      } else if(neutralPower.enabled()) {
        lines.add(Component.literal(String.format(Locale.ROOT,"Neutral Power training: Run(-) / Power(+) %+.0f%%; Swim(-) / Fly(+) %+.0f%%.",neutralPower.runPower()*100,neutralPower.swimFly()*100)));
        lines.add(Component.literal("Maturity blends the fresh Power form into these targets: "+Math.round(neutralPower.maturity()*100)+"%."));
        lines.add(Component.literal("Provisional full axis: "+Math.round(NeutralPowerMorphology063.SPECIALIZATION_PROGRESS)+" net training progress; compatible axes combine."));
        lines.add(Component.literal("Run can cross balance toward its later form; the first Power type stays locked."));
        lines.add(Component.literal("Reductions change specialization, not biological maturity or first identity."));
      } else if(neutralRun.enabled()) {
        lines.add(Component.literal(String.format(Locale.ROOT,"Neutral Run training: Run(-) / Power(+) %+.0f%%; Swim(-) / Fly(+) %+.0f%%.",neutralRun.runPower()*100,neutralRun.swimFly()*100)));
        lines.add(Component.literal("Maturity blends the fresh Run form into these targets: "+Math.round(neutralRun.maturity()*100)+"%."));
        lines.add(Component.literal("Provisional full axis: "+Math.round(NeutralRunMorphology062.SPECIALIZATION_PROGRESS)+" net training progress; compatible axes combine."));
        lines.add(Component.literal("Power can cross balance toward its later form; the first Run type stays locked."));
        lines.add(Component.literal("Reductions change specialization, not biological maturity or first identity."));
      } else if(neutralFly.enabled()) {
        lines.add(Component.literal(String.format(Locale.ROOT,"Neutral Fly training: Run(-) / Power(+) %+.0f%%; later Fly %.0f%%.",neutralFly.runPower()*100,neutralFly.swimFly()*100)));
        lines.add(Component.literal("Maturity blends the fresh Fly form into these targets: "+Math.round(neutralFly.maturity()*100)+"%."));
        lines.add(Component.literal("Provisional full axis: "+Math.round(NeutralFlyMorphology061.SPECIALIZATION_PROGRESS)+" net training progress; earlier baby training counts."));
        lines.add(Component.literal("Swim counters later Fly toward balanced; the first Fly type stays locked."));
        lines.add(Component.literal("Reductions change specialization, not biological maturity or first identity."));
      } else if(darkNormal.enabled()) {
        lines.add(Component.literal(String.format(Locale.ROOT,"Dark Normal training: Run(-) / Power(+) %+.0f%%; Swim(-) / Fly(+) %+.0f%%.",darkNormal.runPower()*100,darkNormal.swimFly()*100)));
        lines.add(Component.literal("Maturity blends the fresh Dark Normal form into these targets: "+Math.round(darkNormal.maturity()*100)+"%."));
        lines.add(Component.literal("Provisional full axis: "+Math.round(DarkNormalMorphology060.SPECIALIZATION_PROGRESS)+" net training progress; compatible axes combine."));
        lines.add(Component.literal("Decreases change training influence, not biological maturity or first identity."));
      } else if(neutralSwim.enabled()) {
        lines.add(Component.literal(String.format(Locale.ROOT,"Neutral Swim training: Run(-) / Power(+) %+.0f%%; later Swim %.0f%%.",neutralSwim.runPower()*100,-neutralSwim.swimFly()*100)));
        lines.add(Component.literal("Maturity blends the fresh Swim form into these targets: "+Math.round(neutralSwim.maturity()*100)+"%."));
        lines.add(Component.literal("Provisional full axis: "+Math.round(NeutralSwimMorphology059.SPECIALIZATION_PROGRESS)+" net training progress; earlier baby training counts."));
        lines.add(Component.literal("Fly counters later Swim toward balanced; the first Swim type stays locked."));
        lines.add(Component.literal("Reductions change specialization, not biological maturity or first identity."));
      } else if(heroNormal.enabled()) {
        lines.add(Component.literal(String.format(Locale.ROOT,"Hero Normal training: Run(-) / Power(+) %+.0f%%; Swim(-) / Fly(+) %+.0f%%.",heroNormal.runPower()*100,heroNormal.swimFly()*100)));
        lines.add(Component.literal("Maturity blends the fresh Hero Normal form into these targets: "+Math.round(heroNormal.maturity()*100)+"%."));
        lines.add(Component.literal("Provisional full axis: "+Math.round(HeroNormalMorphology057.SPECIALIZATION_PROGRESS)+" net training progress; compatible axes combine."));
        lines.add(Component.literal("Decreases change training influence, not biological maturity or first identity."));
      } else if(neutral.enabled()) {
        lines.add(Component.literal(String.format(Locale.ROOT,"Neutral training: Run(-) / Power(+) %+.0f%%; Swim(-) / Fly(+) %+.0f%%.",neutral.runPower()*100,neutral.swimFly()*100)));
        lines.add(Component.literal("Maturity blends the fresh Normal form into these targets: "+Math.round(neutral.maturity()*100)+"%."));
        lines.add(Component.literal("Provisional full axis: "+Math.round(NeutralMorphology056.SPECIALIZATION_PROGRESS)+" net training progress; compatible axes combine."));
        lines.add(Component.literal("Decreases change training influence, not biological maturity or first identity."));
      } else if(morph.enabled()) {
        if(s.adult) {
          lines.add(Component.literal("Hero Run target: "+Math.round(morph.specialization()*100)+"%; visible specialization: "+Math.round(morph.adultGrowth()*100)+"%"));
          lines.add(Component.literal("Run minus Power training changes the target, not biological age."));
          lines.add(Component.literal("Provisional full target: "+Math.round(HeroRunMorphology052.ADULT_RUN_PROGRESS)+" net training progress from zero."));
        } else {
          lines.add(Component.literal("Hero development: "+Math.round(morph.babyHero()*100)+"%; Run development: "+Math.round(morph.babyRun()*100)+"%"));
          lines.add(Component.literal("Provisional full Run target: "+Math.round(HeroRunMorphology052.BABY_RUN_PROGRESS)+" net training progress from zero."));
        }
        lines.add(Component.literal("Earlier baby training counts toward the adult training target."));
      } else lines.add(Component.literal("Later training changes the secondary form as maturity grows."));
      lines.add(Component.literal("Swim opposes Fly; Run opposes Power."));
      lines.add(Component.literal("Chaos admission keeps its separate Stamina/Normal balance check."));
      g.renderComponentTooltip(font,lines,mouseX,mouseY);return;
    }
    int stat=-1;for(int i=ChaoState.SWIM;i<=ChaoState.POWER;i++) {
      int top=105+i*26;if(y>=top&&y<=top+24)stat=i;
    }
    if(stat<0)return;
    int level=s.levels[stat];
    if(stat==ChaoState.RUN) {
      String gait=switch(LocomotionRules.groundGait(s)){case CRAWL->"Crawling";case WALK->"Walking";case RUN->"Running";case DASH->"Dashing";};
      lines.add(Component.literal("Garden gait: "+gait+" ("+s.points[stat]+" points)"));
      String next=!LocomotionRules.canWalk(s)?"Walk at "+LocomotionRules.WALK_POINTS+" points":!LocomotionRules.canRun(s)?"Run at "+LocomotionRules.RUN_POINTS+" points":!LocomotionRules.canDash(s)?"Dash at "+LocomotionRules.DASH_POINTS+" points":"All garden ground milestones learned";
      lines.add(Component.literal(next));
      lines.add(Component.literal("Companion land speed: +"+Math.round(CompanionRules.runSpeedBonus(level)*100)+"%"));
    } else if(stat==ChaoState.SWIM) {
      String stroke=!LocomotionRules.canSwim(s)?"Flailing":LocomotionRules.advancedSwim(s)?"Strong stroke":"Paddling";
      lines.add(Component.literal("Garden swim: "+stroke+" ("+s.points[stat]+" points)"));
      lines.add(Component.literal("Companion water speed: +"+Math.round(CompanionRules.swimSpeedBonus(level)*100)+"%"));
      lines.add(Component.literal("Underwater night vision: "+(CompanionRules.waterNightVision(level)?"Ready":"Swim level 70")));
      lines.add(Component.literal("Underwater breathing: "+(CompanionRules.waterBreathing(level)?"Ready":"Swim level 99")));
    } else if(stat==ChaoState.FLY) {
      lines.add(Component.literal("Garden ledge glide: "+(LocomotionRules.canFly(s)?"Learned":"At "+LocomotionRules.FLY_POINTS+" points")));
      if(CompanionRules.canElytra(s)) {
        lines.add(Component.literal("Companion flight: elytra (double jump while airborne)"));
      } else lines.add(Component.literal("Companion glide per fall: "+String.format(Locale.ROOT,"%.1f seconds",CompanionRules.glideTicks(level)/20f)));
      if(CompanionRules.robotBoost(s))lines.add(Component.literal("S-rank Omochao: boost key (default R), every 5 seconds"));
      else lines.add(Component.literal("Fly level 99 unlocks elytra flight"));
      lines.add(Component.literal("Land or enter water to recharge a limited glide."));
    } else {
      lines.add(Component.literal("Companion attack damage: "+String.format(Locale.ROOT,"%.1f",CompanionRules.powerDamage(level))));
      lines.add(Component.literal("Level 0: punch strength; level 99: Sharpness V netherite sword."));
      lines.add(Component.literal("All adult companions can help defend you."));
    }
    lines.add(Component.literal("Run/Swim unlocks use points; adult bonus amounts use levels."));
    lines.add(Component.literal("Companion benefits require an equipped, active, energized adult."));
    g.renderComponentTooltip(font,lines,mouseX,mouseY);
  }

  public static int gradeColor(int grade) {
    return switch(grade){case 0->0xff875332;case 1->0xffdd7b23;case 2->0xff338957;case 3->0xff367acd;case 4->0xff9552bb;case 5->0xffe2b638;default->0xff687e93;};
  }
  private String[] geneOptions(ChaoState s) {
    return new String[]{options(s.colorGenes,v->title(ChaoState.COLORS[v])),options(s.toneGenes,v->v==0?"One color":"Two-tone"),
      options(s.shinyGenes,v->v==0?"Not shiny":"Shiny"),options(s.jewelGenes,v->title(ChaoState.JEWELS[v])),
      options(s.favoriteFruitGenes,v->new String[]{"Round","Square","Triangle"}[v])};
  }
  private static String options(int[] pair,java.util.function.IntFunction<String> name){String a=name.apply(pair[0]),b=name.apply(pair[1]);return a.equals(b)?a:a+" or "+b;}
  private void genes(GuiGraphics g,ChaoState s) {
    if(geneTab==0){
      String[] labels={"Coat color","Pattern","Shine","Jewel","Favorite fruit"};
      String[] shown={s.baseColorName(),s.twoTone?"Two-tone":"One color",s.shiny?"Shiny":"Not shiny",title(ChaoState.JEWELS[s.expressedJewel]),new String[]{"Round","Square","Triangle"}[s.favoriteFruit]};
      var inherited=geneOptions(s);
      text(g,"TRAIT",220,114,.8f,MUTED);text(g,"SHOWS NOW",315,114,.8f,MUTED);text(g,"omochao".equals(s.character)?"STORED TRAITS":"CAN PASS ON",427,114,.8f,MUTED);
      for(int i=0;i<labels.length;i++){int y=133+i*26;round(g,217,y-4,348,23,4,i%2==0?0xffedf7fa:0xfff7fcff);text(g,labels[i],221,y+3,.8f,INK);fitText(g,shown[i],315,y+3,104,.8f,INK);fitText(g,inherited[i],427,y+3,133,.8f,0xff3d7b76);}
      paragraph(g,"omochao".equals(s.character)?"Omochao does not breed. These are stored traits; a Robo Heart passes its rank to the next robot.":"A baby receives one option from each parent. A trait your Chao carries may be hidden in its own appearance.",220,268,340,.8f,MUTED);
      if(!com.chaogarden.ChaoVariants.activeId(s).equals("normal")||s.immortal)text(g,"Special forms can change how inherited colors appear.",220,301,.8f,MUTED);
    }else if(geneTab==1){
      text(g,"ABILITY",220,114,.8f,MUTED);text(g,"CURRENT GRADE",337,114,.8f,MUTED);text(g,"omochao".equals(s.character)?"STORED GENES":"CAN PASS ON",456,114,.8f,MUTED);
      for(int i=0;i<5;i++){int y=135+i*22;text(g,ChaoState.STATS[i],220,y,.87f,INK);text(g,ChaoState.gradeName(s.grades[i]),370,y,.95f,gradeColor(s.grades[i]));
        text(g,ChaoState.gradeName(s.gradeA[i]),474,y,.9f,gradeColor(s.gradeA[i]));if(s.gradeA[i]!=s.gradeB[i]){text(g,"or",491,y+1,.8f,MUTED);text(g,ChaoState.gradeName(s.gradeB[i]),513,y,.9f,gradeColor(s.gradeB[i]));}}
      text(g,"Higher grades give more points per training level.",220,247,.8f,INK);
      text(g,"Intelligence potential: "+options(s.intelligenceGenes,Integer::toString),220,264,.8f,MUTED);
      text(g,"Luck potential: "+options(s.luckGenes,Integer::toString),220,277,.8f,MUTED);
      text(g,"Potential uses 0–255. Higher values improve training gains.",220,290,.8f,MUTED);
      text(g,"omochao".equals(s.character)?"Omochao passes rank through a Robo Heart, not breeding.":"Current levels and points are not passed to offspring.",220,302,.8f,MUTED);
    }else if("omochao".equals(s.character)){
      text(g,"MECHANICAL GROWTH",220,118,.9f,INK);
      paragraph(g,"Omochao trains its abilities, but does not grow animal parts or evolve into normal Chao forms.",220,148,340,.8f,MUTED);
      text(g,"Current rank: "+ChaoState.gradeName(s.grades[0]),220,201,.9f,INK);
      text(g,"Rank after rebuilding with its Robo Heart: "+ChaoState.gradeName(Math.min(5,s.grades[0]+1)),220,228,.8f,INK);
      paragraph(g,"A cared-for robot can leave a Robo Heart at the end of its life. Rebuilding starts a new robot with fresh training.",220,267,340,.8f,MUTED);
    }else{
      text(g,"TRAINING SHAPES YOUR CHAO",220,115,.85f,INK);
      var neutral=NeutralMorphology056.sample(s);
      var heroNormal=HeroNormalMorphology057.sample(s);
      var neutralSwim=NeutralSwimMorphology059.sample(s);
      var darkNormal=DarkNormalMorphology060.sample(s);
      var neutralFly=NeutralFlyMorphology061.sample(s);
      var neutralRun=NeutralRunMorphology062.sample(s);
      var neutralPower=NeutralPowerMorphology063.sample(s);
      var heroSwim=HeroSwimMorphology064.sample(s);
      text(g,heroSwim.enabled()?"Fly can cross balance; the first type stays Swim.":neutralPower.enabled()?"Run can cross balance; the first type stays Power.":neutralRun.enabled()?"Power can cross balance; the first type stays Run.":neutralFly.enabled()?"Swim counters later Fly; the first type stays Fly.":neutralSwim.enabled()?"Fly counters later Swim; the first type stays Swim.":"These meters show direction, not a chance or a level.",220,133,.8f,MUTED);
      float swimFly=heroSwim.enabled()?heroSwim.swimFly():neutralPower.enabled()?neutralPower.swimFly():neutralRun.enabled()?neutralRun.swimFly():neutralFly.enabled()?neutralFly.swimFly():darkNormal.enabled()?darkNormal.swimFly():neutralSwim.enabled()?neutralSwim.swimFly():neutral.enabled()?neutral.swimFly():heroNormal.enabled()?heroNormal.swimFly():s.swimFly;
      float runPower=heroSwim.enabled()?heroSwim.runPower():neutralPower.enabled()?neutralPower.runPower():neutralRun.enabled()?neutralRun.runPower():neutralFly.enabled()?neutralFly.runPower():darkNormal.enabled()?darkNormal.runPower():neutralSwim.enabled()?neutralSwim.runPower():neutral.enabled()?neutral.runPower():heroNormal.enabled()?heroNormal.runPower():s.runPower;
      axis(g,"Swim","Fly",swimFly,220,159,340,STAT_COLORS[0],STAT_COLORS[1]);
      text(g,lean(swimFly,"Swim","Fly"),220,175,.8f,INK);
      axis(g,"Run","Power",runPower,220,203,340,STAT_COLORS[2],STAT_COLORS[3]);
      text(g,lean(runPower,"Run","Power"),220,219,.8f,INK);
      String form=s.immortal?"This special form no longer evolves.":s.adult?"First type: "+ChaoState.TYPES[s.primaryType]+" · Adult form: "+s.typeName():"First evolution if ready now: "+ChaoState.TYPES[s.prospectivePrimaryType()];
      fitText(g,form,220,246,340,.8f,INK);
      paragraph(g,"First type uses the highest Swim, Fly, Run or Power points; Stamina is excluded. The latest actual increase among tied abilities wins. Training and biological maturity are separate.",220,267,340,.8f,MUTED);
    }
  }
  private static String lean(float value,String left,String right){return Math.abs(value)<.01f?"Balanced between "+left+" and "+right:"Leans "+(value<0?left:right)+" · "+Math.round(Math.min(1,Math.abs(value))*100)+"% from the middle";}
  private void fitText(GuiGraphics g,String value,int x,int y,int width,float scale,int color){text(g,value,x,y,Math.min(scale,width/(float)Math.max(1,font.width(value))),color);}
  private void paragraph(GuiGraphics g,String value,int x,int y,int width,float scale,int color){int row=0;for(var line:font.split(Component.literal(value),(int)(width/scale))){g.pose().pushPose();g.pose().translate(x,y+row*11,0);g.pose().scale(scale,scale,1);g.drawString(font,line,0,0,color,false);g.pose().popPose();row++;}}
  private void geneTooltip(GuiGraphics g,ChaoState s,int mouseX,int mouseY){if("omochao".equals(s.character))return;int x=localX(mouseX),y=localY(mouseY);if(x<427||x>563||y<129||y>=259)return;int row=(y-129)/26;if(row<5)g.renderComponentTooltip(font,List.of(Component.literal("Can pass on: "+geneOptions(s)[row]),Component.literal("One of these inherited options can go to each baby.")),mouseX,mouseY);}

  private void axis(
      GuiGraphics g, String left, String right, float value, int x, int y, int w, int a, int b) {
    text(g, left, x, y, .7f, INK);
    text(g, right, x + w - 27, y, .7f, INK);
    int bx = x + 35, bw = w - 70;
    round(g, bx, y + 1, bw / 2, 6, 2, a);
    round(g, bx + bw / 2, y + 1, bw - bw / 2, 6, 2, b);
    int marker = bx + Math.round((Math.max(-1, Math.min(1, value)) + 1) * .5f * (bw - 2));
    g.fill(marker - 1, y - 1, marker + 2, y + 9, 0xff284b71);
    g.fill(marker, y, marker + 1, y + 8, 0xffffffff);
  }

  private void animals(GuiGraphics g,ChaoState s) {
    int received=Integer.bitCount(s.animalMask&ChaoState.ALL_ANIMALS);
    if(animalTab==1){
      chartTitle(g,"CHAOS DNA CHECKLIST","");
      text(g,received+"/"+ChaoState.ANIMALS.length+" received · "+(ChaoState.ANIMALS.length-received)+" missing",220,109,.8f,INK);
      var ids=visibleDnaIds();
      for(int n=0;n<ids.size();n++){int i=ids.get(n),x=218+(n/7)*117,y=126+(n%7)*19;boolean done=(s.animalMask&(1<<i))!=0;
        round(g,x,y,113,18,4,done?0xffe0f0e5:0xfff6e9ef);g.renderItem(new ItemStack(ChaoGarden.item(ChaoGarden.ANIMALS[i]+"_animal_gene")),x+2,y+1);
        fitText(g,title(ChaoGarden.ANIMALS[i]),x+22,y+3,87,.8f,INK);text(g,done?"RECEIVED":"NEEDED",x+22,y+12,.48f,done?0xff338957:0xffa24f6c);}
      if(ids.isEmpty())text(g,"All "+ChaoState.ANIMALS.length+" animal DNA gifts have been received.",220,147,.8f,0xff338957);
      text(g,"PAGE "+(dnaPage+1)+" / "+dnaPageCount(),352,268,.7f,MUTED);
      text(g,"Every animal DNA type is required in this life.",220,285,.8f,MUTED);
      text(g,"Counts accepted DNA, even after parts are replaced.",220,298,.8f,MUTED);return;
    }
    chartTitle(g,"CURRENT BODY PARTS","");
    outline(g);
    int[] slots={ChaoState.EARS,ChaoState.ARMS,ChaoState.FEET,ChaoState.TAIL,ChaoState.FOREHEAD,ChaoState.HORNS,ChaoState.FACE,ChaoState.WINGS};
    String[] labels={"EARS","ARMS","FEET","TAIL","HEAD TUFT","HORNS","FACE","WINGS"};
    int[][] targets={{375,143},{366,186},{381,222},{401,204},{393,120},{405,136},{398,158},{411,180}};
    for(int i=0;i<slots.length;i++){int x=i<4?220:470,y=111+(i%4)*31;String part=partName(s,slots[i]);text(g,labels[i],x,y,.8f,MUTED);fitText(g,part,x,y+12,92,.8f,INK);
      int start=i<4?310:465,elbow=i<4?337:444,ty=targets[i][1],tx=targets[i][0];line(g,start,y+18,elbow,y+18,0xff6facc0);line(g,elbow,y+18,tx,ty,0xff6facc0);round(g,tx-2,ty-2,4,4,2,0xff248dc8);}
    text(g,"Visible parts can change. Received DNA is tracked separately.",220,239,.8f,MUTED);
    if("omochao".equals(s.character)){text(g,"MECHANICAL BODY",220,261,.8f,0xffaa5884);paragraph(g,"Omochao cannot grow animal parts or become Chaos Chao. Use Drives, fruit and daycare lessons to train.",220,280,340,.8f,INK);return;}
    if(s.immortal){text(g,"normal".equals(s.character)?"CHAOS FORM COMPLETE":"IMMORTAL FORM",220,260,.8f,0xff338957);text(g,"This Chao has already reached an immortal form.",220,283,.8f,INK);return;}
    text(g,"CHAOS CHAO REQUIREMENTS",220,258,.8f,0xffaa5884);
    condition(g,s.reincarnations>=2,"Third life",220,273,82);
    condition(g,s.happiness>80,"Happy >80",400,273,81);
    condition(g,s.chaosNormalCandidate(),"Stamina",486,273,77);
    text(g,s.adult?"Requirements are checked at child-to-adult evolution.":received+"/"+ChaoState.ANIMALS.length+" DNA · click Animal DNA for what is missing",220,299,.8f,MUTED);
  }
  private List<Integer> checklistDnaIds() {
    var ids=new ArrayList<Integer>();var s=chao.state();
    for(int i=0;i<ChaoState.ANIMALS.length;i++)if(!missingOnly||(s.animalMask&(1<<i))==0)ids.add(i);
    return ids;
  }
  int dnaPageCount(){return Math.max(1,(checklistDnaIds().size()+DNA_PER_PAGE-1)/DNA_PER_PAGE);}
  int dnaPageIndex(){return dnaPage;}
  List<Integer> visibleDnaIds(){
    var ids=checklistDnaIds();dnaPage=Math.max(0,Math.min(dnaPage,Math.max(0,(ids.size()-1)/DNA_PER_PAGE)));
    int start=dnaPage*DNA_PER_PAGE;return ids.subList(start,Math.min(ids.size(),start+DNA_PER_PAGE));
  }
  public static String partName(ChaoState s,int slot){
    if("omochao".equals(s.character))return "Mechanical";
    int animal=s.animalParts[slot];if(s.allowsAnimalParts()&&animal>=0&&(ChaoState.animalSlots(animal)&(1<<slot))!=0)return title(ChaoGarden.ANIMALS[animal]);
    // Light and Angel Chaos hide the base tail without replacing it; only Devil has a forked tail.
    if(slot==ChaoState.TAIL&&s.immortal&&"normal".equals(s.character)&&s.align>-.5f)return "None";
    if(s.ghostBody&&(slot==ChaoState.FEET||slot==ChaoState.TAIL||slot==ChaoState.WINGS))return "Hidden";
    return slot==ChaoState.EARS||slot==ChaoState.HORNS?"None":"Chao";
  }
  private void outline(GuiGraphics g){
    int[][] shape={{379,135},{385,127},{390,117},{391,110},{397,119},{399,131},{410,136},{418,145},{419,159},{412,170},{404,174},{410,181},{420,182},{425,190},{421,196},{410,195},{408,209},{416,218},{415,225},{401,226},{394,218},{387,225},{373,225},{369,220},{378,210},{377,195},{367,199},{359,194},{358,187},{367,181},{378,176},{367,169},{361,159},{362,146},{368,138},{379,135}};
    for(int i=1;i<shape.length;i++)line(g,shape[i-1][0],shape[i-1][1],shape[i][0],shape[i][1],0xff407c98);
    line(g,378,175,405,175,0xff94bbc9);line(g,382,184,382,205,0xffb4d6df);line(g,403,184,403,205,0xffb4d6df);
    round(g,380,149,3,7,2,0xff407c98);round(g,400,149,3,7,2,0xff407c98);line(g,389,163,395,163,0xff407c98);
    round(g,390,99,7,7,4,0xffe5b536);
  }
  private static void line(GuiGraphics g,int x1,int y1,int x2,int y2,int color){int steps=Math.max(Math.abs(x2-x1),Math.abs(y2-y1));for(int i=0;i<=steps;i++){float t=steps==0?0:i/(float)steps;int x=Math.round(x1+(x2-x1)*t),y=Math.round(y1+(y2-y1)*t);g.fill(x,y,x+1,y+1,color);}}

  private void condition(GuiGraphics g, boolean pass, String label, int x, int y, int w) {
    round(g, x, y, w, 16, 6, pass ? 0xffd4ebd7 : 0xffffe8f1);
    text(g, (pass ? "+ " : "  ") + label, x + 5, y + 4, .82f, pass ? 0xff448b63 : 0xffa27a8b);
  }

  private void sendCare(String action,String value) {
    if(minecraft!=null && minecraft.player!=null && minecraft.player.distanceToSqr(chao)<=64)
      ChaoCareNetwork.request(chao,action,value);
  }

  private List<Integer> inventoryGifts() {
    var result=new ArrayList<Integer>();if(minecraft==null || minecraft.player==null)return result;
    for(int i=0;i<36;i++)if(ChaoCareServices.accessorySlot(minecraft.player.getInventory().getItem(i))!=null)result.add(i);
    return result;
  }
  private int inventoryChoiceSlot(int choice) {
    var gifts=inventoryGifts();int index=inventoryPage*8+choice;return index>=0 && index<gifts.size()?gifts.get(index):-1;
  }
  private ItemStack inventoryChoice(int choice) {
    int slot=inventoryChoiceSlot(choice);return slot<0 || minecraft==null || minecraft.player==null?ItemStack.EMPTY:minecraft.player.getInventory().getItem(slot);
  }
  private void nextMedal() {
    var choices=new ArrayList<String>();choices.add("");
    for(String id:ChaoCareServices.MEDALS)if(chao.state().traitInt("medal."+id,0)>0)choices.add(id);
    int index=choices.indexOf(chao.state().trait("medal.selected"));sendCare("medal",choices.get((index+1)%choices.size()));
  }
  private void updateCareButtons() {
    if(animalButton!=null){
      boolean checklist=page==2&&animalTab==1;
      animalButton.visible=dnaFilterButton.visible=previousDna.visible=nextDna.visible=checklist;
      dnaFilterButton.setMessage(Component.literal(missingOnly?"SHOW ALL":"MISSING ONLY"));
      visibleDnaIds();previousDna.active=dnaPage>0;nextDna.active=dnaPage+1<dnaPageCount();
      chaosDnaButton.visible=page==2&&animalTab==0&&!chao.state().immortal&&!"omochao".equals(chao.state().character);
    }
    for(var b:geneButtons)b.visible=page==1;
    for(var b:mindButtons)b.visible=page==3;
    for(var b:careButtons)b.visible=page==4;
    if(cureButton==null || minecraft==null || minecraft.player==null)return;
    var player=minecraft.player;var s=chao.state();boolean mine=ChaoCareServices.canManage(player,chao);
    boolean ready=mine && !chao.isHandlingItem() && !chao.isCocooned();
    boolean doctor=page==4 && careTab==0,school=page==4 && careTab==1,wardrobe=page==4 && careTab==2;
    cureButton.visible=doctor;cureButton.active=ready && clinicNearby && s.isIll()
        && (player.getAbilities().instabuild || medicineCount()>0);
    cureButton.setMessage(Component.literal("USE MEDICINE ("+medicineCount()+")"));
    claimButton.visible=school&&ChaoCareServices.pendingLessonRewards(s)>0;claimButton.active=ready;
    for(int i=0;i<5;i++) {
      var upgrade=upgradeButtons.get(i);upgrade.visible=doctor;
      upgrade.active=ready && clinicNearby && s.canUpgradeGrade(i)
          && (player.getAbilities().instabuild || ChaoCareServices.wallet(player)>=(s.gradeUpgradeCostRings(i)+99)/100);
      var slot=ChaoCareServices.WARDROBE[i];var item=chao.getItemBySlot(slot);
      removeButtons.get(i).visible=dyeButtons.get(i).visible=wardrobe;
      removeButtons.get(i).active=ready && !item.isEmpty();
      dyeButtons.get(i).active=ready && ChaoCareServices.dyeIndex(player)>=0
          && item.getItem() instanceof com.chaogarden.cwe.CweGiftItem gift && gift.gift.dyable();
    }
    int count=inventoryGifts().size();inventoryPage=Math.max(0,Math.min(inventoryPage,Math.max(0,(count-1)/8)));
    for(int i=0;i<8;i++){inventoryButtons.get(i).visible=wardrobe;inventoryButtons.get(i).active=ready && !inventoryChoice(i).isEmpty();}
    previousItems.visible=nextItems.visible=wardrobe;previousItems.active=inventoryPage>0;nextItems.active=(inventoryPage+1)*8<count;
    medalButton.visible=wardrobe;medalButton.active=ready;
    String selected=s.trait("medal.selected");medalButton.setMessage(Component.literal("MEDAL: "+(selected.isEmpty()?"NONE":title(selected))));
  }
  private int medicineCount() {
    if(minecraft==null || minecraft.player==null)return 0;int count=0;
    for(int i=0;i<36;i++)if(minecraft.player.getInventory().getItem(i).is(ChaoGarden.item("chao_medicine")))count+=minecraft.player.getInventory().getItem(i).getCount();
    return count;
  }
  private void mind(GuiGraphics g,ChaoState s) {
    if(mindTab==0) {
      for(int i=0;i<ChaoPersonality.COUNT;i++) {
        int col=i/7,row=i%7,x=220+col*176,y=111+row*26,value=s.personalitySlider(i);
        text(g,font.plainSubstrByWidth(ChaoPersonality.NAMES[i],206),x,y,.65f,INK);
        text(g,String.format("%+d",value),x+140,y,.65f,MUTED);
        round(g,x,y+12,158,5,2,0xffd6e7ec);g.fill(x+79,y+10,x+80,y+19,0xff83a9ba);
        int at=x+Math.round((value+100)*.79f);round(g,at-2,y+9,5,11,2,0xff9b7bc2);
      }
      var labels=Arrays.stream(s.personalityProfile()).mapToObj(i->ChaoPreferences.PERSONALITIES[i]).toList();
      text(g,"Profile: "+String.join(" / ",labels),220,301,.61f,MUTED);
      alignmentBar(g,s);
    } else if(mindTab==1) {
      for(int i=0;i<ChaoEmotion.COUNT;i++) {
        int col=i/9,row=i%9,x=220+col*176,y=111+row*20,value=s.emotion(i),max=ChaoEmotion.maximum(i);
        text(g,font.plainSubstrByWidth(ChaoEmotion.NAMES[i],185),x,y,.63f,INK);
        text(g,Integer.toString(value),x+126,y,.63f,MUTED);
        segmented(g,x,y+11,158,4,value/(float)max,0xff9d87d8,12);
      }
      text(g,"Hover a meter for its full name and range",220,301,.64f,MUTED);
    } else if(mindTab==2) {
      for(int i=0;i<ChaoIllness.COUNT;i++) {
        int y=113+i*27,value=s.illness(i);boolean ill=value<0;
        text(g,ChaoIllness.NAMES[i],220,y,.87f,INK);
        text(g,ill?"ACTIVE":value>0?"PROTECTED":"CLEAR",395,y,.75f,ill?0xffd9536f:0xff49a077);
        right(g,Integer.toString(value),558,y,MUTED);
        segmented(g,220,y+13,340,4,(value+100)/200f,ill?0xffe3839b:0xff80bda0,20);
      }
      text(g,"Diagnosis: "+font.plainSubstrByWidth(s.illnessName(),390),220,281,.75f,s.isIll()?0xffc85d78:MUTED);
      text(g,"omochao".equals(s.character)?"Mechanical body: immune to illness":"Doctor care and remedies are under CARE",220,300,.68f,MUTED);
    } else if("omochao".equals(s.character)) {
      robotFamily(g,s);
    } else {
      for(int i=0;i<6;i++) {
        int x=220+(i%2)*176,y=111+(i/2)*62;
        String prefix=i<2?"family.parent."+i:"family.grandparent."+(i-2);
        String dna=s.trait(prefix+".dna");
        text(g,(i<2?"PARENT "+(i+1):"GRANDPARENT "+(i-1)),x,y,.65f,MUTED);
        text(g,font.plainSubstrByWidth(familyDisplayName(s,prefix),216),x,y+12,.76f,INK);
        if(!dna.isEmpty()) {
          var lines=ChaoState.describeDnaSummary(dna);
          for(int row=0;row<Math.min(4,lines.size());row++) {
            String detail=lines.get(row).replace(" alleles:",":");
            g.pose().pushPose();g.pose().translate(x,y+25+row*8,0);g.pose().scale(.6f,.6f,1);
            g.drawString(font,font.plainSubstrByWidth(detail,270),0,0,MUTED,false);g.pose().popPose();
          }
        } else {
          text(g,"No genetic record",x,y+28,.62f,MUTED);
        }
      }
      text(g,"Hover a relative for saved DNA and lineage; training does not inherit",220,301,.57f,MUTED);
    }
  }
  private void robotFamily(GuiGraphics g,ChaoState s) {
    String[] prefixes={"family.parent.0","family.grandparent.0"};
    String[] labels={"CREATOR / PARENT","PREVIOUS CREATOR / GRANDPARENT"};
    for(int i=0;i<2;i++) {
      int x=220+i*176;String prefix=prefixes[i],dna=s.trait(prefix+".dna");
      text(g,labels[i],x,114,i==0?.65f:.51f,MUTED);
      text(g,font.plainSubstrByWidth(familyDisplayName(s,prefix),216),x,132,.83f,INK);
      var details=ChaoState.describeDnaSummary(dna);
      if(dna.isEmpty())text(g,"No earlier creator recorded",x,153,.60f,MUTED);
      else for(int row=0;row<Math.min(4,details.size());row++)text(g,font.plainSubstrByWidth(details.get(row).replace(" alleles:",":"),270),x,153+row*11,.60f,MUTED);
    }
    int grade=s.grades.length==0?2:Math.max(0,Math.min(5,s.grades[0]));
    text(g,"ROBOT LEGACY",220,223,.76f,0xffaa5884);
    text(g,"Current grade: "+"EDCBAS".charAt(grade)+" across all skills",220,244,.85f,INK);
    text(g,"A cared-for robot may leave a Robo Heart.",220,264,.75f,MUTED);
    text(g,grade>=5?"A Robo Heart preserves S rank for the next robot.":"Reconstruction raises the next robot's grades, up to S.",220,280,.70f,MUTED);
    text(g,"The builder becomes its parent; the old creator becomes a grandparent.",220,300,.57f,MUTED);
  }
  private String familyDisplayName(ChaoState state,String prefix) {
    String name=state.familyName(prefix),id=state.trait(prefix+".id");
    if(!name.isEmpty())return name;
    // Older saves recorded an identity but no display name. A currently
    // tracked relative can supply a live name; unloaded unknowns stay honest.
    return observedAncestorNames.getOrDefault(id,id.isEmpty()?"Unknown ancestor":"Name not recorded");
  }
  private void alignmentBar(GuiGraphics g,ChaoState s){
    float alignment=s.lockedAlignment();
    int value=AnalyzerAlignmentBar053.milli(alignment);
    int shownValue=alignmentDragValue!=null?alignmentDragValue:value;
    boolean editable=alignmentEditable();
    text(g,s.adult?"ADULT ALIGNMENT: LOCKED":editable?"ALIGNMENT: DRAG":"ALIGNMENT: VIEW",398,266,.50f,INK);
    String current=AnalyzerAlignmentBar053.category(shownValue/1000f)+String.format(Locale.ROOT," %+.1f",shownValue/10f);
    text(g,current,560-font.width(current)*.60f,266,.60f,MUTED);
    int x=AnalyzerAlignmentBar053.X,y=AnalyzerAlignmentBar053.Y,w=AnalyzerAlignmentBar053.WIDTH;
    round(g,x-1,y-1,w+2,9,2,0xff53758e);
    g.fill(x,y,x+w/4,y+7,0xff9b88c6);
    g.fill(x+w/4,y,x+3*w/4,y+7,0xffc8deda);
    g.fill(x+3*w/4,y,x+w,y+7,0xff79c8e2);
    for(int mark:new int[]{-500,0,500}){
      int at=AnalyzerAlignmentBar053.marker(mark);g.fill(at,y-1,at+1,y+8,mark==0?0xff456c80:0xff76909d);
    }
    int saved=AnalyzerAlignmentBar053.marker(value);
    int shown=AnalyzerAlignmentBar053.marker(shownValue);
    if(shown!=saved)g.fill(saved,y-2,saved+1,y+9,INK);
    round(g,shown-2,y-3,5,13,2,editable?0xff66529a:0xff547185);
    g.fill(shown-1,y-1,shown+2,y+8,0xfff7ffff);
    text(g,"Dark",398,292,.60f,INK);
    String middle="Neutral";text(g,middle,x+w/2f-font.width(middle)*.30f,292,.60f,INK);
    text(g,"Hero",560-font.width("Hero")*.60f,292,.60f,INK);
  }
  private void alignmentTooltip(GuiGraphics g,ChaoState s,int mouseX,int mouseY){
    int x=localX(mouseX),y=localY(mouseY);if(x<396||x>562||y<264||y>299)return;
    var lines=new ArrayList<Component>();
    float alignment=s.lockedAlignment();
    lines.add(Component.literal("Alignment: "+AnalyzerAlignmentBar053.category(alignment)+String.format(Locale.ROOT," (%+.3f)",alignment)));
    lines.add(Component.literal("Testing scale: -100 Dark, 0 Neutral, +100 Hero (internal -1 to +1)."));
    if(alignmentDragValue!=null)lines.add(Component.literal("Requested: "+String.format(Locale.ROOT,"%+.1f",alignmentDragValue/10f)+"; saved value awaits server confirmation."));
    if(s.adult)lines.add(Component.literal("Locked at first evolution. Adults cannot be realigned."));
    else if(!creativeEditor())lines.add(Component.literal("Read-only. Creative mode is required to edit alignment."));
    else if(s.dead||s.immortal||chao.isCocooned())lines.add(Component.literal("Read-only while this Chao cannot receive care."));
    else if(!alignmentEditable())lines.add(Component.literal("Hold a Chao Analyzer to edit this baby's alignment."));
    else {
      lines.add(Component.literal("Drag to edit this baby. The server confirms each change."));
      lines.add(Component.literal("Stay nearby with the Analyzer; occupied Chao may refuse edits."));
    }
    lines.add(Component.literal("Run training is a separate influence from Hero/Dark alignment."));
    g.renderComponentTooltip(font,lines,mouseX,mouseY);
  }

  private void familyTooltip(GuiGraphics g,ChaoState s,int mouseX,int mouseY) {
    int x=localX(mouseX),y=localY(mouseY);if(x<220 || x>562 || y<111 || y>296)return;
    boolean robot="omochao".equals(s.character);
    int col=(x-220)/176,row=robot?0:(y-111)/62,i=row*2+col;if(col>1||row>2||i>=6||robot&&y>205)return;
    String prefix="omochao".equals(s.character)?(col==0?"family.parent.0":"family.grandparent.0"):i<2?"family.parent."+i:"family.grandparent."+(i-2),dna=s.trait(prefix+".dna"),id=s.trait(prefix+".id");
    var lines=new ArrayList<Component>();familyTooltipLine(lines,familyDisplayName(s,prefix));
    if(!id.isEmpty())familyTooltipLine(lines,"Lineage ID: "+id);
    if(s.familyName(prefix).isEmpty()&&!id.isEmpty()&&!observedAncestorNames.containsKey(id))
      familyTooltipLine(lines,"This older record did not save a name.");
    for(String segment:ChaoState.describeDnaSummary(dna))familyTooltipLine(lines,segment);
    g.renderComponentTooltip(font,lines,mouseX,mouseY);
  }
  private void familyTooltipLine(List<Component> lines,String text) {
    int limit=Math.max(160,Math.min(360,width-24));String remaining=text;
    while(!remaining.isEmpty()) {
      String fitted=font.plainSubstrByWidth(remaining,limit);if(fitted.isEmpty())break;
      int end=fitted.length();if(end<remaining.length()) {int space=fitted.lastIndexOf(' ');if(space>0)end=space;}
      lines.add(Component.literal(remaining.substring(0,end)));remaining=remaining.substring(end).stripLeading();
    }
  }
  private void care(GuiGraphics g,ChaoState s) {
    if(careTab==0) {
      for(int i=0;i<6;i++) {
        int x=220+(i/3)*176,y=112+(i%3)*25;boolean ill=s.illness(i)<0;
        text(g,ChaoIllness.NAMES[i],x,y,.76f,INK);
        text(g,ill?"Needs treatment":s.illness(i)>0?"Protected":"Well",x,y+11,.64f,ill?0xffc95070:0xff4e9e79);
      }
      if(clinicNearby)text(g,"Doctor Station in range",393,199,.62f,0xff4b9b71);
      text(g,"GRADE CARE  /  "+s.traitInt("doctor.upgrades",0)+" of 3 upgrades used this life",220,220,.65f,MUTED);
      for(int i=0;i<5;i++) {
        int y=235+i*14,cost=(s.gradeUpgradeCostRings(i)+99)/100;
        text(g,ChaoState.STATS[i]+"  "+ChaoState.gradeName(s.grades[i])+"  Lv."+s.levels[i],220,y,.7f,INK);
        String eligibility=s.grades[i]>=4?"Grade already A or higher":s.traitInt("doctor.upgrades",0)>=3?"Three upgrades already used":cost+" emeralds; level 99 required";
        text(g,eligibility,354,y,.6f,MUTED);
      }
      text(g,"Grade care resets that ability's level and points",220,303,.59f,MUTED);
    } else if(careTab==1) {
      text(g,"LEARN AT DAYCARE",220,114,.94f,INK);
      paragraph(g,"Leave your Chao with the Kindergarten teacher. Pick it up when class finishes; lessons cannot be started from the Analyzer.",220,135,340,.8f,MUTED);
      String active=s.trait("lesson.active");var lesson=ChaoCareServices.lesson(active);int remaining=s.traitInt("lesson.remaining",0);
      boolean attending=lesson!=null&&remaining>0;
      fitText(g,attending?"Current class: "+lesson.name():"Not attending a class.",220,170,340,.8f,INK);
      if(attending)text(g,(s.traitInt("lesson.paused",0)>0?"Paused · ":"")+clock(remaining)+" remaining",220,183,.68f,MUTED);
      g.fill(220,193,563,194,0xffd4e7ec);text(g,"LEARNED SKILLS",220,202,.8f,MUTED);
      for(int i=0;i<ChaoCareServices.LESSONS.size();i++) {var learned=ChaoCareServices.LESSONS.get(i);int x=220+(i/9)*176,y=216+(i%9)*9;int level=ChaoCareServices.lessonLevel(s,learned.id());
        text(g,learned.name(),x,y,.8f,level>0?INK:MUTED);text(g,level==0?"—":learned.maximum()==1?"Learned":level+" / "+learned.maximum(),x+112,y,.8f,level>0?0xff338957:MUTED);}
      text(g,"Song and drawing have 5 levels; other skills are learned once.",220,303,.8f,MUTED);
    } else {
      String[] labels={"Hat","Head accessory","Face","Neck","Body"};
      for(int i=0;i<5;i++) {
        int y=113+i*28;var stack=chao.getItemBySlot(ChaoCareServices.WARDROBE[i]);
        text(g,labels[i],220,y,.65f,MUTED);if(!stack.isEmpty())g.renderItem(stack,291,y-2);
        text(g,font.plainSubstrByWidth(stack.isEmpty()?"Empty":stack.getHoverName().getString(),178),314,y+2,.75f,INK);
      }
      int dye= minecraft==null || minecraft.player==null?-1:ChaoCareServices.dyeIndex(minecraft.player);
      String tint=dye<0?"Carry a dye to change an accessory's colour":"Dye: "+minecraft.player.getInventory().getItem(dye).getHoverName().getString();
      text(g,tint,220,255,.67f,MUTED);
      text(g,"Offer an inventory accessory; your Chao decides",220,266,.64f,MUTED);
      text(g,"Accepted outfits return replaced gear; refusals are thrown",220,303,.61f,MUTED);
    }
  }
  private static String clock(int ticks) {int seconds=Math.max(0,ticks/20);return (seconds/60)+":"+String.format("%02d",seconds%60);}
  private void emotionTooltip(GuiGraphics g,ChaoState s,int mouseX,int mouseY) {
    int x=localX(mouseX),y=localY(mouseY),col=(x-220)/176,row=(y-111)/20;
    if(x<220 || x>562 || y<111 || y>290 || col<0 || col>1 || row<0 || row>8)return;
    int i=col*9+row;if(i>=ChaoEmotion.COUNT)return;
    g.renderComponentTooltip(font,List.of(Component.literal(ChaoEmotion.NAMES[i]),
        Component.literal(s.emotion(i)+" / "+ChaoEmotion.maximum(i))),mouseX,mouseY);
  }
  private void inventoryTooltip(GuiGraphics g,int mouseX,int mouseY) {
    int x=localX(mouseX),y=localY(mouseY);if(y<276 || y>300)return;
    for(int i=0;i<8;i++)if(x>=222+i*40 && x<248+i*40 && !inventoryChoice(i).isEmpty()) {
      g.renderTooltip(font,inventoryChoice(i),mouseX,mouseY);return;
    }
  }

  private static String title(String value) {
    String s = value.replace('_', ' ');
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private static String tone(int n) {
    return n == 0 ? "Mono" : "Two-tone";
  }

  private static String shine(int n) {
    return n == 0 ? "Normal" : "Shiny";
  }
}
