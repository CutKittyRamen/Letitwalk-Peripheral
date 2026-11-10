package dev.letitwalk.computercraft;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.letitwalk.ModContent;
import dev.letitwalk.computercraft.registry.ModBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(LetItWalkCC.MODID)
public class LetItWalkCC {
    public static final String MODID = "letitwalk_cc";

    public LetItWalkCC(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        modBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Expose Pilot Controls as a ComputerCraft Peripheral
        event.registerBlockEntity(
            PeripheralCapability.get(),
            ModContent.PILOT_CONTROLS_BE.get(),
            (be, side) -> new WalkerPeripheral(be)
        );

        // Expose Walker Core as a ComputerCraft Peripheral
        event.registerBlockEntity(
            PeripheralCapability.get(),
            ModContent.WALKER_CORE_BE.get(),
            (be, side) -> new WalkerPeripheral(be)
        );

        // Expose dedicated Walker Computer Interface as a ComputerCraft Peripheral
        event.registerBlockEntity(
            PeripheralCapability.get(),
            ModBlocks.WALKER_INTERFACE_BE.get(),
            (be, side) -> new WalkerPeripheral(be)
        );
    }

    private void onServerTick(ServerTickEvent.Post event) {
        WalkerPeripheral.tickAll();
    }
}
