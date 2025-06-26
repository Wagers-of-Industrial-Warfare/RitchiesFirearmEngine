package rbasamoyai.ritchiesfirearmengine.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface FovModifyingItem {

    float getFov(ItemStack itemStack, Player player, float currentFovModifier, float partialTicks);

}
