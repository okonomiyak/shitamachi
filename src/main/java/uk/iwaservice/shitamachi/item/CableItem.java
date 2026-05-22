package uk.iwaservice.shitamachi.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import uk.iwaservice.shitamachi.block.CableBlockEntity;
import uk.iwaservice.shitamachi.block.ConnectionData;
import uk.iwaservice.shitamachi.block.CrossArmBlock;
import uk.iwaservice.shitamachi.block.DenpoleBlock;
import uk.iwaservice.shitamachi.client.CableConfigScreen;

public class CableItem extends Item {
    private static final String KEY_FROM = "cable_from";

    public CableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) openConfigScreen(player.getItemInHand(hand));
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openConfigScreen(ItemStack stack) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new CableConfigScreen(stack));
    }

    private static boolean isCableBlock(Block block) {
        return block instanceof DenpoleBlock || block instanceof CrossArmBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;
        if (!isCableBlock(level.getBlockState(pos).getBlock())) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        CompoundTag playerData = player.getPersistentData();

        if (!playerData.contains(KEY_FROM)) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            playerData.put(KEY_FROM, posTag);
            player.sendSystemMessage(Component.literal("接続元を選択しました。次をクリックしてください。"));
            return InteractionResult.SUCCESS;
        }

        CompoundTag fromTag = playerData.getCompound(KEY_FROM);
        playerData.remove(KEY_FROM);

        BlockPos fromPos = new BlockPos(fromTag.getInt("x"), fromTag.getInt("y"), fromTag.getInt("z"));

        if (fromPos.equals(pos)) {
            player.sendSystemMessage(Component.literal("接続をキャンセルしました。"));
            return InteractionResult.SUCCESS;
        }

        if (!isCableBlock(level.getBlockState(fromPos).getBlock())) {
            player.sendSystemMessage(Component.literal("接続元のブロックが見つかりません。"));
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(fromPos) instanceof CableBlockEntity fromBE)
                || !(level.getBlockEntity(pos) instanceof CableBlockEntity toBE)) {
            player.sendSystemMessage(Component.literal("エラーが発生しました。"));
            return InteractionResult.SUCCESS;
        }

        if (fromBE.getConnections().stream().anyMatch(c -> c.pos().equals(pos))) {
            player.sendSystemMessage(Component.literal("すでに接続されています。"));
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        float thickness = ConnectionData.DEFAULT_THICKNESS;
        float sag       = ConnectionData.DEFAULT_SAG;
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("cable_thickness")) thickness = tag.getFloat("cable_thickness");
            if (tag.contains("cable_sag"))       sag       = tag.getFloat("cable_sag");
        }

        if (fromBE.canConnect(pos) && toBE.canConnect(fromPos)) {
            fromBE.addConnection(pos, thickness, sag);
            toBE.addConnection(fromPos, thickness, sag);
            level.sendBlockUpdated(fromPos, level.getBlockState(fromPos), level.getBlockState(fromPos), 3);
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            player.sendSystemMessage(Component.literal("接続しました！"));
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        } else {
            player.sendSystemMessage(Component.literal("接続できません（距離が遠すぎるか、接続数が上限です）。"));
        }

        return InteractionResult.SUCCESS;
    }
}
