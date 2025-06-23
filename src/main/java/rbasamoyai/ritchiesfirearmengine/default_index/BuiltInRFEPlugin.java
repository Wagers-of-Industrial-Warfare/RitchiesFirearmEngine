package rbasamoyai.ritchiesfirearmengine.default_index;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.content.ammo.AmmoItem;
import rbasamoyai.ritchiesfirearmengine.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.content.firearms.RFEDefaultFirearmItem;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins.RFEPlugin;

/**
 * Because it's always best to lead by example.
 * Also required for most content anyway.
 */
public class BuiltInRFEPlugin implements RFEPlugin {

    @Override
    public void register() {
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("ammo"), new AmmoItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("magazine"), new MagazineItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("speedloader"), new MagazineItem.Builder()); // Alias of magazine
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("firearm"), new RFEDefaultFirearmItem.Builder());
        // TODO revolver builder

        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("player_ammo_count"), BuiltInRFEPlugin::playerAmmoCount);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("free_ammo_space"), BuiltInRFEPlugin::freeAmmoSpace);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("extra_ammo_space"), BuiltInRFEPlugin::extraAmmoSpace);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("firearm_has_ammo"), BuiltInRFEPlugin::firearmHasAmmo);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("has_loaded_round"), BuiltInRFEPlugin::hasLoadedRound);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("is_charged"), BuiltInRFEPlugin::isCharged);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("is_jammed"), BuiltInRFEPlugin::isJammed);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("player_has_speedloader"), BuiltInRFEPlugin::playerHasSpeedloader);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("player_has_magazine"), BuiltInRFEPlugin::playerHasMagazine);
    }

    // TODO implement

    private static float playerAmmoCount(ItemStack itemStack, LivingEntity entity) {
        return 0;
    }

    private static float freeAmmoSpace(ItemStack itemStack, LivingEntity entity) {
        return 0;
    }

    private static float extraAmmoSpace(ItemStack itemStack, LivingEntity entity) {
        return 0;
    }

    private static float firearmHasAmmo(ItemStack itemStack, LivingEntity entity) {
        return 0;
    }

    private static float hasLoadedRound(ItemStack itemStack, LivingEntity entity) {
        return 0;
    }

    private static float isCharged(ItemStack itemStack, LivingEntity entity) {
        return 0;
    }

    private static float isJammed(ItemStack itemStack, LivingEntity entity) {
        return 0;
    }

    private static float playerHasSpeedloader(ItemStack itemStack, LivingEntity entity) {
        return 0;
    }

    private static float playerHasMagazine(ItemStack itemStack, LivingEntity entity) {
        return 0;
    }

}
