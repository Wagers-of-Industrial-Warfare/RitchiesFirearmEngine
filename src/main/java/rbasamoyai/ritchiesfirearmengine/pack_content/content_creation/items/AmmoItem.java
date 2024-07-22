package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.items;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

public class AmmoItem extends Item {

    private final boolean glint;

    public AmmoItem(Properties properties, boolean glint) {
        super(properties);
        this.glint = glint;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return this.glint || super.isFoil(stack);
    }

    public static class Builder implements RFEItemBuilder {
        @Override
        public Item apply(JsonObject obj) {
            int stacksTo = GsonHelper.getAsInt(obj, "stacks_to", 64);
            Rarity rarity = RFEUtils.getRarityFromString(GsonHelper.getAsString(obj, "rarity", "common"));
            boolean glint = GsonHelper.getAsBoolean(obj, "glint", false);
            return new AmmoItem(new Properties().stacksTo(stacksTo).rarity(rarity), glint);
        }
    }

}
