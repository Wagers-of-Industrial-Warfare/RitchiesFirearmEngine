package rbasamoyai.ritchiesfirearmengine.foundation.pack_loading;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Copied from {@link net.minecraft.server.packs.repository.FolderRepositorySource.FolderPackDetector}
 */
public class FolderPackDetector extends PackDetector<Pack.ResourcesSupplier> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public FolderPackDetector(DirectoryValidator validator) {
        super(validator);
    }

    @Nullable
    protected Pack.ResourcesSupplier createZipPack(Path path) {
        FileSystem filesystem = path.getFileSystem();
        if (filesystem != FileSystems.getDefault() && !(filesystem instanceof LinkFileSystem)) {
            LOGGER.info("Can't open pack archive at {}", path);
            return null;
        } else {
            return new FilePackResources.FileResourcesSupplier(path);
        }
    }

    protected Pack.ResourcesSupplier createDirectoryPack(Path path) {
        return new PathPackResources.PathResourcesSupplier(path);
    }
}
