package dev.letitwalk.computercraft.block;

import dev.letitwalk.computercraft.registry.ModBlocks;
import dev.letitwalk.content.WalkerLink;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class WalkerInterfaceItem extends BlockItem implements WalkerLink.Linkable {
    public WalkerInterfaceItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void link(ItemStack stack, UUID walker) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("linkedWalker", walker);
        BlockItem.setBlockEntityData(stack, ModBlocks.WALKER_INTERFACE_BE.get(), tag);
    }

    @Override
    @Nullable
    public UUID linked(ItemStack stack) {
        CustomData data = (CustomData) stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : WalkerLink.read(data.copyTag());
    }

    @Override
    public Component linkedMessage() {
        return Component.translatable("letitwalk_cc.link.interface");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        WalkerLink.tooltip(this.linked(stack), lines);
    }
}
