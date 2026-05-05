package rbasamoyai.ritchiesfirearmengine.utils;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Rarity;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RFEUtils {

    /**
     * Alias for {@link ResourceLocation#parse(String)} to facilitate
     * porting to 1.21+.
     *
     * @param loc the {@link ResourceLocation} in {@code <namespace>:<path>} form
     * @return a new {@link ResourceLocation} of the passed id
     */
    public static ResourceLocation location(String loc) { return ResourceLocation.parse(loc); }

    /**
     * Alias for {@link ResourceLocation#fromNamespaceAndPath(String, String)} to facilitate
     * porting to 1.21+.
     *
     * @param namespace the id namespace
     * @param path the id path
     * @return a new {@link ResourceLocation} of the form {@code <namespace>:<path>}
     */
    public static ResourceLocation location(String namespace, String path) { return ResourceLocation.fromNamespaceAndPath(namespace, path); }

    private static final Map<String, Rarity> RFE_RARITY_MAPPING = Arrays.stream(Rarity.values())
            .collect(Collectors.toMap(r -> r.name().toLowerCase(Locale.ROOT), Function.identity()));

    /**
     * Only guaranteed to handle vanilla rarities. See {@link Rarity} for more info.
     *
     * @param id the rarity in
     * @return the rarity
     */
    public static Rarity getRarityFromString(String id) { return RFE_RARITY_MAPPING.getOrDefault(id, Rarity.COMMON); }

    public static <T> Predicate<T> orAllPredicates(Collection<? extends Predicate<T>> collection) {
        return t -> {
            for (Predicate<T> pred : collection) {
                if (pred.test(t))
                    return true;
            }
            return false;
        };
    }

    public static Map<String, Tuple<String, String>> provideMismatchedRFEContentPacks(Map<String, String> clientPacks, Map<String, String> serverPacks) {
        Map<String, Tuple<String, String>> mismatchedVersions = new TreeMap<>(String::compareTo);
        for (Iterator<Map.Entry<String, String>> iter = clientPacks.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, String> clientPack = iter.next();
            if (clientPack.getValue().equals(serverPacks.get(clientPack.getKey()))) {
                iter.remove();
                serverPacks.remove(clientPack.getKey());
            }
        }
        for (Map.Entry<String, String> clientPack : clientPacks.entrySet()) {
            String serverVersion = serverPacks.getOrDefault(clientPack.getKey(), null);
            mismatchedVersions.put(clientPack.getKey(), new Tuple<>(clientPack.getValue(), serverVersion));
        }
        for (Map.Entry<String, String> serverPack : serverPacks.entrySet()) {
            if (!mismatchedVersions.containsKey(serverPack.getKey()))
                mismatchedVersions.put(serverPack.getKey(), new Tuple<>((String) null, serverPack.getValue()));
        }
        return mismatchedVersions;
    }

    private RFEUtils() {}

    public static <K, V> ImmutableMap<K, V> toImmutableMap(Map<K, V> map) {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        builder.putAll(map);
        return builder.build();
    }
}
