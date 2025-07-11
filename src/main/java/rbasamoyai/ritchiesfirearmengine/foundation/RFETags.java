package rbasamoyai.ritchiesfirearmengine.foundation;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

import java.util.Locale;

public class RFETags {

    public enum RFEItemTags {
        INFINITE_AMMO;

        public final TagKey<Item> tag = TagKey.create(Registries.ITEM, RitchiesFirearmEngine.resource(this.name().toLowerCase(Locale.ROOT)));
    }

    private RFETags() {}

}
