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
    public float arenaSize = 0.82f;
    public float scale = 92f;
    public float octaves = 3f;
    public float jaggedness = 0.35f;
    public float warp = 26f;

    public float wallDensity = 0.42f;
    public float canyonWidth = 0.22f;
    public float rimWidth = 0.10f;

    public boolean generateWater = true;
    public float waterSize = 0.30f;
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
            new SliderOption("scale", () -> scale, f -> scale = f, 40f, 200f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 5f, 1f),
            new SliderOption("jaggedness", () -> jaggedness, f -> jaggedness = f, 0.1f, 1f, 0.05f),
            new SliderOption("warp", () -> warp, f -> warp = f, 0f, 60f, 1f),

            new SliderOption("wallDensity", () -> wallDensity, f -> wallDensity = f, 0.05f, 0.85f, 0.02f),
            new SliderOption("canyonWidth", () -> canyonWidth, f -> canyonWidth = f, 0.05f, 0.55f, 0.02f),
            new SliderOption("rimWidth", () -> rimWidth, f -> rimWidth = f, 0f, 0.35f, 0.01f),

            new ToggleOption("generateWater", () -> generateWater, b -> generateWater = b),
            new SliderOption("waterSize", () -> waterSize, f -> waterSize = f, 0.1f, 0.7f, 0.02f),
            new ToggleOption("waterIslands", () -> waterIslands, b -> waterIslands = b)
        };
    }

    private float ridge(int s, int oct, float fall, float scl, float x, float y) {
        float n = (float) Simplex.noise2d(s, oct, fall, 1f / scl, x, y);
        return 1f - Math.abs(n * 2f - 1f);
    }

    @Override
    public void apply(GenerateInput in) {
        float nx = (in.x / (float) in.width) * 2f - 1f;
        float ny = (in.y / (float) in.height) * 2f - 1f;

        int mode = Math.round(preset);
        int oct = Math.max(1, Math.round(octaves));

        float wx = in.x;
        float wy = in.y;
        if (warp > 0.1f) {
            float wxs = 1f / (scale * 1.1f);
            wx += ((float) Simplex.noise2d(seed + 11, 2, 0.5f, wxs, in.x, in.y) - 0.5f) * 2f * warp;
            wy += ((float) Simplex.noise2d(seed + 23, 2, 0.5f, wxs, in.x, in.y) - 0.5f) * 2f * warp;
        }

        float shapeDist;
        if (mode == 2) {
            float spine = ((float) Simplex.noise2d(seed + 31, 2, 0.5f, 1f / (scale * 1.8f), 0, in.y) - 0.5f) * 0.55f;
            shapeDist = Math.abs(nx - spine) * 1.15f + Math.abs(ny) * 0.55f;
        } else if (mode == 1) {
            float cheb = Math.max(Math.abs(nx * 0.9f), Math.abs(ny));
            float eucl = Mathf.dst(nx, ny, 0f, 0f);
            shapeDist = Mathf.lerp(eucl, cheb, 0.55f);
        } else {
            float eucl = Mathf.dst(nx, ny, 0f, 0f);
            shapeDist = eucl;
        }

        float macro = (float) Simplex.noise2d(seed + 43, Math.max(1, oct - 1), 0.45f, 1f / (scale * 1.4f), wx, wy);
        float micro = ridge(seed + 59, 2, 0.55f, scale * 0.7f, wx, wy);
        float boundaryVal = shapeDist + (macro - 0.5f) * 0.55f * jaggedness + (micro - 0.5f) * 0.18f * jaggedness;

        if (boundaryVal > arenaSize && mode != 3) {
            in.floor = floor;
            in.block = wall;
            return;
        }

        float c1 = Math.abs((float) Simplex.noise2d(seed + 71, 2, 0.5f, 1f / (scale * 1.25f), wx, wy) - 0.5f) * 2f;
        float c2 = Math.abs((float) Simplex.noise2d(seed + 83, 2, 0.5f, 1f / (scale * 1.25f), wy + 170f, wx + 170f) - 0.5f) * 2f;
        float corridor = Math.min(c1, c2);

        float roomMacro = (float) Simplex.noise2d(seed + 97, Math.max(1, oct - 1), 0.5f, 1f / (scale * 1.05f), wx, wy);
        float roomDetail = ridge(seed + 109, 2, 0.55f, scale * 0.65f, wx, wy);
        float rock = roomMacro * 0.7f + roomDetail * 0.3f;

        boolean carved = corridor < canyonWidth * 0.5f;
        boolean internalWall = !carved && (rock > (1.02f - wallDensity));

        if (mode == 3) {
            boolean outerRing = boundaryVal > arenaSize * 1.05f;
            internalWall = internalWall || outerRing;
        }

        float wallGap = 1f;
        if (!internalWall) {
            float borderGap = (mode != 3) ? (arenaSize - boundaryVal) : 1f;
            float rockGap = (1.02f - wallDensity) - rock;
            float canyonGap = carved ? (canyonWidth * 0.5f - corridor) * 1.6f : 1f;
            wallGap = Math.min(borderGap, Math.max(rockGap, 0f));
            if (carved) wallGap = Math.min(wallGap, canyonGap);
        }

        if (generateWater) {
            float wMask = 10f;
            if (mode == 0) {
                float lake = (float) Simplex.noise2d(seed + 131, 3, 0.55f, 1f / (scale * 1.15f), wx, wy);
                wMask = Mathf.dst(nx, ny, 0f, 0f) + (lake - 0.5f) * 0.7f;
            } else if (mode == 1) {
                float river = ((float) Simplex.noise2d(seed + 149, 2, 0.5f, 1f / (scale * 1.8f), 0f, wy) - 0.5f) * 0.55f;
                float widthMod = ((float) Simplex.noise2d(seed + 163, 3, 0.55f, 1f / (scale * 1.1f), wx, wy) - 0.5f) * 0.30f;
                wMask = Math.abs(nx - river) + widthMod;
            } else if (mode == 2) {
                float bay = (float) Simplex.noise2d(seed + 179, 3, 0.55f, 1f / (scale * 1.2f), wx, wy);
                wMask = Mathf.dst(nx, ny, 0.55f, -0.1f) + (bay - 0.5f) * 0.5f;
            } else {
                float sea = (float) Simplex.noise2d(seed + 193, 3, 0.55f, 1f / (scale * 1.25f), wx, wy);
                wMask = 1f - sea;
            }

            if (wMask < waterSize) {
                float island = (float) Simplex.noise2d(seed + 211, 3, 0.55f, 1f / (scale * 0.55f), wx, wy);
                boolean isIsland = waterIslands && island > 0.74f && wMask > waterSize * 0.28f;

                if (isIsland) {
                    in.block = island > 0.85f ? wall : Blocks.air;
                    in.floor = (in.block == Blocks.air && rimWidth > 0.005f) ? rimFloor : floor;
                    return;
                } else {
                    float depth = waterSize - wMask;
                    in.floor = depth > 0.075f ? deepWater : shallowWater;
                    in.block = Blocks.air;
                    return;
                }
            } else if (rimWidth > 0.005f && wMask < waterSize + rimWidth * 0.5f && !internalWall) {
                in.floor = rimFloor;
                in.block = Blocks.air;
                return;
            }
        }

        if (internalWall) {
            in.floor = floor;
            in.block = wall;
        } else {
            in.block = Blocks.air;
            in.floor = (rimWidth > 0.005f && wallGap < rimWidth * 0.5f) ? rimFloor : floor;
        }
    }
}
