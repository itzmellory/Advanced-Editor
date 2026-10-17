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
    public Block rimFloor = Blocks.sand;
    public Block wall = Blocks.stoneWall;
    public Block water = Blocks.water;
    public Block deepwater = Blocks.deepwater;
    public float scale = 75f;
    public float octaves = 4f;
    public float warp = 25f;
    public float jaggedness = 0.5f;
    public float landCoverage = 0.45f;
    public float wallDensity = 0.35f;
    public float edgeFalloff = 0.4f;
    public boolean useWater = true;
    public float waterCoverage = 0.2f;
    public boolean clearVoid = true;
    @Override
    public String name() {
        return Core.bundle.get("filter.advancedterrain.name", "Organic Terrain Creator");
    }
    @Override
    public char icon() {
        return Iconc.blockStoneWall;
    }
    @Override
    public void randomize() {
        seed = Mathf.random(100000);
        int preset = Mathf.random(0, 3);
        switch(preset) {
            case 0:
                scale = 80f; octaves = 4f; warp = 35f; jaggedness = 0.25f;
                landCoverage = 0.45f; wallDensity = 0.35f; edgeFalloff = 0.35f; waterCoverage = 0.25f;
                break;
            case 1:
                scale = 100f; octaves = 5f; warp = 20f; jaggedness = 0.85f;
                landCoverage = 0.35f; wallDensity = 0.5f; edgeFalloff = 0.15f; waterCoverage = 0.15f;
                break;
            case 2:
                scale = 55f; octaves = 3f; warp = 45f; jaggedness = 0.6f;
                landCoverage = 0.5f; wallDensity = 0.45f; edgeFalloff = 0.5f; waterCoverage = 0.3f;
                break;
            case 3:
                scale = 130f; octaves = 5f; warp = 15f; jaggedness = 0.15f;
                landCoverage = 0.65f; wallDensity = 0.4f; edgeFalloff = 0.7f; waterCoverage = 0.2f;
                break;
        }
    }
    @Override
    public FilterOption[] options() {
        return new FilterOption[]{
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
            new BlockOption("rimFloor", () -> rimFloor, b -> rimFloor = b, floorsOnly),
            new BlockOption("wall", () -> wall, b -> wall = b, wallsOnly),
            new BlockOption("water", () -> water, b -> water = b, floorsOnly),
            new BlockOption("deepwater", () -> deepwater, b -> deepwater = b, floorsOnly),
            new SliderOption("scale", () -> scale, f -> scale = f, 10f, 200f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 8f, 1f),
            new SliderOption("warp", () -> warp, f -> warp = f, 0f, 100f, 1f),
            new SliderOption("jaggedness", () -> jaggedness, f -> jaggedness = f, 0f, 1f, 0.05f),
            new SliderOption("landCoverage", () -> landCoverage, f -> landCoverage = f, 0.1f, 1.5f, 0.05f),
            new SliderOption("wallDensity", () -> wallDensity, f -> wallDensity = f, 0f, 1f, 0.02f),
            new SliderOption("edgeFalloff", () -> edgeFalloff, f -> edgeFalloff = f, 0f, 2f, 0.05f),
            new ToggleOption("useWater", () -> useWater, b -> useWater = b),
            new SliderOption("waterCoverage", () -> waterCoverage, f -> waterCoverage = f, 0f, 1f, 0.02f),
            new ToggleOption("clearVoid", () -> clearVoid, b -> clearVoid = b)
        };
    }
    @Override
    public void apply(GenerateInput in) {
        float nx = (in.x / (float) in.width) * 2f - 1f;
        float ny = (in.y / (float) in.height) * 2f - 1f;
        float dist = Mathf.dst(nx, ny, 0f, 0f);
        float wx = in.x;
        float wy = in.y;
        if (warp > 0.01f) {
            wx += (float) Simplex.noise2d(seed + 11, 3, 0.5f, 1f / scale, in.x, in.y) * warp;
            wy += (float) Simplex.noise2d(seed + 23, 3, 0.5f, 1f / scale, in.x, in.y) * warp;
        }
        float baseNoise = (float) Simplex.noise2d(seed, (int) octaves, 0.5f, 1f / scale, wx, wy);
        float ridgeNoise = 1f - Math.abs((float) Simplex.noise2d(seed + 41, (int) octaves, 0.5f, 1f / scale, wx, wy) * 2f - 1f);
        float mergedNoise = Mathf.lerp(baseNoise, ridgeNoise, jaggedness);
        float landVal = mergedNoise + landCoverage - (dist * edgeFalloff);
        if (landVal < 0.5f) {
            in.block = Blocks.air;
            if (clearVoid) in.floor = Blocks.air;
            return;
        }
        in.floor = (landVal < 0.55f) ? rimFloor : floor;
        float wallNoise = (float) Simplex.noise2d(seed + 37, (int) octaves, 0.5f, 1f / (scale * 0.8f), wx, wy);
        if (wallNoise < wallDensity) {
            in.block = wall;
        } else {
            in.block = Blocks.air;
            if (useWater && in.floor != rimFloor) {
                float waterNoise = (float) Simplex.noise2d(seed + 313, 3, 0.5f, 1f / scale, wx, wy);
                if (waterNoise < waterCoverage) {
                    in.floor = (waterNoise < waterCoverage * 0.6f) ? deepwater : water;
                }
            }
        }
    }
}
