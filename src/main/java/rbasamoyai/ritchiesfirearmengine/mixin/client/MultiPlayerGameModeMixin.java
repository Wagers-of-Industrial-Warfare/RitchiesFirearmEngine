package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.IFirearmItem;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @WrapMethod(method = "attack")
    private void ritchiesfirearmengine$attack(Player player, Entity target, Operation<Void> original) {
        ItemStack mainhand = player.getMainHandItem();
        if (mainhand.getItem() instanceof IFirearmItem firearmItem && !firearmItem.isMeleeing(mainhand, player))
            return;
        // TODO offhand
        original.call(player, target);
    }

}
