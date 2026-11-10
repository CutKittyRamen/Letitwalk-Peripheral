package dev.letitwalk.computercraft.block;

import dev.letitwalk.content.SimpleFacingBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WalkerInterfaceBlock extends SimpleFacingBlock implements EntityBlock {

    public WalkerInterfaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WalkerInterfaceBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WalkerInterfaceBlockEntity wbe) {
                if (wbe.linkedWalker != null) {
                    player.displayClientMessage(Component.translatable("letitwalk_cc.interface.status_linked", wbe.linkedWalker.toString().substring(0, 8)), true);
                } else {
                    SubLevel sub = Sable.HELPER.getContaining(level, pos);
                    if (sub != null) {
                        player.displayClientMessage(Component.translatable("letitwalk_cc.interface.status_onboard"), true);
                    } else {
                        player.displayClientMessage(Component.translatable("letitwalk_cc.interface.status_unlinked"), true);
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
