package uk.iwaservice.shitamachi.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import uk.iwaservice.shitamachi.item.CableItem;

public record ServerboundSetCableConfigPacket(float thickness, float sag) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundSetCableConfigPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("shitamachi", "set_cable_config"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundSetCableConfigPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> { buf.writeFloat(p.thickness()); buf.writeFloat(p.sag()); },
                    buf -> new ServerboundSetCableConfigPacket(buf.readFloat(), buf.readFloat())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundSetCableConfigPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof CableItem)) {
                stack = player.getOffhandItem();
                if (!(stack.getItem() instanceof CableItem)) return;
            }
            CompoundTag tag = stack.has(DataComponents.CUSTOM_DATA)
                    ? stack.get(DataComponents.CUSTOM_DATA).copyTag()
                    : new CompoundTag();
            tag.putFloat("cable_thickness", packet.thickness());
            tag.putFloat("cable_sag", packet.sag());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        });
    }
}
