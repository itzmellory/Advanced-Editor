package advancedredactor.generation;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import advancedredactor.ui.ModFilterOptions.*;
import static advancedredactor.ui.ModFilterOptions.*;

public class GeometricStyleFilter extends GenerateFilter {
    public Block floor = Blocks.metalFloor;
    public Block rimFloor = Blocks.darkPanel3;
    public Block accentFloor = Blocks.metalFloorDamaged;
    public Block wall = Blocks.darkMetal;

    public float arenaSize = 0.78f;
    public float gridSize = 26f;
    public float chamferRatio = 0.65f;
    public float borderIndent = 0.55f;

    public boolean centralSpine = true;
    public float spineScale = 1.0f;
    public float ribFrequency = 0.65f;

    public float pillarDensity = 0.35f;
    public float rimWidth = 3f;
    public float floorPattern = 0.3f;
    public boolean clearExterior = false;

    @Override
    public String name() {
        return Core.bundle.get("filter.geometricstyle.name", "45/90 Styles Generation");
    }

    @Override
    public char icon() {
        return Iconc.blockMetalFloor;
    }

    @Override
    public FilterOption[] options() {
        return new FilterOption[]{
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
            new BlockOption("rimFloor", () -> rimFloor, b -> rimFloor = b, floorsOnly),
            new BlockOption("accentFloor", () -> accentFloor, b -> accentFloor = b, floorsOnly),
            new BlockOption("wall", () -> wall, b -> wall = b, wallsOnly),

            new SliderOption("arenaSize", () -> arenaSize, f -> arenaSize = f, 0.4f, 0.95f, 0.02f),
            new SliderOption("gridSize", () -> gridSize, f -> gridSize = f, 12f, 64f, 2f),
            new SliderOption("chamferRatio", () -> chamferRatio, f -> chamferRatio = f, 0.2f, 1f, 0.05f),
            new SliderOption("borderIndent", () -> borderIndent, f -> borderIndent = f, 0f, 1f, 0.05f),

            new ToggleOption("centralSpine", () -> centralSpine, b -> centralSpine = b),
            new SliderOption("spineScale", () -> spineScale, f -> spineScale = f, 0.4f, 1.8f, 0.05f),
            new SliderOption("ribFrequency", () -> ribFrequency, f -> ribFrequency = f, 0f, 1f, 0.05f),

            new SliderOption("pillarDensity", () -> pillarDensity, f -> pillarDensity = f, 0f, 0.85f, 0.05f),
            new SliderOption("rimWidth", () -> rimWidth, f -> rimWidth = f, 0f, 8f, 1f),
            new SliderOption("floorPattern", () -> floorPattern, f -> floorPattern = f, 0f, 1f, 0.05f),
            new ToggleOption("clearExterior", () -> clearExterior, b -> clearExterior = b)
        };
    }

    private static int hash(int s, int x, int y) {
        int h = s + x * 374761393 + y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return (h ^ (h >> 16)) & 0x7fffffff;
    }

    private static float hashFloat(int s, int x, int y) {
        return (hash(s, x, y) % 10000) / 10000f;
    }

    private static float octBoxDist(float px, float py, float cx, float cy, float rx, float ry, float cut) {
        float dx = Math.abs(px - cx) - rx;
        float dy = Math.abs(py - cy) - ry;
        float d90 = Math.max(dx, dy);
        float d45 = (dx + dy + cut) * 0.5f;
        return Math.max(d90, d45);
    }

    private static float diagSpineSegment(float px, float py, float x1, float y1, float x2, float y2, float thickness, float cut) {
        float minX = Math.min(x1, x2) - thickness;
        float maxX = Math.max(x1, x2) + thickness;
        float minY = Math.min(y1, y2) - thickness;
        float maxY = Math.max(y1, y2) + thickness;

        float boxDx = Math.max(minX - px, px - maxX);
        float boxDy = Math.max(minY - py, py - maxY);
        float box90 = Math.max(boxDx, boxDy);
        float box45 = (boxDx + boxDy + cut) * 0.5f;
        float bound = Math.max(box90, box45);

        float diagDist = Math.abs((px - x1) + (py - y1)) * 0.5f - thickness;
        return Math.max(bound, diagDist);
    }

