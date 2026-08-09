package rbasamoyai.ritchiesfirearmengine;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.AmmoPacketItemPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItemPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmHandlingPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplierHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentsPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProviderPackHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProviderPackHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoader;
import rbasamoyai.ritchiesfirearmengine.network.ClientboundValidateRFEContentPacksPacket;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;

public class RFECommonEvents {

    public static void loadTagsAndTypes() {
        RFEFirearmAmmoHandler.loadProjectileTypes();
    }

    public static void onDatapackReload(boolean singleplayer) {
        loadTagsAndTypes();
        RFEProjectileManager.clearAllProjectiles();
        RFESpreadManager.clearTrackedSpread();
        RFERecoilManager.clearTrackedRecoil();
        RFENetwork.sendToAll(RFEProjectileManager.ClientboundRemoveAllProjectilesPacket.INSTANCE);

        if (singleplayer)
            return;
        RFEProjectileTypeHandler.syncToAll();
        MagazineItemPropertiesHandler.syncToAll();
        AmmoPacketItemPropertiesHandler.syncToAll();
        RFEFirearmAmmoHandler.syncToAll();
        RFEHitMultiplierHandler.syncToAll();
        RFEFirearmHandlingPropertiesHandler.syncToAll();
        RFESpreadProviderPackHandler.syncToAll();
        RFERecoilProviderPackHandler.syncToAll();
        RFEProjectilePenetrationHandler.syncToAll();
        RFEItemAttachmentsPropertiesHandler.syncToAll();
        RFEItemAttachmentsMenuSlotsHandler.syncToAll();
    }

    public static void onDatapackSync(ServerPlayer player, boolean singleplayer) {
        if (singleplayer)
            return;
        RFEProjectileTypeHandler.syncToPlayer(player);
        MagazineItemPropertiesHandler.syncToPlayer(player);
        AmmoPacketItemPropertiesHandler.syncToPlayer(player);
        RFEFirearmAmmoHandler.syncToPlayer(player);
        RFEHitMultiplierHandler.syncToPlayer(player);
        RFEFirearmHandlingPropertiesHandler.syncToPlayer(player);
        RFESpreadProviderPackHandler.syncToPlayer(player);
        RFERecoilProviderPackHandler.syncToPlayer(player);
        RFEProjectilePenetrationHandler.syncToPlayer(player);
        RFEItemAttachmentsPropertiesHandler.syncToPlayer(player);
        RFEItemAttachmentsMenuSlotsHandler.syncToPlayer(player);
    }

    public static void onLevelLoad(LevelAccessor level) {
        if (level.getServer() != null && !level.isClientSide() && level.getServer().overworld() == level)
            loadTagsAndTypes();
    }

    public static void onLevelUnload(LevelAccessor level) {
        RFEProjectileManager.onLevelUnload(level);
    }

    public static void onEntityJoin(Entity entity, Level level) {
        if (!level.isClientSide && entity instanceof ServerPlayer player)
            RFEProjectileManager.syncAllProjectilesToPlayer(player, level);
    }

    public static void onEntityRemoved(Entity entity) {
        if (entity instanceof LivingEntity living) {
            RFESpreadManager.stopTrackingEntity(living);
            RFERecoilManager.stopTrackingEntity(living);
        }
    }

    public static void onPlayerLoggedIn(Player entity) {
        if (entity instanceof ServerPlayer splayer)
            RFENetwork.sendToPlayer(new ClientboundValidateRFEContentPacksPacket(RFEPackLoader.getPackVersions()), splayer);
    }

    public static void onLevelTickEnd(Level level) {
        RFEProjectileManager.tick(level);
        RFESpreadManager.tick(level);
        RFERecoilManager.tick(level);
    }

    public static boolean onLeftClickBlock(Player player) {
        // TODO attachments
        return player.getMainHandItem().getItem() instanceof RFEFirearmItem || player.getOffhandItem().getItem() instanceof RFEFirearmItem;
    }

    public static void onPlayerSwitchGamemode(Player player, GameType oldGamemode, GameType newGamemode) {
        if (newGamemode == GameType.SPECTATOR) {
            ItemStack mainhandItem = player.getMainHandItem();
            if (mainhandItem.getItem() instanceof RFEFirearmItem firearmItem) {
                firearmItem.onReleaseAttackKey(mainhandItem, player);
                firearmItem.stopAiming(mainhandItem, player);
            }
            ItemStack offhandItem = player.getOffhandItem();
            if (offhandItem.getItem() instanceof RFEFirearmItem firearmItem) {
                firearmItem.onReleaseAttackKey(offhandItem, player);
                firearmItem.stopAiming(offhandItem, player);
            }
        }
    }

    public static void onLivingDeath(LivingEntity entity, DamageSource source) {
        ItemStack mainhandItem = entity.getMainHandItem();
        if (mainhandItem.getItem() instanceof RFEFirearmItem firearmItem) {
            firearmItem.onReleaseAttackKey(mainhandItem, entity);
            firearmItem.stopAiming(mainhandItem, entity);
        }
        ItemStack offhandItem = entity.getOffhandItem();
        if (offhandItem.getItem() instanceof RFEFirearmItem firearmItem) {
            firearmItem.onReleaseAttackKey(offhandItem, entity);
            firearmItem.stopAiming(offhandItem, entity);
        }
    }

}
