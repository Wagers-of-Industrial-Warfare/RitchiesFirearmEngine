package rbasamoyai.ritchiesfirearmengine.data_packing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Copied from SimpleJsonResourceReloadListener, with some tweaks to support Multimap; using similar to tag loading
 */
public abstract class RFEJsonResourceReloadListener extends SimplePreparableReloadListener<Multimap<ResourceLocation, JsonElement>> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int PATH_SUFFIX_LENGTH = ".json".length();
	private final Gson gson;
	private final String directory;

	protected RFEJsonResourceReloadListener(Gson gson, String directory) {
		this.gson = gson;
		this.directory = directory;
	}

	@Override
	protected Multimap<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		Multimap<ResourceLocation, JsonElement> map = HashMultimap.create();
		scanDirectory(resourceManager, this.directory, this.gson, map);
		return map;
	}

	public static void scanDirectory(ResourceManager resourceManager, String name, Gson gson, Multimap<ResourceLocation, JsonElement> output) {
		FileToIdConverter filetoidconverter = FileToIdConverter.json(name);

		for(Map.Entry<ResourceLocation, Resource> jsonResource : filetoidconverter.listMatchingResources(resourceManager).entrySet()) {
			ResourceLocation fileId = jsonResource.getKey();
			ResourceLocation id = filetoidconverter.fileToId(fileId);

			try (Reader reader = jsonResource.getValue().openAsReader()) {
				JsonElement jsonelement = GsonHelper.fromJson(gson, reader, JsonElement.class);
				output.put(id, jsonelement);
			} catch (IllegalArgumentException | IOException | JsonParseException e) {
				LOGGER.error("Couldn't parse data file {} from {}", id, fileId, e);
			}
		}
	}

}
