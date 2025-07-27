package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import rbasamoyai.ritchiesfirearmengine.foundation.gui.RFEItemSlotTextureDecorations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {

    @WrapMethod(method = "reload")
    private CompletableFuture<Void> ritchiesfirearmengine$reload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                                                 ResourceManager resourceManager, ProfilerFiller preparationsProfiler,
                                                                 ProfilerFiller reloadProfiler, Executor backgroundExecutor,
                                                                 Executor gameExecutor, Operation<CompletableFuture<Void>> original) {
        RFEItemSlotTextureDecorations.clear();
        return original.call(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
    }

}
