package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.CompareValueSource;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract sealed class FirearmCondition implements Predicate<Map<ResourceLocation, Float>> {

    private FirearmCondition() {
    }

    public abstract void getCompareValueSources(Map<ResourceLocation, CompareValueSource> toEvaluate);

    public static final class Compare extends FirearmCondition {
        private final ResourceLocation sourceId;
        private final Operator operator;
        private final CompareValueSource valueSource;
        private final float compareTo;

        public Compare(ResourceLocation sourceId, Operator operator, CompareValueSource valueSource, float compareTo) {
            this.sourceId = sourceId;
            this.operator = operator;
            this.valueSource = valueSource;
            this.compareTo = compareTo;
        }

        @Override
        public void getCompareValueSources(Map<ResourceLocation, CompareValueSource> toEvaluate) {
            toEvaluate.put(this.sourceId, this.valueSource);
        }

        @Override
        public boolean test(Map<ResourceLocation, Float> context) {
            return this.operator.compareValues(context.getOrDefault(this.sourceId, 0f), this.compareTo);
        }

        public enum Operator implements StringRepresentable {
            GREATER_THAN {
                @Override public boolean compareValues(float source, float compared) { return source > compared; }
            },
            LESS_THAN {
                @Override public boolean compareValues(float source, float compared) { return source < compared; }
            },
            GREATER_THAN_OR_EQUAL_TO {
                @Override public boolean compareValues(float source, float compared) { return source >= compared; }
            },
            LESS_THAN_OR_EQUAL_TO {
                @Override public boolean compareValues(float source, float compared) { return source <= compared; }
            },
            EQUALS {
                @Override public boolean compareValues(float source, float compared) { return source == compared; }
            },
            NOT_EQUAL {
                @Override public boolean compareValues(float source, float compared) { return source != compared; }
            };

            private static final Map<String, Operator> BY_ID = Arrays.stream(values())
                    .collect(Collectors.toMap(Operator::getSerializedName, Function.identity()));

            private final String id = this.name().toLowerCase(Locale.ROOT);

            @Override public String getSerializedName() { return this.id; }

            @Nullable public static Operator byId(String id) { return BY_ID.get(id); }

            public abstract boolean compareValues(float source, float compared);
        }
    }

    public static final class And extends FirearmCondition {
        private final List<FirearmCondition> children;

        public And(List<FirearmCondition> children) {
            this.children = children;
        }

        @Override
        public void getCompareValueSources(Map<ResourceLocation, CompareValueSource> toEvaluate) {
            for (FirearmCondition condition : this.children)
                condition.getCompareValueSources(toEvaluate);
        }

        @Override
        public boolean test(Map<ResourceLocation, Float> context) {
            for (FirearmCondition condition : this.children) {
                if (!condition.test(context))
                    return false;
            }
            return true;
        }
    }

    public static final class Or extends FirearmCondition {
        private final List<FirearmCondition> children;

        public Or(List<FirearmCondition> children) {
            this.children = children;
        }

        @Override
        public void getCompareValueSources(Map<ResourceLocation, CompareValueSource> toEvaluate) {
            for (FirearmCondition condition : this.children)
                condition.getCompareValueSources(toEvaluate);
        }

        @Override
        public boolean test(Map<ResourceLocation, Float> context) {
            for (FirearmCondition condition : this.children) {
                if (condition.test(context))
                    return true;
            }
            return false;
        }
    }

    public static final class Not extends FirearmCondition {
        private final FirearmCondition wrapped;

        public Not(FirearmCondition wrapped) { this.wrapped = wrapped; }

        @Override
        public void getCompareValueSources(Map<ResourceLocation, CompareValueSource> toEvaluate) {
            this.wrapped.getCompareValueSources(toEvaluate);
        }

        @Override public boolean test(Map<ResourceLocation, Float> context) { return !this.wrapped.test(context); }
    }

    public static final class AlwaysTrue extends FirearmCondition {
        public static final AlwaysTrue ALWAYS_TRUE = new AlwaysTrue();

        private AlwaysTrue() {}

        @Override public void getCompareValueSources(Map<ResourceLocation, CompareValueSource> toEvaluate) {}

        @Override public boolean test(Map<ResourceLocation, Float> context) { return true; }
    }

    public static FirearmCondition fromJson(JsonObject obj, boolean macroEnabled) {
        if (GsonHelper.isStringValue(obj, "compare")) {
            ResourceLocation sourceLoc = RFEUtils.location(GsonHelper.getAsString(obj, "compare"));
            CompareValueSource source = RFEContentBuilderRegistry.getCompareValueSource(sourceLoc);
            String operatorString = GsonHelper.getAsString(obj, "operator");
            Compare.Operator operator = Compare.Operator.byId(operatorString);
            if (operator == null) {
                String str = "'" + String.join("', '", Arrays.stream(Compare.Operator.values())
                        .map(Compare.Operator::getSerializedName).toList()) + "'";
                throw new JsonParseException("Invalid firearm condition operator type '" + operatorString + "', must be one of " + str);
            }
            float compareTo = GsonHelper.getAsFloat(obj, "value");
            return new Compare(sourceLoc, operator, source, compareTo);
        }
        if (GsonHelper.isArrayNode(obj, "and")) {
            List<FirearmCondition> conditions = new LinkedList<>();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "and");
            for (JsonElement el : arr)
                conditions.add(fromJson(el.getAsJsonObject(), macroEnabled));
            return new And(conditions);
        }
        if (GsonHelper.isArrayNode(obj, "or")) {
            List<FirearmCondition> conditions = new LinkedList<>();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "or");
            for (JsonElement el : arr)
                conditions.add(fromJson(el.getAsJsonObject(), macroEnabled));
            return new Or(conditions);
        }
        if (GsonHelper.isObjectNode(obj, "not")) {
            FirearmCondition wrapped = fromJson(obj.getAsJsonObject("not"), macroEnabled);
            return new Not(wrapped);
        }
        if (GsonHelper.isStringValue(obj, "macro")) {
            if (!macroEnabled)
                throw new IllegalStateException("Cannot use macro in firearm condition macro definition");
            ResourceLocation macroLoc = RFEUtils.location(GsonHelper.getAsString(obj, "macro"));
            return FirearmCondtionMacroHandler.getMacro(macroLoc);
        }
        throw new JsonParseException("Invalid firearm condition type, must be one of 'compare', 'and', 'or'" + (macroEnabled ? ", 'macro" : ""));
    }

    public static Map<ResourceLocation, Float> evaluateCompareValueSources(Map<ResourceLocation, CompareValueSource> toEvaluate,
                                                                           ItemStack itemStack, LivingEntity entity) {
        Map<ResourceLocation, Float> context = new Object2FloatLinkedOpenHashMap<>();
        for (Map.Entry<ResourceLocation, CompareValueSource> entry : toEvaluate.entrySet())
            context.put(entry.getKey(), entry.getValue().getValue(itemStack, entity));
        return context;
    }

}
