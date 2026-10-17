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
    public Block floor2 = Blocks.sand;
    public Block floor3 = Blocks.darksand;
    public Block wall = Blocks.stoneWall;
    public Block water = Blocks.water;
    public Block deepwater = Blocks.deepwater;
    public float scale = 60f;
    public float octaves = 4f;
    public float warp = 25f;
    public float landCoverage = 0.45f;
    public float wallDensity = 0.35f;
    public float edgeFalloff = 0.65f;
    public boolean useBiomes = true;
    public float biomeScale = 75f;
    public boolean useWater = true;
    public float waterCoverage = 0.25f;
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
        int preset = Mathf.random(0, 4);
        switch(preset) {
            case 0:
                scale = 45f; octaves = 4f; warp = 30f;
                landCoverage = 0.35f; wallDensity = 0.35f; edgeFalloff = 0.4f;
                waterCoverage = 0.25f; biomeScale = 60f;
                break;
            case 1:
                scale = 90f; octaves = 5f; warp = 15f;
                landCoverage = 0.7f; wallDensity = 0.45f; edgeFalloff = 0.8f;
                waterCoverage = 0.15f; biomeScale = 100f;
                break;
            case 2:
                scale = 110f; octaves = 3f; warp = 65f;
                landCoverage = 0.55f; wallDensity = 0.65f; edgeFalloff = 0.6f;
                waterCoverage = 0.2f; biomeScale = 80f;
                break;
            case 3:
                scale = 30f; octaves = 6f; warp = 10f;
                landCoverage = 0.5f; wallDensity = 0.4f; edgeFalloff = 0.9f;
                waterCoverage = 0.1f; biomeScale = 50f;
                break;
            case 4:
                scale = 70f; octaves = 4f; warp = 20f;
                landCoverage = 0.6f; wallDensity = 0.3f; edgeFalloff = 0.5f;
                waterCoverage = 0.45f; biomeScale = 90f;
                break;
        }
    }
    @Override
    public FilterOption[] options() {
        return new FilterOption[]{
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
            new BlockOption("floor2", () -> floor2, b -> floor2 = b, floorsOnly),
            new BlockOption("floor3", () -> floor3, b -> floor3 = b, floorsOnly),
            new BlockOption("wall", () -> wall, b -> wall = b, wallsOnly),
            new BlockOption("water", () -> water, b -> water = b, floorsOnly),
            new BlockOption("deepwater", () -> deepwater, b -> deepwater = b, floorsOnly),
            new SliderOption("scale", () -> scale, f -> scale = f, 10f, 200f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 8f, 1f),
            new SliderOption("warp", () -> warp, f -> warp = f, 0f, 100f, 1f),
            new SliderOption("landCoverage", () -> landCoverage, f -> landCoverage = f, 0.1f, 1.5f, 0.05f),
            new SliderOption("wallDensity", () -> wallDensity, f -> wallDensity = f, 0f, 1f, 0.02f),
            new SliderOption("edgeFalloff", () -> edgeFalloff, f -> edgeFalloff = f, 0f, 2f, 0.05f),
            new ToggleOption("useBiomes", () -> useBiomes, b -> useBiomes = b),
            new SliderOption("biomeScale", () -> biomeScale, f -> biomeScale = f, 10f, 200f, 1f),
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
        float landVal = baseNoise + landCoverage - (dist * edgeFalloff);
        if (landVal < 0.5f) {
            in.block = Blocks.air;
            if (clearVoid) in.floor = Blocks.air;
            return;
        }
        Block selectedFloor = floor;
        if (useBiomes) {
            float b1 = (float) Simplex.noise2d(seed + 141, 3, 0.5f, 1f / biomeScale, wx, wy);
            float b2 = (float) Simplex.noise2d(seed + 211, 3, 0.5f, 1f / biomeScale, wy, wx);
            if (b1 > 0.65f) selectedFloor = floor2;
            else if (b2 > 0.65f) selectedFloor = floor3;
        }
        in.floor = selectedFloor;
        float wallNoise = (float) Simplex.noise2d(seed + 37, (int) octaves, 0.5f, 1f / (scale * 0.8f), wx, wy);
        if (wallNoise < wallDensity) {
            in.block = wall;
        } else {
            in.block = Blocks.air;
            if (useWater) {
                float waterNoise = (float) Simplex.noise2d(seed + 313, 3, 0.5f, 1f / scale, wx, wy);
                if (waterNoise < waterCoverage) {
                    in.floor = (waterNoise < waterCoverage * 0.6f) ? deepwater : water;
                }
            }
        }
    }
}
