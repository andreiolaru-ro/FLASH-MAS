package andrei.abms.gridworld;

/**
 * Implementation for orthogonal orientations in a rectangular grid.
 *
 * @author Andrei Olaru
 */
public enum GridOrientation {
	/**
	 * Going north increments the y coordinate.
	 */
	NORTH(0, 1, "^"),
	
	/**
	 * Going east increments the x coordinate.
	 */
	EAST(1, 0, ">"),
	
	/**
	 * Going south decrements the y coordinate.
	 */
	SOUTH(0, -1, "v"),
	
	/**
	 * Going west decrements the x coordinate.
	 */
	WEST(-1, 0, "<"),
	
	;
	
	/**
	 * Delta x.
	 */
	int		dx;
	/**
	 * Delta y.
	 */
	int		dy;
	/**
	 * 1-character representation.
	 */
	String	representation;
			
	/**
	 * Constructor. It is initialized with the difference of coordinates between a reference position and the position
	 * at that orientation relative to the reference.
	 *
	 * @param deltaX
	 *            - difference between the position at the orientation and the reference position, on x.
	 * @param deltaY
	 *            - difference between the position at the orientation and the reference position, on y.
	 * @param stringRepresentation
	 *            - the representation of the orientation in text mode.
	 */
	private GridOrientation(int deltaX, int deltaY, String stringRepresentation)
	{
		dx = deltaX;
		dy = deltaY;
		representation = stringRepresentation;
	}
	
	/**
	 * @return the delta x
	 */
	int getDx()
	{
		return dx;
	}
	
	/**
	 * @return the delta y
	 */
	int getDy()
	{
		return dy;
	}
	
	@Override
	public String toString()
	{
		return representation;
	}
	
	/**
	 * Computes the absolute orientation that is at the specified orientation relative to <code>this</code>.
	 * <p>
	 * E.g. if the current orientation is SOUTH and the relative orientation is RIGHT, the result is WEST.
	 * 
	 * @param relativeOrientation
	 *            - the relative orientation, considering the current orientation is towards the front.
	 * @return a new {@link GridOrientation} that is at the specified orientation relative to this.
	 * @throws IllegalArgumentException
	 *             if the requested orientation is not orthogonal.
	 */
	public GridOrientation computeRelativeOrientation(GridRelativeOrientation relativeOrientation)
	{
		int angle = relativeOrientation.getAngle();
		boolean isHalfAngle = (angle % 2) != 0;
		if(isHalfAngle)
			throw new IllegalArgumentException("the relative orientation must be at straight angle.");
			
		int straightAngle = angle / 2;
		return GridOrientation.values()[(this.ordinal() + straightAngle) % 4];
	}
	
	/**
	 * Returns the delta x for the position at the indicated orientation, relative to <code>this</code> orientation.
	 * <p>
	 * E.g. if the orientation is EAST, and the relative orientation is BACK-RIGHT, the resulting orientation is
	 * SOUTH-WEST, with a delta x of -1 and a delta y of -1.
	 *
	 * @param relative
	 *            - the relative orientation
	 * @return - the delta x of the position at that relative orientation.
	 */
	int getRelativeDx(GridRelativeOrientation relative)
	{
		int angle = relative.getAngle();
		int straightAngle = angle / 2;
		int isHalfAngle = angle % 2;
		GridOrientation all[] = values();
		int current = 0;
		for(int i = 0; i < all.length; i++)
			if(all[i].equals(this))
				current = i;
		int straightResult = (current + straightAngle) % 4;
		
		int res = all[straightResult].getDx();
		if(isHalfAngle > 0)
			res += all[(straightResult + 1) % 4].getDx();
			
		return res;
	}
	
	/**
	 * Returns the delta y for the position at the indicated orientation, relative to <code>this</code> orientation.
	 * <p>
	 * See example in {@link #getRelativeDx(GridRelativeOrientation)}.
	 *
	 * @param relative
	 *            - the relative orientation
	 * @return - the delta y of the position at that relative orientation.
	 */
	int getRelativeDy(GridRelativeOrientation relative)
	{
		int angle = relative.getAngle();
		int straightAngle = angle / 2;
		int isHalfAngle = angle % 2;
		GridOrientation all[] = values();
		int current = 0;
		for(int i = 0; i < all.length; i++)
			if(all[i].equals(this))
				current = i;
		int straightResult = (current + straightAngle) % 4;
		
		int res = all[straightResult].getDy();
		if(isHalfAngle > 0)
			res += all[(straightResult + 1) % 4].getDy();
		return res;
	}
}