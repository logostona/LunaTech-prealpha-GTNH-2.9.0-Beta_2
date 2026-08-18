package lunatech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins LunaTech's reserved metatile id block.
 * <p>
 * Metatile ids are stored in world data, so moving one silently reassigns every machine already
 * placed in a save. This is therefore a decision to be changed loudly if at all, and the numbers
 * are pinned here for the same reason the ratified budgets are.
 */
class LunaTechIDsTest {

    /**
     * Highest id observed in each absorbed addon when the block was chosen. The reserved range must
     * stay clear of all of them, and of GregTech itself, which reaches 32765.
     */
    private static final int HIGHEST_OBSERVED_ADDON_ID = 18000;

    @Test
    @DisplayName("The reserved block sits in the span the id scan found free")
    void blockIsWhereTheScanSaidItWasFree() {
        assertEquals(20001, LunaTechIDs.BASE);
        assertEquals(20101, LunaTechIDs.LIMIT);
        assertTrue(LunaTechIDs.BASE > HIGHEST_OBSERVED_ADDON_ID, "block overlaps a known addon range");
        assertTrue(LunaTechIDs.LIMIT <= 24000, "block runs past the free span the scan identified");
    }

    @Test
    @DisplayName("Membership is exact at both ends")
    void membershipIsHalfOpen() {
        assertTrue(LunaTechIDs.isReserved(LunaTechIDs.BASE));
        assertTrue(LunaTechIDs.isReserved(LunaTechIDs.LIMIT - 1));
        assertFalse(LunaTechIDs.isReserved(LunaTechIDs.BASE - 1));
        assertFalse(LunaTechIDs.isReserved(LunaTechIDs.LIMIT));
    }

    @Test
    @DisplayName("Every assigned id is inside the block")
    void assignmentsAreInsideTheBlock() {
        assertTrue(
            LunaTechIDs.isReserved(LunaTechIDs.CONTINUOUS_FLOW_REACTOR),
            "the reactor id escaped the reserved block");
    }

    @Test
    @DisplayName("An id outside the block is refused rather than used")
    void outsideIsRefused() {
        assertEquals(LunaTechIDs.BASE, LunaTechIDs.requireReserved(LunaTechIDs.BASE));
        assertThrows(IllegalStateException.class, () -> LunaTechIDs.requireReserved(19999));
        assertThrows(IllegalStateException.class, () -> LunaTechIDs.requireReserved(31000));
    }
}
