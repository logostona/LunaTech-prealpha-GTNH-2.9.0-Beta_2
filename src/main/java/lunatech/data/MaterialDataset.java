package lunatech.data;

import java.util.ArrayList;
import java.util.List;

/** Root of the materials dataset. See DATA.md for the schema and versioning rules. */
public final class MaterialDataset {

    /** Breaking shape changes only. The loader rejects versions it does not understand. */
    public int schemaVersion;

    /** ISO date of the last content change. Advisory. */
    public String datasetVersion;

    /**
     * The GregTech build these values were verified against. AUDIT.md is only valid for one build,
     * so a mismatch against the pin in dependencies.gradle is a defect, not a warning.
     */
    public String gt5uVersion;

    public List<Material> materials = new ArrayList<Material>();

    /** @return the material with this id, or null if absent. */
    public Material find(String id) {
        for (Material material : materials) {
            if (material.id != null && material.id.equals(id)) {
                return material;
            }
        }
        return null;
    }

    /** @return the material with this id, never null. */
    public Material require(String id) {
        Material material = find(id);
        if (material == null) {
            throw new IllegalArgumentException("No material '" + id + "' in the dataset");
        }
        return material;
    }
}
