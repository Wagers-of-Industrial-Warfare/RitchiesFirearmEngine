package rbasamoyai.ritchiesfirearmengine.foundation.index;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.content.FirearmCatalogItem;

public class FoundationItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RitchiesFirearmEngine.MOD_ID);

    public static final DeferredItem<FirearmCatalogItem> FIREARM_CATALOG = ITEMS.register("firearm_catalog", () ->
            new FirearmCatalogItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

}