    @Override
    public void apply(GenerateInput in) {
        float w = in.width;
        float h = in.height;
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float x = in.x;
        float y = in.y;

        float step = Math.max(12f, gridSize);
        float halfW = (w * 0.5f) * arenaSize;
        float halfH = (h * 0.5f) * arenaSize;
        float mainChamfer = Math.min(halfW, halfH) * 0.45f * chamferRatio;

        float arenaDist = -octBoxDist(x, y, cx, cy, halfW, halfH, mainChamfer);

        if (borderIndent > 0.01f) {
            float bStep = step * 1.5f;
            int gx = Mathf.floor((x + bStep * 0.5f) / bStep);
            int gy = Mathf.floor((y + bStep * 0.5f) / bStep);

            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    int nx = gx + ox;
                    int ny = gy + oy;
                    float cellX = nx * bStep;
                    float cellY = ny * bStep;

                    float edgeDistCheck = Math.abs(octBoxDist(cellX, cellY, cx, cy, halfW, halfH, mainChamfer));
                    if (edgeDistCheck < bStep * 1.15f) {
                        float rnd = hashFloat(seed + 17, nx, ny);
                        if (rnd < borderIndent * 0.65f) {
                            int shapeType = hash(seed + 31, nx, ny) % 3;
                            float bRx = (shapeType == 1) ? bStep * 0.75f : bStep * 0.5f;
                            float bRy = (shapeType == 2) ? bStep * 0.75f : bStep * 0.5f;
                            float bCut = bStep * 0.45f * chamferRatio;
                            float blockD = octBoxDist(x, y, cellX, cellY, bRx, bRy, bCut);
                            arenaDist = Math.min(arenaDist, blockD);
                        }
                    }
                }
            }
        }

        float internalDist = 9999f;

        if (centralSpine) {
            float s = Math.min(w, h) * 0.16f * spineScale;
            float thick = Math.max(4f, Math.round(s * 0.24f));
            float cut = thick * 0.6f * chamferRatio;

            float dDiag = diagSpineSegment(x, y, cx - s * 0.7f, cy + s * 0.9f, cx + s * 0.5f, cy - s * 0.3f, thick, cut);
            internalDist = Math.min(internalDist, dDiag);

            float dRightArm = octBoxDist(x, y, cx + s * 0.85f, cy + s * 0.15f, thick * 0.75f, s * 0.65f, cut);
            internalDist = Math.min(internalDist, dRightArm);

            float dBottomHook = octBoxDist(x, y, cx + s * 0.45f, cy - s * 0.45f, s * 0.55f, thick * 0.8f, cut);
            internalDist = Math.min(internalDist, dBottomHook);

            if (ribFrequency > 0.05f) {
                int ribCount = 4;
                for (int i = 0; i < ribCount; i++) {
                    if (hashFloat(seed + 53, i, 0) <= ribFrequency) {
                        float ribY = cy + s * (0.85f - i * 0.42f);
                        float ribLen = s * (0.45f + (i % 2) * 0.2f);
                        float ribX = ( cx - (ribY - cy) * 0.75f ) - ribLen * 0.65f;
                        float ribH = Math.max(3f, Math.round(thick * 0.48f));
                        float dRib = octBoxDist(x, y, ribX, ribY, ribLen * 0.5f, ribH, ribH * 0.5f * chamferRatio);
                        internalDist = Math.min(internalDist, dRib);
                    }
                }
            }
        }

        if (pillarDensity > 0.01f) {
            float pStep = step * 1.35f;
            int px = Mathf.floor((x + pStep * 0.5f) / pStep);
            int py = Mathf.floor((y + pStep * 0.5f) / pStep);

            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    int nx = px + ox;
                    int ny = py + oy;
                    float cellX = nx * pStep;
                    float cellY = ny * pStep;

                    float checkArena = -octBoxDist(cellX, cellY, cx, cy, halfW - pStep * 0.6f, halfH - pStep * 0.6f, mainChamfer);
                    if (checkArena > pStep * 0.35f) {
                        float rnd = hashFloat(seed + 89, nx, ny);
                        if (rnd < pillarDensity * 0.42f) {
                            int pType = hash(seed + 101, nx, ny) % 4;
                            float dPillar;
                            if (pType == 0) {
                                float r = Math.round(pStep * 0.26f);
                                dPillar = octBoxDist(x, y, cellX, cellY, r, r, r * 1.1f * chamferRatio);
                            } else if (pType == 1) {
                                float r = Math.round(pStep * 0.22f);
                                dPillar = diagSpineSegment(x, y, cellX - r, cellY - r, cellX + r, cellY + r, r * 0.75f, r * 0.5f);
                            } else if (pType == 2) {
                                float rx = Math.round(pStep * 0.32f);
                                float ry = Math.round(pStep * 0.18f);
                                dPillar = octBoxDist(x, y, cellX, cellY, rx, ry, ry * 0.8f * chamferRatio);
                            } else {
                                float r = Math.round(pStep * 0.16f);
                                dPillar = octBoxDist(x, y, cellX, cellY, r, r, 0f);
                            }
                            internalDist = Math.min(internalDist, dPillar);
                        }
                    }
                }
            }
        }

        float finalSignedDist = Math.min(arenaDist, internalDist);

        if (finalSignedDist <= 0f) {
            if (clearExterior && arenaDist <= -rimWidth * 2f) {
                in.block = Blocks.air;
                in.floor = floor;
            } else {
                in.block = wall;
                in.floor = rimFloor;
            }
        } else {
            in.block = Blocks.air;
            if (rimWidth > 0.1f && finalSignedDist <= rimWidth) {
                in.floor = rimFloor;
            } else {
                if (floorPattern > 0.01f) {
                    int patStep = Math.max(6, Math.round(step * 0.4f));
                    int tx = ((int) x) / patStep;
                    int ty = ((int) y) / patStep;
                    int lx = ((int) x) % patStep;
                    int ly = ((int) y) % patStep;

                    boolean isTechLine = (lx == 0 || ly == 0 || (lx + ly == patStep)) && hashFloat(seed + 149, tx, ty) < floorPattern;
                    in.floor = isTechLine ? accentFloor : floor;
                } else {
                    in.floor = floor;
                }
            }
        }
    }
}
