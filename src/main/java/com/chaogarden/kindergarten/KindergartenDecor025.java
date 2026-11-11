package com.chaogarden.kindergarten;

import com.chaogarden.ChaoGarden;
import com.mojang.serialization.MapCodec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.StringRepresentable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Authored SA2-inspired scenery. These are ordinary native block models, not a decoration-mod dependency. */
public final class KindergartenDecor025 {
  public enum Kind { WALL, COUNTER, STOOL, BED, DRAPE, SMALL, PANEL, LEAF, WINDOW, TOY, COLUMN, RUG, CLOUD, ARCH, CABINET, DESK, DOOR, MURAL, CURTAIN, GUARD }
  private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,ChaoGarden.MODID);
  private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,ChaoGarden.MODID);
  private static final Map<String,DeferredHolder<Block,? extends Block>> blocks=new LinkedHashMap<>();
  private static final Map<String,DeferredHolder<Item,BlockItem>> items=new LinkedHashMap<>();
  static {
    add(Kind.WALL,"school_lobby_rug","school_stone_tile","school_principal_rug","school_classroom_floor","school_clinic_tile",
        "school_cream_wall","school_wainscot","school_clinic_wainscot","school_fortune_stone","school_frieze","school_cloud_mural","school_clapboard","school_red_shingle","school_yellow_brick");
    add(Kind.COUNTER,"school_red_counter","school_green_counter");add(Kind.DESK,"school_principal_desk","school_fortune_altar");
    add(Kind.WALL,"school_blue_cabinet");add(Kind.CABINET,"school_medicine_cabinet");add(Kind.STOOL,"school_pink_stool","school_pink_chair","school_green_chair","school_fortune_chair");add(Kind.BED,"school_white_exam_bed");
    add(Kind.DRAPE,"school_purple_drape","school_green_drape","school_purple_drape_plain","school_green_drape_plain");
    add(Kind.CURTAIN,"school_purple_room_curtain","school_purple_room_curtain_tie","school_green_room_curtain","school_green_room_curtain_tie");
    add(Kind.SMALL,"school_crystal_ball","school_globe","school_book_stack","school_open_book","school_potion_display","school_candelabra","school_quill","school_bust");
    add(Kind.PANEL,"school_noticeboard","school_art_chao","school_art_chao_small","school_art_friends","school_art_family","school_art_garden",
        "school_label_principal","school_label_classroom","school_label_health","school_label_fortune","school_label_market","school_label_exit","school_label_kindergarten","school_emblem","school_facade_title_left","school_facade_title_middle","school_facade_title_right");
    add(Kind.LEAF,"school_leaf_principal","school_leaf_classroom","school_leaf_health","school_leaf_fortune","school_leaf_exit",
        "school_leaf_principal_upper","school_leaf_classroom_upper","school_leaf_health_upper","school_leaf_fortune_upper","school_leaf_exit_upper");
    add(Kind.DOOR,"school_door_principal","school_door_classroom","school_door_health","school_door_fortune","school_door_exit","school_door_market","school_tool_cabinet");
    add(Kind.GUARD,"school_altar_guard");
    add(Kind.MURAL,"school_mountain_composite");
    add(Kind.WINDOW,"school_round_window");
    add(Kind.TOY,"school_toy_house_red","school_toy_house_blue","school_toy_house_yellow","school_toy_tower","school_toy_tree","school_toy_ball");
    add(Kind.COLUMN,"school_twisted_column","school_hall_column");
    add(Kind.RUG,"school_lobby_composite");add(Kind.CLOUD,"school_cloud_composite");
    add(Kind.ARCH,"school_arch_principal","school_arch_classroom","school_arch_health","school_arch_fortune","school_arch_market","school_arch_exit");
  }
  private KindergartenDecor025(){}
  private static void add(Kind kind,String... ids){for(String id:ids){var b=BLOCKS.register(id,()->{var properties=BlockBehaviour.Properties.of().strength(1).lightLevel(s->kind==Kind.CLOUD?15:id.equals("school_candelabra")?10:id.equals("school_crystal_ball")?5:0);if(kind!=Kind.WALL&&kind!=Kind.RUG&&kind!=Kind.CLOUD)properties.noOcclusion();if(id.equals("school_tool_cabinet"))properties.strength(2).sound(SoundType.METAL);return kind==Kind.DOOR?new SchoolDoorBlock(properties,id.equals("school_tool_cabinet")):kind==Kind.RUG||kind==Kind.CLOUD||kind==Kind.MURAL?new AtlasBlock(properties):kind==Kind.COLUMN?new KindergartenIonicColumn0254(properties):new DecorBlock(properties,kind,id);});blocks.put(id,b);items.put(id,ITEMS.register(id,()->new BlockItem(b.get(),new Item.Properties())));}}
  public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
  public static Block block(String id){var b=blocks.get(id);if(b==null)throw new IllegalArgumentException(id);return b.get();}
  public static BlockState state(String id,Direction facing){return block(id).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,facing);}
  public static BlockState atlas(String id,int tile,Direction facing){return state(id,facing).setValue(AtlasBlock.TILE,Math.max(0,Math.min(127,tile)));}
  public static List<String> ids(){return List.copyOf(blocks.keySet());}
  public static Item item(String id){return items.get(id).get();}
  /** Two finite registry IDs carry their large composed art through bounded native tile states. */
  public static final class AtlasBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty TILE=IntegerProperty.create("tile",0,127);
    private static final MapCodec<AtlasBlock> CODEC=simpleCodec(AtlasBlock::new);
    AtlasBlock(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(TILE,0));}
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,TILE);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
  }
  /** Native voxel slices keep selection, collision and the rendered leaves in the same cells. */
  public static final class SchoolDoorBlock extends HorizontalDirectionalBlock {
    public enum DoorSection implements StringRepresentable {
      LEAF, CENTER, TIP, FAR_TIP;
      @Override public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static final BooleanProperty OPEN=BlockStateProperties.OPEN;
    public static final EnumProperty<DoorHingeSide> HINGE=BlockStateProperties.DOOR_HINGE;
    public static final IntegerProperty SEGMENT=IntegerProperty.create("segment",0,2);
    public static final EnumProperty<DoorSection> SECTION=EnumProperty.create("section",DoorSection.class);
    public static final int PART_COUNT=21,CABINET_PART_COUNT=15;
    private static final ThreadLocal<Boolean> REMOVING=ThreadLocal.withInitial(()->false);
    // Build each fine curved outline once. Repeated Shapes.or would optimize
    // after every strip and turn ordinary registry baking into a long stall.
    private static final VoxelShape[][][][][] CURVED_SHAPES=curvedShapes();
    private static VoxelShape[][][][][] curvedShapes(){
      var cache=new VoxelShape[2][2][4][3][4];
      for(int hand=0;hand<2;hand++)for(int open=0;open<2;open++)for(int section=0;section<4;section++)for(int segment=0;segment<3;segment++)for(Direction face:Direction.Plane.HORIZONTAL){
        var members=com.chaogarden.core.SchoolDoorGeometry0255.boxes(hand==0,open==1,section,segment);
        var shapes=new VoxelShape[members.size()];int i=0;
        for(var b:members)shapes[i++]=rotatedBox(face,b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1());
        cache[hand][open][section][segment][face.get2DDataValue()]=curveUnion(shapes,0,shapes.length).optimize();
      }
      return cache;
    }
    private static VoxelShape curveUnion(VoxelShape[] boxes,int first,int limit){
      if(first==limit)return Shapes.empty();if(limit-first==1)return boxes[first];int middle=(first+limit)/2;
      return Shapes.joinUnoptimized(curveUnion(boxes,first,middle),curveUnion(boxes,middle,limit),net.minecraft.world.phys.shapes.BooleanOp.OR);
    }
    private static final MapCodec<SchoolDoorBlock> CODEC=simpleCodec(SchoolDoorBlock::new);
    private final boolean toolCabinet;
    SchoolDoorBlock(BlockBehaviour.Properties p){this(p,false);}
    SchoolDoorBlock(BlockBehaviour.Properties p,boolean toolCabinet){super(p);this.toolCabinet=toolCabinet;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(OPEN,false).setValue(HINGE,DoorHingeSide.LEFT).setValue(SEGMENT,0).setValue(SECTION,DoorSection.LEAF));}
    public boolean isToolCabinet(){return toolCabinet;}
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,OPEN,HINGE,SEGMENT,SECTION);}
    public static BlockPos leftBase(BlockPos center,Direction face){return center.relative(face.getClockWise());}
    public static BlockPos rightBase(BlockPos center,Direction face){return center.relative(face.getCounterClockWise());}
    public static BlockState part(Block b,Direction face,DoorHingeSide hinge,int segment,boolean open){return part(b,face,hinge,segment,open,DoorSection.LEAF);}
    public static BlockState part(Block b,Direction face,DoorHingeSide hinge,int segment,boolean open,DoorSection section){return b.defaultBlockState().setValue(FACING,face).setValue(HINGE,hinge).setValue(SEGMENT,segment).setValue(OPEN,open).setValue(SECTION,section);}
    /** Closed ornate leaves stay at the facing edge; their front-edge hinges swing forward. */
    public static Direction swingDirection(Block b,Direction face){return face;}
    public static int partCount(Block b){return b instanceof SchoolDoorBlock door&&door.toolCabinet?CABINET_PART_COUNT:PART_COUNT;}
    /** Stable order preserves the original roots and first tip; ornate leaves add a bounded far tip. */
    public static Map<BlockPos,BlockState> pairStates(Block b,BlockPos center,Direction face,boolean open){
      Map<BlockPos,BlockState> out=new LinkedHashMap<>();
      for(int i=0;i<3;i++){
        out.put(leftBase(center,face).above(i),part(b,face,DoorHingeSide.LEFT,i,open,DoorSection.LEAF));
        out.put(rightBase(center,face).above(i),part(b,face,DoorHingeSide.RIGHT,i,open,DoorSection.LEAF));
        out.put(center.above(i),part(b,face,DoorHingeSide.LEFT,i,open,DoorSection.CENTER));
        out.put(leftBase(center,face).relative(swingDirection(b,face)).above(i),part(b,face,DoorHingeSide.LEFT,i,open,DoorSection.TIP));
        out.put(rightBase(center,face).relative(swingDirection(b,face)).above(i),part(b,face,DoorHingeSide.RIGHT,i,open,DoorSection.TIP));
        if(!(b instanceof SchoolDoorBlock door&&door.toolCabinet)){
          out.put(leftBase(center,face).relative(face,2).above(i),part(b,face,DoorHingeSide.LEFT,i,open,DoorSection.FAR_TIP));
          out.put(rightBase(center,face).relative(face,2).above(i),part(b,face,DoorHingeSide.RIGHT,i,open,DoorSection.FAR_TIP));
        }
      }
      return out;
    }
    public static BlockPos pairCenter(BlockState state,BlockPos pos){
      BlockPos base=pos.below(state.getValue(SEGMENT));Direction face=state.getValue(FACING);
      if(state.getValue(SECTION)==DoorSection.CENTER)return base;
      if(state.getValue(SECTION)==DoorSection.TIP)base=base.relative(swingDirection(state.getBlock(),face).getOpposite());
      if(state.getValue(SECTION)==DoorSection.FAR_TIP)base=base.relative(face.getOpposite(),2);
      return base.relative(state.getValue(HINGE)==DoorHingeSide.LEFT?face.getCounterClockWise():face.getClockWise());
    }
    private static boolean matches(LevelAccessor level,Map<BlockPos,BlockState> parts,BlockPos removed){
      for(var part:parts.entrySet())if(!part.getKey().equals(removed)&&(!level.hasChunkAt(part.getKey())||!level.getBlockState(part.getKey()).equals(part.getValue())))return false;
      return true;
    }
    public boolean complete(LevelAccessor level,BlockPos pos,BlockState state){
      return matches(level,pairStates(this,pairCenter(state,pos),state.getValue(FACING),state.getValue(OPEN)),null);
    }
    /** Existing six-root pairs expand only when every newly owned cell is loaded and empty. */
    private boolean expandLegacy(Level level,BlockPos pos,BlockState state){
      if(state.getValue(SECTION)!=DoorSection.LEAF)return false;
      var parts=pairStates(this,pairCenter(state,pos),state.getValue(FACING),state.getValue(OPEN));
      for(var part:parts.entrySet()){
        if(!level.hasChunkAt(part.getKey()))return false;
        BlockState there=level.getBlockState(part.getKey());
        if(part.getValue().getValue(SECTION)==DoorSection.LEAF){if(!there.equals(part.getValue()))return false;}
        else if(!there.isAir())return false;
      }
      for(var part:parts.entrySet())if(part.getValue().getValue(SECTION)!=DoorSection.LEAF)level.setBlock(part.getKey(),part.getValue(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_IMMEDIATE);
      return true;
    }
    /** Exact saved fifteen-part layout: rear tips in0.25.7, forward tips before that. */
    private Map<BlockPos,BlockState> legacyTipStates(BlockPos center,Direction face,boolean open,boolean rear){
      var old=new LinkedHashMap<BlockPos,BlockState>();
      for(var part:pairStates(this,center,face,open).entrySet()){
        var section=part.getValue().getValue(SECTION);if(section==DoorSection.FAR_TIP)continue;
        old.put(section==DoorSection.TIP&&rear?part.getKey().relative(face.getOpposite(),2):part.getKey(),part.getValue());
      }
      return old;
    }
    private java.util.List<BlockPos> candidateCenters(BlockState state,BlockPos pos){
      var center=pairCenter(state,pos);
      return state.getValue(SECTION)==DoorSection.TIP&&!toolCabinet?java.util.List.of(center,center.relative(state.getValue(FACING),2)):java.util.List.of(center);
    }
    /** Conservative first-use migration reserves the full forward swing before removing old rear tips. */
    private BlockPos migrateLegacyTips(Level level,BlockPos pos,BlockState state){
      if(toolCabinet||state.getValue(SECTION)==DoorSection.FAR_TIP)return null;
      Direction face=state.getValue(FACING);
      for(var center:candidateCenters(state,pos))for(boolean rear:new boolean[]{true,false}){
        var old=legacyTipStates(center,face,state.getValue(OPEN),rear);
        if(!matches(level,old,null))continue;
        var current=pairStates(this,center,face,state.getValue(OPEN));boolean free=true;
        for(var part:current.entrySet())if(!level.hasChunkAt(part.getKey())||!old.containsKey(part.getKey())&&!level.getBlockState(part.getKey()).isAir()){free=false;break;}
        if(!free)continue;
        int flags=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_IMMEDIATE;
        REMOVING.set(true);
        try{
          for(var at:old.keySet())if(!current.containsKey(at))level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),flags);
          for(var part:current.entrySet())if(!old.containsKey(part.getKey()))level.setBlock(part.getKey(),part.getValue(),flags);
        }finally{REMOVING.set(false);}
        return center;
      }
      return null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit){
      BlockPos center=pairCenter(s,pos);
      if(!complete(level,pos,s)){
        var migrated=migrateLegacyTips(level,pos,s);
        if(migrated!=null)center=migrated;
        else if(!expandLegacy(level,pos,s))return InteractionResult.PASS;
      }
      boolean open=!s.getValue(OPEN);
      // All owned changes share the ordinary native client interaction prediction scope.
      // A canceled server use is reconciled by vanilla acknowledgement for every changed cell.
      int flags=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_IMMEDIATE;
      pairStates(this,center,s.getValue(FACING),open).forEach((at,state)->level.setBlock(at,state,flags));
      if(!level.isClientSide)level.playSound(null,pos,toolCabinet?(open?SoundEvents.IRON_DOOR_OPEN:SoundEvents.IRON_DOOR_CLOSE):(open?SoundEvents.WOODEN_DOOR_OPEN:SoundEvents.WOODEN_DOOR_CLOSE),SoundSource.BLOCKS,.7f,.95f);
      return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
      Direction face=c.getHorizontalDirection().getOpposite();BlockPos center=c.getClickedPos().relative(face.getCounterClockWise());
      if(center.getY()+2>=c.getLevel().getMaxBuildHeight())return null;
      // The invisible inactive slices reserve the real swing area, never corridor masonry.
      for(var at:pairStates(this,center,face,false).keySet())if(!c.getLevel().getBlockState(at).canBeReplaced(c))return null;
      return part(this,face,DoorHingeSide.LEFT,0,false);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState s,LivingEntity entity,ItemStack stack){
      pairStates(this,pairCenter(s,pos),s.getValue(FACING),false).forEach((at,state)->{if(!at.equals(pos))level.setBlock(at,state,Block.UPDATE_ALL);});
    }
    @Override protected void onRemove(BlockState s,Level level,BlockPos pos,BlockState next,boolean moved){
      if(!level.isClientSide&&!next.is(this)&&!REMOVING.get()){
        REMOVING.set(true);
        try{
          // Resolve the exact current or historical assembly, including a click on an old rear tip.
          Direction face=s.getValue(FACING);Map<BlockPos,BlockState> parts=null;
          for(var center:candidateCenters(s,pos)){
            var current=pairStates(this,center,face,s.getValue(OPEN));
            if(matches(level,current,pos)){parts=current;break;}
            if(!toolCabinet)for(boolean rear:new boolean[]{true,false}){
              var old=legacyTipStates(center,face,s.getValue(OPEN),rear);
              if(old.containsKey(pos)&&matches(level,old,pos)){parts=old;break;}
            }
            if(parts!=null)break;
            var roots=new LinkedHashMap<BlockPos,BlockState>();
            for(var entry:current.entrySet())if(entry.getValue().getValue(SECTION)==DoorSection.LEAF)roots.put(entry.getKey(),entry.getValue());
            if(roots.containsKey(pos)&&matches(level,roots,pos)){parts=roots;break;}
          }
          if(parts!=null)for(var part:parts.entrySet())
            if(!part.getKey().equals(pos)&&level.getBlockState(part.getKey()).equals(part.getValue()))level.setBlock(part.getKey(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        }finally{REMOVING.set(false);}
      }super.onRemove(s,level,pos,next,moved);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext context){
      boolean left=s.getValue(HINGE)==DoorHingeSide.LEFT,open=s.getValue(OPEN);DoorSection section=s.getValue(SECTION);
      int segment=s.getValue(SEGMENT);Direction face=s.getValue(FACING);
      if(!toolCabinet){
        return CURVED_SHAPES[left?0:1][open?1:0][section.ordinal()][segment][face.get2DDataValue()];
      }
      if(section==DoorSection.FAR_TIP)return Shapes.empty();
      double y0=toolCabinet&&segment==0?2:0,y1=toolCabinet&&segment==2?14:16;
      VoxelShape shape=Shapes.empty();
      if(section==DoorSection.CENTER){if(!open)shape=rotatedBox(face,0,y0,7,16,y1,9);}
      else if(section==DoorSection.TIP){if(open)shape=rotatedBox(face,left?0:14,y0,0,left?2:16,y1,15);}
      else if(open)shape=rotatedBox(face,left?0:14,y0,8,left?2:16,y1,16);
      else shape=rotatedBox(face,left?1:0,y0,7,left?16:15,y1,9);
      // This new block is a cabinet with a permanent, projecting metal carcass.
      // The back is intentionally open into the concealed utility passage.
      if(toolCabinet&&section!=DoorSection.TIP){
        if(segment==0)shape=Shapes.or(shape,rotatedBox(face,0,0,0,16,2,10));
        if(segment==2)shape=Shapes.or(shape,rotatedBox(face,0,14,0,16,16,10));
        if(section==DoorSection.LEAF)shape=Shapes.or(shape,rotatedBox(face,left?0:15,y0,0,left?1:16,y1,7));
      }
      return shape;
    }
    private static VoxelShape rotatedBox(Direction face,double x0,double y0,double z0,double x1,double y1,double z1){
      if(face==Direction.SOUTH)return box(x0,y0,z0,x1,y1,z1);
      if(face==Direction.NORTH)return box(16-x1,y0,16-z1,16-x0,y1,16-z0);
      if(face==Direction.WEST)return box(16-z1,y0,x0,16-z0,y1,x1);
      return box(z0,y0,16-x1,z1,y1,16-x0);
    }
    @Override protected boolean isPathfindable(BlockState s,PathComputationType type){return type!=PathComputationType.WATER&&(s.getValue(OPEN)||s.getValue(SECTION)==DoorSection.TIP||s.getValue(SECTION)==DoorSection.FAR_TIP);}
  }
  public static final class DecorBlock extends HorizontalDirectionalBlock {
    private static final MapCodec<DecorBlock> CODEC=simpleCodec(p->new DecorBlock(p,Kind.WALL,""));
    final Kind kind;
    final String id;
    DecorBlock(BlockBehaviour.Properties properties,Kind kind,String id){super(properties);this.kind=kind;this.id=id;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH));}
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
      return switch(kind){case WALL,RUG,CLOUD,MURAL->Shapes.block();case COUNTER->box(0,0,0,16,10,16);case DESK->state.getValue(FACING).getAxis()==Direction.Axis.X?box(0,0,-4,16,10,20):box(-4,0,0,20,10,16);case STOOL->box(3,0,3,13,8,13);case BED->switch(state.getValue(FACING)){
          case WEST->box(-16,0,0,16,15,16);case NORTH->box(0,0,-16,16,15,16);case EAST->box(0,0,0,32,15,16);default->box(0,0,0,16,15,32);};
        case CABINET->box(-4,0,1,20,32,15);case COLUMN->box(3,0,3,13,16,13);case ARCH->Shapes.empty();case PANEL->box(0,0,0,16,16,16);case DRAPE,LEAF->box(0,0,0,16,16,16);case WINDOW->switch(state.getValue(FACING)){
          case WEST->box(0,0,0,16,32,32);case EAST->box(0,0,-16,16,32,16);case NORTH->box(-16,0,0,16,32,16);default->box(0,0,0,32,32,16);};case CURTAIN->state.getValue(FACING).getAxis()==Direction.Axis.X?box(6,0,-16,10,16,32):box(-16,0,6,32,16,10);case SMALL->smallShape(state);case TOY->toyShape(state);case DOOR,GUARD->Shapes.empty();};
    }
    private VoxelShape toyShape(BlockState state){
      double[] p=com.chaogarden.core.ClassroomToyGeometry0258.bounds(id);
      return switch(state.getValue(FACING)){
        case NORTH->box(16-p[3],p[1],16-p[5],16-p[0],p[4],16-p[2]);
        case WEST->box(16-p[5],p[1],p[0],16-p[2],p[4],p[3]);
        case EAST->box(p[2],p[1],16-p[3],p[5],p[4],16-p[0]);
        default->box(p[0],p[1],p[2],p[3],p[4],p[5]);
      };
    }
    private VoxelShape smallShape(BlockState state){
      // These are the bounds of the actual scaled/rotated native mesh, including its tabletop offset.
      double[] p=smallBounds(id);double x0=p[0],z0=p[2],x1=p[3],z1=p[5];
      return switch(state.getValue(FACING)){
        case NORTH->box(16-x1,p[1],16-z1,16-x0,p[4],16-z0);
        case WEST->box(16-z1,p[1],x0,16-z0,p[4],x1);
        case EAST->box(z0,p[1],16-x1,z1,p[4],16-x0);
        default->box(x0,p[1],z0,x1,p[4],z1);
      };
    }
    public static double[] smallBounds(String id){return switch(id){
      case "school_crystal_ball"->new double[]{6.5,-6,6.5,9.5,-2.5,9.5};
      case "school_globe"->new double[]{5.9,-6,5.9,10.1,-.4,10.1};
      case "school_book_stack"->new double[]{5.6,-6,5.6,10.4,-2.4,9.6};
      case "school_open_book"->new double[]{5.043585,-5.6,5.6,10.956415,-4.005861,10};
      case "school_potion_display"->new double[]{5.2,-6,5.2,10.4,-2,10};
      case "school_candelabra"->new double[]{5.55,-6,6.25,10.45,-.05,9.75};
      case "school_quill"->new double[]{6.4,-6,6.4,9.683807,-.181857,9.6};
      case "school_bust"->new double[]{5.9,-6,5.9,10.1,.3,10.1};
      default->throw new IllegalArgumentException("Unknown small scenery: "+id);
    };}
    private boolean passThroughCollision(){return switch(kind){case ARCH,PANEL,DRAPE,LEAF->true;default->false;};}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return kind==Kind.GUARD?Shapes.block():passThroughCollision()?Shapes.empty():getShape(s,l,p,c);}
    @Override protected VoxelShape getVisualShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return kind==Kind.GUARD?Shapes.empty():super.getVisualShape(s,l,p,c);}
    @Override protected boolean isPathfindable(BlockState s,PathComputationType type){
      // Partial furniture boxes are solid land obstacles, while the genuinely empty decorations remain passages.
      return passThroughCollision()&&type!=PathComputationType.WATER;
    }
  }
}
