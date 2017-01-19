package bg.bachanov.gamedev;

import java.awt.Rectangle;


public abstract class Entity {
	private int x;
	private int y;
	private MySprite sprite;
	private boolean visible;
	
	private Rectangle me = new Rectangle();
	private Rectangle him = new Rectangle();

	public Entity(MySprite sprite, int x, int y) {
		this.x = x;
		this.y = y;
		this.sprite = sprite;
		this.visible = true;
	}

	public boolean collidesWith(Entity other) {
		me.setBounds((int) x, (int) y, sprite.getWidth(), sprite.getHeight());
		him.setBounds((int) other.x, (int) other.y, other.sprite.getWidth(),
				other.sprite.getHeight());

		return me.intersects(him);
	}

	public abstract void collidedWith(Entity other);
	
	public void draw() {
		sprite.draw(x, y);
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return sprite.getWidth();
	}

	public int getHeight() {
		return sprite.getHeight();
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
