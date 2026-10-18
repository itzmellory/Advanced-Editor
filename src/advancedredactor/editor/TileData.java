package advancedredactor.editor;

import mindustry.world.Block;

public class TileData {
    public int x;
    public int y;
    public Block floor;
    public Block block;
    public Block overlay;

    public TileData(int x, int y, Block floor, Block block, Block overlay) {
        this.x = x;
        this.y = y;
        this.floor = floor;
        this.block = block;
        this.overlay = overlay;
    }
}
