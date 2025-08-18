package rbasamoyai.ritchiesfirearmengine.foundation;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

import java.util.Locale;

public class RFETags {

    public enum RFEItemTags {
        INFINITE_AMMO;

        public final TagKey<Item> tag = TagKey.create(Registries.ITEM, RitchiesFirearmEngine.resource(this.name().toLowerCase(Locale.ROOT)));
    }

    public enum RFEBlockTags {
        ;

        public final TagKey<Block> tag = TagKey.create(Registries.BLOCK, RitchiesFirearmEngine.resource(this.name().toLowerCase(Locale.ROOT)));
    }

    public enum RFEEntityTypeTags {
        HUMANOID,
        VULNERABLE_TO_BIRDSHOT;

        public final TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, RitchiesFirearmEngine.resource(this.name().toLowerCase(Locale.ROOT)));
    }

    private RFETags() {}

}
