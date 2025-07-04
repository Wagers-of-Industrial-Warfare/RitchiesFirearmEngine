package rbasamoyai.ritchiesfirearmengine.builtin_content.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface HoldAttackKeyInteraction {

    boolean isHoldingAttackKey(ItemStack itemStack, Player player);
    void onReleaseAttackKey(ItemStack itemStack, Player player);

}
