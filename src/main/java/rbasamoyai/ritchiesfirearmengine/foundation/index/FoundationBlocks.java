package rbasamoyai.ritchiesfirearmengine.foundation.index;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.reloading.ReloadingStationBlock;

public class FoundationBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RitchiesFirearmEngine.MOD_ID);

    public static final DeferredBlock<ReloadingStationBlock> RELOADING_STATION = BLOCKS.register("reloading_station", () ->
            new ReloadingStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()));

}
