package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins;

public interface RFEPlugin {

    default void register() {}

    record Info(String modId, String classPath) {
    }

}
