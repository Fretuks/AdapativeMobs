package net.fretux.adaptivemobs;

import com.mojang.logging.LogUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.fretux.adaptivemobs.event.AdaptiveMobsEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(AdaptiveMobs.MODID)
public class AdaptiveMobs {

    public static final String MODID = "adaptivemobs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdaptiveMobs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AMConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new AdaptiveMobsEvents());
    }
}
