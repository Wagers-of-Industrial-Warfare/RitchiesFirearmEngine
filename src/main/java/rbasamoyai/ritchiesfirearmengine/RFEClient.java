package rbasamoyai.ritchiesfirearmengine;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.FovModifyingItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.AmmoPacketItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FireMode;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudOverlayRenderer;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudOverlayRendererPacksHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering.RFEProjectileRenderer;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering.RFEProjectileRendererPacksHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilManager;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundFirearmActionPacket;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundSetAttackKeyPacket;

import java.util.Collection;
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
           return (entity instanceof Player ? entity.isUsingItem() : FirearmDataUtils.isAiming(itemStack)) ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("has_magazine"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm && firearm.hasMagazine(itemStack) ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_reloading"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm
                    && firearm.getCurrentAction(itemStack) == RFEFirearmItem.Action.RELOAD ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_unloading"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm
                    && firearm.getCurrentAction(itemStack) == RFEFirearmItem.Action.UNLOAD ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_charging"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm
                    && firearm.getCurrentAction(itemStack) == RFEFirearmItem.Action.CHARGING ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_firing"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm
                    && firearm.getCurrentAction(itemStack) == RFEFirearmItem.Action.FIRING ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_charged"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm && firearm.isCharged(itemStack) ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_single_action"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm
                    && firearm.getCurrentMode(itemStack).getFireMode() == FireMode.SINGLE_ACTION ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_semi_auto"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm
                    && firearm.getCurrentMode(itemStack).getFireMode() == FireMode.SEMI_AUTO ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_full_auto"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm
                    && firearm.getCurrentMode(itemStack).getFireMode() == FireMode.FULL_AUTO ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_burst"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm
                    && firearm.getCurrentMode(itemStack).getFireMode() == FireMode.BURST ? 1 : 0;
        });
        ItemProperties.registerGeneric(RitchiesFirearmEngine.resource("is_safety"), (itemStack, level, entity, seed) -> {
            return itemStack.getItem() instanceof RFEFirearmItem firearm
                    && firearm.getCurrentMode(itemStack).getFireMode() == FireMode.SAFETY ? 1 : 0;
        });
    }

    public static void onMouseInput(int button, int action, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean attacking = mc.options.keyAttack.isDown();
        ItemStack mainhandItem = mc.player.getMainHandItem();
        if (mainhandItem.getItem() instanceof HoldAttackKeyInteraction holdAttackKeyInteraction) {
            if (button == 0 && attacking) {
                RFENetwork.sendToServer(new ServerboundSetAttackKeyPacket(false));
                holdAttackKeyInteraction.onReleaseAttackKey(mainhandItem, mc.player);
            }
        }
    }

    public static void onKeyInput(int key, int scancode, int action, int mods) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && mc.screen == null) {
            ItemStack useStack = mc.player.getMainHandItem();
            if (useStack.getItem() instanceof RFEFirearmItem || useStack.getItem() instanceof MagazineItem
                || useStack.getItem() instanceof AmmoPacketItem) {
                if (RELOAD_FIREARM.isDown()) {
                    RFENetwork.sendToServer(new ServerboundFirearmActionPacket(RFEFirearmItem.Action.RELOAD));
                } else if (UNLOAD_FIREARM.isDown()) {
                    RFENetwork.sendToServer(new ServerboundFirearmActionPacket(RFEFirearmItem.Action.UNLOAD));
                } else if (SWITCH_MODE.isDown()) {
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

    public static void modifyCameraAngles(SetCameraAngles setCameraAngles) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused() && mc.hasSingleplayerServer())
            return;
        float dt = mc.getDeltaFrameTime();
        float dCamPitch = 0;
        float dCamYaw = 0;
        float dCamRoll = 0;
        float dAimPitch = 0;
        float dAimYaw = 0;
        RFERecoilInstance mainhandRecoilInstance = RFERecoilManager.getRecoilInstance(mc.player, mc.player.getMainHandItem());
        if (mainhandRecoilInstance != null) {
            RFEAimAngles aimRecoil = mainhandRecoilInstance.getAimRecoil(dt);
            RFEAimAngles cameraRecoil = mainhandRecoilInstance.getCameraRecoil(dt);
            dCamRoll += mainhandRecoilInstance.getCameraRoll(dt);
            dCamPitch += cameraRecoil.pitch();
            dCamYaw += cameraRecoil.yaw();
            dAimPitch += aimRecoil.pitch();
            dAimYaw += aimRecoil.yaw();
        }
        RFERecoilInstance offhandRecoilInstance = RFERecoilManager.getRecoilInstance(mc.player, mc.player.getOffhandItem());
        if (offhandRecoilInstance != null) {
            RFEAimAngles aimRecoil = offhandRecoilInstance.getAimRecoil(dt);
            RFEAimAngles cameraRecoil = offhandRecoilInstance.getCameraRecoil(dt);
            dCamRoll += offhandRecoilInstance.getCameraRoll(dt);
            dCamPitch += cameraRecoil.pitch();
            dCamYaw += cameraRecoil.yaw();
            dAimPitch += aimRecoil.pitch();
            dAimYaw += aimRecoil.yaw();
        }
        mc.player.turn(dAimYaw / 0.15f, -dAimPitch / 0.15f);
        setCameraAngles.setPitch(setCameraAngles.getPitch() - dCamPitch);
        setCameraAngles.setYaw(setCameraAngles.getYaw() + dCamYaw);
        setCameraAngles.setRoll(setCameraAngles.getRoll() + dCamRoll);
    }

    public static void onClientLogout() {
        RFEProjectileManager.clearAllProjectiles();
    }

    public static void renderAfterEntities(PoseStack poseStack, Matrix4f projectionMatrix, int renderTick,
                                           float partialTick, Camera camera, Frustum frustum) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null)
            return; // Should not happen
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();
        renderProjectiles(level, poseStack, partialTick, camera, frustum, buffers);
    }

    private static void renderProjectiles(Level level, PoseStack poseStack, float partialTick, Camera camera, Frustum frustum,
                                          MultiBufferSource buffers) {
        Vec3 cameraPos = camera.getPosition();
        double camX = cameraPos.x;
        double camY = cameraPos.y;
        double camZ = cameraPos.z;

        Collection<RFEProjectileInstance> projectiles = RFEProjectileManager.getProjectiles(level);
        for (RFEProjectileInstance instance : projectiles) {
            RFEProjectileRenderer renderer = RFEProjectileRendererPacksHandler.getProjectileRenderer(instance);
            Vec3 pos = instance.getPosition(partialTick);
            double posX = pos.x - camX;
            double posY = pos.y - camY;
            double posZ = pos.z - camZ;
            try {
                if (!renderer.shouldRender(instance, level, frustum, camX, camY, camZ))
                    continue;
                int light = renderer.getPackedLightCoords(instance, partialTick, level);

                poseStack.pushPose();
                poseStack.translate(posX, posY, posZ);
                renderer.renderProjectile(instance, level, partialTick, poseStack, buffers, light);
                poseStack.popPose();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering RFE projectile in world");
                CrashReportCategory renderingDetails = crashreport.addCategory("Renderer details");
                renderingDetails.setDetail("Assigned renderer", renderer);
                renderingDetails.setDetail("Location", CrashReportCategory.formatLocation(level, posX, posY, posZ));
                renderingDetails.setDetail("Delta", partialTick);
                throw new ReportedException(crashreport);
            }
        }
    }

    public static void renderHUDOverlay(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        ItemStack mainhand = mc.player.getMainHandItem();
        ItemStack offhand = mc.player.getOffhandItem();
        RFEHudOverlayRenderer mainhandHud = RFEHudOverlayRendererPacksHandler.getHudOverlayRenderer(mainhand);
        mainhandHud.renderHUD(graphics, partialTick, mainhand, mc.player, false);
        // TODO offhand rendering, though prioritize primary. May have something regarding supporting offhand rendering
    }

    public interface SetCameraAngles {
        float getPitch();
        void setPitch(float pitch);

        float getYaw();
        void setYaw(float yaw);

        float getRoll();
        void setRoll(float roll);
    }

}
