package lunatech.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
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
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.misc.GTStructureChannels;
import lunatech.data.Datasets;
import lunatech.data.Reaction;
import lunatech.kinetics.Arrhenius;
import lunatech.kinetics.BatchAmount;
import lunatech.kinetics.ContinuousReactor;
import lunatech.kinetics.ReactionBatch;

/**
 * The Continuous Flow Reactor: objective O4, milestone M2.
 * <p>
 * Its coil ring sets the reactor temperature, and the kinetics in {@link ContinuousReactor} turn
 * that plus a residence time into conversion. Output therefore depends on how the machine is
 * operated rather than on a fixed recipe duration.
 * <p>
 * Processing overrides {@code checkProcessing} rather than supplying a recipe map, because a
 * rate-driven reactor has no recipes: one feed and one temperature give a continuum of outcomes.
 * Each batch consumes its reactants whole, produces the converted fraction, and returns the
 * unconverted remainder.
 */
public class MTEContinuousFlowReactor extends MTEExtendedPowerMultiBlockBase<MTEContinuousFlowReactor>
    implements ISurvivalConstructable {

    /** The reaction this reactor runs. Selecting one in world is a later refinement. */
    private static final String DEMONSTRATION_REACTION = "water_gas_shift";

    /**
     * Declared machine power, EU/t.
     * <p>
     * This is <em>not</em> a thermodynamic minimum, and objective O2 cannot yet be checked against
     * it. Doing so needs the reaction enthalpy and the heat capacity of every reactant, and the
     * dataset carries neither -- only iron has Cp(T), and no reaction carries a heat of reaction.
     * Until those exist this is an honest placeholder rather than a derived figure, and AUDIT.md
     * and SCOPE.md both say so.
     */
    private static final int DECLARED_EU_PER_TICK = 480;

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
                        new String[][] { { "CCC", "CCC", "CCC" }, { "C~C", "H-H", "HHH" }, { "CCC", "CCC", "CCC" }, }))
                .addElement(
                    'C',
                    buildHatchAdder(MTEContinuousFlowReactor.class)
                        .atLeast(InputHatch, OutputHatch, Maintenance, Energy)
                        .casingIndex(Casings.ChemicallyInertMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(x -> ++x.mCasing, Casings.ChemicallyInertMachineCasing.asElement())))
                .addElement(
                    'H',
                    GTStructureChannels.HEATING_COIL.use(
                        activeCoils(
                            ofCoil(MTEContinuousFlowReactor::setCoilLevel, MTEContinuousFlowReactor::getCoilLevel))))
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

    /**
     * Runs one batch. Conversion comes from the coil temperature and the reaction's residence time,
     * so a hotter reactor converts more of the same feed rather than running a different recipe.
     * <p>
     * Nothing is consumed until every reactant has been confirmed available, so a partial feed
     * cannot leave the reactor having eaten one input and not the other.
     */
    @Override
    public CheckRecipeResult checkProcessing() {
        HeatingCoilLevel level = getCoilLevel();
        if (level == null || level == HeatingCoilLevel.None) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        Reaction reaction = Datasets.reaction(DEMONSTRATION_REACTION);
        if (reaction.referencePoint == null || reaction.basis == null) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        double kelvin = level.getHeat();
        double residence = reaction.referencePoint.residenceTimeSeconds;
        double conversion = ContinuousReactor.conversion(reaction, kelvin, residence);

        List<FluidStack> feed = resolve(ReactionBatch.feed(reaction));
        List<FluidStack> products = resolve(ReactionBatch.outputs(reaction, conversion));
        if (feed == null || products == null || feed.isEmpty()) {
            // A material name that does not resolve is a defect in our own data, not a player
            // error, and validateReactionMaterials in LunaTech should already have refused to load.
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        for (FluidStack required : feed) {
            if (!depleteInput(required, true)) {
                return CheckRecipeResultRegistry.NO_RECIPE;
            }
        }
        for (FluidStack required : feed) {
            depleteInput(required);
        }

        mOutputFluids = products.toArray(new FluidStack[products.size()]);
        mEfficiency = 10000 - (getIdealStatus() - getRepairStatus()) * 1000;
        mEfficiencyIncrease = 10000;
        mMaxProgresstime = (int) Math.max(1.0d, residence * 20.0d);
        mEUt = -DECLARED_EU_PER_TICK;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    /** @return the amounts as GregTech fluids, or null if any material fails to resolve. */
    private static List<FluidStack> resolve(List<BatchAmount> amounts) {
        List<FluidStack> stacks = new ArrayList<FluidStack>();
        for (BatchAmount amount : amounts) {
            FluidStack stack = fluidFor(amount);
            if (stack == null) {
                return null;
            }
            stacks.add(stack);
        }
        return stacks;
    }

    /**
     * Resolves one amount to a GregTech fluid.
     * <p>
     * {@code Materials.get} is annotated non-null and falls back to {@code Materials._NULL} for an
     * unknown name, so a null check alone would never fire and a typo would silently produce
     * nothing. The sentinel has to be compared explicitly.
     */
    static FluidStack fluidFor(BatchAmount amount) {
        Materials material = Materials.get(amount.material);
        if (material == null || material == Materials._NULL) {
            return null;
        }
        if (amount.millibuckets <= 0L) {
            return null;
        }
        return amount.isGas() ? material.getGas(amount.millibuckets) : material.getFluid(amount.millibuckets);
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }
}
