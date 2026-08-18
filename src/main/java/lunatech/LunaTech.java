package lunatech;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import lunatech.machines.MTEContinuousFlowReactor;

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

    /** Held so the controller item can be handed to recipes and the creative tab later. */
    public static MTEContinuousFlowReactor continuousFlowReactor;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("{} {} loading against GregTech 5.09.52.594.", MOD_NAME, Tags.VERSION);
    }

    /**
     * Registers LunaTech's machines. Metatile ids are claimed through {@link LunaTechIDs}, which
     * refuses to overwrite a slot another mod already holds -- ids live in world data, so a silent
     * collision would rewrite someone else machines on the next load.
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        int id = claimMetaTileId(LunaTechIDs.CONTINUOUS_FLOW_REACTOR);
        continuousFlowReactor = new MTEContinuousFlowReactor(
            id,
            "lunatech.reactor.continuous",
            "Continuous Flow Reactor");
        LOG.info("Registered Continuous Flow Reactor at metatile id {}.", Integer.valueOf(id));
    }

    /**
     * Verifies a metatile id is both inside LunaTech's reserved block and not already taken.
     * <p>
     * The scan that chose the block could only see GregTech addons bundled in the GregTech jar, so
     * this is the check that catches anything else in the pack. It refuses rather than overwrites:
     * ids live in world data, and quietly taking an occupied slot would turn another mod machines
     * into ours the next time the world loads.
     */
    private static int claimMetaTileId(int id) {
        LunaTechIDs.requireReserved(id);
        IMetaTileEntity existing = GregTechAPI.METATILEENTITIES[id];
        if (existing != null) {
            throw new IllegalStateException(
                "Metatile id " + id
                    + " is already registered by "
                    + existing.getClass()
                        .getName()
                    + "; LunaTech will not overwrite it because ids are stored in world data");
        }
        return id;
    }

    /**
     * Post-init is where recipe replacement will eventually run, because it must execute after
     * GTNH has finished registering. Nothing is replaced yet — see AUDIT.md for what is queued and
     * SCOPE.md section 6 for the rules any replacement has to satisfy first.
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {}
}
