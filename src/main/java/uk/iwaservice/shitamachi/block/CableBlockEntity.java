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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class CableBlockEntity extends BlockEntity {
    public static final int MAX_CONNECTIONS = 8;
    public static final int MAX_DISTANCE    = 32;

    private final List<ConnectionData> connections = new ArrayList<>();

    protected CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public List<ConnectionData> getConnections() {
        return List.copyOf(connections);
    }

    public boolean canConnect(BlockPos other) {
        return connections.size() < MAX_CONNECTIONS
                && connections.stream().noneMatch(c -> c.pos().equals(other))
                && !other.equals(worldPosition)
                && worldPosition.distSqr(other) <= (double) (MAX_DISTANCE * MAX_DISTANCE);
    }

    public boolean addConnection(BlockPos other, float thickness, float sag) {
        if (!canConnect(other)) return false;
        connections.add(new ConnectionData(other.immutable(), thickness, sag));
        setChanged();
        return true;
    }

    public boolean removeConnection(BlockPos other) {
        boolean removed = connections.removeIf(c -> c.pos().equals(other));
        if (removed) setChanged();
        return removed;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (ConnectionData conn : connections) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", conn.pos().getX());
            entry.putInt("y", conn.pos().getY());
            entry.putInt("z", conn.pos().getZ());
            entry.putFloat("thickness", conn.thickness());
            entry.putFloat("sag", conn.sag());
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
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            float thickness = entry.contains("thickness") ? entry.getFloat("thickness") : ConnectionData.DEFAULT_THICKNESS;
            float sag       = entry.contains("sag")       ? entry.getFloat("sag")       : ConnectionData.DEFAULT_SAG;
            connections.add(new ConnectionData(pos, thickness, sag));
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
