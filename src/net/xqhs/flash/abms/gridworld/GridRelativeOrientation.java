package net.xqhs.flash.abms.gridworld;

/**
 * Implementation for orthogonal and diagonal relative orientations in a rectangular grid.
 * 
 * @author Andrei Olaru
 */
public enum GridRelativeOrientation {
	/**
	 * Same direction as the current orientation.
	 */
	NORTH(0),
	
	/**
	 * Direction clockwise from the previous value.
	 */
	NORTH_EAST(1),
	
	/**
	 * Direction clockwise from the previous value.
	 */
	EAST(2),
	
	/**
	 * Direction clockwise from the previous value.
	 */
	SOUTH_EAST(3),
	
	/**
	 * Direction clockwise from the previous value.
	 */
	SOUTH(4),
	
	/**
	 * Direction clockwise from the previous value.
	 */
	SOUTH_WEST(5),
	
	/**
	 * Direction clockwise from the previous value.
	 */
	WEST(6),
	
	/**
	 * Direction clockwise from the previous value.
	 */
	NORTH_WEST(7),
	
	;
	
	/**
	 * 0 to 7
	 */
	int angle;
	
	/**
	 * @param relativeAngle
	 *            - angle relative to the front, clockwise, in increments such that 8 increments is a full circle.
	 */
	private GridRelativeOrientation(int relativeAngle)
	{
		angle = relativeAngle;
	}
	
	/**
	 * @return the angle (see constructor).
	 */
	int getAngle()
	{
		return angle;
	}
}