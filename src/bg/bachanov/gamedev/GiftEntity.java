package bg.bachanov.gamedev;

public class GiftEntity extends Entity{

	public GiftEntity(MySprite sprite, int x, int y) {
		super(sprite, x, y);
	}

	@Override
	public void collidedWith(Entity other) {
		// System.out.println("Collision detected ObjectEntity");
	}

}
