
-- Save some functions as local to improve efficiency a bit
local table_filterMut = table.filterMut
local table_unpack = table.unpack


Event = {}
Event.__index = Event

function Event.new()
    return setmetatable({}, Event)
end

function Event:register(func)
    self[#self+1] = func
end

function Event:__call(...)
    local args = {...} -- Save args in a table, which can be captured by closure and unpacked
    table_filterMut(self, function(func) return not func(table_unpack(args)) end)
end

-- "events" table, for convenient registration syntax "function events.someEvent(args) ... end"
local backing = {}
events = setmetatable({}, {
    __index = backing,
    __newindex = function(_, name, func)
        backing[name]:register(func)
    end
})

-- The argument to this file is a table of the default event names, as strings, that exist
-- Return a table of Event objects, for Java to __call into later!
return table.map(..., function(eventName)
    local e = Event.new()
    backing[eventName] = e
    return e
end)