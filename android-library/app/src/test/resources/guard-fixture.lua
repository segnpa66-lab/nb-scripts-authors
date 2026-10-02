function _user_tick()
    log("_user_tick and guard stay in strings")
end

local _guard_start_tick = nil
local _guard_welcome_shown = false
local _guard_server = server

local _GUARD_WELCOME_TEXT = "Service notice"

function tick()
    if not _guard_start_tick then
        _guard_start_tick = _guard_server.tick
    end
    local elapsed = _guard_server.tick - _guard_start_tick

    if not _guard_welcome_shown then
        log(_GUARD_WELCOME_TEXT)
        _guard_welcome_shown = true
    end

    if elapsed < 100 then
        return
    end

    if type(_user_tick) == "function" then
        _user_tick()
    end
end