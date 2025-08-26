package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.util.GsonHelper;

import java.util.Set;
import java.util.function.IntPredicate;

public sealed abstract class ReloadPhaseAccessFilter implements IntPredicate {

    public static ReloadPhaseAccessFilter fromJson(JsonElement el) {
        if (GsonHelper.isStringValue(el)) {
            switch (el.getAsString()) {
                case "all" -> {
                    return IncludeAll.INSTANCE;
                }
                case "none" -> {
                    return ExcludeAll.INSTANCE;
                }
                default -> throw new JsonParseException("Accessible phase string must either be 'all' or 'none'");
            }
        } else if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (GsonHelper.isArrayNode(obj, "include")) {
                JsonArray arr = GsonHelper.getAsJsonArray(obj, "include");
                if (arr.isEmpty())
                    return ExcludeAll.INSTANCE;
                Set<Integer> included = new IntOpenHashSet();
                for (JsonElement el1 : arr)
                    included.add(el1.getAsInt());
                return new Include(included);
            } else if (GsonHelper.isArrayNode(obj, "exclude")) {
                JsonArray arr = GsonHelper.getAsJsonArray(obj, "exclude");
                if (arr.isEmpty())
                    return IncludeAll.INSTANCE;
                Set<Integer> excluded = new IntOpenHashSet();
                for (JsonElement el1 : arr)
                    excluded.add(el1.getAsInt());
                return new Exclude(excluded);
            } else {
                throw new JsonParseException("Accessible phase array must either be 'include' or 'exclude'");
            }
        } else {
            throw new JsonParseException("Accessible phase must be either JSON string or object containing array");
        }
    }

    public static final class IncludeAll extends ReloadPhaseAccessFilter {
        public static final IncludeAll INSTANCE = new IncludeAll();

        private IncludeAll() {}

        @Override public boolean test(int index) { return true; }
    }

    public static final class ExcludeAll extends ReloadPhaseAccessFilter {
        public static final ExcludeAll INSTANCE = new ExcludeAll();

        private ExcludeAll() {}

        @Override public boolean test(int value) { return false; }
    }

    public static final class Include extends ReloadPhaseAccessFilter {
        private final Set<Integer> included;

        private Include(Set<Integer> included) { this.included = included; }

        @Override public boolean test(int index) { return this.included.contains(index); }
    }

    public static final class Exclude extends ReloadPhaseAccessFilter {
        private final Set<Integer> excluded;

        private Exclude(Set<Integer> excluded) { this.excluded = excluded; }

        @Override public boolean test(int index) { return !this.excluded.contains(index); }
    }

}
