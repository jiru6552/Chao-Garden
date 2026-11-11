package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Regress the changed floor geometry through actual natural approach and play. */
@GameTestHolder("chao_floor49") @PrefixGameTestTemplate(false)
public final class FloorToyGameTests049 {
  private static final List<String> TOYS=List.of("chao_accordion","chao_guitar","chao_triangle","chao_bubble_wand","chao_castanets");
  private static final Set<String> LESSON_TOYS=Set.of("chao_accordion","chao_guitar","chao_triangle","chao_castanets");
  private static void cleanup(GameTestHelper h,ChaoEntity c){h.testInfo.addListener(new GameTestListener(){
    private void release(){ToyContent.endPlaying(c,false);c.discard();}
    public void testStructureLoaded(GameTestInfo info){}
    public void testPassed(GameTestInfo info,GameTestRunner runner){release();}
    public void testFailed(GameTestInfo info,GameTestRunner runner){release();}
    public void testAddedForRerun(GameTestInfo oldInfo,GameTestInfo newInfo,GameTestRunner runner){release();}
  });}
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var tests=new ArrayList<TestFunction>();
    for(String kind:TOYS)for(boolean adult:new boolean[]{false,true})
      for(Direction facing:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
        String name=kind.substring(5)+"_flat_natural_"+(adult?"adult":"run0_baby")+"_"+facing.getName();
        tests.add(new TestFunction("floor049_"+kind,name,"chao_floor49:activity_test",1800,0,true,h->natural(h,kind,adult,facing)));
      }
    return tests;
  }
  private static void natural(GameTestHelper h,String kind,boolean adult,Direction facing){
    var level=h.getLevel();BlockPos at=RattleGameTests02520.floor(h);
    level.setBlock(at,ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);
    var c=ChaoGarden.CHAO.get().create(level);h.assertTrue(c!=null,"real Chao creates");
    var state=new ChaoState();state.adult=adult;state.primaryType=ChaoState.NORMAL;
    state.points[ChaoState.RUN]=adult?667:0;state.happiness=20;state.hunger=90;state.energy=100;state.boredom=100;
    state.personality=ChaoPreferences.EASILY_BORED;
    if(LESSON_TOYS.contains(kind))state.setTraitInt("learned."+kind.substring(5),1);
    state.setTrait("floor49.unrelated","retain-this-value");c.setState(state);c.setInvulnerable(true);c.getRandom().setSeed(18);
    // Always start west of the source. With the north-facing guitar/wand this
    // approaches their short axis and exercises contact inside the toy's cell.
    Vec3 origin=Vec3.atBottomCenterOf(at.relative(Direction.WEST,3));c.moveTo(origin.x,origin.y,origin.z,Direction.EAST.toYRot(),0);
    level.addFreshEntity(c);c.setOnGround(true);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));cleanup(h,c);
    long[] began={-1},ended={-1};int[] playFrames={0};double[] travelled={0};Vec3[] previous={c.position()};
    h.onEachTick(()->{
      h.assertTrue(c.state().adult==adult&&c.state().points[ChaoState.RUN]==(adult?667:0),"fixture keeps the real age and Run milestone");
      h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"floor presentation never creates an inventory copy");
      h.assertTrue(c.getY()<=at.getY()+.25,"flat-toy approach does not jump or climb above the low model: "+c.position());
      var source=level.getBlockState(at);h.assertTrue(source.is(ToyContent.block(kind)),"the one recoverable floor source remains placed");
      boolean playing=ToyContent.isPlaying(c);
      if(began[0]<0&&!playing){travelled[0]+=c.position().subtract(previous[0]).horizontalDistance();previous[0]=c.position();}
      if(playing){
        if(began[0]<0)began[0]=c.getPersistentData().getLong("ChaoToyUntil")-160;
        h.assertTrue(kind.equals(c.toyPlayingKind())&&at.equals(c.toyTarget()),"natural play owns the intended flat source");
        h.assertTrue(source.getValue(ChaoToyBlock.PLAYING)&&source.getRenderShape()==RenderShape.INVISIBLE,"source is hidden during its actual held presentation");
        h.assertTrue(level.noCollision(c,c.getBoundingBox()),"body remains clear of the flattened source collision");
        h.assertTrue(Math.abs(c.getY()-at.getY())<.08,"performer stays on the ground beside the floor toy");
        playFrames[0]++;
      }else{
        if(began[0]>=0&&ended[0]<0)ended[0]=level.getGameTime();
        // The terminal deadline can be observed just before the entity's goal
        // clears PLAYING on the same tick; allow that one native update only.
        if(began[0]<0||level.getGameTime()>began[0]+160)
          h.assertTrue(source.getRenderShape()==RenderShape.MODEL,"idle source is never left invisible outside a play session");
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(began[0]>=0&&level.getGameTime()-began[0]>=180,
          "observe the natural approach and complete phrase: kind="+kind+" adult="+adult+" facing="+facing+" pos="+c.position()+" navDone="+c.getNavigation().isDone()+" velocity="+c.getDeltaMovement());
      h.assertTrue(travelled[0]>.35,"actual navigation approaches the toy rather than starting already at contact");
      h.assertTrue(ended[0]-began[0]==160&&playFrames[0]>=158,"the production phrase lasts160ticks and does not cancel early");
      h.assertTrue(!ToyContent.isPlaying(c)&&c.toyTarget()==null,"completion releases clock and reservation");
      h.assertTrue(c.state().happiness==24,"one ordinary completion reward only");
      h.assertTrue(c.getPersistentData().getLong("ChaoToyNextPlay")>level.getGameTime(),"the rest cooldown survives completion");
      h.assertTrue(c.state().trait("floor49.unrelated").equals("retain-this-value"),"unrelated saved data remains intact");
      var source=level.getBlockState(at);
      h.assertTrue(source.getRenderShape()==RenderShape.MODEL&&!source.getValue(ChaoToyBlock.PLAYING)&&source.getValue(ChaoToyBlock.FRAME)==0,"one visible idle flat model returns");
      h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4),e->e.getItem().is(ChaoGarden.item(kind))).isEmpty(),"no duplicate toy drops or inventory props");
    });
  }
  @GameTest(template="empty",timeoutTicks=100,batch="floor049_runtime_recipes")
  public static void newToyRecipesResolveThroughActualRecipeManager(GameTestHelper h){
    var recipes=h.getLevel().getRecipeManager();
    for(String kind:List.of("chao_toy_car","chao_sonic_doll","chao_shadow_doll","chao_broom","chao_picture_book","chao_pogo_stick","chao_crayons")){
      var key=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,kind);
      h.assertTrue(recipes.byKey(key).isPresent(),"actual runtime recipe codec and ingredient registry resolve "+key);
    }
    h.succeed();
  }
}
