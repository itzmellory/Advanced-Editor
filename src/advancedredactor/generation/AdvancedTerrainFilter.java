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
    public Block floor3 = Blocks.snow;
    public Block rimFloor = Blocks.sand;
    public Block block = Blocks.stoneWall;
    public Block block2 = Blocks.duneWall;
    public Block borderWall = Blocks.duneWall;
    public Block shallowWater = Blocks.water;
    public Block waterFloor = Blocks.deepwater;

    public float scale = 55f;
    public float magnitude = 1.25f;
    public float octaves = 5f;
    public float falloff = 0.55f;
    public float jaggedness = 0.35f;

    public float warpScale = 28f;
    public float warpStrength = 14f;

    public float wallThreshold = 0.45f;
    public float rimWidth = 0.08f;

    public boolean useVoidBorder = true;
    public float borderSize = 0.72f;
    public float borderRoughness = 0.45f;

    public boolean generateCaves = true;
    public float caveScale = 42f;
    public float caveWidth = 0.14f;

    public boolean useMultiBiome = true;
    public float biomeScale = 65f;
    public float patchScale = 25f;
    public float patchThreshold = 0.32f;

    public boolean generateWater = true;
    public float waterMode = 0f;
    public float waterSize = 0.32f;

    public boolean clearWalls = true;
    public boolean clearFloors = true;

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
            new BlockOption("floor2", () -> floor2, b -> floor2 = b, floorsOnly),
            new BlockOption("floor3", () -> floor3, b -> floor3 = b, floorsOnly),
            new BlockOption("rimFloor", () -> rimFloor, b -> rimFloor = b, floorsOnly),
            new BlockOption("wall", () -> block, b -> block = b, wallsOnly),
            new BlockOption("wall2", () -> block2, b -> block2 = b, wallsOnly),
            new BlockOption("borderWall", () -> borderWall, b -> borderWall = b, wallsOnly),
            new BlockOption("water", () -> shallowWater, b -> shallowWater = b, floorsOnly),
            new BlockOption("deepwater", () -> waterFloor, b -> waterFloor = b, floorsOnly),

            new SliderOption("scale", () -> scale, f -> scale = f, 10f, 200f, 1f),
            new SliderOption("magnitude", () -> magnitude, f -> magnitude = f, 0.2f, 3f, 0.05f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 8f, 1f),
            new SliderOption("falloff", () -> falloff, f -> falloff = f, 0.1f, 0.9f, 0.05f),
            new SliderOption("jaggedness", () -> jaggedness, f -> jaggedness = f, 0f, 1f, 0.05f),

            new SliderOption("warpScale", () -> warpScale, f -> warpScale = f, 5f, 120f, 1f),
            new SliderOption("warpStrength", () -> warpStrength, f -> warpStrength = f, 0f, 50f, 1f),

            new SliderOption("wallThreshold", () -> wallThreshold, f -> wallThreshold = f, -0.2f, 1.2f, 0.02f),
            new SliderOption("rimWidth", () -> rimWidth, f -> rimWidth = f, 0f, 0.3f, 0.01f),

            new ToggleOption("useVoidBorder", () -> useVoidBorder, b -> useVoidBorder = b),
            new SliderOption("borderSize", () -> borderSize, f -> borderSize = f, 0.3f, 1.2f, 0.02f),
            new SliderOption("borderRoughness", () -> borderRoughness, f -> borderRoughness = f, 0f, 1f, 0.05f),

            new ToggleOption("generateCaves", () -> generateCaves, b -> generateCaves = b),
            new SliderOption("caveScale", () -> caveScale, f -> caveScale = f, 10f, 120f, 1f),
            new SliderOption("caveWidth", () -> caveWidth, f -> caveWidth = f, 0.02f, 0.4f, 0.01f),

            new ToggleOption("useMultiBiome", () -> useMultiBiome, b -> useMultiBiome = b),
            new SliderOption("biomeScale", () -> biomeScale, f -> biomeScale = f, 15f, 180f, 1f),
            new SliderOption("patchScale", () -> patchScale, f -> patchScale = f, 8f, 100f, 1f),
            new SliderOption("patchThreshold", () -> patchThreshold, f -> patchThreshold = f, 0.05f, 0.8f, 0.02f),

            new ToggleOption("generateWater", () -> generateWater, b -> generateWater = b),
            new SliderOption("waterMode", () -> waterMode, f -> waterMode = f, 0f, 2f, 1f),
            new SliderOption("waterSize", () -> waterSize, f -> waterSize = f, 0.05f, 0.75f, 0.02f),

            new ToggleOption("clearWalls", () -> clearWalls, b -> clearWalls = b),
            new ToggleOption("clearFloors", () -> clearFloors, b -> clearFloors = b)
        };
    }

    @Override
    public void apply(GenerateInput in) {
        float nx = (in.x / (float) in.width) * 2f - 1f;
        float ny = (in.y / (float) in.height) * 2f - 1f;
        float centerDst = Mathf.dst(nx, ny, 0f, 0f);

        float wx = in.x, wy = in.y;
        if (warpStrength > 0.01f) {
            wx += (float) Simplex.noise2d(seed + 11, 3, 0.5, 1f / warpScale, in.x, in.y) * warpStrength;
            wy += (float) Simplex.noise2d(seed + 23, 3, 0.5, 1f / warpScale, in.x, in.y) * warpStrength;
        }

        if (useVoidBorder) {
            float bNoise = (float) Simplex.noise2d(seed + 99, 4, 0.55, 1f / (scale * 0.75f), wx, wy) * borderRoughness;
            if (centerDst + bNoise > borderSize) {
                if (rimWidth > 0.005f && centerDst + bNoise <= borderSize + rimWidth * 0.65f) {
                    in.floor = rimFloor;
                    in.block = Blocks.air;
                } else {
                    in.floor = floor;
                    in.block = borderWall;
                }
                return;
            }
        }

        float baseNoise = (float) Simplex.noise2d(seed, (int) octaves, falloff, 1f / scale, wx, wy);
        float ridgeNoise = 1f - Math.abs((float) Simplex.noise2d(seed + 37, Math.max(1, (int) octaves - 1), falloff, 1f / (scale * 0.6f), wx, wy) * 2f - 1f);
        float height = Mathf.lerp(baseNoise, ridgeNoise, jaggedness) * magnitude;

        Block selectedFloor = floor;
        Block selectedWall = block;

        if (useMultiBiome) {
            float bVal = (float) Simplex.noise2d(seed + 141, 3, 0.5, 1f / biomeScale, wx, wy);
            float pVal = (float) Simplex.noise2d(seed + 211, 3, 0.5, 1f / patchScale, in.x, in.y);

            if (bVal > 0.15f) {
                selectedFloor = floor2;
                selectedWall = block2;
            }
            if (pVal > (1f - patchThreshold)) {
                selectedFloor = floor3;
            }
        }

        if (clearFloors) {
            in.floor = selectedFloor;
        }
        if (clearWalls) {
            in.block = Blocks.air;
        }

        if (generateWater) {
            int mode = Math.round(waterMode);
            if (mode == 0) {
                float lakeMask = centerDst - (baseNoise * 0.35f);
                if (lakeMask < waterSize) {
                    in.floor = (lakeMask < waterSize - 0.1f) ? waterFloor : shallowWater;
                    in.block = Blocks.air;
                    return;
                } else if (rimWidth > 0.005f && lakeMask < waterSize + rimWidth * 0.6f) {
                    in.floor = rimFloor;
                }
            } else if (mode == 1) {
                float riverCurve = (float) Simplex.noise2d(seed + 313, 3, 0.5, 1f / (scale * 1.3f), in.x, in.y) * 0.45f;
                float riverDist = Math.abs(nx + riverCurve);
                if (riverDist < waterSize * 0.65f) {
                    in.floor = (riverDist < waterSize * 0.38f) ? waterFloor : shallowWater;
                    in.block = Blocks.air;
                    return;
                } else if (rimWidth > 0.005f && riverDist < waterSize * 0.65f + rimWidth * 0.5f) {
                    in.floor = rimFloor;
                }
            } else {
                if (height < waterSize) {
                    in.floor = (height < waterSize - 0.1f) ? waterFloor : shallowWater;
                    in.block = Blocks.air;
                    return;
                } else if (rimWidth > 0.005f && height < waterSize + rimWidth * 0.5f) {
                    in.floor = rimFloor;
                }
            }
        }

        boolean carvedByCave = false;
        boolean nearCaveEdge = false;
        if (generateCaves) {
            float c1 = Math.abs((float) Simplex.noise2d(seed + 77, 3, 0.5, 1f / caveScale, wx, wy) - 0.5f);
            float c2 = Math.abs((float) Simplex.noise2d(seed + 88, 3, 0.5, 1f / (caveScale * 0.8f), wy, wx) - 0.5f);
            float caveVal = Math.min(c1, c2);
            if (caveVal < caveWidth * 0.35f) {
                carvedByCave = true;
                if (caveVal > (caveWidth * 0.35f) - (rimWidth * 0.25f)) {
                    nearCaveEdge = true;
                }
            }
        }

        if (height >= wallThreshold && !carvedByCave) {
            in.block = selectedWall;
        } else {
            in.block = Blocks.air;
            if (rimWidth > 0.005f) {
                if ((height >= wallThreshold - rimWidth && !carvedByCave) || (height >= wallThreshold && nearCaveEdge)) {
                    in.floor = rimFloor;
                }
            }
        }
    }
}
