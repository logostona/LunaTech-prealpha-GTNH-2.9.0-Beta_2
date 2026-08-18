package lunatech;

/**
 * LunaTech's reserved block of GregTech metatile entity ids.
 * <p>
 * These ids are written into world data, so they must never move once a world has been saved with
 * them. That makes a collision with another addon a silent, save-corrupting bug rather than a
 * crash: whoever registers last wins, and the loser's machines become the winner's on next load.
 * <p>
 * The range was chosen by scanning the whole GT5-Unofficial tree at 5.09.52.594 — which includes
 * the absorbed addons bartworks, tectech, GT++, goodgenerator, gtnhlanth and the rest — for every
 * four- and five-digit literal, and taking a block inside the largest span nothing mentions.
 * 20001 to 23999 was free by that measure; LunaTech claims the first hundred of it.
 * <p>
 * That scan cannot see GregTech addons outside the jar, so registration additionally verifies the
 * slot is empty at runtime. This class deliberately holds no reference to GregTech, so the
 * reservation itself stays testable without a Minecraft classpath.
 */
public final class LunaTechIDs {

    private LunaTechIDs() {}

    /** First id in LunaTech's reserved block. */
    public static final int BASE = 20001;

    /** One past the last id LunaTech may use. */
    public static final int LIMIT = 20101;

    public static final int CONTINUOUS_FLOW_REACTOR = BASE;

    /** @return true if the id lies inside the reserved block. */
    public static boolean isReserved(int id) {
        return id >= BASE && id < LIMIT;
    }

    /**
     * @return the id, so this reads as a guard at the point of use
     * @throws IllegalStateException if the id is outside the reserved block
     */
    public static int requireReserved(int id) {
        if (!isReserved(id)) {
            throw new IllegalStateException(
                "Metatile id " + id + " is outside the LunaTech reserved block " + BASE + " to " + (LIMIT - 1));
        }
        return id;
    }
}
