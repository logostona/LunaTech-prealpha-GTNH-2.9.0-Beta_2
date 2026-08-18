package lunatech.kinetics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lunatech.data.Reaction;
import lunatech.data.ReactionComponent;

/**
 * Turns a reaction and a conversion into the fluid amounts one batch moves.
 * <p>
 * Amounts are aggregated by material before being returned. Without that, a reaction listing the
 * same substance twice on one side would be checked for availability twice at half the true
 * requirement, and the reactor could start a batch it cannot finish.
 * <p>
 * Everything is floored to whole millibuckets. Flooring rather than rounding is deliberate: it can
 * only ever produce less output and consume no more feed than the continuous model implies, so
 * rounding can never manufacture matter.
 */
public final class ReactionBatch {

    private ReactionBatch() {}

    /** Everything one batch consumes, aggregated by material. */
    public static List<BatchAmount> feed(Reaction reaction) {
        requireUsable(reaction);
        return amounts(reaction.reactants, basisOf(reaction), 1.0d);
    }

    /**
     * Everything one batch returns to the output hatches: the converted products, plus the feed
     * that did not react.
     * <p>
     * Returning the remainder is the point of modelling conversion at all. A reaction that stops
     * short leaves something behind, and quietly destroying it would make the kinetics decorative.
     */
    public static List<BatchAmount> outputs(Reaction reaction, double conversion) {
        requireUsable(reaction);
        if (conversion < 0.0d || conversion > 1.0d) {
            throw new IllegalArgumentException("Conversion must lie between 0 and 1, was " + conversion);
        }
        double basis = basisOf(reaction);

        Map<String, BatchAmount> merged = new LinkedHashMap<String, BatchAmount>();
        accumulate(merged, reaction.products, basis, conversion);
        accumulate(merged, reaction.reactants, basis, 1.0d - conversion);

        List<BatchAmount> result = new ArrayList<BatchAmount>();
        for (BatchAmount amount : merged.values()) {
            if (amount.millibuckets > 0L) {
                result.add(amount);
            }
        }
        return result;
    }

    private static List<BatchAmount> amounts(List<ReactionComponent> components, double basis, double factor) {
        Map<String, BatchAmount> merged = new LinkedHashMap<String, BatchAmount>();
        accumulate(merged, components, basis, factor);
        List<BatchAmount> result = new ArrayList<BatchAmount>();
        for (BatchAmount amount : merged.values()) {
            if (amount.millibuckets > 0L) {
                result.add(amount);
            }
        }
        return result;
    }

    private static void accumulate(Map<String, BatchAmount> into, List<ReactionComponent> components, double basis,
        double factor) {
        for (ReactionComponent component : components) {
            long millibuckets = (long) Math.floor(component.moles * basis * factor);
            BatchAmount existing = into.get(component.material);
            long total = millibuckets;
            if (existing != null) {
                total = total + existing.millibuckets;
            }
            into.put(component.material, new BatchAmount(component.material, component.phase, total));
        }
    }

    private static double basisOf(Reaction reaction) {
        return reaction.basis.millibucketsPerMole;
    }

    private static void requireUsable(Reaction reaction) {
        if (reaction == null) {
            throw new IllegalArgumentException("Reaction must not be null");
        }
        if (reaction.basis == null) {
            throw new IllegalArgumentException("Reaction " + reaction.id + " declares no basis");
        }
        if (!(reaction.basis.millibucketsPerMole > 0.0d)) {
            throw new IllegalArgumentException(
                "Reaction " + reaction.id + " has a non-positive basis: " + reaction.basis.millibucketsPerMole);
        }
        if (reaction.reactants.isEmpty() || reaction.products.isEmpty()) {
            throw new IllegalArgumentException("Reaction " + reaction.id + " is missing a side of its stoichiometry");
        }
    }
}
