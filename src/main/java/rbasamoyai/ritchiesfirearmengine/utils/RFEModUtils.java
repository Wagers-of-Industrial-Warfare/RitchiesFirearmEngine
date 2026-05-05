package rbasamoyai.ritchiesfirearmengine.utils;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

public class RFEModUtils {

    public static String getModVersionSpecifier() {
        return "forge_version";
    }

    public static boolean isModPresent(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    public static boolean isModPresentAndSatisfiesVersion(String modId, String version) {
        net.neoforged.neoforgespi.language.IModFileInfo modFileInfo = LoadingModList.get().getModFileById(modId);
        if (modFileInfo == null)
            return false;
        VersionRange range;
        try {
            range = VersionRange.createFromVersionSpec(version);
        } catch (InvalidVersionSpecificationException exception) {
            throw new IllegalStateException("Mod dependency " + modId + " has invalid version specification " + version);
        }
        for (IModInfo modInfo : modFileInfo.getMods()) {
            if (modInfo.getModId().equals(modId) && range.containsVersion(modInfo.getVersion()))
                return true;
        }
        return false;
    }

    private RFEModUtils () {
    }

}
