package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ChargingBehavior implements StringRepresentable {
    HOLD,
    MANUAL,
    AUTO;

    private static final Map<String, ChargingBehavior> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(ChargingBehavior::getSerializedName, Function.identity()));

    private final String id = this.name().toLowerCase(Locale.ROOT);

    @Override public String getSerializedName() { return this.id; }

    public boolean canChargeAfterFiring(boolean hold) { return this == HOLD && !hold || this == AUTO; }

    public static ChargingBehavior byId(String id) { return BY_ID.getOrDefault(id, HOLD); }

    public static ChargingBehavior byIdStrict(String id) {
        if (!BY_ID.containsKey(id))
            throw new IllegalStateException("Charging behavior '" + id + "' doesn't exist, must be one of 'hold', 'manual', 'auto'");
        return BY_ID.get(id);
    }

}
