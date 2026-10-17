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

public class AdvancedWaterFilter extends GenerateFilter {
    public Block shoalBlock = Blocks.mud;
    public Block shallowBlock = Blocks.water;
    public Block deepBlock = Blocks.deepwater;

    public float scale = 90f;
    public float complexity = 0.55f;
    public float waterLevel = 0.35f;
    public float edgeBias = 0f;

    public float shoalWidth = 2f;
    public float shallowWidth = 5f;
    public float warpScale = 0.02f;
    public float warpMag = 3f;

    @Override
    public String name() {
        return Core.bundle.get("filter.advancedwater.name", "Natural Water");
    }

    @Override
    public char icon() {
        return Iconc.liquidWater;
    }

    @Override
    public FilterOption[] options() {
        return new FilterOption[]{
            new BlockOption("shoalBlock", () -> shoalBlock, b -> shoalBlock = b, floorsOnly),
            new BlockOption("shallowBlock", () -> shallowBlock, b -> shallowBlock = b, floorsOnly),
            new BlockOption("deepBlock", () -> deepBlock, b -> deepBlock = b, floorsOnly),
            new SliderOption("scale", () -> scale, f -> scale = f, 10f, 200f, 1f),
            new SliderOption("complexity", () -> complexity, f -> complexity = f, 0f, 1f, 0.05f),
            new SliderOption("waterLevel", () -> waterLevel, f -> waterLevel = f, 0f, 1f, 0.05f),
            new SliderOption("edgeBias", () -> edgeBias, f -> edgeBias = f, -1f, 1f, 0.05f),
            new SliderOption("shoalWidth", () -> shoalWidth, f -> shoalWidth = f, 0f, 10f, 1f),
            new SliderOption("shallowWidth", () -> shallowWidth, f -> shallowWidth = f, 0f, 15f, 1f),
            new SliderOption("warpScale", () -> warpScale, f -> warpScale = f, 0f, 0.1f, 0.005f),
            new SliderOption("warpMag", () -> warpMag, f -> warpMag = f, 0f, 10f, 0.5f)
        };
    }

    private float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private float centeredNoise(int seed, float x, float y, float scale, int octaves, float persistence) {
        return (float) Simplex.noise2d(seed, octaves, persistence, 1f / Math.max(1f, scale), x + 10f, y + 10f) * 2f - 1f;
    }

    @Override
    public void apply(GenerateInput in) {
        float comp = clamp(complexity, 0f, 1f);
        float wScale = Math.max(4f, scale);
        float wScaleWarp = wScale * (1.25f - comp * 0.35f);
        float wStrength = wScale * (0.07f + comp * 0.24f);
        float detailScale = Math.max(3f, wScale * (0.22f + comp * 0.18f));
        int octaves = 3 + Math.round(comp * 3f);
        float persistence = 0.46f + comp * 0.12f;
        float halfMin = Math.max(1f, Math.min(in.width, in.height) * 0.5f);

        float warpX = centeredNoise(seed + 0x2f31, in.x, in.y, wScaleWarp, 2, 0.55f);
        float warpY = centeredNoise(seed - 0x5a17, in.x, in.y, wScaleWarp, 2, 0.55f);
        float sampleX = in.x + warpX * wStrength;
        float sampleY = in.y + warpY * wStrength;

        float base = centeredNoise(seed, sampleX, sampleY, wScale, octaves, persistence);
        float detail = centeredNoise(seed + 0x714b, sampleX, sampleY, detailScale, 2, 0.5f);
        float minEdge = Math.min(Math.min(in.x, in.width - 1 - in.x), Math.min(in.y, in.height - 1 - in.y));
        float edgeInfluence = 1f - clamp(minEdge / halfMin, 0f, 1f);

        float noiseVal = base + detail * (0.12f + comp * 0.34f) - edgeBias * edgeInfluence * 1.15f;

        // Simplified thresholding and distance approximation for per-pixel evaluation
        float threshold = -1f + (waterLevel * 2f);
        boolean isWater = noiseVal < threshold;

        if (isWater) {
            in.block = Blocks.air;
            
            // Approximate distance calculation using multiple noise samples to simulate erosion
            float distEstimate = (threshold - noiseVal) * 10f;
            
            int shoalLimit = Math.max(1, Math.round(Math.max(0f, shoalWidth)));
            int shallowLimit = shoalLimit + Math.max(0, Math.round(Math.max(0f, shallowWidth)));
            
            float layerDist = distEstimate;
            
            if (warpMag > 0.001f && warpScale > 0.0001f) {
                float distWarp = centeredNoise(seed + 777, in.x, in.y, 1f / Math.max(1f, warpScale), 1, 1f);
                layerDist += distWarp * warpMag;
            }

            if (layerDist <= shoalLimit) {
                in.floor = shoalBlock;
            } else if (layerDist <= shallowLimit) {
                in.floor = shallowBlock;
            } else {
                in.floor = deepBlock;
            }
        }
    }
}
