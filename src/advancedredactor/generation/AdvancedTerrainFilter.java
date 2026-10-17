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
    public Block shallowWater = Blocks.water;
    public Block deepWater = Blocks.deepwater;

    public float preset = 0f;
    public float arenaSize = 0.78f;
    public float scale = 68f;
    public float octaves = 5f;
    public float jaggedness = 0.55f;
    public float warp = 18f;

    public float wallDensity = 0.38f;
    public float canyonWidth = 0.24f;
    public float rimWidth = 0.14f;

    public boolean generateWater = true;
    public float waterSize = 0.36f;
    public boolean waterIslands = true;

    @Override
    public String name() {
        return Core.bundle.get("filter.advancedterrain.name", "Advanced Terrain Creator");
    }

    @Override
    public char icon() {
        return Iconc.blockStoneWall;
    }

    @Override
    public FilterOption[] options() {
        return new FilterOption[]{
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
            new BlockOption("rimFloor", () -> rimFloor, b -> rimFloor = b, floorsOnly),
            new BlockOption("wall", () -> wall, b -> wall = b, wallsOnly),
            new BlockOption("water", () -> shallowWater, b -> shallowWater = b, floorsOnly),
            new BlockOption("deepwater", () -> deepWater, b -> deepWater = b, floorsOnly),

            new SliderOption("preset", () -> preset, f -> preset = f, 0f, 3f, 1f),
            new SliderOption("arenaSize", () -> arenaSize, f -> arenaSize = f, 0.45f, 1.05f, 0.02f),
            new SliderOption("scale", () -> scale, f -> scale = f, 25f, 160f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 2f, 8f, 1f),
            new SliderOption("jaggedness", () -> jaggedness, f -> jaggedness = f, 0.1f, 1f, 0.05f),
            new SliderOption("warp", () -> warp, f -> warp = f, 0f, 45f, 1f),

            new SliderOption("wallDensity", () -> wallDensity, f -> wallDensity = f, 0.05f, 0.85f, 0.02f),
            new SliderOption("canyonWidth", () -> canyonWidth, f -> canyonWidth = f, 0.05f, 0.55f, 0.02f),
            new SliderOption("rimWidth", () -> rimWidth, f -> rimWidth = f, 0f, 0.35f, 0.01f),

            new ToggleOption("generateWater", () -> generateWater, b -> generateWater = b),
            new SliderOption("waterSize", () -> waterSize, f -> waterSize = f, 0.1f, 0.7f, 0.02f),
            new ToggleOption("waterIslands", () -> waterIslands, b -> waterIslands = b)
        };
    }

    private float ridged(int s, int oct, float fall, float scl, float x, float y) {
        float n = (float) Simplex.noise2d(s, oct, fall, 1f / scl, x, y);
        return 1f - Math.abs(n * 2f - 1f);
    }

    @Override
    public void apply(GenerateInput in) {
        float nx = (in.x / (float) in.width) * 2f - 1f;
        float ny = (in.y / (float) in.height) * 2f - 1f;

        int mode = Math.round(preset);
        int oct = Math.max(2, Math.round(octaves));

        float wx = in.x;
        float wy = in.y;
        if (warp > 0.1f) {
            wx += ((float) Simplex.noise2d(seed + 11, 3, 0.5f, 1f / (scale * 0.65f), in.x, in.y) - 0.5f) * 2f * warp;
            wy += ((float) Simplex.noise2d(seed + 23, 3, 0.5f, 1f / (scale * 0.65f), in.x, in.y) - 0.5f) * 2f * warp;
        }

        float shapeDist;
        if (mode == 2) {
            float spine = ((float) Simplex.noise2d(seed + 31, 2, 0.5f, 1f / (scale * 1.6f), 0, in.y) - 0.5f) * 0.75f;
            shapeDist = Math.abs(nx - spine) * 1.25f + Math.abs(ny) * 0.45f;
        } else {
            float cheb = Math.max(Math.abs(nx), Math.abs(ny));
            float eucl = Mathf.dst(nx, ny, 0f, 0f);
            shapeDist = Mathf.lerp(eucl, cheb, 0.35f);
        }

        float edgeMacro = ((float) Simplex.noise2d(seed + 43, oct, 0.55f, 1f / scale, wx, wy) - 0.5f) * 0.85f * jaggedness;
        float edgeMicro = (ridged(seed + 59, 3, 0.6f, scale * 0.35f, wx, wy) - 0.5f) * 0.3f * jaggedness;
        float boundaryVal = shapeDist + edgeMacro + edgeMicro;

        if (boundaryVal > arenaSize && mode != 3) {
            in.floor = floor;
            in.block = wall;
            return;
        }

        float c1 = Math.abs((float) Simplex.noise2d(seed + 71, oct, 0.55f, 1f / (scale * 0.9f), wx, wy) - 0.5f) * 2f;
        float c2 = Math.abs((float) Simplex.noise2d(seed + 83, oct, 0.55f, 1f / (scale * 0.9f), wy + 100f, wx + 100f) - 0.5f) * 2f;
        float corridorField = Math.min(c1, c2);

        float roomNoise = (float) Simplex.noise2d(seed + 97, oct, 0.55f, 1f / (scale * 0.75f), wx, wy);
        float ridgeDetail = ridged(seed + 109, 3, 0.6f, scale * 0.32f, wx, wy);
        float rockField = Mathf.lerp(roomNoise, ridgeDetail, jaggedness * 0.6f);

        boolean carvedCorridor = corridorField < canyonWidth * 0.55f;
        boolean solidInternalWall = !carvedCorridor && (rockField > (1.05f - wallDensity));

        if (mode == 3) {
            boolean outerRing = boundaryVal > arenaSize * 1.15f;
            solidInternalWall = solidInternalWall || outerRing;
        }

        float wallDistEstimate = 1f;
        if (!solidInternalWall) {
            float borderGap = (mode != 3) ? (arenaSize - boundaryVal) : 1f;
            float rockGap = (1.05f - wallDensity) - rockField;
            float canyonGap = carvedCorridor ? (canyonWidth * 0.55f - corridorField) * 1.4f : 1f;

            if (carvedCorridor && rockField > (1.05f - wallDensity)) {
                wallDistEstimate = Math.min(borderGap, canyonGap);
            } else {
                wallDistEstimate = Math.min(borderGap, Math.max(0f, rockGap));
            }
        }

        if (generateWater) {
            float waterMask = 10f;
            if (mode == 0) {
                float lakeWarp = ((float) Simplex.noise2d(seed + 131, oct, 0.55f, 1f / (scale * 0.8f), wx, wy) - 0.5f) * 0.55f;
                waterMask = Mathf.dst(nx, ny, 0f, 0f) + lakeWarp;
            } else if (mode == 1) {
                float riverMeander = ((float) Simplex.noise2d(seed + 149, 3, 0.5f, 1f / (scale * 1.4f), 0f, wy) - 0.5f) * 0.65f;
                float widthMod = ((float) Simplex.noise2d(seed + 163, oct, 0.55f, 1f / (scale * 0.7f), wx, wy) - 0.5f) * 0.38f;
                waterMask = Math.abs(nx - riverMeander) + widthMod;
            } else if (mode == 2) {
                float bayNoise = ((float) Simplex.noise2d(seed + 179, oct, 0.55f, 1f / (scale * 0.85f), wx, wy) - 0.5f) * 0.45f;
                waterMask = Mathf.dst(nx, ny, 0.55f, -0.1f) + bayNoise;
            } else {
                float seaNoise = (float) Simplex.noise2d(seed + 193, oct, 0.55f, 1f / (scale * 0.9f), wx, wy);
                waterMask = 1f - seaNoise;
            }

            if (waterMask < waterSize) {
                float islandVal = (float) Simplex.noise2d(seed + 211, 4, 0.55f, 1f / (scale * 0.42f), wx, wy);
                boolean isIsland = waterIslands && (islandVal > 0.72f) && (waterMask > waterSize * 0.22f);

                if (isIsland) {
                    in.block = (islandVal > 0.81f) ? wall : Blocks.air;
                    in.floor = (in.block == Blocks.air && rimWidth > 0.005f) ? rimFloor : floor;
                    return;
                } else {
                    float depth = waterSize - waterMask;
                    in.floor = (depth > 0.085f) ? deepWater : shallowWater;
                    in.block = Blocks.air;
                    return;
                }
            } else if (rimWidth > 0.005f && waterMask < waterSize + rimWidth * 0.45f && !solidInternalWall) {
                in.floor = rimFloor;
                in.block = Blocks.air;
                return;
            }
        }

        if (solidInternalWall) {
            in.floor = floor;
            in.block = wall;
        } else {
            in.block = Blocks.air;
            if (rimWidth > 0.005f && wallDistEstimate < rimWidth * 0.55f) {
                in.floor = rimFloor;
            } else {
                in.floor = floor;
            }
        }
    }
}
