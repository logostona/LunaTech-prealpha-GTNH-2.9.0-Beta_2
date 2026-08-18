package lunatech.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import gregtech.api.metatileentity.implementations.MTEBasicBatteryBuffer;
import lunatech.units.SiFormat;
import lunatech.units.Units;

/**
 * Converts the Waila in-world tooltip's average input and output to watts. See UNITS.md section 7.1.
 * <p>
 * Waila is a fourth display surface, independent of the item tooltip and NEI. The value reaches the
 * lang key through {@code formatNumber}, so the redirect below is ordinal-sensitive: within
 * {@code getWailaBody} calls 0 and 1 format stored and maximum energy — which need no arithmetic at
 * κ = 1 and are relabelled in the lang file — while calls 2 and 3 are the EU/t rates.
 */
@Mixin(value = MTEBasicBatteryBuffer.class, remap = false)
public abstract class MixinBatteryBufferWaila {

    @Redirect(
        method = "getWailaBody",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/gtnhlib/util/numberformatting/NumberFormatUtil;formatNumber(Ljava/lang/Number;)Ljava/lang/String;",
            ordinal = 2),
        remap = false)
    private String lunatech$averageInputAsWatts(Number euPerTick) {
        return SiFormat.power(Units.watts(euPerTick.longValue()));
    }

    @Redirect(
        method = "getWailaBody",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/gtnhlib/util/numberformatting/NumberFormatUtil;formatNumber(Ljava/lang/Number;)Ljava/lang/String;",
            ordinal = 3),
        remap = false)
    private String lunatech$averageOutputAsWatts(Number euPerTick) {
        return SiFormat.power(Units.watts(euPerTick.longValue()));
    }
}
