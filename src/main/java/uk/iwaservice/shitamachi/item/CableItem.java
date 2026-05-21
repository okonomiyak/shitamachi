package uk.iwaservice.shitamachi.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import uk.iwaservice.shitamachi.block.DenpoleBlock;
import uk.iwaservice.shitamachi.block.DenpoleBlockEntity;

public class CableItem extends Item {
    private static final String KEY_FROM = "denpole_from";

    public CableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;
        if (!(level.getBlockState(pos).getBlock() instanceof DenpoleBlock)) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        CompoundTag playerData = player.getPersistentData();

        if (!playerData.contains(KEY_FROM)) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            playerData.put(KEY_FROM, posTag);
            player.sendSystemMessage(Component.literal("接続元の電柱を選択しました。次の電柱をクリックしてください。"));
            return InteractionResult.SUCCESS;
        }

        CompoundTag fromTag = playerData.getCompound(KEY_FROM);
        playerData.remove(KEY_FROM);

        BlockPos fromPos = new BlockPos(fromTag.getInt("x"), fromTag.getInt("y"), fromTag.getInt("z"));

        if (fromPos.equals(pos)) {
            player.sendSystemMessage(Component.literal("接続をキャンセルしました。"));
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockState(fromPos).getBlock() instanceof DenpoleBlock)) {
            player.sendSystemMessage(Component.literal("接続元の電柱が見つかりません。"));
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(fromPos) instanceof DenpoleBlockEntity fromBE)
                || !(level.getBlockEntity(pos) instanceof DenpoleBlockEntity toBE)) {
            player.sendSystemMessage(Component.literal("エラーが発生しました。"));
            return InteractionResult.SUCCESS;
        }

        if (fromBE.getConnections().contains(pos)) {
            player.sendSystemMessage(Component.literal("すでに接続されています。"));
            return InteractionResult.SUCCESS;
        }

        if (fromBE.canConnect(pos) && toBE.canConnect(fromPos)) {
            fromBE.addConnection(pos);
            toBE.addConnection(fromPos);
            level.sendBlockUpdated(fromPos, level.getBlockState(fromPos), level.getBlockState(fromPos), 3);
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            player.sendSystemMessage(Component.literal("電柱を接続しました！"));
            if (!player.isCreative()) {
                context.getItemInHand().shrink(1);
            }
        } else {
            player.sendSystemMessage(Component.literal("接続できません（距離が遠すぎるか、接続数が上限です）。"));
        }

        return InteractionResult.SUCCESS;
    }
}
