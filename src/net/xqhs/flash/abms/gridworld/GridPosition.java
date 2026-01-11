package net.xqhs.flash.abms.gridworld;

/**
 * Implementation for positions in a rectangular grid.
 * 
 * @author Andrei Olaru
 */
public class GridPosition
{
	/**
	 * x coordinate
	 */
	int	positionX;
	/**
	 * y coordinate
	 */
	int	positionY;
	
	/**
	 * Default constructor.
	 * 
	 * @param x
	 *            - x
	 * @param y
	 *            - y
	 */
	public GridPosition(int x, int y)
	{
		positionX = x;
		positionY = y;
	}
	
	/**
	 * Creates a new instance that is a copy of the given instance.
	 * 
	 * @param copySource
	 *            - the position to copy.
	 */
	public GridPosition(GridPosition copySource)
	{
		this.positionX = copySource.positionX;
		this.positionY = copySource.positionY;
	}
	
	/**
	 * @return the X coordinate (column) of this GridPosition
	 */
	public int getX()
	{
		return positionX;
	}
	
	/**
	 * @return the Y coordinate (row) of this GridPosition
	 */
	public int getY()
	{
		return positionY;
	}
	
	/**
	 * Returns the position that is one of the 4 orthogonal neighbors of <code>this</code> and is in a direction
	 * indicated by the orientation argument.
	 * <p>
	 * E.g. if direction is NORTH, the returned position will be north of this.
	 * 
	 * @param direction
	 *            - direction of the neighbor position, as an {@link GridOrientation} instance.
	 * @return the neighbor {@link GridPosition}.
	 */
	public GridPosition getNeighborPosition(GridOrientation direction)
	{
		GridOrientation or = direction;
		switch(or)
		{
		case NORTH:
			return new GridPosition(positionX, positionY + 1);
		case EAST:
			return new GridPosition(positionX + 1, positionY);
		case SOUTH:
			return new GridPosition(positionX, positionY - 1);
		case WEST:
			return new GridPosition(positionX - 1, positionY);
		default:
			break;
		}
		return null;
	}
	
	/**
	 * Returns the position that is one of the 8 (orthogonal and diagonal) neighbors of <code>this</code> and is in a
	 * direction that has the provided orientation relative to the provided direction.
	 * 
	 * @param direction
	 *            // * - direction used to compute the neighbor position, as an {@link GridOrientation} instance.
	 * @param relativeOrientation
	 *            - orientation of the returned position, relative to the direction in the first argument.
	 * @return the neighbor {@link GridPosition}.
	 */
	public GridPosition getNeighborPosition(GridOrientation direction, GridRelativeOrientation relativeOrientation)
	{
		GridOrientation or = direction;
		GridRelativeOrientation ror = relativeOrientation;
		return new GridPosition(positionX + or.getRelativeDx(ror), positionY + or.getRelativeDy(ror));
	}
	
	@Override
	public String toString()
	{
		return "(" + positionX + ", " + positionY + ")";
	}
	
	/**
	 * Indicates whether the provided position is a neighbor (orthogonal or diagonal).
	 * 
	 * @param neighbor
	 *            - the candidate neighbor.
	 * @return <code>true</code> if the candidate is a neighbor.
	 */
	public boolean isNeighbor(GridPosition neighbor)
	{
		GridPosition pos = neighbor;
		return (Math.abs(positionX - pos.positionX) <= 1) && (Math.abs(positionY - pos.positionY) <= 1);
	}
	
	/**
	 * Indicates whether the provided position is an orthogonal neighbor (north, south, east or west).
	 * 
	 * @param neighbor
	 *            - the candidate neighbor.
	 * @return <code>true</code> if the candidate is a neighbor.
	 */
	public boolean isNeighborOrtho(GridPosition neighbor)
	{
		GridPosition pos = neighbor;
		return ((Math.abs(positionX - pos.positionX) == 1) && (positionY == pos.positionY))
				|| ((Math.abs(positionY - pos.positionY) == 1) && (positionX == pos.positionX));
	}
	
	/**
	 * Determines the relative orientation of another GridPosition with respect to <code>this</code>.
	 * 
	 * @param otherPosition
	 *            The position for which the relative orientation needs to be determined.
	 * @return The relative orientation of <code>otherPosition</code> with respect to the current one.
	 */
	public GridRelativeOrientation getRelativeOrientation(GridPosition otherPosition)
	{
		int deltaX = (int) Math.signum(otherPosition.positionX - positionX);
		int deltaY = (int) Math.signum(otherPosition.positionY - positionY);
		
		if(deltaX == 0)
		{
			if(deltaY >= 0)
				return GridRelativeOrientation.NORTH;
			return GridRelativeOrientation.SOUTH;
		}
		else if(deltaX > 0)
		{
			if(deltaY > 0)
				return GridRelativeOrientation.NORTH_EAST;
			else if(deltaY < 0)
				return GridRelativeOrientation.SOUTH_EAST;
			else
				return GridRelativeOrientation.EAST;
		}
		else
		{
			if(deltaY > 0)
				return GridRelativeOrientation.NORTH_WEST;
			else if(deltaY < 0)
				return GridRelativeOrientation.SOUTH_WEST;
			else
				return GridRelativeOrientation.WEST;
		}
	}
	
	/**
	 * Returns the relative orientation of a position relative to <code>this</code> position and a reference
	 * orientation.
	 * <p>
	 * E.g. for a referenceOrientation of EAST, an orthogonal neighbor position to the right of this position will be in
	 * FRONT.
	 * 
	 * @param referenceOrientation
	 *            - the absolute orientation which is considered as 'front', or 'forward'.
	 * @param neighborPosition
	 *            - the position for which to compute the relative orientation.
	 * @return the relative orientation.
	 * @throws IllegalArgumentException
	 *             if the given position is not a neighbor.
	 */
	public GridRelativeOrientation getRelativeOrientation(GridOrientation referenceOrientation,
			GridPosition neighborPosition)
	{
		if(!isNeighbor(neighborPosition))
			throw new IllegalArgumentException("Given position is not a neighbor");
		for(GridRelativeOrientation orient : GridRelativeOrientation.values())
			if(getNeighborPosition(referenceOrientation, orient).equals(neighborPosition))
				return orient;
		throw new IllegalStateException("Should not be here");
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof GridPosition)
			return (positionX == ((GridPosition) obj).positionX) && (positionY == ((GridPosition) obj).positionY);
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return positionX + positionY;
	}
	
	/**
	 * Computes Manhattan distance from specified position.
	 * 
	 * @param pos
	 *            - the other position.
	 * @return the distance.
	 */
	public int getDistanceTo(GridPosition pos)
	{
		return Math.abs(positionX - pos.positionX) + Math.abs(positionY - pos.positionY);
	}
	
	/**
	 * @return <code>true</code> if the y coordinate is even.
	 */
	public boolean isYEven()
	{
		return (Math.abs(positionY) % 2) == 0;
	}
	
	/**
	 * @return <code>true</code> if the x coordinate is even.
	 */
	public boolean isXEven()
	{
		return (Math.abs(positionX) % 2) == 0;
	}
}
