package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;

import javax.annotation.Nullable;
import java.util.Map;

public record RFEFirearmModeAmmoProperties(ImmutableMap<AmmoPredicate, RFEProjectileType> primaryAmmo, ImmutableList<AmmoPredicate> magazines,
                                           ImmutableList<AmmoPredicate> speedloaders, ImmutableList<AmmoPredicate> secondaryAmmo,
                                           @Nullable RFEProjectileType unlimitedAmmo) {

    public ImmutableCollection<AmmoPredicate> primaryAmmoPredicates() { return this.primaryAmmo.keySet(); }

    public static void toNetwork(FriendlyByteBuf buf, RFEFirearmModeAmmoProperties properties) {
        buf.writeVarInt(properties.primaryAmmo().size());
        for (Map.Entry<AmmoPredicate, RFEProjectileType> entry : properties.primaryAmmo().entrySet()) {
            ResourceLocation id = RFEProjectileTypeHandler.getProjectileTypeId(entry.getValue());
            if (id == null)
                id = RitchiesFirearmEngine.resource("invalid");
            AmmoPredicate.writeToNetwork(entry.getKey(), buf);
            buf.writeResourceLocation(id);
        }
        
        buf.writeVarInt(properties.magazines().size());
        for (AmmoPredicate pred : properties.magazines())
            AmmoPredicate.writeToNetwork(pred, buf);
        
        buf.writeVarInt(properties.speedloaders().size());
        for (AmmoPredicate pred : properties.speedloaders())
            AmmoPredicate.writeToNetwork(pred, buf);
        
        buf.writeVarInt(properties.secondaryAmmo().size());
        for (AmmoPredicate pred : properties.secondaryAmmo())
            AmmoPredicate.writeToNetwork(pred, buf);

        buf.writeBoolean(properties.unlimitedAmmo() != null);
        if (properties.unlimitedAmmo() != null) {
            ResourceLocation id = RFEProjectileTypeHandler.getProjectileTypeId(properties.unlimitedAmmo());
            if (id == null)
                id = RitchiesFirearmEngine.resource("invalid");
            buf.writeResourceLocation(id);
        }
    }

    public static RFEFirearmModeAmmoProperties fromNetwork(FriendlyByteBuf buf) {
        ImmutableMap.Builder<AmmoPredicate, RFEProjectileType> primaryAmmo = ImmutableMap.builder();
        int primarySz = buf.readVarInt();
        for (int i = 0; i < primarySz; ++i) {
            AmmoPredicate pred = AmmoPredicate.fromNetwork(buf);
            RFEProjectileType type = RFEProjectileTypeHandler.getProjectileType(buf.readResourceLocation());
            if (type != null)
                primaryAmmo.put(pred, type);
        }
        
        ImmutableList.Builder<AmmoPredicate> magazines = ImmutableList.builder();
        int magazineSz = buf.readVarInt();
        for (int i = 0; i < magazineSz; ++i)
            magazines.add(AmmoPredicate.fromNetwork(buf));
        
        ImmutableList.Builder<AmmoPredicate> speedloaders = ImmutableList.builder();
        int speedloaderSz = buf.readVarInt();
        for (int i = 0; i < speedloaderSz; ++i)
            speedloaders.add(AmmoPredicate.fromNetwork(buf));
        
        ImmutableList.Builder<AmmoPredicate> secondaryAmmo = ImmutableList.builder();
        int secondaryAmmoSz = buf.readVarInt();
        for (int i = 0; i < secondaryAmmoSz; ++i)
            secondaryAmmo.add(AmmoPredicate.fromNetwork(buf));

        boolean hasUnlimited = buf.readBoolean();
        RFEProjectileType unlimitedAmmo = hasUnlimited ? RFEProjectileTypeHandler.getProjectileType(buf.readResourceLocation()) : null;
        return new RFEFirearmModeAmmoProperties(primaryAmmo.build(), magazines.build(), speedloaders.build(), secondaryAmmo.build(), unlimitedAmmo);
    }

}
