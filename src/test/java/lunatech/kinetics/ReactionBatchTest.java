package lunatech.kinetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lunatech.data.Datasets;
import lunatech.data.Reaction;
import lunatech.data.ReactionComponent;

/**
 * Pins the batch arithmetic the reactor runs on.
 * <p>
 * This lives outside the metatile precisely so it can be tested: the parts of a GregTech machine
 * that need a running game are the parts hardest to get right, so as little as possible should be
 * in there.
 */
class ReactionBatchTest {

    private static Reaction shift() {
        return Datasets.reaction("water_gas_shift");
    }

    @Test
    @DisplayName("Feed is the full stoichiometric requirement, unaffected by conversion")
    void feedIsIndependentOfConversion() {
        List<BatchAmount> feed = ReactionBatch.feed(shift());
        assertEquals(2, feed.size());
        for (BatchAmount amount : feed) {
            assertEquals(1000L, amount.millibuckets, amount.material + " should take a full mole");
        }
    }

    @Test
    @DisplayName("Outputs are products scaled by conversion plus the feed that did not react")
    void outputsCarryTheRemainder() {
        List<BatchAmount> outputs = ReactionBatch.outputs(shift(), 0.8d);

        long total = 0L;
        for (BatchAmount amount : outputs) {
            total = total + amount.millibuckets;
            if ("CarbonDioxide".equals(amount.material) || "Hydrogen".equals(amount.material)) {
                assertEquals(800L, amount.millibuckets, amount.material + " should be the converted fraction");
            } else {
                assertEquals(200L, amount.millibuckets, amount.material + " should be the unreacted remainder");
            }
        }
        // Two moles in, two moles out, on this reaction's one-to-one stoichiometry.
        assertEquals(2000L, total, "the declared basis should balance in and out");
    }

    @Test
    @DisplayName("Complete conversion returns no remainder, and zero conversion returns it all")
    void extremesOfConversion() {
        List<BatchAmount> complete = ReactionBatch.outputs(shift(), 1.0d);
        for (BatchAmount amount : complete) {
            boolean isProduct = "CarbonDioxide".equals(amount.material) || "Hydrogen".equals(amount.material);
            assertTrue(isProduct, "nothing unreacted should survive complete conversion, saw " + amount.material);
        }

        List<BatchAmount> none = ReactionBatch.outputs(shift(), 0.0d);
        for (BatchAmount amount : none) {
            boolean isReactant = "CarbonMonoxide".equals(amount.material) || "Water".equals(amount.material);
            assertTrue(isReactant, "nothing should be produced at zero conversion, saw " + amount.material);
        }
    }

    @Test
    @DisplayName("A material listed twice on one side is aggregated, not counted twice")
    void duplicatesAggregate() {
        Reaction reaction = Datasets.reaction("water_gas_shift");

        ReactionComponent extra = new ReactionComponent();
        extra.material = "Water";
        extra.phase = "liquid";
        extra.moles = 2.0d;
        reaction.reactants.add(extra);
        try {
            List<BatchAmount> feed = ReactionBatch.feed(reaction);
            long water = 0L;
            for (BatchAmount amount : feed) {
                if ("Water".equals(amount.material)) {
                    water = amount.millibuckets;
                }
            }
            // Three moles total, in one entry. Two entries of 1000 and 2000 would each be checked
            // against the hatch separately, and a hatch holding 2000 would wrongly pass both.
            assertEquals(3000L, water, "duplicate components should merge into one requirement");
            assertEquals(2, feed.size(), "duplicates should not produce a second entry");
        } finally {
            reaction.reactants.remove(extra);
        }
    }

    @Test
    @DisplayName("Nothing is created or destroyed, at any conversion")
    void conservationHoldsEverywhere() {
        long fed = total(ReactionBatch.feed(shift()));

        // 0.8 is the value that first exposed this: 1.0 - 0.8 is 0.19999999999999996, so computing
        // the remainder as feed x (1 - conversion) floored to 199 mB and destroyed a millibucket
        // every batch. Sweeping catches the whole family rather than the one case that was noticed.
        double[] conversions = { 0.0d, 0.1d, 0.2d, 0.3d, 0.3333d, 0.5d, 0.7d, 0.8d, 0.9d, 0.95d, 0.99d, 1.0d };
        for (double conversion : conversions) {
            long out = total(ReactionBatch.outputs(shift(), conversion));
            assertEquals(fed, out, "matter was created or destroyed at conversion " + conversion);
        }
    }

    private static long total(List<BatchAmount> amounts) {
        long sum = 0L;
        for (BatchAmount amount : amounts) {
            sum = sum + amount.millibuckets;
        }
        return sum;
    }

    @Test
    @DisplayName("Impossible conversions are rejected rather than silently clamped")
    void rejectsImpossibleConversion() {
        assertThrows(IllegalArgumentException.class, () -> ReactionBatch.outputs(shift(), -0.1d));
        assertThrows(IllegalArgumentException.class, () -> ReactionBatch.outputs(shift(), 1.1d));
        assertThrows(IllegalArgumentException.class, () -> ReactionBatch.feed(null));
    }
}
