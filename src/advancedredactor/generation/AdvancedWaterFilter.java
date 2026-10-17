package advancedredactor.generation;
import arc.*;
import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import java.util.Arrays;
import advancedredactor.ui.ModFilterOptions.*;
import static advancedredactor.ui.ModFilterOptions.*;
public class AdvancedWaterFilter extends GenerateFilter {
    public Block shoalBlock = Blocks.mud;
    public Block shallowBlock = Blocks.water;
    public Block deepBlock = Blocks.deepwater;
    public float coverage = 0.35f;
    public float scale = 90f;
    public float complexity = 0.55f;
    public float shoalWidth = 2f;
    public float shallowWidth = 5f;
    public float depthWarpScale = 0.02f;
    public float depthWarpMag = 3f;
    private transient byte[] cachedLayers = null;
    private transient int lastW, lastH, lastSeed = -1;
    private transient float lastCov, lastScale, lastComp, lastShoal, lastShal, lastWarpS, lastWarpM;
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
            new SliderOption("coverage", () -> coverage, f -> coverage = f, 0f, 1f, 0.02f),
            new SliderOption("scale", () -> scale, f -> scale = f, 10f, 200f, 1f),
            new SliderOption("complexity", () -> complexity, f -> complexity = f, 0f, 1f, 0.05f),
            new SliderOption("shoalWidth", () -> shoalWidth, f -> shoalWidth = f, 0f, 10f, 1f),
            new SliderOption("shallowWidth", () -> shallowWidth, f -> shallowWidth = f, 0f, 15f, 1f),
            new SliderOption("depthWarpScale", () -> depthWarpScale, f -> depthWarpScale = f, 0f, 0.1f, 0.005f),
            new SliderOption("depthWarpMag", () -> depthWarpMag, f -> depthWarpMag = f, 0f, 10f, 0.5f)
        };
    }
    private synchronized void buildCache(int width, int height) {
        if (cachedLayers != null && lastSeed == seed && lastW == width && lastH == height &&
            lastCov == coverage && lastScale == scale && lastComp == complexity &&
            lastShoal == shoalWidth && lastShal == shallowWidth &&
            lastWarpS == depthWarpScale && lastWarpM == depthWarpMag) return;
        lastSeed = seed; lastW = width; lastH = height;
        lastCov = coverage; lastScale = scale; lastComp = complexity;
        lastShoal = shoalWidth; lastShal = shallowWidth;
        lastWarpS = depthWarpScale; lastWarpM = depthWarpMag;
        int area = width * height;
        float[] field = new float[area];
        float wScale = Math.max(4f, scale);
        float warpScale = wScale * (1.25f - complexity * 0.35f);
        float warpStrength = wScale * (0.07f + complexity * 0.24f);
        float detailScale = Math.max(3f, wScale * (0.22f + complexity * 0.18f));
        int octaves = 3 + Math.round(complexity * 3f);
        float persistence = 0.46f + complexity * 0.12f;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float wx = (float) Simplex.noise2d(seed + 0x2f31, 2, 0.55f, 1f / Math.max(1f, warpScale), x + 10f, y + 10f) * 2f - 1f;
                float wy = (float) Simplex.noise2d(seed - 0x5a17, 2, 0.55f, 1f / Math.max(1f, warpScale), x + 10f, y + 10f) * 2f - 1f;
                float sx = x + wx * warpStrength;
                float sy = y + wy * warpStrength;
                float base = (float) Simplex.noise2d(seed, octaves, persistence, 1f / wScale, sx + 10f, sy + 10f) * 2f - 1f;
                float detail = (float) Simplex.noise2d(seed + 0x714b, 2, 0.5f, 1f / detailScale, sx + 10f, sy + 10f) * 2f - 1f;
                field[x + y * width] = base + detail * (0.12f + complexity * 0.34f);
            }
        }
        float[] sorted = field.clone();
        Arrays.sort(sorted);
        int targetIdx = Math.max(0, Math.min(area - 1, Math.round(area * coverage)));
        float threshold = targetIdx < area ? sorted[targetIdx] : Float.POSITIVE_INFINITY;
        boolean[] water = new boolean[area];
        for (int i = 0; i < area; i++) water[i] = field[i] <= threshold;
        boolean[] next = water.clone();
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (water[x + y * width]) continue;
                for (int d = 0; d < 4; d++) {
                    int nx = x + dx[d], ny = y + dy[d];
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && water[nx + ny * width]) {
                        next[x + y * width] = true; break;
                    }
                }
            }
        }
        water = next.clone();
        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (!water[x + y * width]) continue;
                    for (int d = 0; d < 4; d++) {
                        int nx = x + dx[d], ny = y + dy[d];
                        if (nx < 0 || nx >= width || ny < 0 || ny >= height || !water[nx + ny * width]) {
                            next[x + y * width] = false; break;
                        }
                    }
                }
            }
            water = next.clone();
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (water[x + y * width]) continue;
                for (int d = 0; d < 4; d++) {
                    int nx = x + dx[d], ny = y + dy[d];
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && water[nx + ny * width]) {
                        next[x + y * width] = true; break;
                    }
                }
            }
        }
        water = next.clone();
        for (int s = 0; s < 2; s++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int neighbors = 0;
                    for (int oy = -1; oy <= 1; oy++) {
                        for (int ox = -1; ox <= 1; ox++) {
                            if (ox == 0 && oy == 0) continue;
                            int nx = x + ox, ny = y + oy;
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height && water[nx + ny * width]) neighbors++;
                        }
                    }
                    int i = x + y * width;
                    if (water[i] && neighbors <= 2) next[i] = false;
                    if (!water[i] && neighbors >= 6) next[i] = true;
                }
            }
            water = next.clone();
        }
        int[] dist = new int[area];
        int inf = width + height + 2;
        for (int i = 0; i < area; i++) {
            int x = i % width, y = i / width;
            dist[i] = water[i] ? ((x == 0 || y == 0 || x == width - 1 || y == height - 1) ? 1 : inf) : 0;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = x + y * width;
                if (!water[i]) continue;
                if (x > 0) dist[i] = Math.min(dist[i], dist[i - 1] + 1);
                if (y > 0) dist[i] = Math.min(dist[i], dist[i - width] + 1);
            }
        }
        for (int y = height - 1; y >= 0; y--) {
            for (int x = width - 1; x >= 0; x--) {
                int i = x + y * width;
                if (!water[i]) continue;
                if (x + 1 < width) dist[i] = Math.min(dist[i], dist[i + 1] + 1);
                if (y + 1 < height) dist[i] = Math.min(dist[i], dist[i + width] + 1);
            }
        }
        cachedLayers = new byte[area];
        int shoalLimit = Math.max(1, Math.round(Math.max(0f, shoalWidth)));
        int shallowLimit = shoalLimit + Math.max(0, Math.round(Math.max(0f, shallowWidth)));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = x + y * width;
                if (!water[i]) continue;
                float warp = (float) Simplex.noise2d(seed + 777, 1, 1f, 1f / Math.max(1f, depthWarpScale), x + 10f, y + 10f) * 2f - 1f;
                float shifted = dist[i] + warp * depthWarpMag;
                if (shifted <= shoalLimit) cachedLayers[i] = 1;
                else if (shifted <= shallowLimit) cachedLayers[i] = 2;
                else cachedLayers[i] = 3;
            }
        }
    }
    @Override
    public void apply(GenerateInput in) {
        buildCache(in.width, in.height);
        byte layer = cachedLayers[in.x + in.y * in.width];
        if (layer > 0) {
            in.block = Blocks.air;
            if (layer == 1) in.floor = shoalBlock;
            else if (layer == 2) in.floor = shallowBlock;
            else in.floor = deepBlock;
        }
    }
}
