package rbasamoyai.ritchiesfirearmengine.mixin.ai;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BackUpIfTooClose;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.behavior.RangedFirearmAttackBehavior;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.behavior.TickFirearmInHandsBehavior;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    @WrapOperation(method = "initCoreActivity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;addActivity(Lnet/minecraft/world/entity/schedule/Activity;ILcom/google/common/collect/ImmutableList;)V"))
    private static void ritchiesfirearmengine$initCoreActivity(Brain<Piglin> instance, Activity activity, int priorityStart,
                                                               ImmutableList<? extends BehaviorControl<? super LivingEntity>> tasks, Operation<Void> original) {
        original.call(instance, activity, priorityStart, ImmutableList.builder().addAll(tasks).add(new TickFirearmInHandsBehavior()).build());
    }

    @WrapOperation(method = "initFightActivity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;addActivityAndRemoveMemoryWhenStopped(Lnet/minecraft/world/entity/schedule/Activity;ILcom/google/common/collect/ImmutableList;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;)V"))
    private static void ritchiesfirearmengine$initFightActivity(Brain<Piglin> instance, Activity activity, int priorityStart,
                                                                ImmutableList<? extends BehaviorControl<? super Piglin>> tasks,
                                                                MemoryModuleType<?> memoryType, Operation<Void> original) {
        original.call(instance, activity, priorityStart, ImmutableList.builder().addAll(tasks)
                        .add(new RangedFirearmAttackBehavior(MemoryModuleType.ATTACK_COOLING_DOWN, 32f, 2.5f))
                        .add(BehaviorBuilder.triggerIf(RangedFirearmAttackBehavior::isHoldingShootableFirearm, BackUpIfTooClose.create(5, 0.75F)))
                        .build(),
                memoryType);
    }

}
