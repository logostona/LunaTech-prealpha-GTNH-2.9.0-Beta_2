package lunatech.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import lunatech.units.SiFormat;
import lunatech.units.Units;

/**
 * Converts a multiblock's Waila power lines to watts. See UNITS.md section 7.1.
 * <p>
 * Calls 0 to 3 inside {@code getWailaBody} are the four energy-rate branches: use and produce, each
 * with and without amperage. Later calls in the same method format item counts, fluid counts and an
 * average tick time, none of which are powers, so the redirects are bounded by ordinal.
 */
@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MixinMultiBlockWaila {

    @Redirect(
        method = "getWailaBody",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/gtnhlib/util/numberformatting/NumberFormatUtil;formatNumber(Ljava/lang/Number;)Ljava/lang/String;",
            ordinal = 0),
        remap = false)
    private String lunatech$useWithAmperageAsWatts(Number euPerTick) {
        return SiFormat.power(Units.watts(euPerTick.longValue()));
    }

    @Redirect(
        method = "getWailaBody",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/gtnhlib/util/numberformatting/NumberFormatUtil;formatNumber(Ljava/lang/Number;)Ljava/lang/String;",
            ordinal = 1),
        remap = false)
    private String lunatech$produceWithAmperageAsWatts(Number euPerTick) {
        return SiFormat.power(Units.watts(euPerTick.longValue()));
    }

    @Redirect(
        method = "getWailaBody",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/gtnhlib/util/numberformatting/NumberFormatUtil;formatNumber(Ljava/lang/Number;)Ljava/lang/String;",
            ordinal = 2),
        remap = false)
    private String lunatech$useAsWatts(Number euPerTick) {
        return SiFormat.power(Units.watts(euPerTick.longValue()));
    }

    @Redirect(
        method = "getWailaBody",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/gtnhlib/util/numberformatting/NumberFormatUtil;formatNumber(Ljava/lang/Number;)Ljava/lang/String;",
            ordinal = 3),
        remap = false)
    private String lunatech$produceAsWatts(Number euPerTick) {
        return SiFormat.power(Units.watts(euPerTick.longValue()));
    }
}
