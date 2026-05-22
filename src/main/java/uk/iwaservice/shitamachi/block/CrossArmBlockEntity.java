package uk.iwaservice.shitamachi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import uk.iwaservice.shitamachi.Shitamachi;

import java.util.HashSet;
import java.util.Set;

public class CrossArmBlockEntity extends CableBlockEntity {

    private final Set<String> disabledArms = new HashSet<>();

    public CrossArmBlockEntity(BlockPos pos, BlockState state) {
        super(Shitamachi.CROSS_ARM_BE.get(), pos, state);
    }

    public boolean isDisabled(String key) {
        return disabledArms.contains(key);
    }

    /** Toggles disabled state. Returns true if now disabled. */
    public boolean toggleDisabled(String key) {
        if (disabledArms.contains(key)) {
            disabledArms.remove(key);
            setChanged();
            return false;
        } else {
            disabledArms.add(key);
            setChanged();
            return true;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!disabledArms.isEmpty()) {
            tag.putString("disabled_arms", String.join(",", disabledArms));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        disabledArms.clear();
        if (tag.contains("disabled_arms")) {
            for (String key : tag.getString("disabled_arms").split(",")) {
                if (!key.isEmpty()) disabledArms.add(key);
            }
        }
    }
}
