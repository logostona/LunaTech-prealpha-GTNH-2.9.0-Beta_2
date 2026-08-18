package lunatech.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE;
import static gregtech.api.util.GTStructureUtility.activeCoils;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofCoil;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.misc.GTStructureChannels;
import lunatech.data.Datasets;
import lunatech.data.Reaction;
import lunatech.kinetics.Arrhenius;
import lunatech.kinetics.ContinuousReactor;

/**
 * The Continuous Flow Reactor: objective O4, milestone M2.
 * <p>
 * Its coil ring sets the reactor temperature, and the kinetics in {@link ContinuousReactor} turn
 * that plus a residence time into conversion. Output therefore depends on how the machine is
 * operated rather than on a fixed recipe duration.
 * <p>
 * <b>This stage forms and reports; it does not yet process.</b> The GregTech processing pipeline is
 * built around a recipe map and a rate-driven reactor is not, so custom processing is deliberately
 * left to a later stage rather than bolted onto a mechanism that does not fit it. What is wired now
 * is the physics readout: the info panel shows the temperature the coils give, the Arrhenius rate
 * multiplier at that temperature, and the conversion it implies.
 */
public class MTEContinuousFlowReactor extends MTEExtendedPowerMultiBlockBase<MTEContinuousFlowReactor>
    implements ISurvivalConstructable {

    /** The reaction this stage reports on. Selecting one in world arrives with processing. */
    private static final String DEMONSTRATION_REACTION = "water_gas_shift";

    private static IStructureDefinition<MTEContinuousFlowReactor> STRUCTURE_DEFINITION = null;

    private int mCasing;

    private HeatingCoilLevel coilLevel;

    public MTEContinuousFlowReactor(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEContinuousFlowReactor(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEContinuousFlowReactor(this.mName);
    }

    public HeatingCoilLevel getCoilLevel() {
        return coilLevel;
    }

    public void setCoilLevel(HeatingCoilLevel level) {
        coilLevel = level;
    }

    @Override
    public IStructureDefinition<MTEContinuousFlowReactor> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = StructureDefinition.<MTEContinuousFlowReactor>builder()
                .addShape(
                    mName,
                    transpose(
                        new String[][] { { "CCC", "CCC", "CCC" }, { "C~C", "H-H", "HHH" },
                            { "CCC", "CCC", "CCC" }, }))
                .addElement(
                    'C',
                    buildHatchAdder(MTEContinuousFlowReactor.class)
                        .atLeast(InputHatch, OutputHatch, Maintenance, Energy)
                        .casingIndex(Casings.ChemicallyInertMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(Casings.ChemicallyInertMachineCasing.asElement()))
                .addElement(
                    'H',
                    GTStructureChannels.HEATING_COIL.use(
                        activeCoils(
                            ofCoil(
                                MTEContinuousFlowReactor::setCoilLevel,
                                MTEContinuousFlowReactor::getCoilLevel))))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCasing = 0;
        setCoilLevel(HeatingCoilLevel.None);
        if (!checkPiece(mName, 1, 1, 0, errors)) return;
        checkCasingMin(errors, mCasing, 12);
        checkHasMaintenanceHatch(errors);
        checkHasEnergyHatch(errors);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 1, 1, 0, elementBudget, env, false, true);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 1, 1, 0);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Continuous Flow Reactor, CFR")
            .addInfo("Conversion follows Arrhenius kinetics, not a fixed recipe duration")
            .addInfo("Coil tier sets the reactor temperature; rate rises exponentially with it")
            .addInfo("Rate is capped at 64x, where a process stops being limited by chemistry")
            .addInfo("and becomes limited by heat and mass transfer instead")
            .beginStructureBlock(3, 3, 3, true)
            .addController("Front centre")
            .addCasingInfoMin("Chemically Inert Machine Casing", 12, false)
            .addOtherStructurePart("Heating Coils", "Five, forming the reactor volume")
            .addEnergyHatch("Any casing", 1)
            .addMaintenanceHatch("Any casing", 1)
            .addInputHatch("Any casing", 1)
            .addOutputHatch("Any casing", 1)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture casing = Casings.ChemicallyInertMachineCasing.getCasingTexture();
        if (side != aFacing) {
            return new ITexture[] { casing };
        }
        ITexture overlay = TextureFactory.builder()
            .addIcon(aActive ? OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE : OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR)
            .extFacing()
            .build();
        return new ITexture[] { casing, overlay };
    }

    /**
     * Reports the kinetics the coils imply. This readout is what connects AUDIT.md A5 to something
     * a player can actually see.
     */
    @Override
    public String[] getInfoData() {
        List<String> lines = new ArrayList<String>();
        String[] inherited = super.getInfoData();
        for (String line : inherited) {
            lines.add(line);
        }

        HeatingCoilLevel level = getCoilLevel();
        if (level == null || level == HeatingCoilLevel.None) {
            lines.add("Reactor temperature: no coils detected");
            return lines.toArray(new String[lines.size()]);
        }

        double kelvin = level.getHeat();
        Reaction reaction = Datasets.reaction(DEMONSTRATION_REACTION);
        double reference = reaction.referenceTemperature.value;
        double rate = Arrhenius.rateMultiplier(reaction.activationEnergy.value, reference, kelvin);
        double residence = reaction.referencePoint.residenceTimeSeconds;
        double conversion = ContinuousReactor.conversion(reaction, kelvin, residence);

        lines.add("Reactor temperature: " + (long) kelvin + " K");
        lines.add("Reaction: " + reaction.id + ", reference " + (long) reference + " K");
        lines.add("Rate multiplier: " + Math.round(rate * 10.0d) / 10.0d + "x");
        lines.add("Conversion at " + (long) residence + " s: " + Math.round(conversion * 1000.0d) / 10.0d + "%");
        if (rate >= Arrhenius.DEFAULT_MAX_MULTIPLIER) {
            // Worth surfacing rather than hiding: GregTech coil temperatures sit far above the
            // reference temperature of any ordinary industrial reaction, so every modelled reaction
            // pins against the cap. That is AUDIT.md A1 showing through A5.
            lines.add("Rate capped: coil temperature far exceeds the regime for this reaction");
        }
        return lines.toArray(new String[lines.size()]);
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }
}
