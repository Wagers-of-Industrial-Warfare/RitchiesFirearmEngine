package rbasamoyai.ritchiesfirearmengine.content.firearms;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.RFEFirearmMode;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.RFEFirearmModeBuilder;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.items.RFEItemBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RFEDefaultFirearmItem extends RFEFirearmItem {

    public RFEDefaultFirearmItem(Properties properties, Map<String, RFEFirearmMode> baseFirearmModes, List<String> modeOrder) {
        super(properties, baseFirearmModes, modeOrder, "default");
    }

    public static class Builder implements RFEItemBuilder {
        @Override
        public Item apply(JsonObject obj) {
            Properties properties = new Properties();
            RFEFirearmModeBuilder.Parser modeParser = new RFEFirearmModeBuilder.Parser();
            List<String> modeOrder = new ArrayList<>();
            modeOrder.add("default");

            Map<String, RFEFirearmMode> firearmModes = new HashMap<>();
            RFEFirearmMode defaultMode = modeParser.fromJson(obj, "default");
            firearmModes.put("default", defaultMode);

            if (GsonHelper.isObjectNode(obj, "modes")) {
                JsonObject modes = obj.getAsJsonObject("modes");
                for (String modeName : modes.keySet()) {
                    JsonElement modeEl = modes.get(modeName);
                    if (!modeEl.isJsonObject())
                        throw new JsonParseException("Invalid mode format, must be a JSON object");
                    RFEFirearmMode mode = modeParser.fromJson(modeEl.getAsJsonObject(), modeName);
                    firearmModes.put(modeName, mode);
                    modeOrder.add(modeName);
                }
            }

            return new RFEDefaultFirearmItem(properties, firearmModes, modeOrder);
        }
    }

}
