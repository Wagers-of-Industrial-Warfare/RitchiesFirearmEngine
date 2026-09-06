package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.bayonets;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;

public class BayonetItem extends Item {

    private final boolean defaultBlocksShooting;
    private final float defaultAddedAttackDamage;
    private final float defaultAddedAttackSpeed;
    private final float defaultAddedAttackRange;

    public BayonetItem(Properties properties, boolean defaultBlocksShooting, float defaultAddedAttackDamage,
                       float defaultAddedAttackSpeed, float defaultAddedAttackRange) {
        super(properties);
        this.defaultBlocksShooting = defaultBlocksShooting;
        this.defaultAddedAttackDamage = defaultAddedAttackDamage;
        this.defaultAddedAttackSpeed = defaultAddedAttackSpeed;
        this.defaultAddedAttackRange = defaultAddedAttackRange;
        // TODO folding bayonets?
    }

    public static ItemAttributeModifiers createAttributes(float damage, float attackSpeed, float range) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(RFEFirearmItem.BASE_ATTACK_RANGE_ID, range, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public boolean blocksShootingByDefault() { return this.defaultBlocksShooting; }

    public float defaultAddedAttackDamage() { return this.defaultAddedAttackDamage; }
    public float defaultAddedAttackSpeed() { return this.defaultAddedAttackSpeed; }
    public float defaultAddedAttackRange() { return this.defaultAddedAttackRange; }

    public static class Builder implements RFEItemBuilder {
        @Override
        public Item apply(JsonObject jsonObject) {
            boolean defaultBlocksShooting = GsonHelper.getAsBoolean(jsonObject, "blocks_shooting", false);
            float defaultAttackDamage = GsonHelper.getAsFloat(jsonObject, "added_attack_damage");
            float defaultAttackSpeed = GsonHelper.getAsFloat(jsonObject, "added_attack_speed");
            float defaultAddedAttackRange = GsonHelper.getAsFloat(jsonObject, "added_attack_range");
            return new BayonetItem(new Item.Properties().stacksTo(1).attributes(createAttributes(defaultAttackDamage, defaultAttackSpeed, defaultAddedAttackRange)),
                    defaultBlocksShooting, defaultAttackDamage, defaultAttackSpeed, defaultAddedAttackRange);
        }
    }

}
