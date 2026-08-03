package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

public record RFEItemAttachmentsRenderData(Map<Item, Map<ResourceLocation, SlotAttachmentRenderData>> renderDataByItemAndSlot) {

    public record SlotAttachmentRenderData(ModelResourceLocation model, Matrix4f transforms) {
        public static final MapCodec<SlotAttachmentRenderData> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                ResourceLocation.CODEC.xmap(ModelResourceLocation::standalone, ModelResourceLocation::id).fieldOf("model").forGetter(SlotAttachmentRenderData::model),
                ExtraCodecs.VECTOR3F.optionalFieldOf("scale", new Vector3f(1f, 1f, 1f)).forGetter(SlotAttachmentRenderData::scale),
                ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", new Vector3f()).forGetter(SlotAttachmentRenderData::rotations),
                ExtraCodecs.VECTOR3F.optionalFieldOf("translation", new Vector3f()).forGetter(SlotAttachmentRenderData::translation)
        ).apply(o, SlotAttachmentRenderData::fromSRTVectors));

        public static SlotAttachmentRenderData fromSRTVectors(ModelResourceLocation model, Vector3f scale,
                                                              Vector3f rotationXYZDeg, Vector3f translation) {
            Matrix4f transforms = new Matrix4f().identity();
            transforms.translation(translation.div(16f));
            transforms.rotateAffineZYX(rotationXYZDeg.z * Mth.DEG_TO_RAD, rotationXYZDeg.y * Mth.DEG_TO_RAD, rotationXYZDeg.x * Mth.DEG_TO_RAD);
            transforms.scale(scale);
            return new SlotAttachmentRenderData(model, transforms);
        }

        private Vector3f scale() { return this.transforms.getScale(new Vector3f()); }
        private Vector3f rotations() { return this.transforms.getEulerAnglesZYX(new Vector3f()).mul(Mth.RAD_TO_DEG); }
        private Vector3f translation() { return this.transforms.getTranslation(new Vector3f()); }
    }

}
