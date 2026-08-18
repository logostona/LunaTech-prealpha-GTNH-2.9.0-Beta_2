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
    @DisplayName("Flooring can never manufacture matter")
    void flooringIsConservative() {
        // A conversion that does not divide evenly into the basis.
        List<BatchAmount> outputs = ReactionBatch.outputs(shift(), 0.3333d);
        long total = 0L;
        for (BatchAmount amount : outputs) {
            total = total + amount.millibuckets;
        }
        long fed = 0L;
        for (BatchAmount amount : ReactionBatch.feed(shift())) {
            fed = fed + amount.millibuckets;
        }
        assertTrue(total <= fed, "outputs " + total + " exceeded feed " + fed);
    }

    @Test
    @DisplayName("Impossible conversions are rejected rather than silently clamped")
    void rejectsImpossibleConversion() {
        assertThrows(IllegalArgumentException.class, () -> ReactionBatch.outputs(shift(), -0.1d));
        assertThrows(IllegalArgumentException.class, () -> ReactionBatch.outputs(shift(), 1.1d));
        assertThrows(IllegalArgumentException.class, () -> ReactionBatch.feed(null));
    }
}
