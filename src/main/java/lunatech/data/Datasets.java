package lunatech.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import com.google.gson.Gson;

/**
 * Loads LunaTech's JSON datasets from the classpath. See DATA.md.
 * <p>
 * Gson comes from Minecraft, which already ships it, so this adds no runtime dependency.
 */
public final class Datasets {

    private Datasets() {}

    /** Schema version this build understands. */
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    /** The GregTech build LunaTech is pinned to. Must match dependencies.gradle and the dataset. */
    public static final String PINNED_GT5U_VERSION = "5.09.52.594";

    private static final String MATERIALS_PATH = "/lunatech/data/materials.json";

    private static final String REACTIONS_PATH = "/lunatech/data/reactions.json";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static MaterialDataset materials;

    private static ReactionDataset reactions;

    /** Loads and caches the materials dataset. */
    public static synchronized MaterialDataset materials() {
        if (materials == null) {
            materials = load(MATERIALS_PATH, MaterialDataset.class);
            verify(materials);
        }
        return materials;
    }

    /** Convenience accessor for a single material. Throws if the id is absent. */
    public static Material material(String id) {
        MaterialDataset dataset = materials();
        return dataset.require(id);
    }

    /** Loads and caches the reactions dataset. */
    public static synchronized ReactionDataset reactions() {
        if (reactions == null) {
            reactions = load(REACTIONS_PATH, ReactionDataset.class);
            if (reactions.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
                throw new IllegalStateException(
                    "Reaction dataset schemaVersion " + reactions.schemaVersion
                        + " but this build understands "
                        + SUPPORTED_SCHEMA_VERSION);
            }
        }
        return reactions;
    }

    /** Convenience accessor for a single reaction. Throws if the id is absent. */
    public static Reaction reaction(String id) {
        ReactionDataset dataset = reactions();
        return dataset.require(id);
    }

    private static <T> T load(String path, Class<T> type) {
        InputStream stream = Datasets.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Dataset resource missing from the classpath: " + path);
        }
        try {
            Reader reader = new InputStreamReader(stream, UTF_8);
            try {
                T parsed = new Gson().fromJson(reader, type);
                if (parsed == null) {
                    throw new IllegalStateException("Dataset parsed to nothing: " + path);
                }
                return parsed;
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read dataset: " + path, e);
        }
    }

    /**
     * Fails loudly on the two mismatches that would silently invalidate everything downstream: an
     * unknown schema shape, and data verified against a different GregTech build than we compile
     * against.
     */
    private static void verify(MaterialDataset dataset) {
        if (dataset.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalStateException(
                "Dataset schemaVersion " + dataset.schemaVersion
                    + " but this build understands "
                    + SUPPORTED_SCHEMA_VERSION);
        }
        if (!PINNED_GT5U_VERSION.equals(dataset.gt5uVersion)) {
            throw new IllegalStateException(
                "Dataset was verified against GregTech " + dataset.gt5uVersion
                    + " but LunaTech pins "
                    + PINNED_GT5U_VERSION
                    + "; AUDIT.md is only valid for the pinned build");
        }
    }
}
