package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import com.google.common.collect.ImmutableMap;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

public record RFEFirearmItemAmmoProperties(RFEFirearmModeAmmoProperties defaultProperties, ImmutableMap<String, RFEFirearmModeAmmoProperties> propertiesByMode) {

    public RFEFirearmModeAmmoProperties getProperties(String name) { return this.propertiesByMode.getOrDefault(name, this.defaultProperties); }

    public static void toNetwork(FriendlyByteBuf buf, RFEFirearmItemAmmoProperties properties) {
        RFEFirearmModeAmmoProperties.toNetwork(buf, properties.defaultProperties());
        buf.writeVarInt(properties.propertiesByMode().size());
        for (Map.Entry<String, RFEFirearmModeAmmoProperties> entry : properties.propertiesByMode().entrySet()) {
            buf.writeUtf(entry.toString());
            RFEFirearmModeAmmoProperties.toNetwork(buf, entry.getValue());
        }
    }

    public static RFEFirearmItemAmmoProperties fromNetwork(FriendlyByteBuf buf) {
        RFEFirearmModeAmmoProperties defaultProperties = RFEFirearmModeAmmoProperties.fromNetwork(buf);
        ImmutableMap.Builder<String, RFEFirearmModeAmmoProperties> propertiesByMode = ImmutableMap.builder();
        int sz = buf.readVarInt();
        for (int i = 0; i < sz; ++i) {
            String modeName = buf.readUtf();
            RFEFirearmModeAmmoProperties modeProperties = RFEFirearmModeAmmoProperties.fromNetwork(buf);
            propertiesByMode.put(modeName, modeProperties);
        }
        return new RFEFirearmItemAmmoProperties(defaultProperties, propertiesByMode.build());
    }

}
