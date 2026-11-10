package dev.letitwalk.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.letitwalk.computercraft.block.WalkerInterfaceBlockEntity;
import dev.letitwalk.content.PilotControlsBlockEntity;
import dev.letitwalk.content.WalkerCoreBlock;
import dev.letitwalk.content.WalkerCoreBlockEntity;
import dev.letitwalk.walker.WalkerBrain;
import dev.letitwalk.walker.WalkerFlight;
import dev.letitwalk.walker.WalkerTracker;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WalkerPeripheral implements IPeripheral {
    public static final int KEY_UP = 1;
    public static final int KEY_DOWN = 2;
    public static final int KEY_LEFT = 4;
    public static final int KEY_RIGHT = 8;
    public static final int KEY_RAISE = 0x10;
    public static final int KEY_LOWER = 0x20;
    public static final int KEY_JUMP = 0x40;
    public static final int KEY_SPRINT = 0x80;
    public static final int KEY_FLIGHT = 0x8000;
    public static final int KEY_DASH = 0x10000;
    public static final int KEY_GRAB = 0x20000;
    public static final int KEY_CRAB = 0x40000;

    private static final Set<WalkerPeripheral> ACTIVE_PERIPHERALS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final BlockEntity sourceBe;
    private final Set<IComputerAccess> computers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Active state
    private int heldKeys = 0;
    private int currentGear = 1; // 0..4 (display 1..5)
    private boolean crabMode = false;
    private boolean gallopMode = false;

    private int remainingDriveTicks = 0; // -1 = continuous
    private int jumpChargeTicksRemaining = 0;
    private int jumpAimDirection = 0;
    private int pulseTicksRemaining = 0;
    private int pulseKeyMask = 0;

    private double turnRate = 0.0;
    private int turnTicksRemaining = 0;

    private double leanRoll = 0.0;
    private double leanPitch = 0.0;

    public WalkerPeripheral(BlockEntity sourceBe) {
        this.sourceBe = sourceBe;
    }

    public static void tickAll() {
        for (Iterator<WalkerPeripheral> it = ACTIVE_PERIPHERALS.iterator(); it.hasNext(); ) {
            WalkerPeripheral p = it.next();
            p.tick();
            if (!p.hasActiveMotion()) {
                it.remove();
            }
        }
    }

    private void markActive() {
        ACTIVE_PERIPHERALS.add(this);
    }

    public boolean hasActiveMotion() {
        return (heldKeys & 0xFF) != 0 || remainingDriveTicks != 0 || jumpChargeTicksRemaining > 0
                || pulseTicksRemaining > 0 || turnTicksRemaining > 0 || Math.abs(turnRate) > 1e-5
                || Math.abs(leanRoll) > 1e-5 || Math.abs(leanPitch) > 1e-5;
    }

    public void tick() {
        if (sourceBe == null || sourceBe.isRemoved() || sourceBe.getLevel() == null) {
            heldKeys = 0;
            turnRate = 0;
            return;
        }

        WalkerCoreBlockEntity core = resolveCore();
        if (core == null || !core.isAssembled()) {
            if (remainingDriveTicks > 0) remainingDriveTicks = 0;
            jumpChargeTicksRemaining = 0;
            pulseTicksRemaining = 0;
            turnTicksRemaining = 0;
            return;
        }

        long time = sourceBe.getLevel().getGameTime();

        // Calculate combined keys
        int keysToSend = heldKeys;
        if (crabMode) keysToSend |= KEY_CRAB;
        if (gallopMode) keysToSend |= KEY_SPRINT;
        keysToSend |= ((currentGear & 7) << 8);

        // Handle jump charge sequence
        if (jumpChargeTicksRemaining > 0) {
            keysToSend |= KEY_JUMP;
            if (jumpAimDirection != 0) {
                keysToSend |= (jumpAimDirection & 0xF);
            }
            jumpChargeTicksRemaining--;
            if (jumpChargeTicksRemaining == 0) {
                // Next tick jump key is released, which launches the jump
            }
        }

        // Handle temporary pulses (dash, grab, flight toggle, body up/down)
        if (pulseTicksRemaining > 0) {
            keysToSend |= pulseKeyMask;
            pulseTicksRemaining--;
            if (pulseTicksRemaining == 0) {
                pulseKeyMask = 0;
            }
        }

        // Auto unpark if driving
        if (core.parked && (keysToSend & 0xFF) != 0) {
            core.parked = false;
            core.syncToClients();
        }

        // Apply input to core
        core.receiveInput(keysToSend, time);
        core.receiveLean(leanRoll, leanPitch);

        if (turnTicksRemaining > 0 || Math.abs(turnRate) > 1e-5) {
            core.receiveTurn(turnRate);
            if (turnTicksRemaining > 0) {
                turnTicksRemaining--;
                if (turnTicksRemaining == 0) {
                    turnRate = 0.0;
                }
            }
        }

        // Handle timed drive
        if (remainingDriveTicks > 0) {
            remainingDriveTicks--;
            if (remainingDriveTicks == 0) {
                heldKeys &= ~(KEY_UP | KEY_DOWN | KEY_LEFT | KEY_RIGHT);
                core.receiveInput(0, time);
            }
        }
    }

    @Nullable
    public WalkerCoreBlockEntity resolveCore() {
        if (sourceBe == null || sourceBe.isRemoved() || sourceBe.getLevel() == null) {
            return null;
        }
        Level level = sourceBe.getLevel();
        if (sourceBe instanceof WalkerCoreBlockEntity core) {
            return core.isAssembled() ? core : null;
        }

        UUID linkedId = null;
        if (sourceBe instanceof PilotControlsBlockEntity pilot) {
            linkedId = pilot.linkedWalker;
        } else if (sourceBe instanceof WalkerInterfaceBlockEntity inter) {
            linkedId = inter.linkedWalker;
        }

        if (linkedId != null && level.getServer() != null) {
            WalkerCoreBlockEntity linked = WalkerCoreBlockEntity.linked(level.getServer(), linkedId);
            if (linked != null && linked.isAssembled()) {
                return linked;
            }
        }

        SubLevel sub = Sable.HELPER.getContaining(level, sourceBe.getBlockPos());
        if (sub != null) {
            BlockPos corePos = WalkerTracker.coreOf(sub);
            if (corePos != null && level.getBlockEntity(corePos) instanceof WalkerCoreBlockEntity core && core.isAssembled()) {
                return core;
            }
        }

        return null;
    }

    private WalkerCoreBlockEntity requireCore() throws LuaException {
        WalkerCoreBlockEntity core = resolveCore();
        if (core == null) {
            throw new LuaException("No assembled walker connected or loaded");
        }
        return core;
    }

    @Override
    public String getType() {
        return "walker";
    }

    @Override
    public Set<String> getAdditionalTypes() {
        if (sourceBe instanceof PilotControlsBlockEntity) {
            return Set.of("pilot_controls", "walker_pilot");
        } else if (sourceBe instanceof WalkerCoreBlockEntity) {
            return Set.of("walker_core");
        }
        return Set.of("walker_interface");
    }

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
        if (computers.isEmpty()) {
            stop();
        }
    }

    @Override
    public Object getTarget() {
        return this.sourceBe;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if (this == other) return true;
        if (!(other instanceof WalkerPeripheral p)) return false;
        return Objects.equals(this.sourceBe, p.sourceBe);
    }

    // ==========================================
    //           LUA FUNCTIONS: DRIVING
    // ==========================================

    @LuaFunction(mainThread = true)
    public final void stop() {
        heldKeys = 0;
        turnRate = 0.0;
        turnTicksRemaining = 0;
        remainingDriveTicks = 0;
        jumpChargeTicksRemaining = 0;
        pulseTicksRemaining = 0;
        pulseKeyMask = 0;
        leanRoll = 0.0;
        leanPitch = 0.0;

        WalkerCoreBlockEntity core = resolveCore();
        if (core != null && sourceBe.getLevel() != null) {
            core.receiveInput(0, sourceBe.getLevel().getGameTime());
            core.receiveLean(0.0, 0.0);
        }
    }

    @LuaFunction(mainThread = true)
    public final void setDrive(IArguments args) throws LuaException {
        requireCore();
        double fwd = args.getDouble(0);
        double side = args.getDouble(1);
        int duration = args.optInt(2, -1);

        int mask = 0;
        if (fwd > 0.01) mask |= KEY_UP;
        else if (fwd < -0.01) mask |= KEY_DOWN;

        if (side > 0.01) mask |= KEY_RIGHT;
        else if (side < -0.01) mask |= KEY_LEFT;

        this.heldKeys = (this.heldKeys & ~(KEY_UP | KEY_DOWN | KEY_LEFT | KEY_RIGHT)) | mask;
        this.remainingDriveTicks = duration;
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void forward(IArguments args) throws LuaException {
        requireCore();
        int duration = args.optInt(0, -1);
        this.heldKeys = (this.heldKeys & ~KEY_DOWN) | KEY_UP;
        this.remainingDriveTicks = duration;
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void back(IArguments args) throws LuaException {
        requireCore();
        int duration = args.optInt(0, -1);
        this.heldKeys = (this.heldKeys & ~KEY_UP) | KEY_DOWN;
        this.remainingDriveTicks = duration;
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void left(IArguments args) throws LuaException {
        requireCore();
        int duration = args.optInt(0, -1);
        this.heldKeys = (this.heldKeys & ~KEY_RIGHT) | KEY_LEFT;
        this.remainingDriveTicks = duration;
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void right(IArguments args) throws LuaException {
        requireCore();
        int duration = args.optInt(0, -1);
        this.heldKeys = (this.heldKeys & ~KEY_LEFT) | KEY_RIGHT;
        this.remainingDriveTicks = duration;
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void step(IArguments args) throws LuaException {
        requireCore();
        String dir = args.getString(0).toLowerCase(Locale.ROOT);
        int ticks = args.optInt(1, 20);

        int mask = switch (dir) {
            case "forward", "fwd", "f" -> KEY_UP;
            case "back", "backward", "b" -> KEY_DOWN;
            case "left", "l" -> KEY_LEFT;
            case "right", "r" -> KEY_RIGHT;
            default -> throw new LuaException("Invalid direction: " + dir + " (expected 'forward', 'back', 'left', 'right')");
        };

        this.heldKeys = (this.heldKeys & ~(KEY_UP | KEY_DOWN | KEY_LEFT | KEY_RIGHT)) | mask;
        this.remainingDriveTicks = ticks;
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void turn(IArguments args) throws LuaException {
        requireCore();
        double degrees = args.getDouble(0);
        int duration = args.optInt(1, 10);
        if (duration <= 0) duration = 1;

        // Spread total turn degrees across duration ticks
        this.turnRate = Math.toRadians(degrees / duration);
        this.turnTicksRemaining = duration;
        markActive();
    }

    // ==========================================
    //           LUA FUNCTIONS: GEARS
    // ==========================================

    @LuaFunction(mainThread = true)
    public final void setGear(int gear) throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        if (gear < 1 || gear > 5) {
            throw new LuaException("Gear must be between 1 and 5 (got " + gear + ")");
        }
        int maxG = WalkerBrain.maxGear(core.getTheoreticalSpeed()) + 1;
        if (gear > maxG) {
            throw new LuaException("Gear " + gear + " requires higher shaft RPM (max unlocked is " + maxG + ")");
        }
        this.currentGear = gear - 1;
        core.gear = this.currentGear;
    }

    @LuaFunction(mainThread = true)
    public final int getGear() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return core.gear + 1;
    }

    @LuaFunction(mainThread = true)
    public final int getMaxGear() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return WalkerBrain.maxGear(core.getTheoreticalSpeed()) + 1;
    }

    // ==========================================
    //           LUA FUNCTIONS: MODES
    // ==========================================

    @LuaFunction(mainThread = true)
    public final void setCrabWalk(boolean enable) throws LuaException {
        requireCore();
        this.crabMode = enable;
    }

    @LuaFunction(mainThread = true)
    public final boolean isCrabWalk() {
        return this.crabMode;
    }

    @LuaFunction(mainThread = true)
    public final boolean toggleCrabWalk() throws LuaException {
        requireCore();
        this.crabMode = !this.crabMode;
        return this.crabMode;
    }

    @LuaFunction(mainThread = true)
    public final void setGallop(boolean enable) throws LuaException {
        requireCore();
        this.gallopMode = enable;
    }

    @LuaFunction(mainThread = true)
    public final boolean isGalloping() {
        return this.gallopMode;
    }

    // ==========================================
    //           LUA FUNCTIONS: JUMP & SUSPENSION
    // ==========================================

    @LuaFunction(mainThread = true)
    public final void jump(IArguments args) throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        int chargeTicks = args.optInt(0, 25);
        int aim = args.optInt(1, 0);

        this.jumpChargeTicksRemaining = Math.max(5, chargeTicks);
        this.jumpAimDirection = aim;
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void setHeightOffset(double offset) throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        core.heightOffset = offset;
    }

    @LuaFunction(mainThread = true)
    public final double getHeightOffset() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return core.heightOffset;
    }

    @LuaFunction(mainThread = true)
    public final void bodyUp(IArguments args) throws LuaException {
        requireCore();
        int ticks = args.optInt(0, 5);
        this.pulseKeyMask |= KEY_RAISE;
        this.pulseTicksRemaining = Math.max(this.pulseTicksRemaining, ticks);
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void bodyDown(IArguments args) throws LuaException {
        requireCore();
        int ticks = args.optInt(0, 5);
        this.pulseKeyMask |= KEY_LOWER;
        this.pulseTicksRemaining = Math.max(this.pulseTicksRemaining, ticks);
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void setLean(double rollDeg, double pitchDeg) throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        this.leanRoll = Math.toRadians(rollDeg);
        this.leanPitch = Math.toRadians(pitchDeg);
        core.receiveLean(this.leanRoll, this.leanPitch);
        markActive();
    }

    // ==========================================
    //           LUA FUNCTIONS: ABILITIES
    // ==========================================

    @LuaFunction(mainThread = true)
    public final void setFlight(boolean enable) throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        if (core.flying != enable) {
            toggleFlight();
        }
    }

    @LuaFunction(mainThread = true)
    public final void toggleFlight() throws LuaException {
        requireCore();
        this.pulseKeyMask |= KEY_FLIGHT;
        this.pulseTicksRemaining = Math.max(this.pulseTicksRemaining, 2);
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final boolean isFlying() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return core.flying;
    }

    @LuaFunction(mainThread = true)
    public final void dash() throws LuaException {
        requireCore();
        this.pulseKeyMask |= KEY_DASH;
        this.pulseTicksRemaining = Math.max(this.pulseTicksRemaining, 2);
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final void grab() throws LuaException {
        requireCore();
        this.pulseKeyMask |= KEY_GRAB;
        this.pulseTicksRemaining = Math.max(this.pulseTicksRemaining, 2);
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final boolean isGrabbed() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return !core.worldHolds.isEmpty();
    }

    // ==========================================
    //           LUA FUNCTIONS: TELEMETRY
    // ==========================================

    @LuaFunction(mainThread = true)
    public final boolean isAssembled() {
        WalkerCoreBlockEntity core = resolveCore();
        return core != null && core.isAssembled();
    }

    @LuaFunction(mainThread = true)
    public final boolean isParked() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return core.parked;
    }

    @LuaFunction(mainThread = true)
    public final void setParked(boolean parked) throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        if (core.parked != parked) {
            core.parked = parked;
            core.syncToClients();
        }
    }

    @LuaFunction(mainThread = true)
    public final boolean isPowered() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return core.isPowered();
    }

    @LuaFunction(mainThread = true)
    public final double getRpm() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return Math.abs(core.getTheoreticalSpeed());
    }

    @LuaFunction(mainThread = true)
    public final MethodResult getFuel() throws LuaException {
        return getLava();
    }

    @LuaFunction(mainThread = true)
    public final MethodResult getLava() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return MethodResult.of(core.lavaTank.getFluidAmount(), core.lavaTank.getCapacity());
    }

    @LuaFunction(mainThread = true)
    public final int getLegCount() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        return core.legs.size();
    }

    @LuaFunction(mainThread = true)
    public final MethodResult getPosition() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        Level level = core.getLevel();
        SubLevel sub = Sable.HELPER.getContaining(level, core.getBlockPos());
        if (sub != null) {
            Vector3dc pos = sub.logicalPose().position();
            return MethodResult.of(pos.x(), pos.y(), pos.z());
        }
        BlockPos pos = core.getBlockPos();
        return MethodResult.of(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    @LuaFunction(mainThread = true)
    public final MethodResult getOrientation() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        Level level = core.getLevel();
        SubLevel sub = Sable.HELPER.getContaining(level, core.getBlockPos());
        if (sub != null) {
            Quaterniondc q = sub.logicalPose().orientation();
            Vector3d euler = new Vector3d();
            q.getEulerAnglesYXZ(euler);
            return MethodResult.of(Math.toDegrees(euler.y), Math.toDegrees(euler.x), Math.toDegrees(euler.z));
        }
        Direction facing = (Direction) core.getBlockState().getValue(WalkerCoreBlock.FACING);
        return MethodResult.of(facing.toYRot(), 0.0, 0.0);
    }

    @LuaFunction(mainThread = true)
    public final MethodResult getVelocity() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        Level level = core.getLevel();
        SubLevel sub = Sable.HELPER.getContaining(level, core.getBlockPos());
        if (sub != null) {
            Vector3dc currPos = sub.logicalPose().position();
            Vector3dc lastPos = sub.lastPose().position();
            double vx = (currPos.x() - lastPos.x()) * 20.0;
            double vy = (currPos.y() - lastPos.y()) * 20.0;
            double vz = (currPos.z() - lastPos.z()) * 20.0;
            return MethodResult.of(vx, vy, vz);
        }
        return MethodResult.of(core.targetVelocity.x, 0.0, core.targetVelocity.z);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getStatus() {
        Map<String, Object> map = new LinkedHashMap<>();
        WalkerCoreBlockEntity core = resolveCore();
        if (core == null || !core.isAssembled()) {
            map.put("assembled", false);
            return map;
        }

        map.put("assembled", true);
        map.put("parked", core.parked);
        map.put("powered", core.isPowered());
        map.put("rpm", Math.abs(core.getTheoreticalSpeed()));
        map.put("gear", core.gear + 1);
        map.put("maxGear", WalkerBrain.maxGear(core.getTheoreticalSpeed()) + 1);
        map.put("flying", core.flying);
        map.put("grabbed", !core.worldHolds.isEmpty());
        map.put("crab", crabMode);
        map.put("gallop", gallopMode);
        map.put("fuel", core.lavaTank.getFluidAmount());
        map.put("fuelCapacity", core.lavaTank.getCapacity());
        map.put("legs", core.legs.size());
        map.put("heightOffset", core.heightOffset);
        map.put("throttle", core.throttle);
        map.put("strafe", core.strafe);
        map.put("load", core.load);

        if (core.walkerId() != null) {
            map.put("walkerId", core.walkerId().toString());
        }

        Level level = core.getLevel();
        SubLevel sub = level != null ? Sable.HELPER.getContaining(level, core.getBlockPos()) : null;
        if (sub != null) {
            Vector3dc pos = sub.logicalPose().position();
            map.put("x", pos.x());
            map.put("y", pos.y());
            map.put("z", pos.z());

            Vector3dc currPos = sub.logicalPose().position();
            Vector3dc lastPos = sub.lastPose().position();
            map.put("vx", (currPos.x() - lastPos.x()) * 20.0);
            map.put("vy", (currPos.y() - lastPos.y()) * 20.0);
            map.put("vz", (currPos.z() - lastPos.z()) * 20.0);

            Quaterniondc q = sub.logicalPose().orientation();
            Vector3d euler = new Vector3d();
            q.getEulerAnglesYXZ(euler);
            map.put("yaw", Math.toDegrees(euler.y));
            map.put("pitch", Math.toDegrees(euler.x));
            map.put("roll", Math.toDegrees(euler.z));
        } else {
            BlockPos bp = core.getBlockPos();
            map.put("x", bp.getX() + 0.5);
            map.put("y", bp.getY() + 0.5);
            map.put("z", bp.getZ() + 0.5);
            map.put("vx", 0.0);
            map.put("vy", 0.0);
            map.put("vz", 0.0);
            Direction facing = (Direction) core.getBlockState().getValue(WalkerCoreBlock.FACING);
            map.put("yaw", (double) facing.toYRot());
            map.put("pitch", 0.0);
            map.put("roll", 0.0);
        }

        return map;
    }

    @LuaFunction(mainThread = true)
    public final String getWalkerId() throws LuaException {
        WalkerCoreBlockEntity core = requireCore();
        UUID id = core.walkerId();
        return id != null ? id.toString() : "";
    }

    @LuaFunction(mainThread = true)
    public final void setRawKeys(IArguments args) throws LuaException {
        requireCore();
        int keys = args.getInt(0);
        int duration = args.optInt(1, -1);
        this.heldKeys = keys;
        this.remainingDriveTicks = duration;
        markActive();
    }

    @LuaFunction(mainThread = true)
    public final int getRawKeys() {
        return this.heldKeys;
    }
}
