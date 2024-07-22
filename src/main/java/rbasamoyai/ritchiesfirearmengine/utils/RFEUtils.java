package rbasamoyai.ritchiesfirearmengine.utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RFEUtils {

    /**
     * Alias for {@link ResourceLocation#ResourceLocation(String)} to facilitate
     * porting to 1.21+.
     *
     * @param loc the {@link ResourceLocation} in {@code <namespace>:<path>} form
     * @return a new {@link ResourceLocation} of the passed id
     */
    public static ResourceLocation location(String loc) { return new ResourceLocation(loc); }

    /**
     * Alias for {@link ResourceLocation#ResourceLocation(String)} to facilitate
     * porting to 1.21+.
     *
     * @param namespace the id namespace
     * @param path the id path
     * @return a new {@link ResourceLocation} of the form {@code <namespace>:<path>}
     */
    public static ResourceLocation location(String namespace, String path) { return new ResourceLocation(namespace, path); }

    private static final Map<String, Rarity> RFE_RARITY_MAPPING = Arrays.stream(Rarity.values())
            .collect(Collectors.toMap(r -> r.name().toLowerCase(Locale.ROOT), Function.identity()));

    /**
     * Only guaranteed to handle vanilla rarities. See {@link Rarity} for more info.
     *
     * @param id the rarity in
     * @return the rarity
     */
    public static Rarity getRarityFromString(String id) { return RFE_RARITY_MAPPING.getOrDefault(id, Rarity.COMMON); }

}
