
CircleGhost = {xmul=100, ymul=100}

function CircleGhost.new(o)
	local o = extend(CircleGhost, o or {})
	return Sprite.new(o)
end

function CircleGhost:init()
	print("CircleGhost:init")
		
	self:setPos(400, 300);
	self:setZ(-100)
	self:setTexture(textureStore:getTexture("test.png#g1"));
end

function CircleGhost:update()
	print("CircleGhost:update")
	
	local n = 0
	while n < 100 do	
		self:setPos(levelWidth/2 + self.xmul * math.cos(n),
			levelHeight/2 + self.ymul * math.sin(n))
		n = n + 1
		
		yield()
	end
	
	self:destroy()
end

function CircleGhost:animate()
	while false do
		self:setTexture(textureStore:getTexture("test.png#g0"));
		yield(10)
		self:setTexture(textureStore:getTexture("test.png#g1"));
		yield(10)
	end
end

function CircleGhost:onDestroy()
	print("I am already dead")
	
	--return false from this function to prevent the destruction of the object
	--You can call destroy() again later to attempt another destruction
	return false
end

--------------------------------------------------------------------------------

function main()
	buildLevel("level-bg.png")

	player = Player.new()

	local ghost = CircleGhost.new{xmul=100, ymul=100}
	ghost:test("abc", 1)
    yield(10)
	ghost:test("def", 1)

	local ghost2 = CircleGhost.new{xmul=-100, ymul=100}
	
	Thread.new(function()
		while true do
			if input:consumeKey(Keys.Q) then
				print("The global key listener thread saw your key. And will proceed to kill you.")
				player:destroy()
			end
			yield()
		end
	end)
	
	for group=1,50 do
		for n=1,200 do
			local s = Sprite.new()
			
			if math.random(10) >= 10 then
				s:setTexture(textureStore:getTexture("test.png#g0"));
				s:setZ(-1)
				s:setDrawAngleAuto(false)
			else
				s:setTexture(textureStore:getTexture("test.png#g1"));
				s:setBlendMode(BlendMode.ADD)
			end
			
			s:setPos(levelWidth*.9, levelHeight*.35)
			s:setSpeed(2 + math.random() * 2)
			s:setAngle(256)
			s:setAngleInc(2 + math.random() * 1)
		end		
		yield(10)
	end
end
