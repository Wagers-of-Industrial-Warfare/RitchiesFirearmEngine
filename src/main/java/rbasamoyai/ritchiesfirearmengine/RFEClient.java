package rbasamoyai.ritchiesfirearmengine;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import rbasamoyai.ritchiesfirearmengine.content.FovModifyingItem;
import rbasamoyai.ritchiesfirearmengine.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.content.SimultaneousUseAndAttack;
import rbasamoyai.ritchiesfirearmengine.content.ammo.AmmoPacketItem;
import rbasamoyai.ritchiesfirearmengine.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundFirearmActionPacket;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundReleaseAttackKeyPacket;

import java.util.function.Consumer;

public class RFEClient {

    public static final KeyMapping RELOAD_FIREARM = createSafeKeyMapping("key.ritchiesfirearmengine.reload_firearm", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R);
    public static final KeyMapping UNLOAD_FIREARM = createSafeKeyMapping("key.ritchiesfirearmengine.unload_firearm", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U);
    public static final KeyMapping SWITCH_MODE = createSafeKeyMapping("key.ritchiesfirearmengine.switch_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V);

    public static void onClientSetup() {
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("round_count"), (itemStack, level, entity, seed) -> {
           return itemStack.getItem() instanceof MagazineItem magazine ? magazine.countAmmo(itemStack) : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("aiming"), (itemStack, level, entity, seed) -> {
           return FirearmDataUtils.isAiming(itemStack) ? 1 : 0;
        });
    }

    public static void onMouseInput(int button, int action, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack useStack = mc.player.getUseItem();
        InteractionHand useHand = mc.player.getUsedItemHand();
        if (useHand == InteractionHand.MAIN_HAND) {
            if (useStack.getItem() instanceof SimultaneousUseAndAttack) {
                while (mc.options.keyAttack.consumeClick()) {
                    mc.player.swing(useHand);
                }
            }
        }

        ItemStack mainhandItem = mc.player.getMainHandItem();
        if (mainhandItem.getItem() instanceof HoldAttackKeyInteraction holdAttackKeyInteraction) {
            if (holdAttackKeyInteraction.isHoldingAttackKey(mainhandItem, mc.player) && !mc.options.keyAttack.isDown())
                RFENetwork.sendToServer(new ServerboundReleaseAttackKeyPacket());
        }
    }

    public static void onKeyInput(int key, int scancode, int action, int mods) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && mc.screen == null) {
            ItemStack useStack = mc.player.getMainHandItem();
            if (useStack.getItem() instanceof RFEFirearmItem || useStack.getItem() instanceof MagazineItem
                || useStack.getItem() instanceof AmmoPacketItem) {
                if (RELOAD_FIREARM.consumeClick()) {
                    RFENetwork.sendToServer(new ServerboundFirearmActionPacket(RFEFirearmItem.Action.RELOAD));
                } else if (UNLOAD_FIREARM.consumeClick()) {
                    RFENetwork.sendToServer(new ServerboundFirearmActionPacket(RFEFirearmItem.Action.UNLOAD));
                } else if (SWITCH_MODE.consumeClick()) {
                    RFENetwork.sendToServer(new ServerboundFirearmActionPacket(RFEFirearmItem.Action.SWITCH_MODE));
                }
            }
        }
    }

    public static KeyMapping createSafeKeyMapping(String description, InputConstants.Type type, int key) {
        return new KeyMapping(description, type, key, "key.ritchiesfirearmengine.category");
    }

    public static void registerKeyMappings(Consumer<KeyMapping> cons) {
        cons.accept(RELOAD_FIREARM);
        cons.accept(UNLOAD_FIREARM);
        cons.accept(SWITCH_MODE);
    }

    public static float modifyFov(float currentFovModifier, Player player) {
        Minecraft mc = Minecraft.getInstance();
        // TODO offhand modifier - take lowest fov modifier
        ItemStack itemStack = player.getMainHandItem();
        float partialTicks = mc.getPartialTick();
        return itemStack.getItem() instanceof FovModifyingItem fovModifier ? fovModifier.getFov(itemStack, player, currentFovModifier, partialTicks) : currentFovModifier;
    }

}
