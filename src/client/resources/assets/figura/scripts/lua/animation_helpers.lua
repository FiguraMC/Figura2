
-- Map animation instances -> speed
local playingInstances = {}
-- Accumulated animation ticks, used to track time elapsed between frames
local ticksAccum = 0

-- Play: starts running the instance with speed 1
function AnimationInstance:play()
    self:time(0)               -- Set time to 0
    playingInstances[self] = 1 -- Set speed to 1
    return self
end

-- Stop: Set its time to 0, remove it from the set of playing instances
function AnimationInstance:stop()
    self:time(0) -- Set time to 0
    playingInstances[self] = nil -- Stop updating it each frame
    return self
end

-- Set speed. Note that doing `:play` will currently reset speed to 1... not sure what to do about that, or if we care
function AnimationInstance:speed(value)
    if value then
        playingInstances[self] = value
        return self
    else
        return playingInstances[self] or 0
    end
end

-- Each frame/tick, update the timing of playing instances:
function events.tick()
    ticksAccum = ticksAccum + 1
end
function events.render(delta)
    -- Calculate time elapsed since last frame
    local diff = (delta + ticksAccum) * 0.05 -- 0.05 seconds per tick
    ticksAccum = -delta
    -- Update animation instances by adding diff * speed
    for instance, speed in pairs(playingInstances) do
        instance:time(instance:time() + diff * speed)
    end
end