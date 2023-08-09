package stefania.TreasureHunt.util;

public class Coord{
    public int x;
    public int y;

    public Coord(int x, int y){
        this.x = x;
        this.y = y;
    }

    public Coord(){
        this(0, 0);
    }

    public double distanceTo(Coord other){
        int dx = x - other.x;
        int dy = y - other.y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public void move(String direction) {
        switch (direction) {
            case "up":
                y -= 1;
                break;
            case "down":
                y += 1;
                break;
            case "left":
                x -= 1;
                break;
            case "right":
                x += 1;
            default:
                break;
        }
    }

    public Coord[] neighbours(){
        Coord up = this.clone();
        up.move("up");
        Coord down = this.clone();
        down.move("down");
        Coord left = this.clone();
        left.move("left");
        Coord right = this.clone();
        right.move("right");
        Coord[] neighbourhood = {up, down, left, right};
        return neighbourhood;
    }

    public String determineDirectionTo(Coord target) {
        Coord newPos = this.clone();
        Coord[] neighbourhood = this.neighbours();
        int bestNeighbourIndex = 0;
        for (int i = 0; i < neighbourhood.length; i++) {
            if(neighbourhood[i].distanceTo(target)<newPos.distanceTo(target)){
                newPos = neighbourhood[i];
                bestNeighbourIndex = i;
            }
        }
        String[] neighbourDirections={"up", "down", "left", "right"};
        return neighbourDirections[bestNeighbourIndex];
    }

    public boolean equals(Coord other){
        return (x == other.x) && (y == other.y);
    }

    @Override
    public Coord clone(){
        return new Coord(x, y);
    }

    @Override
    public String toString(){
        return "[" + x + "; "+ y +"]";
    }
}