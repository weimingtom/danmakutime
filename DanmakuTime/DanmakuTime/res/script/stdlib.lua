
function clone(obj)
	local result = {}
	for k,v in pairs(obj) do
		result[k] = v
	end
	return result
end

function extend(...)
	local result = {}
	for tableIndex,table in ipairs(arg) do
		for k,v in pairs(table) do
			result[k] = v
		end
	end
	return result
end

function append(a, b)
	for i,v in ipairs(b) do
		table.insert(a, v)
	end
end

function signum(x)
	if x > 0 then
		return 1
	elseif x < 0 then
		return -1
	end
	return 0
end

function pauseHandler()
	local dx = screenWidth/2
	local dy = screenHeight/2

	local pausedText = TextDrawable.new(999)
	pausedText:setPos(dx, dy)
	pausedText:setBlockAnchor(5)
	pausedText:setFontName("DejaVuSans")
	pausedText:setFontStyle(FontStyle.BOLD)
	pausedText:setFontSize(14)	
	pausedText:setText("Paused")
	pausedText:setZ(-100)

	while true do
		if input:consumeKey(Keys.ESCAPE) then
			break
		end
		
		yield()
	end
	
	pausedText:destroy()
end
