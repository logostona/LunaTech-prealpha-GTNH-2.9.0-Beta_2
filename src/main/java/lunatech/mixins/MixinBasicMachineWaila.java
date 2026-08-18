package lunatech.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import lunatech.units.SiFormat;
import lunatech.units.Units;

/**
 * Converts a single machine's Waila power line to watts. See UNITS.md section 7.1.
 * <p>
 * Only calls 0 and 1 inside {@code getWailaBody} are EU/t. Calls 2 and 3 format {@code mEUt * 40},
 * which is a steam flow in litres per second rather than a power, and are deliberately left alone —
 * converting them would state watts for a volumetric rate.
 */
@Mixin(value = MTEBasicMachine.class, remap = false)
public abstract class MixinBasicMachineWaila {

    @Redirect(
        method = "getWailaBody",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/gtnhlib/util/numberformatting/NumberFormatUtil;formatNumber(Ljava/lang/Number;)Ljava/lang/String;",
            ordinal = 0),
        remap = false)
    private String lunatech$usageAsWatts(Number euPerTick) {
        return SiFormat.power(Units.watts(euPerTick.longValue()));
    }

    @Redirect(
        method = "getWailaBody",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/gtnhlib/util/numberformatting/NumberFormatUtil;formatNumber(Ljava/lang/Number;)Ljava/lang/String;",
            ordinal = 1),
        remap = false)
    private String lunatech$productionAsWatts(Number euPerTick) {
        return SiFormat.power(Units.watts(euPerTick.longValue()));
    }
}
