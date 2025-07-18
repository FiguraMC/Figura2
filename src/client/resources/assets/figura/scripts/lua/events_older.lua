-- Special info for this file!
-- Vararg arguments are a list of strings indicating default events to exist, like tick/render.
-- Returns a table mapping default event name -> Event.
-- This table will be reused by user code, so rawget from it before then.

--- Number is the # of invocations it's wait()ing
--- Function is the function it was constructed with
--- Thread is the current coroutine running the function
--- @alias QueueElem [number, function?, thread]

-- Global Metatable / Class for Event data type
--- @class Event
--- @field queue QueueElem[]
--- @operator call(...):nil
Event = {}
Event.__index = Event

-- Local variable for event deletion, prevents reassignment
local DELETE = {}
Event.DELETE = DELETE -- Make it accessible

-- Events stored by name
--- Backing table tracking all events by name
--- @type table<string, Event>
local byName = {}

-- Create methods for Event objects:

-- Static methods

--- @return Event
function Event.new(name)
    local res = setmetatable({queue = {}}, Event)
    if name then
        if byName[name] then error("Event named '"..name.."' already exists") end
        byName[name] = res
    end
    return res
end

--- Run the function, which may contain wait() in it.
--- Waiting times are counted in ticks through events.tick.
--- Code before the first wait() is run immediately.
--- If your code doesn't wait, no point in using this.
--- @param func function
function Event.runOnce(func)
    local coro = coroutine.create(func)
    local success, res = coroutine.resume(coro)
    if not success then error(res, 0) end
    -- If it's not dead, add it to the event
    if coroutine.status(coro) ~= "dead" then
        -- TODO should I have this check? It slows things down in correct code.
        assert(type(res) == 'number', "Yielding in an event should pass a number")
        table.insert(events.tick.queue, {res, nil, coro})
    end
end

-- Regular methods

-- Register the given function into this event.
--- @param func function
function Event:register(func)
  table.insert(self.queue, {0, func, coroutine.create(func)})
end

-- Invoke the event, calling all registered functions with the passed arguments.
--- @param ...any
function Event:__call(...)
  local i = 1
  local t = self.queue[i]
  while t do
    -- If we're not waiting on the function, run it.
    -- Otherwise, decrement the wait time.
    if t[1] <= 1 then -- wait(1) in a loop means we should run it every tick
        -- Resume the coroutine
        local success, res = coroutine.resume(t[3], ...)
        -- Propagate the error, if any
        if not success then error(res, 0) end
        -- If we returned, delete or refresh
        if coroutine.status(t[3]) == "dead" then
          -- If there's no func, or if it returned DELETE, then delete.
          if not t[2] or rawequal(res, DELETE) then
            table.remove(self.queue, i)
            i = i - 1
          else t[3] = coroutine.create(t[2]) end
        else
            -- Coroutine isn't dead, it's just yielded, so set the wait time.
            -- TODO should I have this check? It slows things down in correct code
            assert(type(res) == "number", "Yielding an event should pass a number")
            t[1] = res
        end
    else t[1] = t[1] - 1 end
    -- Update loop variables
    i = i + 1
    t = self.queue[i]
  end
end

--- Set up the global "events" API table.
events = {}

-- Create built-in events through vararg:
for _,name in ipairs({...}) do
    Event.new(name)
end

--- Special case functions
--- @return Event
setmetatable(events, {
  -- events.something will return the Event object
  __index = byName,
  -- function events.something() will register a new function to the event
  --- @param name string
  --- @param func function
  __newindex = function(_, name, func)
    if not byName[name] then error("No event called \""..name.."\"") end
    byName[name]:register(func)
  end
})

--- Call this from within a registered function.
--- Will "wait", from this function's POV, for n event calls.
--- Always pauses for at least 1 call, even if n <= 0.
--- Should only be called inside of an event-registered function.
--- @param n number
function wait(n) coroutine.yield(n) end

-- Return the table of events by name to the java code
return byName