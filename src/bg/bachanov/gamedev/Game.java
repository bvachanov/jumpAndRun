package bg.bachanov.gamedev;

import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;

/**
 * @author Blagovest Achanov
 * @version 1.0 $Id$
 */
public class Game {

	/** Game title */
	public static final String GAME_TITLE = "Jump And Run";

	/** Screen size */
	private static int SCREEN_SIZE_WIDTH = 800;
	private static int SCREEN_SIZE_HEIGHT = 600;

	/** Desired frame time */
	private static final int FRAMERATE = 60;

	private static final int MAX_LEVEL = 5;
	private static final int MAX_LIFES = 3;
	private static final int MAX_TREASURES_COUNT = 20;
	private static final int MAX_MINES_COUNT = 5;
	private static final int HERO_START_X = 50;
	private static final int HERO_START_Y = 500;

	/** Exit the game */
	private boolean finished;

	private BackgroundTile[] backgroundTile = new BackgroundTile[MAX_LEVEL];
	private ArrayList<Entity> entities;
	private ArrayList<Entity> treasures;
	private ArrayList<Entity> mines;
	private HeroEntity heroEntity;
	private int currentLevel = 1;
	private int lifes = MAX_LIFES;

	private TrueTypeFont font;

	private int treasuresCollected = 0;

	/**
	 * Application init
	 * 
	 * @param args
	 *            Commandline args
	 */
	public static void main(String[] args) {
		Game myGame = new Game();
		myGame.start();
	}

	public void start() {
		try {
			init();
			run();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			Sys.alert(GAME_TITLE, "An error occured and the game will exit.");
		} finally {
			cleanup();
		}

		System.exit(0);
	}

	/**
	 * Initialise the game
	 * 
	 * @throws Exception
	 *             if init fails
	 */
	private void init() throws Exception {
		// Create a fullscreen window with 1:1 orthographic 2D projection, and
		// with
		// mouse, keyboard, and gamepad inputs.
		try {
			initGL(SCREEN_SIZE_WIDTH, SCREEN_SIZE_HEIGHT);

			initTextures();
		} catch (IOException e) {
			e.printStackTrace();
			finished = true;
		}
	}

