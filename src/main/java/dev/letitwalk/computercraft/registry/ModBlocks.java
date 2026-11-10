package dev.letitwalk.computercraft.registry;

import dev.letitwalk.computercraft.LetItWalkCC;
import dev.letitwalk.computercraft.block.WalkerInterfaceBlock;
import dev.letitwalk.computercraft.block.WalkerInterfaceBlockEntity;
import dev.letitwalk.computercraft.block.WalkerInterfaceItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LetItWalkCC.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LetItWalkCC.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LetItWalkCC.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LetItWalkCC.MODID);

    public static final DeferredBlock<WalkerInterfaceBlock> WALKER_INTERFACE_BLOCK = BLOCKS.register(
        "walker_interface",
        () -> new WalkerInterfaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0f))
    );

    public static final DeferredItem<WalkerInterfaceItem> WALKER_INTERFACE_ITEM = ITEMS.register(
        "walker_interface",
        () -> new WalkerInterfaceItem(WALKER_INTERFACE_BLOCK.get(), new Item.Properties().stacksTo(1))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WalkerInterfaceBlockEntity>> WALKER_INTERFACE_BE = BLOCK_ENTITIES.register(
        "walker_interface",
        () -> BlockEntityType.Builder.of(WalkerInterfaceBlockEntity::new, WALKER_INTERFACE_BLOCK.get()).build(null)
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.letitwalk_cc"))
            .icon(() -> new ItemStack(WALKER_INTERFACE_ITEM.get()))
            .displayItems((parameters, output) -> output.accept(WALKER_INTERFACE_ITEM.get()))
            .build()
    );

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
    }

    private ModBlocks() {
    }
}
