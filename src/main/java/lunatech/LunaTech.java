package lunatech;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * LunaTech — a GregTech 5 Unofficial addon for GTNH 2.9.0-beta2.
 * <p>
 * Milestone M1 deliberately does nothing but load. Its purpose is to prove the toolchain, the
 * dependency pin, and the validation harness before any content exists. See SCOPE.md.
 */
@Mod(
    modid = LunaTech.MOD_ID,
    name = LunaTech.MOD_NAME,
    version = Tags.VERSION,
    dependencies = "required-after:gregtech")
public class LunaTech {

    public static final String MOD_ID = "lunatech";
    public static final String MOD_NAME = "LunaTech";

    public static final Logger LOG = LogManager.getLogger(MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("{} {} loading against GregTech 5.09.52.594.", MOD_NAME, Tags.VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {}

    /**
     * Post-init is where recipe replacement will eventually run, because it must execute after
     * GTNH has finished registering. Nothing is replaced yet — see AUDIT.md for what is queued and
     * SCOPE.md section 6 for the rules any replacement has to satisfy first.
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {}
}
