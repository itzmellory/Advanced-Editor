package advancedredactor.generation;
import arc.*;
import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import advancedredactor.ui.ModFilterOptions.*;
import static advancedredactor.ui.ModFilterOptions.*;
public class AdvancedTerrainFilter extends GenerateFilter {
    public Block floor = Blocks.stone;
    public Block wall = Blocks.stoneWall;
    public float scale = 80f;
    public float octaves = 4f;
    public float wallDensity = 0.5f;
    @Override
    public String name() {
        return Core.bundle.get("filter.advancedterrain.name", "Advanced Terrain");
    }
    @Override
    public char icon() {
        return Iconc.blockStoneWall;
    }
    @Override
    public FilterOption[] options() {
        return new FilterOption[]{
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
            new BlockOption("wall", () -> wall, b -> wall = b, wallsOnly),
            new SliderOption("scale", () -> scale, f -> scale = f, 10f, 200f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 8f, 1f),
            new SliderOption("wallDensity", () -> wallDensity, f -> wallDensity = f, 0f, 1f, 0.05f)
        };
    }
    @Override
    public void apply(GenerateInput in) {
        float noise = (float) Simplex.noise2d(seed, (int) octaves, 0.5f, 1f / scale, in.x, in.y);
        in.floor = floor;
        in.block = (noise > (1f - wallDensity)) ? wall : Blocks.air;
    }
}
