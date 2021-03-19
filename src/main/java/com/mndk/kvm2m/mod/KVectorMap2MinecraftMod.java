package com.mndk.kvm2m.mod;

import java.io.File;

import org.apache.logging.log4j.Logger;

import com.mndk.kvm2m.mod.commands.DxfMapGeneratorCommand;
import com.mndk.kvm2m.mod.commands.NgiMapGeneratorCommand;

import net.minecraft.command.ICommand;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = KVectorMap2MinecraftMod.MODID, version = KVectorMap2MinecraftMod.VERSION, acceptableRemoteVersions = "*")
public class KVectorMap2MinecraftMod {

    public static final String MODID = "kvm2m";
    public static final String VERSION = "b1.0";

    private static final ICommand[] serverCommands = {
    		new DxfMapGeneratorCommand(),
    		new NgiMapGeneratorCommand()
    };

    public static Logger logger;
    
    public static String dxfFileDirectory;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        KVectorMap2MinecraftMod.logger = event.getModLog();
    }
    
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
    	
        this.registerCommands(event);
        this.initializeMapDirectory();
        
    }
    
    private void registerCommands(FMLServerStartingEvent event) {
    	for(ICommand command : serverCommands) {
    		event.registerServerCommand(command);
    	}
    }
    
    private void initializeMapDirectory() {
    	dxfFileDirectory = DimensionManager.getCurrentSaveRootDirectory().getAbsolutePath() + "/kvecmap_files/";
        File temp = new File(dxfFileDirectory);
        if(!temp.isDirectory()) {
        	temp.mkdirs();
        }
    }

}
