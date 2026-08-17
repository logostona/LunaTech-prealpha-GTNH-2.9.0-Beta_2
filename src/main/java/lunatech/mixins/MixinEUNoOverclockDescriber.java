package lunatech.mixins;

import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.objects.overclockdescriber.EUNoOverclockDescriber;
import gregtech.api.util.OverclockCalculator;
import lunatech.units.SiFormat;
import lunatech.units.Units;

/**
 * Converts NEI's recipe power lines to watts. See UNITS.md section 7.1.
 * <p>
 * This is a different code path from the machine tooltip: NEI builds its lines here, formatting the
 * raw EU/t into a lang key, so neither the language override nor the {@code TooltipHelper} mixin
 * reaches it.
 * <p>
 * Targeting this one class covers the whole EU describer chain, since
 * {@code EUOverclockDescriber} and {@code FusionOverclockDescriber} inherit these methods rather
 * than overriding them. The lang keys are overridden to carry no unit of their own, because the
 * unit now travels with the value.
 */
@Mixin(value = EUNoOverclockDescriber.class, remap = false)
public abstract class MixinEUNoOverclockDescriber {

    @Shadow
    protected abstract boolean shouldShowAmperage(OverclockCalculator calculator);

    @Shadow
    protected abstract String getTierNameWithParentheses(long voltage, OverclockCalculator calculator);

    @Shadow
    protected abstract long computeVoltageForEURate(long euPerTick);

    @Inject(
        method = "getEUtDisplay(Lgregtech/api/util/OverclockCalculator;)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void lunatech$usageInWatts(OverclockCalculator calculator, CallbackInfoReturnable<String> cir) {
        long euPerTick = calculator.getConsumption();
        boolean amperageShown = shouldShowAmperage(calculator);
        String tier = amperageShown ? "" : getTierNameWithParentheses(euPerTick, calculator);
        String power = SiFormat.power(Units.watts(euPerTick));
        cir.setReturnValue(StatCollector.translateToLocalFormatted("GT5U.nei.display.usage", power, tier));
    }

    /**
     * GregTech calls this "voltage", but the quantity is EU/t divided by amperage — a power per
     * amp, not a potential. It is converted to watts and relabelled rather than being given a unit
     * it does not have; naming a real voltage depends on D11.
     */
    @Inject(
        method = "getVoltageString(Lgregtech/api/util/OverclockCalculator;)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void lunatech$voltageInWatts(OverclockCalculator calculator, CallbackInfoReturnable<String> cir) {
        long euPerTick = calculator.getConsumption();
        long perAmp = computeVoltageForEURate(euPerTick);
        String power = SiFormat.power(Units.watts(perAmp));
        String tier = getTierNameWithParentheses(perAmp, calculator);
        cir.setReturnValue(StatCollector.translateToLocalFormatted("GT5U.nei.display.voltage", power, tier));
    }
}
