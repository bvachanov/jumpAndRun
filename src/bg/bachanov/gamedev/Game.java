package bg.bachanov.gamedev;

import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
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

	private static final int MAX_RESULT_PER_LEVEL = 20;
	private static final int MAX_LIFES = 3;
	private static final int MAX_gifts_COUNT = 20;
	private static final int MAX_MINES_COUNT = 2;
	private static final int HERO_START_X = 50;
	private static final int HERO_START_Y = 500;

	/** Exit the game */
	private boolean finished;

	private boolean isPaused;

	private BackgroundTile[] backgroundTile = new BackgroundTile[2];
	private ArrayList<Entity> entities;
	private ArrayList<Entity> gifts;
	private ArrayList<Entity> mines;
	private HeroEntity heroEntity;
	private int currentLevel = 1;
	private int lifes = MAX_LIFES;
	private LifeIcon lifeIcon;

	private TrueTypeFont font;

	private int giftsCollected = 0;
	private int record = 0;

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

	private void initGL(int width, int height) throws IOException {
		try {
			Display.setDisplayMode(new DisplayMode(width, height));
			Display.setTitle(GAME_TITLE);
			Display.setFullscreen(false);
			Display.create();

			// Enable vsync if we can
			Display.setVSyncEnabled(true);

			// Start up the sound system
			// AL.create();
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

		initHUD();

		Texture texture;

		// Load hero sprite
		texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/avatar.png"));
		heroEntity = new HeroEntity(this, new MySprite(texture), HERO_START_X, HERO_START_Y);
		entities.add(heroEntity);

		// Generate the gifts
		initGifts();
	}

	private void initBackground() throws IOException {
		Texture texture;

		// Load background tiles
		texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/tile_1.png"));
		backgroundTile[0] = new BackgroundTile(texture);

		texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/tile_5.png"));
		backgroundTile[1] = new BackgroundTile(texture);

	}

	private void initHUD() throws IOException {
		Texture texture;

		// Load life tiles
		texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/life.png"));
		lifeIcon = new LifeIcon(texture);

	}

	private void initGifts() throws IOException {
		gifts = new ArrayList<Entity>();
		mines = new ArrayList<Entity>();

		Texture texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/chest.png"));
		Random rand = new Random();
		int objectX;
		int objectY;
		for (int m = 0; m < MAX_gifts_COUNT; m++) {
			objectX = rand.nextInt(SCREEN_SIZE_WIDTH - texture.getImageWidth());
			objectY = rand.nextInt(SCREEN_SIZE_HEIGHT - texture.getImageHeight()) * (-2);
			GiftEntity objectEntity = new GiftEntity(new MySprite(texture), objectX, objectY);
			gifts.add(objectEntity);
		}

		texture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/mine.png"));
		rand = new Random();
		for (int m = 0; m < MAX_MINES_COUNT; m++) {
			objectX = rand.nextInt(SCREEN_SIZE_WIDTH - texture.getImageWidth());
			objectY = rand.nextInt(SCREEN_SIZE_HEIGHT - texture.getImageHeight()) * (-2);
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
				// The window is in the foreground, so we should play the
				// game
				logic();
				render();
				Display.sync(FRAMERATE);
			} else {
				// The window is not in the foreground, so we can allow
				// other
				// stuff to run and
				// infrequently update
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				logic();
				if (Display.isVisible() || Display.isDirty()) {
					// Only bother rendering if the window is visible or
					// dirty
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
		// AL.destroy();

		// Close the window
		Display.destroy();
	}

	private void resetScore() {
		lifes = MAX_LIFES;
		giftsCollected = 0;
		currentLevel = 1;
		isPaused = false;
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

		else if (Keyboard.isKeyDown(Keyboard.KEY_P) ) {
			isPaused =true;
		}
		
		else if (Keyboard.isKeyDown(Keyboard.KEY_R) ) {
			isPaused =false;
		}
		
		else if (Keyboard.isKeyDown(Keyboard.KEY_N)){
			//reset game
			// cleanup();
			resetScore();
			try {
				initTextures();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			run();
		}

		if (lifes > 0) {
			if (!isPaused) {
				logicHero();
				logicEntities();
				checkForCollision();
			}

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
		// don't draw the bottom row
		for (int a = 0; a * backgroundTile[0].getHeight() < SCREEN_SIZE_HEIGHT - backgroundTile[1].getHeight(); a++) {
			for (int b = 0; b * backgroundTile[0].getWidth() < SCREEN_SIZE_WIDTH; b++) {
				int textureX = backgroundTile[0].getWidth() * b;
				int textureY = backgroundTile[0].getHeight() * a;

				backgroundTile[0].draw(textureX, textureY);
			}
		}
		// make the bottom of the screen with different texture
		for (int b = 0; b * backgroundTile[1].getWidth() < SCREEN_SIZE_WIDTH; b++) {
			int textureX = backgroundTile[1].getWidth() * b;
			int textureY = SCREEN_SIZE_HEIGHT - backgroundTile[1].getHeight();
			backgroundTile[1].draw(textureX, textureY);
		}
	}

	private void drawObjects() {

		for (Entity entity : gifts) {
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
		if (lifes > 0) {
			font.drawString(10, 0, String.format("Gifts collected: %d", giftsCollected), Color.yellow);
			font.drawString(10, 30, String.format("Current level: %d", currentLevel), Color.yellow);
			font.drawString(10, 60, String.format("Record: %d", record), Color.yellow);

			drawLifes();
			
			if(isPaused){
				// pause message
				font.drawString(190, 300, String.format("Game paused. Press R to resume."), Color.yellow);
			}
		}
		
		else {
			// game over message
			font.drawString(190, 200, String.format("Game over!"), Color.yellow);
			font.drawString(190, 230, String.format("Current score: %d", giftsCollected), Color.yellow);
			font.drawString(190, 260, String.format("Record: %d", record), Color.yellow);
			font.drawString(190, 290, String.format("Press N for new game or ECS for exit."), Color.yellow);

		}
	}

	private void drawLifes() {

		for (int l = 0; l < lifes; l++) {
			int textureX = SCREEN_SIZE_WIDTH - (lifeIcon.getWidth() + 5) * (l + 1); // add
																					// space
																					// between
																					// icons
			int textureY = lifeIcon.getHeight();
			lifeIcon.draw(textureX, textureY);
		}
	}

	private void logicHero() {
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			if (heroEntity.getX() + heroEntity.getWidth() + 10 < Display.getDisplayMode().getWidth()) {
				heroEntity.setX(heroEntity.getX() + 10 + currentLevel);
			}
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			if (heroEntity.getX() - 10 >= 0) {
				heroEntity.setX(heroEntity.getX() - 10 - currentLevel);
			}
		}
	}

	private void logicEntities() {
		for (Entity entity : gifts) {

			if (entity.getY() + 2 + entity.getHeight() < SCREEN_SIZE_HEIGHT) {
				entity.setY(entity.getY() + 1 * currentLevel); // move to bottom
																// with speed
																// depending on
																// the level
			} else {
				// reset treasure's coordinates if moved out of the screen
				resetCoordinates(entity);
			}
		}

		for (Entity entity : mines) {
			if (entity.getY() + 2 + entity.getHeight() < SCREEN_SIZE_HEIGHT) {
				entity.setY(entity.getY() + 1 * currentLevel); // move to bottom
																// with speed
																// depending on
																// the current
																// level
			} else {
				// reset bomb's coordinates if out of the screen
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

		for (int i = 0; i < gifts.size(); i++) {
			Entity him = gifts.get(i);

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
		if (object instanceof GiftEntity) {
			GiftEntity treasureEntity = (GiftEntity) object;
			// reset treasure's coordinates
			resetCoordinates(treasureEntity);
			if (record == giftsCollected) { // check if record # of
											// gifts is collected and if
											// yes update it
				record++;
			}
			giftsCollected++;
			// level up if number of collected gifts reached
			if (giftsCollected == currentLevel * currentLevel * MAX_RESULT_PER_LEVEL) {
				currentLevel++;

			}

		} else if (object instanceof MineEntity) {
			MineEntity mineEntity = (MineEntity) object;
			// reset mine's coordinates
			resetCoordinates(mineEntity);
			// play bomb sound
			lifes--;
		}
	}
}
