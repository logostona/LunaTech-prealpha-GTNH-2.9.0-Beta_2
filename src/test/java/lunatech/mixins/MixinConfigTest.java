package lunatech.mixins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Guards the mixin configuration, which the build does not verify.
 * <p>
 * The GTNH convention generates the refmap and writes {@code MixinConfigs: mixins.lunatech.json}
 * into the manifest, but it does <em>not</em> generate that config — GregTech commits its own by
 * hand. Omitting it still compiles, still passes every other test, and still produces a jar; the
 * failure only appears when the game refuses to start with "The specified resource was invalid or
 * could not be read". That is exactly the kind of gap a build-time check should close.
 */
class MixinConfigTest {

    private static final String CONFIG = "/mixins.lunatech.json";

    private static final String EXPECTED_PACKAGE = "lunatech.mixins";

    @Test
    @DisplayName("The mixin config the manifest promises actually exists and is valid JSON")
    void configExists() {
        JsonObject config = readConfig();
        assertEquals(EXPECTED_PACKAGE, config.get("package").getAsString());
        assertEquals("mixins.lunatech.refmap.json", config.get("refmap").getAsString());
        assertEquals("JAVA_8", config.get("compatibilityLevel").getAsString());
    }

    @Test
    @DisplayName("Every mixin the config names is actually present in the build")
    void listedMixinsExist() {
        JsonObject config = readConfig();
        JsonArray mixins = config.getAsJsonArray("mixins");
        assertNotNull(mixins, "config lists no mixins array");
        assertFalse(mixins.size() == 0, "config declares no mixins, so nothing would be applied");

        String packagePath = EXPECTED_PACKAGE.replace('.', '/');
        for (int i = 0; i < mixins.size(); i++) {
            String name = mixins.get(i)
                .getAsString();
            String resource = "/" + packagePath + "/" + name + ".class";
            InputStream found = MixinConfigTest.class.getResourceAsStream(resource);
            assertNotNull(found, "config names " + name + " but " + resource + " is not in the build");
        }
    }

    @Test
    @DisplayName("Mixin classes present in the build are all declared in the config")
    void noMixinIsSilentlyUnregistered() {
        JsonObject config = readConfig();
        JsonArray declared = config.getAsJsonArray("mixins");

        // Kept explicit rather than scanned: a mixin that exists but is missing from the config
        // is silently inert, which is harder to notice than a crash.
        List<String> expected = java.util.Collections.singletonList("MixinTooltipHelper");
        assertEquals(expected.size(), declared.size(), "mixin count differs from the expected set");
        for (String name : expected) {
            boolean present = false;
            for (int i = 0; i < declared.size(); i++) {
                String candidate = declared.get(i)
                    .getAsString();
                if (name.equals(candidate)) {
                    present = true;
                }
            }
            assertTrue(present, name + " exists but is not registered in " + CONFIG);
        }
    }

    private static JsonObject readConfig() {
        InputStream stream = MixinConfigTest.class.getResourceAsStream(CONFIG);
        assertNotNull(stream, "missing " + CONFIG + "; the manifest promises it and the game will not start");
        Charset utf8 = Charset.forName("UTF-8");
        Reader reader = new InputStreamReader(stream, utf8);
        return new Gson().fromJson(reader, JsonObject.class);
    }
}
