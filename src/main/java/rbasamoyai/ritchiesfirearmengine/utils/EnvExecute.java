package rbasamoyai.ritchiesfirearmengine.utils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public class EnvExecute {

    public static void runOnClient(Supplier<Runnable> toRun) {
        if (FMLEnvironment.dist == Dist.CLIENT)
            toRun.get().run();
    }

}
