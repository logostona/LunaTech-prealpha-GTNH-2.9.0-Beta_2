package lunatech.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Root of the reactions dataset. See DATA.md.
 * <p>
 * Unlike {@link MaterialDataset} this carries no {@code gt5uVersion}: activation energies are
 * chemistry, not values read out of GregTech, so they are not invalidated by a GregTech version
 * bump and claiming otherwise would be misleading.
 */
public final class ReactionDataset {

    public int schemaVersion;

    public String datasetVersion;

    public List<Reaction> reactions = new ArrayList<Reaction>();

    /** @return the reaction with this id, or null if absent. */
    public Reaction find(String id) {
        for (Reaction reaction : reactions) {
            if (reaction.id != null && reaction.id.equals(id)) {
                return reaction;
            }
        }
        return null;
    }

    /** @return the reaction with this id, never null. */
    public Reaction require(String id) {
        Reaction reaction = find(id);
        if (reaction == null) {
            throw new IllegalArgumentException("No reaction '" + id + "' in the dataset");
        }
        return reaction;
    }
}
