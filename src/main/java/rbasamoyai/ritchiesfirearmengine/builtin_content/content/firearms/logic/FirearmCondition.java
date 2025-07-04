package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract sealed class FirearmCondition implements BiPredicate<ItemStack, LivingEntity> {

    private FirearmCondition() {
    }

    public static final class Compare extends FirearmCondition {
        private final Operator operator;
        private final CompareValueSource valueSource;
        private final float compareTo;

        public Compare(Operator operator, CompareValueSource valueSource, float compareTo) {
            this.operator = operator;
            this.valueSource = valueSource;
            this.compareTo = compareTo;
        }

        @Override
        public boolean test(ItemStack itemStack, LivingEntity entity) {
            return this.operator.compareValues(this.valueSource.getValue(itemStack, entity), this.compareTo);
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
        public boolean test(ItemStack itemStack, LivingEntity entity) {
            for (FirearmCondition condition : this.children) {
                if (!condition.test(itemStack, entity))
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
        public boolean test(ItemStack itemStack, LivingEntity entity) {
            for (FirearmCondition condition : this.children) {
                if (condition.test(itemStack, entity))
                    return true;
            }
            return false;
        }
    }

    public static final class AlwaysTrue extends FirearmCondition {
        public static final AlwaysTrue ALWAYS_TRUE = new AlwaysTrue();

        private AlwaysTrue() {}

        @Override
        public boolean test(ItemStack itemStack, LivingEntity entity) {
            return true;
        }
    }

    public static FirearmCondition fromJson(JsonObject obj) {
        String typeString = GsonHelper.getAsString(obj, "type", "compare");
        Type type = Type.byId(typeString);
        if (type == null)
            throw new JsonParseException("Invalid firearm condition type '" + typeString + "', must be one of 'compare', 'and', 'or' (can omit for 'compare')");
        return switch (type) {
            case COMPARE -> {
                ResourceLocation sourceLoc = RFEUtils.location(GsonHelper.getAsString(obj, "source"));
                CompareValueSource source = RFEContentBuilderRegistry.getCompareValueSource(sourceLoc);
                String operatorString = GsonHelper.getAsString(obj, "operator");
                Compare.Operator operator = Compare.Operator.byId(operatorString);
                if (operator == null) {
                    String str = "'" + String.join("', '", Arrays.stream(Compare.Operator.values())
                            .map(Compare.Operator::getSerializedName).toList()) + "'";
                    throw new JsonParseException("Invalid firearm condition operator type '" + operatorString + "', must be one of " + str);
                }
                float compareTo = GsonHelper.getAsFloat(obj, "value");
                yield new Compare(operator, source, compareTo);
            }
            case AND, OR -> {
                List<FirearmCondition> conditions = new LinkedList<>();
                JsonArray arr = GsonHelper.getAsJsonArray(obj, "conditions");
                for (JsonElement el : arr)
                    conditions.add(fromJson(el.getAsJsonObject()));
                yield type == Type.AND ? new And(conditions) : new Or(conditions);
            }
        };
    }

    public enum Type implements StringRepresentable {
        COMPARE,
        AND,
        OR;

        private static final Map<String, Type> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(Type::getSerializedName, Function.identity()));

        private final String id = this.name().toLowerCase(Locale.ROOT);

        @Override public String getSerializedName() { return this.id; }

        @Nullable public static Type byId(String id) { return BY_ID.get(id); }
    }

}
