package nl.weeaboo.dt;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.weeaboo.common.GraphicsUtil;
import nl.weeaboo.dt.field.Field;
import nl.weeaboo.dt.field.IField;
import nl.weeaboo.dt.input.Input;
import nl.weeaboo.dt.lua.LuaException;
import nl.weeaboo.dt.lua.LuaFunctionLink;
import nl.weeaboo.dt.lua.LuaRunState;
import nl.weeaboo.dt.lua.LuaUtil;
import nl.weeaboo.dt.renderer.ITextureStore;
import nl.weeaboo.dt.renderer.Renderer;
import nl.weeaboo.dt.renderer.TextureStore;
import nl.weeaboo.game.GameBase;
import nl.weeaboo.game.ResourceManager;
import nl.weeaboo.game.gl.GLManager;
import nl.weeaboo.game.gl.GLVideoCapture;
import nl.weeaboo.game.input.UserInput;
import nl.weeaboo.game.text.MutableTextStyle;
import nl.weeaboo.game.text.ParagraphRenderer;

import org.luaj.vm.LuaState;

public class Game extends GameBase {

	private boolean error;
	private GLVideoCapture videoCapture;
	private Notifier notifier;
	private ITextureStore texStore;

	private LuaRunState luaRunState;
	private LuaFunctionLink mainFunc;	
	private TinyMap<IField> fieldMap;
	
	public Game(Config c, ResourceManager rm, GameFrame gf) {
		super(c, rm, gf);
	}
	
	//Functions
	public static void loadConfig(Config config, ResourceManager rm) throws IOException {
		String configFilename = "prefs.xml";

		config.load(rm.getInputStream("prefs.default.xml"));
		if (rm.getFileExists(configFilename)) {
			InputStream in = null;
			try {
				in = new BufferedInputStream(rm.getInputStream(configFilename));
				config.load(in);
			} finally {
				if (in != null) in.close();
			}
		}
	}
	
	public void startRecordingVideo(String filename) throws IOException {
		stopRecordingVideo();
		
		Config config = getConfig();
		
		int rw = getWidth();
		int rh = getHeight();
		int scw = config.capture.getWidth();
		int sch = config.capture.getHeight();
		
		if (scw <= 0) scw = rw;
		if (sch <= 0) sch = rh;
		
		Dimension d = GraphicsUtil.getProportionalScale(rw, rh, scw, sch);
		
		videoCapture = new GLVideoCapture(this, filename, config.capture.getX264CRF(),
				config.graphics.getFPS(), rw, rh, d.width, d.height, true);
		
		try {
			videoCapture.start();
		} catch (IOException ioe) {
			videoCapture = null;
			throw ioe;
		}
	}
	
	public void stopRecordingVideo() {
		if (videoCapture != null) {
			videoCapture.stop();
			videoCapture = null;
		}		
	}
	
	public void loadResources() {
		super.loadResources();
						
		ResourceManager rm = getResourceManager();
		
		notifier = new Notifier(createParagraphRenderer());
		
		OutputStream err = null;
		try {
			rm.getOutputStream("err.txt");
		} catch (IOException e) {
			Log.warning(e);
		}
		
		Log.getInstance().start(isDebug(), notifier, err);

		restart();
	}
	
	public void unloadResources() {
		stopRecordingVideo();
		
		super.unloadResources();
	}
	
	public void restart() {
		error = false;

		ResourceManager rm = getResourceManager();
						
		texStore = new TextureStore(rm.getImageStore());
		
		fieldMap = new TinyMap<IField>();
		fieldMap.put(0, new Field());
		
		//Init Lua		
		luaRunState = new LuaRunState(System.nanoTime(), fieldMap, texStore);
		LuaState vm = getLuaState();
				
		for (String path : rm.getFolderContents("script")) {
			InputStream in = null;
			try {				
				in = rm.getInputStream(path);
				LuaUtil.load(vm, path, in);
			} catch (Exception e) {
				Log.showError(e);
				error = true;
				return;
			} finally {
				try {
					if (in != null) in.close();
				} catch (IOException ioe) { }
			}
		}
		
		mainFunc = new LuaFunctionLink(luaRunState, vm, "main");		
		error = false;
	}
	
	public void update(UserInput input, float dt) {
		super.update(input, dt);
				
		notifier.update(Math.round(1000f * dt));
		
		if (error) {
			return;
		}
		
		Input ii = new Input(input);
		
		if (ii.consumeKey(KeyEvent.VK_F8)) {
			try {
				startRecordingVideo("capture.mkv");
				Log.message("Starting video recording");
			} catch (IOException e) {
				Log.showError(e);
			}
		}
		
		try {
			if (mainFunc != null && !mainFunc.isFinished()) {
				mainFunc.update();
			}
		} catch (LuaException e) {
			Log.error(e);
			//mainFunc = null;
		}
			
		for (IField field : fieldMap.getValues()) {
			field.update(ii);
		}
	}
	
	public void draw(GLManager glm) {		
		Renderer r = new Renderer(glm);

		int w = getWidth();
		int h = getHeight();
		
		/*
		//Raw draw performance test
		 
		int rows = 100;
		int cols = 100;
		
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				float a = x * w / cols;
				float b = y * h / rows;
				
				float dx = w / cols;
				float dy = h / rows;
				
				glm.setColorARGB(luaRunState.getRandom().nextInt());
				glm.fillRect(a+dx*.1f, b+dy*.1f, dx*.8f, dy*.8f);
			}
		}

		glm.setColorARGB(0xFFFFFFFF);
		*/
		
		for (IField field : fieldMap.getValues()) {
			field.draw(r);
		}
		
		r.flush();
		
		//Draw HUD
		ParagraphRenderer pr = createParagraphRenderer();
		pr.setBounds(20, 20, w-40, h-40);
		
		MutableTextStyle mts = pr.getDefaultStyle().mutableCopy();
		mts.setFontName("DejaVuSans");
		mts.setAnchor(9);
		mts.setFontSize(16);
		pr.setDefaultStyle(mts.immutableCopy());
		
		String hudText = String.format("%.2f FPS\n%d Objects\n",
				getFPS(), getObjectCount());
		pr.drawText(glm, hudText);
		
		//Take screen capture
		if (videoCapture != null) {
			try {
				videoCapture.update(glm, getRealWidth(), getRealHeight());
			} catch (IOException e) {
				Log.showError(e);
				videoCapture = null;
			}
		}
		
		//Draw notifier
		notifier.draw(glm, w, h);
		
		super.draw(glm);
	}
	
	//Getters
	public Config getConfig() {
		return (Config)super.getConfig();
	}
	public LuaRunState getLuaRunState() {
		return luaRunState;
	}
	public LuaState getLuaState() {
		return luaRunState.vm;
	}
	public int getObjectCount() {
		int count = 0;
		for (IField field : fieldMap.getValues()) {
			count += field.getObjectCount();
		}
		return count;		
	}
	public float getFPS() {
		return getGameFrame().getCurrentFPS();
	}	
	public boolean isRecordingVideo() {
		return videoCapture != null;
	}
	
	//Setters
	
}