package advancedredactor.generation;

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
    public Block block = Blocks.stoneWall;
    public Block waterFloor = Blocks.deepwater;
    public Block shallowWater = Blocks.water;

    public float scale = 45f;
    public float threshold = 0.55f;
    public float floorThreshold = 0.35f;
    public float octaves = 4f;
    public float falloff = 0.5f;
    public float magnitude = 1.2f;
    public float warpScale = 25f;
    public float warpStrength = 10f;
    public float circleFalloff = 0f;

    public boolean generateCaves = false;
    public float caveScale = 20f;
    public float caveThreshold = 0.22f;

    public boolean generateWater = false;
    public float waterLevel = 0.25f;

    public boolean clearWalls = false;
    public boolean clearFloors = false;

    @Override
    public FilterOption[] options() {
        return new FilterOption[]{
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
            new BlockOption("wall", () -> block, b -> block = b, wallsOnly),
            new BlockOption("water", () -> shallowWater, b -> shallowWater = b, floorsOnly),
            new BlockOption("deepwater", () -> waterFloor, b -> waterFloor = b, floorsOnly),

            new SliderOption("scale", () -> scale, f -> scale = f, 5f, 250f, 1f),
            new SliderOption("magnitude", () -> magnitude, f -> magnitude = f, 0.1f, 3f, 0.05f),
            new SliderOption("threshold", () -> threshold, f -> threshold = f, 0f, 1.2f, 0.02f),
            new SliderOption("floorThreshold", () -> floorThreshold, f -> floorThreshold = f, 0f, 1.2f, 0.02f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f, 1f),
            new SliderOption("falloff", () -> falloff, f -> falloff = f, 0.05f, 1f, 0.05f),

            new SliderOption("warpScale", () -> warpScale, f -> warpScale = f, 5f, 150f, 1f),
            new SliderOption("warpStrength", () -> warpStrength, f -> warpStrength = f, 0f, 60f, 1f),
            new SliderOption("circleFalloff", () -> circleFalloff, f -> circleFalloff = f, 0f, 2f, 0.05f),

            new ToggleOption("generateCaves", () -> generateCaves, b -> generateCaves = b),
            new SliderOption("caveScale", () -> caveScale, f -> caveScale = f, 5f, 100f, 1f),
            new SliderOption("caveThreshold", () -> caveThreshold, f -> caveThreshold = f, 0.05f, 0.6f, 0.01f),

            new ToggleOption("generateWater", () -> generateWater, b -> generateWater = b),
            new SliderOption("waterLevel", () -> waterLevel, f -> waterLevel = f, 0f, 0.8f, 0.02f),

            new ToggleOption("clearWalls", () -> clearWalls, b -> clearWalls = b),
            new ToggleOption("clearFloors", () -> clearFloors, b -> clearFloors = b)
        };
    }

    @Override
    public char icon() {
        return Iconc.blockStoneWall;
    }

    @Override
    public void apply() {
        float wx = in.x, wy = in.y;

        if (warpStrength > 0.01f) {
            float dx = (float) Simplex.noise2d(seed + 11, 2, 0.5, 1f / warpScale, in.x, in.y) * warpStrength;
            float dy = (float) Simplex.noise2d(seed + 23, 2, 0.5, 1f / warpScale, in.x, in.y) * warpStrength;
            wx += dx;
            wy += dy;
        }

        float n = (float) Simplex.noise2d(seed, octaves, falloff, 1f / scale, wx, wy) * magnitude;

        if (circleFalloff > 0.01f) {
            float dst = Mathf.dst(in.x / (float) in.width, in.y / (float) in.height, 0.5f, 0.5f) * 2f;
            n -= dst * circleFalloff;
        }

        if (clearFloors && floor != Blocks.air) {
            in.floor = floor;
        }
        if (clearWalls) {
            in.block = Blocks.air;
        }

        if (generateWater && n < waterLevel) {
            if (n < waterLevel - 0.12f) {
                in.floor = waterFloor;
            } else {
                in.floor = shallowWater;
            }
            in.block = Blocks.air;
            return;
        }

        if (n >= floorThreshold && floor != Blocks.air) {
            in.floor = floor;
        }

        if (n >= threshold) {
            boolean isCave = false;
            if (generateCaves) {
                float cn = (float) Math.abs(Simplex.noise2d(seed + 57, 2, 0.5, 1f / caveScale, in.x, in.y) - 0.5f);
                if (cn < caveThreshold * 0.5f) {
                    isCave = true;
                }
            }
            if (!isCave && block != Blocks.air) {
                in.block = block;
            } else if (isCave) {
                in.block = Blocks.air;
            }
        }
    }
    }
