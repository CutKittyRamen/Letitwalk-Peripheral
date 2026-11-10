# Let It Walk: ComputerCraft Addon (`letitwalk_cc`)

<p align="center">
  <img src="icon.png" width="160" height="160" alt="Let It Walk CC Icon">
</p>

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)]()
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.249-orange.svg)]()
[![CC: Tweaked](https://img.shields.io/badge/CC:%20Tweaked-1.120.2+-blue.svg)]()
[![Let It Walk](https://img.shields.io/badge/Let%20It%20Walk-0.1.4%20--%200.1.5+-red.svg)]()
[![Version](https://img.shields.io/badge/Release-0.0.2--alpha-blue.svg)]()

A NeoForge 1.21.1 addon for [Let It Walk](https://modrinth.com/mod/let-it-walk) that turns walking mechs and contraptions into fully programmable, autonomous units controlled via **ComputerCraft: Tweaked**.

**Authors:** CutCatRamen, Antigravity

---

## Features

- **On-Board Peripheral Control**: Directly hook a CC Computer or Wired Modem to existing **Pilot Controls** or **Walker Core** block entities.
- **Remote Station Control**: A dedicated **Walker Computer Interface** (`letitwalk_cc:walker_interface`) block that links to any walker core with a right-click, enabling long-range remote telemetry and piloting.
- **Complete Lua API**: Full access to locomotion (throttle, steering, stepping, turning), transmission (gears 1-5), modes (crab walk, gallop), hull leaning, suspension jumps, flight thrusters, magnetic foot locking, and real-time 3D telemetry (position, orientation, velocity, RPM, fuel).
- **Tick-Synchronized Input Engine**: Keeps input keys active and refreshed every server tick so movement is completely smooth.
- **CC: Sable & Celestial Navigation Compatible**: Works seamlessly with *CC: Sable* Sub-Level physics tracking and *Create* Navigation Table compass bearings.

---

## Getting Started in Game

1. Place `letitwalk_cc-0.0.2-alpha.jar` and `cc-tweaked-1.21.1-forge-*.jar` into your Minecraft `mods` folder.
2. **On-Board Setup**:
   - Place a CC Computer directly adjacent to the **Pilot Controls** or **Walker Core**.
3. **Remote Setup**:
   - Craft a **Walker Computer Interface** (`letitwalk_cc:walker_interface`).
   - Right-click an assembled Walker Core with the interface item to link them.
   - Place the interface block anywhere in the world and place a CC Computer adjacent to it.
4. Open the computer terminal and start coding!

---

## 📚 Complete Lua Peripheral API Reference

Once wrapped (`local walker = peripheral.find("walker")`), the peripheral exposes the following methods:

### 🚶 Locomotion & Steering
| Method | Description |
| :--- | :--- |
| `walker.setDrive(throttle, steer, [ticks])` | Drives the mech forward/backward (`-1.0` to `1.0`) and lateral stride (`-1.0` to `1.0`) for `[ticks]` (or continuous if omitted). |
| `walker.forward([ticks])` | Strides forward for `[ticks]` (or continuous if omitted). |
| `walker.back([ticks])` | Strides backward for `[ticks]` (or continuous if omitted). |
| `walker.left([ticks])` | Strides left for `[ticks]` (or continuous if omitted). |
| `walker.right([ticks])` | Strides right for `[ticks]` (or continuous if omitted). |
| `walker.step(direction, [ticks])` | Takes a discrete step in `"forward"`, `"back"`, `"left"`, or `"right"` direction for `[ticks]`. |
| `walker.turn(rate, [ticks])` | Applies steering yaw turn rate (`-1.0` to `1.0`) for `[ticks]`. |
| `walker.stop()` | Instantly cancels all active drive inputs and brakes to a halt. |

### ⚙️ Transmission & Gears
| Method | Description |
| :--- | :--- |
| `walker.setGear(gear)` | Shifts transmission gear (`1` to `5`). Fails if rotational RPM is too low for that gear. |
| `walker.getGear()` | Returns current active gear (`1` to `5`). |
| `walker.getMaxGear()` | Returns maximum unlocked gear supported by the current input RPM. |
| `walker.getRpm()` | Returns input rotational speed in RPM. |

### 🦀 Modes & Suspension
| Method | Description |
| :--- | :--- |
| `walker.setCrabWalk(boolean)` | Enables or disables lateral crab-walking mode. |
| `walker.isCrabWalk()` | Returns `true` if crab-walking mode is active. |
| `walker.toggleCrabWalk()` | Toggles crab-walking mode on/off. |
| `walker.setGallop(boolean)` | Enables or disables sprint galloping mode. |
| `walker.isGalloping()` | Returns `true` if galloping mode is active. |
| `walker.jump([chargeTicks], [aim])` | Charges kinetic suspension leap for `[chargeTicks]` (default 25) in directional aim `[aim]`. |
| `walker.bodyUp([ticks])` | Raises walker chassis suspension height. |
| `walker.bodyDown([ticks])` | Lowers walker chassis suspension height. |
| `walker.setHeightOffset(offset)` | Directly sets chassis hover height offset (in blocks). |
| `walker.getHeightOffset()` | Returns current chassis hover height offset. |
| `walker.setLean(rollDeg, pitchDeg)` | Actively tilts and leans the mech chassis by specified degrees. |

### 🚀 Jet Thrusters & Abilities
| Method | Description |
| :--- | :--- |
| `walker.setFlight(boolean)` | Activates or shuts down jet engine flight mode. |
| `walker.toggleFlight()` | Toggles jet engine flight mode. |
| `walker.isFlying()` | Returns `true` if currently flying with jet engines. |
| `walker.dash()` | Triggers an instantaneous jet boost dash. |
| `walker.grab()` | Toggles suction/climbing clamps on terrain and surfaces. |
| `walker.isGrabbed()` | Returns `true` if walker limbs are clamped onto a surface. |

### 📊 Telemetry & State
| Method | Description |
| :--- | :--- |
| `walker.isAssembled()` | Returns `true` if the walker is assembled into a physics sub-level. |
| `walker.isParked()` | Returns `true` if the mech is parked (legs splayed, brakes locked). |
| `walker.setParked(boolean)` | Parks or unparks the walker. |
| `walker.isPowered()` | Returns `true` if the Walker Core has rotational kinetic power. |
| `walker.getLava()` / `getFuel()` | Returns `(amount, capacity)` of the onboard lava fuel tank in mB. |
| `walker.getLegCount()` | Returns total number of active articulated limbs. |
| `walker.getPosition()` | Returns world position `(x, y, z)`. |
| `walker.getOrientation()` | Returns chassis angles `(yaw, pitch, roll)` in degrees. |
| `walker.getVelocity()` | Returns live velocity vector `(vx, vy, vz)` in blocks/second. |
| `walker.getStatus()` | Returns a comprehensive status dictionary containing all telemetry in a single call. |
| `walker.getWalkerId()` | Returns the unique UUID string of the assembled walker. |

### ⌨️ Low-Level Key Injection
| Method | Description |
| :--- | :--- |
| `walker.setRawKeys(keysBitmask, [ticks])` | Direct low-level key bitmask injection (Forward=1, Back=2, Left=4, Right=8, Jump=64, Sprint=128). |
| `walker.getRawKeys()` | Returns the active raw key bitmask. |

---

## Quick Lua Example

```lua
local walker = peripheral.find("walker")
if not walker then
    error("No walker peripheral found! Connect to Pilot Controls or Walker Core.")
end

-- Unpark the mech and shift to Gear 2
walker.setParked(false)
walker.setGear(2)

-- Drive forward for 3 seconds (60 ticks)
print("Striding forward...")
walker.setDrive(1.0, 0.0, 60)

-- Check live position and speed
local status = walker.getStatus()
print(string.format("Position: X=%.1f, Z=%.1f | Speed: %.2f b/s", status.x, status.z, math.sqrt(status.vx^2 + status.vz^2)))
```

---

## Building from Source

This mod uses standard **Gradle** with the **NeoForge ModDevGradle** plugin. All dependencies are resolved automatically via Maven:

```bash
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

---

## License

Licensed under the [MIT License](LICENSE).
