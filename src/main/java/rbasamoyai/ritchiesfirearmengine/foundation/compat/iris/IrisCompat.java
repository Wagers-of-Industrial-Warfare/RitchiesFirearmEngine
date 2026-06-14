package rbasamoyai.ritchiesfirearmengine.foundation.compat.iris;

import net.irisshaders.iris.api.v0.IrisApi;

public class IrisCompat {

    private IrisCompat() {}

    public static boolean isShadersEnabled() { return IrisApi.getInstance().isShaderPackInUse(); }

}
