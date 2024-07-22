package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins;

public interface RFEPlugin {

    void registerItemBuilders();

    record Info(String modId, String classPath) {
    }

}