	private void initGL(int width, int height) {
		try {
			Display.setDisplayMode(new DisplayMode(width, height));
			Display.setTitle(GAME_TITLE);
			Display.setFullscreen(false);
			Display.create();

			// Enable vsync if we can
			Display.setVSyncEnabled(true);

			// Start up the sound system
			AL.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(0);
		}

		GL11.glEnable(GL11.GL_TEXTURE_2D);

		GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// enable alpha blending
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		GL11.glViewport(0, 0, width, height);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, width, height, 0, 1, -1);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);

		Font awtFont = new Font("Times New Roman", Font.BOLD, 24);
		font = new TrueTypeFont(awtFont, true);
	}

	private void initTextures() throws IOException {
		entities = new ArrayList<Entity>();

		initBackground();

		Texture texture;

		// Load hero sprite
		texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/avatar.png"));
		heroEntity = new HeroEntity(this, new MySprite(texture), HERO_START_X, HERO_START_Y);
		entities.add(heroEntity);

		// Generate the treasures
		initTreasures();
	}

	private void initBackground() throws IOException {
		Texture texture;

		// Load background tiles
		texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/tile_1.png"));
		backgroundTile[0] = new BackgroundTile(texture);

		texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/tile_5.png"));
		backgroundTile[1] = new BackgroundTile(texture);

	}

	private void initTreasures() throws IOException {
		treasures = new ArrayList<Entity>();
		mines = new ArrayList<Entity>();

		Texture texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/chest.png"));
		Random rand = new Random();
		int objectX;
		int objectY;
		for (int m = 0; m < MAX_TREASURES_COUNT; m++) {
			objectX = rand.nextInt(SCREEN_SIZE_WIDTH - texture.getImageWidth());
			objectY = rand.nextInt(SCREEN_SIZE_HEIGHT - texture.getImageHeight()) * (-1);
			TreasureEntity objectEntity = new TreasureEntity(new MySprite(texture), objectX, objectY);
			treasures.add(objectEntity);
		}

		texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/mine.png"));
		rand = new Random();
		for (int m = 0; m < MAX_MINES_COUNT; m++) {
			objectX = rand.nextInt(SCREEN_SIZE_WIDTH - texture.getImageWidth());
			objectY = rand.nextInt(SCREEN_SIZE_HEIGHT - texture.getImageHeight()) * (-1);
			MineEntity objectEntity = new MineEntity(new MySprite(texture), objectX, objectY);
			mines.add(objectEntity);
		}
	}

	private void resetCoordinates(Entity entity) {
		Random rand = new Random();
		entity.setX(rand.nextInt(SCREEN_SIZE_WIDTH - entity.getWidth()));
		entity.setY(rand.nextInt(SCREEN_SIZE_HEIGHT - entity.getHeight()) * (-1));
	}

	/**
	 * Runs the game (the "main loop")
	 */
	private void run() {
		while (!finished) {
			// Always call Window.update(), all the time
			Display.update();

			if (Display.isCloseRequested()) {
				// Check for O/S close requests
				finished = true;
			} else if (Display.isActive()) {
				// The window is in the foreground, so we should play the game
				logic();
				render();
				Display.sync(FRAMERATE);
			} else {
				// The window is not in the foreground, so we can allow other
				// stuff to run and
				// infrequently update
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				logic();
				if (Display.isVisible() || Display.isDirty()) {
					// Only bother rendering if the window is visible or dirty
					render();
				}
			}
		}
	}

	/**
	 * Do any game-specific cleanup
	 */
	private void cleanup() {
		// TODO: save anything you want to disk here

		// Stop the sound
		AL.destroy();

		// Close the window
		Display.destroy();
	}

	/**
	 * Do all calculations, handle input, etc.
	 */
	private void logic() {
		// Example input handler: we'll check for the ESC key and finish the
		// game instantly when it's pressed
		if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			finished = true;
		}

		if (lifes > 0) {
			logicHero();
			logicEntities();

			checkForCollision();
		} else {
			Sys.alert(GAME_TITLE, "Game over!");
			finished = true;
		}
	}

	/**
	 * Render the current frame
	 */
	private void render() {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
		Color.white.bind();

		drawBackground();

		drawObjects();

		heroEntity.draw();

		drawHUD();
	}

	private void drawBackground() {
		for (int a = 0; a * backgroundTile[0].getHeight() < SCREEN_SIZE_HEIGHT - backgroundTile[0].getHeight(); a++) {
			for (int b = 0; b * backgroundTile[0].getWidth() < SCREEN_SIZE_WIDTH; b++) {
				int textureX = backgroundTile[0].getWidth() * b;
				int textureY = backgroundTile[0].getHeight() * a;

				backgroundTile[0].draw(textureX, textureY);
			}
		}

		for (int b = 0; b * backgroundTile[1].getWidth() < SCREEN_SIZE_WIDTH; b++) {
			int textureX = backgroundTile[1].getWidth() * b;
			int textureY = SCREEN_SIZE_HEIGHT - backgroundTile[1].getHeight();
			backgroundTile[1].draw(textureX, textureY);
		}
	}

	private void drawObjects() {

		for (Entity entity : treasures) {
			if (entity.isVisible()) {
				entity.draw();
			}
		}

		for (Entity entity : mines) {
			if (entity.isVisible()) {
				entity.draw();
			}
		}
	}

	private void drawHUD() {
		font.drawString(10, 0,
				String.format("Treasures collected %d/%d", treasuresCollected, MAX_TREASURES_COUNT * MAX_LEVEL),
				Color.black);

		font.drawString(SCREEN_SIZE_WIDTH - 120, 0, String.format("Lifes %d/%d", lifes, MAX_LIFES), Color.black);
	}

	private void logicHero() {
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			if (heroEntity.getX() + heroEntity.getWidth() + 10 < Display.getDisplayMode().getWidth()) {
				heroEntity.setX(heroEntity.getX() + 10);
			}
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			if (heroEntity.getX() - 10 >= 0) {
				heroEntity.setX(heroEntity.getX() - 10);
			}
		}
	}

	private void logicEntities() {
		for (Entity entity : treasures) {

			if (entity.getY() + 2 + entity.getHeight() < SCREEN_SIZE_HEIGHT) {
				entity.setY(entity.getY() + 2);
			} else {
				resetCoordinates(entity);
			}
		}

		for (Entity entity : mines) {
			if (entity.getY() + 2 + entity.getHeight() < SCREEN_SIZE_HEIGHT) {
				entity.setY(entity.getY() + 2);
			} else {
				resetCoordinates(entity);
			}
		}
	}

	private void checkForCollision() {
		for (int p = 0; p < entities.size(); p++) {
			for (int s = p + 1; s < entities.size(); s++) {
				Entity me = entities.get(p);
				Entity him = entities.get(s);

				if (me.collidesWith(him)) {
					me.collidedWith(him);
					him.collidedWith(me);
				}
			}
		}

		for (int i = 0; i < treasures.size(); i++) {
			Entity him = treasures.get(i);

			if (heroEntity.collidesWith(him)) {
				heroEntity.collidedWith(him);
				him.collidedWith(heroEntity);
			}
		}

		for (int i = 0; i < mines.size(); i++) {
			Entity him = mines.get(i);

			if (heroEntity.collidesWith(him)) {
				heroEntity.collidedWith(him);
				him.collidedWith(heroEntity);
			}
		}
	}

	public void notifyObjectCollision(Entity notifier, Object object) {
		if (object instanceof TreasureEntity) {
			TreasureEntity treasureEntity = (TreasureEntity) object;
			// treasures.remove(treasureEntity);
			resetCoordinates(treasureEntity);
			treasuresCollected++;
		} else if (object instanceof MineEntity) {
			MineEntity mineEntity = (MineEntity) object;
			// mines.remove(object);
			resetCoordinates(mineEntity);
			lifes--;
		}
	}
}
