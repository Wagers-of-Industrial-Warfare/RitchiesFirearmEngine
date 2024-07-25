package rbasamoyai.ritchiesfirearmengine.default_index;

import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.content.AmmoItem;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins.RFEPlugin;

/**
 * Because it's always best to lead by example.
 */
public class BuiltInRFEPlugin implements RFEPlugin {

    @Override
    public void registerItemBuilders() {
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("ammo"), new AmmoItem.Builder());
    }

}
