package lunatech.mixins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Guards the mixin configuration, which the build does not verify.
 * <p>
 * The GTNH convention generates the refmap and writes {@code MixinConfigs: mixins.lunatech.json}
 * into the manifest, but it does <em>not</em> generate that config — GregTech commits its own by
 * hand and so must we. Omitting it still compiles, still passes every other test, and still
 * produces a jar containing the mixin class and a valid refmap; the failure appears only when the
 * game refuses to start with "The specified resource was invalid or could not be read".
 */
class MixinConfigTest {

    private static final String CONFIG = "/mixins.lunatech.json";

    private static final String EXPECTED_PACKAGE = "lunatech.mixins";

    /** Every mixin expected in the build, named explicitly rather than scanned for. */
    private static final List<String> EXPECTED_MIXINS = Arrays.asList(
        "MixinTooltipHelper",
        "MixinEUNoOverclockDescriber",
        "MixinBatteryBufferWaila",
        "MixinBasicMachineWaila",
        "MixinMultiBlockWaila");

    @Test
    @DisplayName("The mixin config the manifest promises actually exists and is valid JSON")
    void configExists() {
        JsonObject config = readConfig();
        assertEquals(EXPECTED_PACKAGE, stringField(config, "package"));
        assertEquals("mixins.lunatech.refmap.json", stringField(config, "refmap"));
        assertEquals("JAVA_8", stringField(config, "compatibilityLevel"));
    }

    @Test
    @DisplayName("Every mixin the config names is actually present in the build")
    void listedMixinsExist() {
        JsonArray mixins = declaredMixins();
        assertFalse(mixins.size() == 0, "config declares no mixins, so nothing would be applied");

        String packagePath = EXPECTED_PACKAGE.replace('.', '/');
        for (int i = 0; i < mixins.size(); i++) {
            String name = elementAt(mixins, i);
            String resource = "/" + packagePath + "/" + name + ".class";
            InputStream found = MixinConfigTest.class.getResourceAsStream(resource);
            assertNotNull(found, "config names " + name + " but " + resource + " is not in the build");
        }
    }

    @Test
    @DisplayName("Mixin classes present in the build are all declared in the config")
    void noMixinIsSilentlyUnregistered() {
        JsonArray declared = declaredMixins();
        assertEquals(EXPECTED_MIXINS.size(), declared.size(), "mixin count differs from the expected set");

        // A mixin that exists but is missing from the config is silently inert, which is harder to
        // notice than a crash.
        for (String name : EXPECTED_MIXINS) {
            boolean present = false;
            for (int i = 0; i < declared.size(); i++) {
                String candidate = elementAt(declared, i);
                if (name.equals(candidate)) {
                    present = true;
                }
            }
            assertTrue(present, name + " exists but is not registered in " + CONFIG);
        }
    }

    private static JsonArray declaredMixins() {
        JsonObject config = readConfig();
        JsonArray mixins = config.getAsJsonArray("mixins");
        assertNotNull(mixins, "config lists no mixins array");
        return mixins;
    }

    private static String stringField(JsonObject config, String key) {
        JsonElement value = config.get(key);
        assertNotNull(value, "config has no '" + key + "' field");
        return value.getAsString();
    }

    private static String elementAt(JsonArray array, int index) {
        JsonElement value = array.get(index);
        return value.getAsString();
    }

    private static JsonObject readConfig() {
        InputStream stream = MixinConfigTest.class.getResourceAsStream(CONFIG);
        assertNotNull(stream, "missing " + CONFIG + "; the manifest promises it and the game will not start");
        Charset utf8 = Charset.forName("UTF-8");
        Reader reader = new InputStreamReader(stream, utf8);
        Gson gson = new Gson();
        return gson.fromJson(reader, JsonObject.class);
    }
}
