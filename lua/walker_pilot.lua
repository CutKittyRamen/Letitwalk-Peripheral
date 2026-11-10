-- ============================================================================
--  walker_pilot.lua : Interactive ComputerCraft Cockpit for Let it Walk!
-- ============================================================================
-- Connect your CC Computer to:
--   - A Pilot Controls block (directly or via wired modem/cable)
--   - A Walker Core block
--   - A Walker Computer Interface block
-- ============================================================================

local walker = peripheral.find("walker")
if not walker then
    printError("No walker peripheral found!")
    print("Connect this computer to a Pilot Controls, Walker Core,")
    print("or Walker Computer Interface block.")
    return
end

term.clear()
term.setCursorPos(1, 1)

local w, h = term.getSize()

local function drawHeader()
    term.setCursorPos(1, 1)
    if term.isColor() then
        term.setBackgroundColor(colors.gray)
        term.setTextColor(colors.yellow)
    end
    local title = " === WALKER PILOT CONTROLLER === "
    local pad = math.floor((w - #title) / 2)
    term.write(string.rep(" ", pad) .. title .. string.rep(" ", w - #title - pad))
    if term.isColor() then
        term.setBackgroundColor(colors.black)
        term.setTextColor(colors.white)
    end
end

local function drawStatus()
    local s = walker.getStatus()
    if not s or not s.assembled then
        term.setCursorPos(1, 3)
        term.setTextColor(colors.red)
        term.write(" [!] WALKER NOT ASSEMBLED OR LOADED           ")
        return
    end

    term.setCursorPos(2, 3)
    term.setTextColor(colors.cyan)
    term.write("Status: ")
    if s.parked then
        term.setTextColor(colors.orange)
        term.write("PARKED    ")
    elseif s.powered then
        term.setTextColor(colors.lime)
        term.write("ACTIVE    ")
    else
        term.setTextColor(colors.red)
        term.write("NO ROTATION")
    end

    term.setCursorPos(24, 3)
    term.setTextColor(colors.cyan)
    term.write("RPM: ")
    term.setTextColor(colors.white)
    term.write(string.format("%-6.1f", s.rpm or 0))

    term.setCursorPos(2, 4)
    term.setTextColor(colors.cyan)
    term.write("Gear: ")
    term.setTextColor(colors.yellow)
    term.write(string.format("%d / %d   ", s.gear or 1, s.maxGear or 1))

    term.setCursorPos(24, 4)
    term.setTextColor(colors.cyan)
    term.write("Fuel: ")
    term.setTextColor(colors.orange)
    term.write(string.format("%d/%d mB ", s.fuel or 0, s.fuelCapacity or 0))

    term.setCursorPos(2, 5)
    term.setTextColor(colors.cyan)
    term.write("Position: ")
    term.setTextColor(colors.white)
    term.write(string.format("X:%.1f Y:%.1f Z:%.1f    ", s.x or 0, s.y or 0, s.z or 0))

    term.setCursorPos(2, 6)
    term.setTextColor(colors.cyan)
    term.write("Heading:  ")
    term.setTextColor(colors.white)
    term.write(string.format("Yaw:%.1f' Pitch:%.1f' Roll:%.1f'   ", s.yaw or 0, s.pitch or 0, s.roll or 0))

    term.setCursorPos(2, 7)
    term.setTextColor(colors.cyan)
    term.write("Modes: ")
    term.setTextColor(s.crab and colors.lime or colors.gray)
    term.write("[CRAB] ")
    term.setTextColor(s.gallop and colors.lime or colors.gray)
    term.write("[GALLOP] ")
    term.setTextColor(s.flying and colors.lime or colors.gray)
    term.write("[FLIGHT] ")
    term.setTextColor(s.grabbed and colors.lime or colors.gray)
    term.write("[GRAB] ")
end

local function drawControls()
    term.setCursorPos(1, 9)
    term.setTextColor(colors.lightGray)
    print("--------------------------------------------------")
    print(" [W / S]   : Throttle Forward / Backward")
    print(" [A / D]   : Steer Left / Right (Strafe if Crab)")
    print(" [1 - 5]   : Shift Gear 1..5")
    print(" [SPACE]   : Charge & Execute Jump")
    print(" [V] Crab  | [G] Gallop | [F] Flight | [R] Grab")
    print(" [X / Z]   : Body Suspension Up / Down")
    print(" [P]       : Park / Unpark Toggle")
    print(" [Q]       : Stop & Quit")
    print("--------------------------------------------------")
end

drawHeader()
drawControls()

local running = true

-- Start update timer
local timerId = os.startTimer(0.2)

while running do
    local ev, p1, p2 = os.pullEvent()

    if ev == "timer" and p1 == timerId then
        drawStatus()
        timerId = os.startTimer(0.2)

    elseif ev == "key" then
        local key = p1
        local held = p2

        -- Navigation
        if key == keys.w then
            walker.forward(8)
        elseif key == keys.s then
            walker.back(8)
        elseif key == keys.a then
            walker.left(8)
        elseif key == keys.d then
            walker.right(8)
        -- Gears
        elseif key == keys.one then
            pcall(walker.setGear, 1)
        elseif key == keys.two then
            pcall(walker.setGear, 2)
        elseif key == keys.three then
            pcall(walker.setGear, 3)
        elseif key == keys.four then
            pcall(walker.setGear, 4)
        elseif key == keys.five then
            pcall(walker.setGear, 5)
        -- Jump
        elseif key == keys.space then
            walker.jump(25)
        -- Modes
        elseif key == keys.v then
            walker.toggleCrabWalk()
        elseif key == keys.g then
            walker.setGallop(not walker.isGalloping())
        elseif key == keys.f then
            walker.toggleFlight()
        elseif key == keys.r then
            walker.grab()
        -- Body suspension
        elseif key == keys.x then
            walker.bodyUp(5)
        elseif key == keys.z then
            walker.bodyDown(5)
        -- Park
        elseif key == keys.p then
            walker.setParked(not walker.isParked())
        -- Exit
        elseif key == keys.q then
            walker.stop()
            running = false
        end

        drawStatus()

    elseif ev == "key_up" then
        local key = p1
        if key == keys.w or key == keys.s or key == keys.a or key == keys.d then
            -- Let timed ticks expire, or call stop if immediate release
        end
    end
end

term.clear()
term.setCursorPos(1, 1)
print("Walker pilot session ended. Walker stopped.")
