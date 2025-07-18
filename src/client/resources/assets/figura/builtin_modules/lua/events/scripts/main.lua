
-- Store this stuff in the ACTUAL global scope.
local _ENV = getmetatable(_ENV).__index

-- Declare locals

-- Events stored by name
local byName = {}

-- Locals for performance (hopefully)
local setmetatable = setmetatable
local rawequal = rawequal
local create = coroutine.create
local resume = coroutine.resume
local status = coroutine.status
local yield = coroutine.yield
local floor = math.floor
local push, pop

-- Declare globals

-- Global Metatable / Class for Event data type
Event = {}
Event.__index = Event
Event.byName = byName -- Make byName accessible for people who want to see all events or something

-- Global events table for "function events.tick() end" syntax
events = setmetatable({}, {
    __index = byName,
    __newindex = function(_, name, func)
        if not byName[name] then error("No event called '"..name.."'") end
        byName[name]:register(func)
    end
})

-- Global wait function (alias for coroutine.yield)
wait = yield

-- Implement Event class
function Event.new(name)
    local res = setmetatable({{}, {}}, Event)
    if name then
        if byName[name] then error("Event named '"..name.."' already exists") end
        byName[name] = res
    end
    return res
end

-- Register the function into this event, so it runs every time the event is triggered.
-- If the function returns Event.DELETE, then it will be deleted.
local DELETE = {}
Event.DELETE = DELETE
function Event:register(func)
    push(self, 0, create(function(...)
        if rawequal(func(...), DELETE) then return end
        while true do
            if rawequal(func(yield(1)), DELETE) then break end
        end
    end))
end

-- Run the given function in the event only one time, not repeating like register() does
function Event:once(func)
    push(self, 0, create(func))
end

function Event:__call(...)
    -- Poll functions from the heap
    local p = self[1]
    local p1 = p[1]
    while p1 and p1 < 1 do
        local coro = pop(self)
        local success, res = resume(coro, ...)
        if not success then error(res, 0) end
        if status(coro) ~= "dead" then
            -- Re-insert the coroutine with the result as the priority
            -- TODO maybe typecheck here that res is a number?
            push(self, res, coro)
        end
        p1 = p[1]
    end
    -- Decrement wait time of all functions
    for i=1,#p do p[i] = p[i] - 1 end
end


-- Binary heap operations
push = function(heap, priority, element)
    local p = heap[1] -- Priorities
    local e = heap[2] -- Elements
    local i = #p + 1 -- Index
    p[i] = priority
    e[i] = element
    local pi = floor(i/2) -- Parent index
    local pp = p[pi] -- Parent priority
    while i ~= 1 and pp > priority do
        p[i], p[pi] = pp, p[i]
        e[i], e[pi] = e[pi], e[i]
        i = pi
        pi = floor(i/2)
        pp = p[pi]
    end
end
pop = function(heap)
    local p = heap[1]
    local e = heap[2]
    local res = e[1] -- Result
    local c = #p -- Number of nodes
    p[1] = p[c]; p[c] = nil -- Swap last element into index 1
    e[1] = e[c]; e[c] = nil
    -- Setup variables
    local i = 1 -- Current index
    local c1i = 2 -- Child 1 index
    local c2i = 3 -- Child 2 index
    local ip -- Current index priority
    local c1p = p[2] -- Child 1 priority
    local c2p -- Child 2 priority
    -- Start looping
    while c1p do -- While we have at least 1 child
        ip = p[i]
        c2p = p[c2i]
        if c1p < ip then -- If the first child is smaller than this, we'll swap
            -- If the second child is smaller than the first child, swap with second child instead
            if c2p and c2p < c1p then
                p[i], p[c2i] = c2p, ip
                e[i], e[c2i] = e[c2i], e[i]
                i = c2i
            else
                p[i], p[c1i] = c1p, ip
                e[i], e[c1i] = e[c1i], e[i]
                i = c1i
            end
        elseif c2p and c2p < ip then -- If we have a second child, and second child is smaller than this, then swap with it
            p[i], p[c2i] = c2p, ip
            e[i], e[c2i] = e[c2i], e[i]
            i = c2i
        else break end -- Otherwise, neither child is smaller than this, so break
        -- Update variables
        c1i = i*2
        c2i = c1i + 1
        c1p = p[c1i];
    end
    -- Return result
    return res
end

-- Register to the "any_event" event, which is special.
return {
    ["$any_event"] = function(name, args)
        local e = events[name]
        if e then e(table.unpack(args)) end
    end
}