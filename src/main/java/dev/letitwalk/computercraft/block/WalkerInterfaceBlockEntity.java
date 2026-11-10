package dev.letitwalk.computercraft.block;

import dev.letitwalk.computercraft.registry.ModBlocks;
import dev.letitwalk.content.WalkerLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class WalkerInterfaceBlockEntity extends BlockEntity {
    @Nullable
    public UUID linkedWalker;

    public WalkerInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WALKER_INTERFACE_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.linkedWalker != null) {
            tag.putUUID("linkedWalker", this.linkedWalker);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.linkedWalker = WalkerLink.read(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = this.saveWithoutMetadata(registries);
        if (this.linkedWalker != null) {
            tag.putUUID("linkedWalker", this.linkedWalker);
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
