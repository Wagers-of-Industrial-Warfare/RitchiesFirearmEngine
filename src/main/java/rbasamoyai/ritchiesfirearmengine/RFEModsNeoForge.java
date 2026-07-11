package rbasamoyai.ritchiesfirearmengine;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public enum RFEModsNeoForge {
    IRIS,
    SABLE,
    SHOULDERSURFING;

    private final String id = this.name().toLowerCase(Locale.ROOT);

    public String id() { return this.id; }

    public ResourceLocation resource(String path) { return ResourceLocation.fromNamespaceAndPath(this.id, path); }

    public Block getBlock(String id) {
        return BuiltInRegistries.BLOCK.get(this.resource(id));
    }

    public boolean isLoaded() {
        return ModList.get().isLoaded(this.id);
    }

    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        return this.isLoaded() ? Optional.of(toRun.get().get()) : Optional.empty();
    }

    public void executeIfInstalled(Supplier<Runnable> toExecute) { if (this.isLoaded()) toExecute.get().run(); }

}
