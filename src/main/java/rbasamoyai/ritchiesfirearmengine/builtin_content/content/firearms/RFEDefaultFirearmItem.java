package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmMode;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmModeBuilder;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RFEDefaultFirearmItem extends RFEFirearmItem {

    public RFEDefaultFirearmItem(Properties properties, Map<String, RFEFirearmMode> baseFirearmModes, List<String> modeOrder) {
        super(properties, baseFirearmModes, modeOrder, "default");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(@Nullable Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }

    public static class Builder implements RFEItemBuilder {
        @Override
        public Item apply(JsonObject obj) {
            Properties properties = new Properties();
            RFEFirearmModeBuilder.Parser modeParser = new RFEFirearmModeBuilder.Parser();
            List<String> modeOrder = new ArrayList<>();
            modeOrder.add("default");

            Map<String, RFEFirearmMode> firearmModes = new HashMap<>();
            RFEFirearmModeBuilder rootBuilder = modeParser.modifyBuilder(modeParser.getBuilder("default"), obj, "default");
            RFEFirearmMode defaultMode = rootBuilder.build();
            firearmModes.put("default", defaultMode);

            if (GsonHelper.isObjectNode(obj, "modes")) {
                JsonObject modes = obj.getAsJsonObject("modes");
                for (Map.Entry<String, JsonElement> entry : modes.entrySet()) {
                    String modeName = entry.getKey();
                    JsonElement modeEl = entry.getValue();
                    if (!modeEl.isJsonObject())
                        throw new JsonParseException("Invalid mode format, must be a JSON object");
                    RFEFirearmMode mode = modeParser.modifyBuilder(rootBuilder.forkBuilder(modeName), modeEl.getAsJsonObject(), modeName).build();
                    firearmModes.put(modeName, mode);
                    modeOrder.add(modeName);
                }
            }

            return new RFEDefaultFirearmItem(properties, firearmModes, modeOrder);
        }
    }

}
