package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

/** Excluded diagnostic: cross-namespace attach is unavailable in this build host. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class GameTestShutdownWatchdog025 {
  private static volatile boolean stopped;
  private GameTestShutdownWatchdog025(){}
  @SubscribeEvent public static void stopping(ServerStoppingEvent event){
    if(!GameTestHooks.isGametestServer())return;
    stopped=false;
    var observer=new Thread(()->{
      try{
        Thread.sleep(15_000);
        if(stopped)return;
        var text=new StringBuilder("GameTest shutdown pending after15 seconds; wallClockMillis=")
            .append(System.currentTimeMillis()).append('\n');
        for(var info:ManagementFactory.getThreadMXBean().dumpAllThreads(true,true)){
          text.append('"').append(info.getThreadName()).append("\" ").append(info.getThreadState())
              .append(" lock=").append(info.getLockName()).append(" owner=").append(info.getLockOwnerName()).append('\n');
          for(var frame:info.getStackTrace())text.append("    at ").append(frame).append('\n');
          text.append('\n');
        }
        Files.writeString(Path.of("shutdown-thread-dump025.txt"),text.toString());
        System.err.println(text);
      }catch(InterruptedException ignored){Thread.currentThread().interrupt();}
       catch(Exception problem){System.err.println("GameTest shutdown diagnostic failed: "+problem);}
    },"Chao GameTest shutdown diagnostic");
    observer.setDaemon(true);observer.start();
  }
  @SubscribeEvent public static void stopped(ServerStoppedEvent event){stopped=true;}
}
