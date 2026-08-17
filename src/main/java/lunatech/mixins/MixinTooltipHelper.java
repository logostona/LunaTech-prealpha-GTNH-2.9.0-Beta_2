package lunatech.mixins;

import net.minecraft.util.EnumChatFormatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.util.tooltip.TooltipHelper;
import lunatech.units.SiFormat;
import lunatech.units.Units;

/**
 * Converts GregTech's tooltip energy and power text to SI. See UNITS.md section 7.1.
 * <p>
 * These two strings cannot be reached by a language-file override, because the unit is appended in
 * Java rather than living in the lang value. This is the first place LunaTech modifies GregTech
 * behaviour rather than adding to it, and the reason D2 is no longer deferred.
 * <p>
 * {@code remap = false} throughout: GregTech is a mod, not obfuscated Minecraft, so its names are
 * already correct at runtime and must not be run through the SRG mapping.
 */
@Mixin(value = TooltipHelper.class, remap = false)
public abstract class MixinTooltipHelper {

    /**
     * Capacity is a plain energy quantity, so at κ = 1 the number is already joules and only the
     * label changes.
     */
    @Inject(
        method = "euCapacityText(J)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private static void lunatech$capacityInJoules(long capacity, CallbackInfoReturnable<String> cir) {
        String text = SiFormat.energy(Units.joules(capacity));
        cir.setReturnValue(TooltipHelper.CAPACITY_COLOR + text + TooltipHelper.EU_AMOUNT_COLOR);
    }

    /**
     * A rate is not a plain relabelling: watts are EU/t x 20, so the displayed number changes too.
     * This is exactly why the language-file pass deliberately left every EU/t key alone.
     */
    @Inject(
        method = "euRateText(J)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private static void lunatech$rateInWatts(long euPerTick, CallbackInfoReturnable<String> cir) {
        String text = SiFormat.power(Units.watts(euPerTick));
        cir.setReturnValue(TooltipHelper.EU_VOLT_COLOR + text + EnumChatFormatting.GRAY);
    }
}
