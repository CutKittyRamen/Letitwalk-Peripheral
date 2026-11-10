-- ============================================================================
--  walker_auto.lua : Autonomous Waypoint Navigation for Let it Walk!
-- ============================================================================
-- Usage:
--   walker_auto <targetX> <targetZ> [gear] [tolerance]
-- Example:
--   walker_auto 150 -320 2
-- ============================================================================

local args = { ... }

local walker = peripheral.find("walker")
if not walker then
    printError("No walker peripheral found!")
    print("Connect this computer to a Pilot Controls, Walker Core,")
    print("or Walker Computer Interface block.")
    return
end

local targetX, targetZ
local targetGear = 2
local tolerance = 3.0

if #args >= 2 then
    targetX = tonumber(args[1])
    targetZ = tonumber(args[2])
    if #args >= 3 then targetGear = tonumber(args[3]) or 2 end
    if #args >= 4 then tolerance = tonumber(args[4]) or 3.0 end
else
    term.clear()
    term.setCursorPos(1, 1)
    print("=== WALKER AUTONOMOUS NAVIGATION ===")
    local s = walker.getStatus()
    if s and s.assembled then
        print(string.format("Current Pos: X: %.1f, Z: %.1f", s.x or 0, s.z or 0))
    end
    write("Enter Target X: ")
    targetX = tonumber(read())
    write("Enter Target Z: ")
    targetZ = tonumber(read())
    write("Enter Cruise Gear (1-5, default 2): ")
    local gInput = read()
    if tonumber(gInput) then targetGear = tonumber(gInput) end
    write("Arrival Radius (blocks, default 3.0): ")
    local tInput = read()
    if tonumber(tInput) then tolerance = tonumber(tInput) end
end

if not targetX or not targetZ then
    printError("Invalid target coordinates.")
    return
end

term.clear()
term.setCursorPos(1, 1)

local function normalizeAngle(deg)
    deg = deg % 360
    if deg > 180 then deg = deg - 360
    elseif deg < -180 then deg = deg + 360 end
    return deg
end

-- Ensure walker is unparked
if walker.isParked() then
    walker.setParked(false)
end

-- Set target gear if permitted
local maxG = walker.getMaxGear()
local gearToUse = math.min(targetGear, maxG)
pcall(walker.setGear, gearToUse)

print(string.format("Navigating to: [%.1f, %.1f] (tolerance: %.1fm, gear: %d)", targetX, targetZ, tolerance, gearToUse))
print("Press 'Q' to abort navigation.")
print("--------------------------------------------------")

local running = true
local lastPos = nil
local stuckCounter = 0

-- Launch keyboard listener in parallel
local function inputListener()
    while running do
        local ev, key = os.pullEvent("key")
        if key == keys.q then
            running = false
            walker.stop()
            print("\nNavigation aborted by user.")
            break
        end
    end
end

local function navLoop()
    while running do
        local s = walker.getStatus()
        if not s or not s.assembled then
            printError("\nWalker lost or unassembled!")
            running = false
            break
        end

        local x, z = s.x or 0, s.z or 0
        local yaw = s.yaw or 0

        local dx = targetX - x
        local dz = targetZ - z
        local dist = math.sqrt(dx * dx + dz * dz)

        -- Status printout (line 6)
        term.setCursorPos(1, 6)
        term.clearLine()
        print(string.format("Pos: [%.1f, %.1f] | Dist: %.1f m | Yaw: %.1f'", x, z, dist, yaw))

        if dist <= tolerance then
            walker.stop()
            print("\n>>> WAYPOINT REACHED! <<<")
            running = false
            break
        end

        -- Calculate desired Minecraft heading (0: +Z South, 90: -X West, 180: -Z North, -90: +X East)
        local targetYaw = math.deg(math.atan2(-dx, dz))
        local yawDiff = normalizeAngle(targetYaw - yaw)

        -- Check if stuck
        if lastPos then
            local moved = math.sqrt((x - lastPos.x)^2 + (z - lastPos.z)^2)
            if moved < 0.15 and not s.parked then
                stuckCounter = stuckCounter + 1
            else
                stuckCounter = 0
            end
        end
        lastPos = { x = x, z = z }

        if stuckCounter > 10 then
            term.setCursorPos(1, 8)
            print("[WARN] Obstacle detected! Attempting evasive jump...")
            walker.stop()
            sleep(0.2)
            walker.jump(25)
            sleep(1.5)
            stuckCounter = 0
        end

        -- Steering & Driving logic
        if math.abs(yawDiff) > 10 then
            -- Steer walker
            local turnSpeed = math.max(-40, math.min(40, yawDiff * 0.8))
            walker.turn(turnSpeed, 3)
        end

        -- If roughly facing target, advance forward
        if math.abs(yawDiff) < 60 then
            -- Slow down as we approach
            if dist < 8 and gearToUse > 1 then
                pcall(walker.setGear, 1)
            elseif dist >= 8 and (s.gear or 1) < gearToUse then
                pcall(walker.setGear, gearToUse)
            end
            walker.forward(5)
        else
            -- Large turn angle, step into turn
            if yawDiff > 0 then
                walker.right(3)
            else
                walker.left(3)
            end
        end

        sleep(0.15)
    end
end

parallel.waitForAny(navLoop, inputListener)
walker.stop()
