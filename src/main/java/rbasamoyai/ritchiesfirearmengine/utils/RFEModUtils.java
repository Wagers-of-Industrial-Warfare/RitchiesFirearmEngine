package rbasamoyai.ritchiesfirearmengine.utils;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

public class RFEModUtils {

    public static String getModVersionSpecifier() {
        return "forge_version";
    }

    public static boolean isModPresent(String modId) {
        return ModList.get().getModFileById(modId) != null;
    }

    public static boolean isModPresentAndSatisfiesVersion(String modId, String version) {
        IModFileInfo modFileInfo = ModList.get().getModFileById(modId);
        if (modFileInfo == null)
            return false;
        VersionRange range;
        try {
            range = VersionRange.createFromVersionSpec(version);
        } catch (InvalidVersionSpecificationException exception) {
            RitchiesFirearmEngine.LOGGER.error("Mod dependency {} has invalid version specification {}", modId, version);
            return false;
        }
        for (IModInfo modInfo : modFileInfo.getMods()) {
            if (modInfo.getModId().equals(modId) && range.containsVersion(modInfo.getVersion()))
                return true;
        }
        return false;
    }

}
