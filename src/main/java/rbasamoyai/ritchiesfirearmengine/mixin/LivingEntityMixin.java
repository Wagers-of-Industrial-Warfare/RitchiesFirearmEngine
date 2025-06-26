package rbasamoyai.ritchiesfirearmengine.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rbasamoyai.ritchiesfirearmengine.content.ammo.AmmoPacketItem;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    private LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) { super(pEntityType, pLevel); }

    @Shadow public abstract boolean isUsingItem();

    @Shadow protected abstract void spawnItemParticles(ItemStack pStack, int pAmount);

    @WrapMethod(method = "triggerItemUseEffects")
    private void ritchiesfirearmengine$triggerItemUseEffects(ItemStack itemStack, int amount, Operation<Void> original) {
        if (!itemStack.isEmpty() && this.isUsingItem() && itemStack.getItem() instanceof AmmoPacketItem ammoPacket) {
            if (ammoPacket.spawnParticlesOnUse(itemStack))
                this.spawnItemParticles(itemStack, amount);
            SoundEvent sound = ammoPacket.getPacketUseSound(itemStack);
            if (sound != null)
                this.playSound(sound, 0.5F + 0.5F * (float)this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            return;
        }
        original.call(itemStack, amount);
    }

}
