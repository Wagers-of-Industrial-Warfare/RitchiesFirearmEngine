package rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.workstation_crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.Optional;

public abstract class AbstractWorkstationBlockEntity extends BlockEntity {

    protected NonNullList<ItemStack> items = NonNullList.create();
    protected boolean isCraftingRecipe;
    protected int recipeProgress;
    protected @Nullable ResourceLocation currentRecipeId;

    public AbstractWorkstationBlockEntity(BlockEntityType<? extends AbstractWorkstationBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    protected abstract RecipeType<? extends AbstractWorkstationRecipe> getRecipeType();

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag itemsTag = new ListTag(this.items.size());
        for (ItemStack itemStack : this.items)
            itemsTag.add(itemStack.isEmpty() ? new CompoundTag() : itemStack.save(registries, new CompoundTag()));
        tag.put("Items", itemsTag);
        if (this.isCraftingRecipe)
            tag.putBoolean("IsCraftingRecipe", true);
        if (this.currentRecipeId != null) {
            tag.putString("Recipe", this.currentRecipeId.toString());
            tag.putInt("RecipeProgress", Math.max(this.recipeProgress, 0));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ListTag itemsTag = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.size(); ++i)
             this.items.add(ItemStack.parseOptional(registries, itemsTag.getCompound(i)));
        if (!this.tryLoadingRecipe(tag, registries)) {
            this.currentRecipeId = null;
            this.isCraftingRecipe = false;
            this.recipeProgress = 0;
        }
    }

    protected boolean tryLoadingRecipe(CompoundTag tag, HolderLookup.Provider registries) {
        if (!this.hasLevel() || !tag.contains("Recipe", Tag.TAG_STRING))
            return false;
        this.currentRecipeId = ResourceLocation.tryParse(tag.getString("Recipe"));
        this.isCraftingRecipe = tag.contains("IsCraftingRecipe");
        this.recipeProgress = Math.max(0, tag.getInt("RecipeProgress"));
        return true;
    }

    public void tick() {
        if (this.level.isClientSide) {
            return;
        }
        if (this.isCraftingRecipe)
            this.tickRecipe();
    }

    protected void tickRecipe() {
        Optional<? extends RecipeHolder<? extends AbstractWorkstationRecipe>> recipeOptional = this.getCurrentRecipe();
        if (recipeOptional.isEmpty() || !this.recipeMatchesInput(recipeOptional.get())) {
            this.resetCraftingProgress();
            return;
        }
        ++this.recipeProgress;
        if (this.recipeProgress >= recipeOptional.get().value().getCraftingTime()) {
            this.assembleRecipe(recipeOptional.get());
            this.resetCraftingProgress();
        }
    }

    protected void resetCraftingProgress() {
        this.recipeProgress = 0;
        this.isCraftingRecipe = false; // TODO automation
    }

    protected boolean recipeMatchesInput(RecipeHolder<? extends AbstractWorkstationRecipe> recipeHolder) {
        return recipeHolder.value().matches(new WorkstationInput(this.items), this.level);
    }

    protected void assembleRecipe(RecipeHolder<? extends AbstractWorkstationRecipe> recipeHolder) {
        recipeHolder.value().assemble(new WorkstationInput(this.items), this.level.registryAccess());
    }

    public int getRecipeProgress() { return this.recipeProgress; }

    public void setCurrentRecipe(@Nullable ResourceLocation recipeId) { this.currentRecipeId = recipeId; }
    public @Nullable ResourceLocation getCurrentRecipeId() { return this.currentRecipeId; }

    public Optional<? extends RecipeHolder<? extends AbstractWorkstationRecipe>> getCurrentRecipe() {
        return this.currentRecipeId == null ? Optional.empty() : RFEUtils.getTypedRecipeByKey(this.level.getRecipeManager(),
                this.getRecipeType(), this.currentRecipeId);
    }

}
