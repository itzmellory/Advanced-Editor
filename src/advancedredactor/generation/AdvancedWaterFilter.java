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
    public Block water = Blocks.water;
    public Block deepwater = Blocks.deepwater;
    public float scale = 100f;
    public float octaves = 4f;
    public float waterLevel = 0.35f;
    public float riverWidth = 0.15f;
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
            new BlockOption("water", () -> water, b -> water = b, floorsOnly),
            new BlockOption("deepwater", () -> deepwater, b -> deepwater = b, floorsOnly),
            new SliderOption("scale", () -> scale, f -> scale = f, 10f, 200f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 8f, 1f),
            new SliderOption("waterLevel", () -> waterLevel, f -> waterLevel = f, 0f, 1f, 0.05f),
            new SliderOption("riverWidth", () -> riverWidth, f -> riverWidth = f, 0f, 0.5f, 0.01f)
        };
    }
    @Override
    public void apply(GenerateInput in) {
        float lakeNoise = (float) Simplex.noise2d(seed + 1, (int) octaves, 0.5f, 1f / scale, in.x, in.y);
        float riverNoise = Math.abs((float) Simplex.noise2d(seed + 2, 2, 0.5f, 1f / (scale * 1.5f), in.x, in.y));
        boolean isRiver = riverNoise < riverWidth;
        boolean isDeepRiver = riverNoise < riverWidth * 0.4f;
        if (lakeNoise < waterLevel || isRiver) {
            in.block = Blocks.air;
            in.floor = (lakeNoise < waterLevel * 0.6f || isDeepRiver) ? deepwater : water;
        }
    }
}
