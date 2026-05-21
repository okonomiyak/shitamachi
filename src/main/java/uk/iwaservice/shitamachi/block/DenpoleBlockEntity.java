package uk.iwaservice.shitamachi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import uk.iwaservice.shitamachi.Shitamachi;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DenpoleBlockEntity extends BlockEntity {
    public static final int MAX_CONNECTIONS = 4;
    public static final int MAX_DISTANCE = 32;

    private final List<BlockPos> connections = new ArrayList<>();

    public DenpoleBlockEntity(BlockPos pos, BlockState state) {
        super(Shitamachi.DENPOLE_BE.get(), pos, state);
    }

    public List<BlockPos> getConnections() {
        return List.copyOf(connections);
    }

    public boolean canConnect(BlockPos other) {
        return connections.size() < MAX_CONNECTIONS
                && !connections.contains(other)
                && !other.equals(worldPosition)
                && worldPosition.distSqr(other) <= (double) (MAX_DISTANCE * MAX_DISTANCE);
    }

    public boolean addConnection(BlockPos other) {
        if (!canConnect(other)) return false;
        connections.add(other.immutable());
        setChanged();
        return true;
    }

    public boolean removeConnection(BlockPos other) {
        boolean removed = connections.remove(other);
        if (removed) setChanged();
        return removed;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (BlockPos pos : connections) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", pos.getX());
            entry.putInt("y", pos.getY());
            entry.putInt("z", pos.getZ());
            list.add(entry);
        }
        tag.put("connections", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        connections.clear();
        ListTag list = tag.getList("connections", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            connections.add(new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
