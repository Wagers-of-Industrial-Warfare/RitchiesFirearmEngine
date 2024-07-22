package rbasamoyai.ritchiesfirearmengine.pack_content.resources;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class SubFolderResourceSupplier implements PackResources {

    private final PackResources delegate;
    private final String subfolder;

    public SubFolderResourceSupplier(PackResources delegate, String subfolder) {
        this.delegate = delegate;
        this.subfolder = subfolder;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        List<String> list = Lists.asList(this.subfolder, elements);
        return this.delegate.getRootResource(list.toArray(new String[0]));
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        return this.delegate.getResource(packType, location);
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
        this.delegate.listResources(packType, namespace, path, resourceOutput);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return this.delegate.getNamespaces(type);
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
        IoSupplier<InputStream> iosupplier = this.getRootResource("pack.mcmeta");
        if (iosupplier == null)
            return null;
        try (InputStream inputstream = iosupplier.get()) {
            return AbstractPackResources.getMetadataFromStream(deserializer, inputstream);
        }
    }

    @Override public String packId() { return this.delegate.packId(); }
    @Override public boolean isBuiltin() { return this.delegate.isBuiltin(); }
    @Override public void close() { this.delegate.close(); }

}
