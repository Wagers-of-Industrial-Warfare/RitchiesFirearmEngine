package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import org.spongepowered.asm.mixin.Mixin;
import rbasamoyai.ritchiesfirearmengine.remix.RFEClientRemix;

import java.util.function.Function;

@Mixin(BlockModel.class)
public abstract class BlockModelMixin {

    @WrapMethod(method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/model/BlockModel;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Z)Lnet/minecraft/client/resources/model/BakedModel;")
    private BakedModel ritchiesfirearmengine$bake(ModelBaker bakery, BlockModel model, Function<Material, TextureAtlasSprite> spriteGetter,
                                                  ModelState modelState, boolean guiLight3d, Operation<BakedModel> original) {
        BakedModel result = original.call(bakery, model, spriteGetter, modelState, guiLight3d);
        RFEClientRemix.loadSlotOverlaysFromBlockModel(model, result, spriteGetter);
        return result;
    }

}
