package rbasamoyai.ritchiesfirearmengine.content.firearms.logic;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum FireMode implements StringRepresentable {

    SAFETY,
    SINGLE_ACTION,
    SEMI_AUTO,
    FULL_AUTO,
    BURST;

    private static final Map<String, FireMode> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(FireMode::getSerializedName, Function.identity()));

    private final String id = this.name().toLowerCase(Locale.ROOT);

    public boolean isSelfLoading() { return this == SEMI_AUTO || this == FULL_AUTO || this == BURST; }

    @Override public String getSerializedName() { return this.id; }

    public static FireMode byId(String id) { return BY_ID.getOrDefault(id, SAFETY); }

}
