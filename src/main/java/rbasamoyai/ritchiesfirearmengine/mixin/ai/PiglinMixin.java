package rbasamoyai.ritchiesfirearmengine.mixin.ai;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.ICanFireRFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEDefaultFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;

@Mixin(Piglin.class)
public abstract class PiglinMixin extends AbstractPiglin implements ICanFireRFEFirearmItem {

    @Shadow public abstract Brain<Piglin> getBrain();

    PiglinMixin(EntityType<? extends AbstractPiglin> entityType, Level level) { super(entityType, level); }

    @WrapMethod(method = "getArmPose")
    private PiglinArmPose ritchiesfirearmengine$getArmPose(Operation<PiglinArmPose> original) {
        PiglinArmPose ret = original.call();
        if (this.getMainHandItem().getItem() instanceof RFEDefaultFirearmItem)
            return PiglinArmPose.CROSSBOW_HOLD; // TODO other poses
        return ret;
    }

    @WrapMethod(method = "canReplaceCurrentItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z")
    private boolean ritchiesfirearmengine$canReplaceCurrentItem(ItemStack candidate, ItemStack existing, Operation<Boolean> original) {
        if (original.call(candidate, existing))
            return true;
        return this.isAdult() && candidate.getItem() instanceof RFEFirearmItem && existing.is(Items.CROSSBOW);
    }

    @WrapOperation(method = "canReplaceCurrentItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean ritchiesfirearmengine$canReplaceCurrentItem$is(ItemStack instance, Item item, Operation<Boolean> original) {
        // crude but piglins and firearms are not intended to appear in normal gameplay anyway, this is just a proof of concept --ritchie
        return original.call(instance, item) || instance.getItem() instanceof RFEFirearmItem;
    }

    @Override
    public void ritchiesfirearmengine$onShotFired(ItemStack itemStack) {
        this.getBrain().setMemory(MemoryModuleType.ATTACK_COOLING_DOWN, true);
    }

}
