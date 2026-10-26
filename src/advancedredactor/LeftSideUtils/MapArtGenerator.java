/* Decompiler 273ms, total 748ms, lines 217 */
package advancedredactor.LeftSideUtils;

import advancedredactor.LeftSideUtils.MapArtDialog.ArtStyle;
import advancedredactor.LeftSideUtils.MapArtDialog.LayerMode;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import java.lang.reflect.Field;
import java.util.Iterator;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

public class MapArtGenerator {
   public static void generate(Fi file, int targetW, int targetH, ArtStyle style, LayerMode layerMode, boolean useAll, ObjectSet<Block> allowed) {
      try {
         Pixmap raw = new Pixmap(file);
         int w = Math.max(1, targetW);
         int h = Math.max(1, targetH);
         Pixmap img = StencilManager.scalePixmap(raw, w, h);
         raw.dispose();
         if (Vars.editor.width() != w || Vars.editor.height() != h) {
            Vars.editor.resize(w, h, 0, 0);
         }

         if (style == ArtStyle.colored) {
            applyColoredArt(img, w, h, layerMode);
         } else {
            applyTerrainArt(img, w, h, layerMode, useAll, allowed);
         }

         img.dispose();
         Vars.editor.clearOp();
      } catch (Exception var11) {
         Vars.ui.showException("@editor.errorload", var11);
      }

   }

   private static void setExtra(Tile t, int val) {
      try {
         Field f = t.getClass().getField("extraData");
         f.set(t, val);
      } catch (Throwable var5) {
         try {
            Field f2 = t.getClass().getDeclaredField("extraData");
            f2.setAccessible(true);
            f2.set(t, val);
         } catch (Throwable var4) {
         }
      }

   }

   private static void applyColoredArt(Pixmap img, int w, int h, LayerMode mode) {
      Block cFloorRaw = Vars.content.block("colored-floor");
      Block cWallRaw = Vars.content.block("colored-wall");
      Block cFloor = cFloorRaw == null ? Blocks.stone : cFloorRaw;
      Block cWall = cWallRaw == null ? Blocks.stoneWall : cWallRaw;
      Vars.editor.load(() -> {
         for(int ty = 0; ty < h; ++ty) {
            int sy = h - 1 - ty;

            for(int x = 0; x < w; ++x) {
               Tile t = Vars.editor.tile(x, ty);
               if (t != null) {
                  int rgba = img.getRaw(x, sy);
                  int r = rgba >>> 24 & 255;
                  int g = rgba >>> 16 & 255;
                  int b = rgba >>> 8 & 255;
                  int a = rgba & 255;
                  if (a < 255) {
                     float alpha = (float)a / 255.0F;
                     r = Math.round((float)r * alpha);
                     g = Math.round((float)g * alpha);
                     b = Math.round((float)b * alpha);
                  }

                  int extra = r << 24 | g << 16 | b << 8 | 1;
                  t.setOverlay(Blocks.air);
                  if (mode == LayerMode.floorOnly) {
                     if (cFloor instanceof Floor) {
                        t.setFloor((Floor)cFloor);
                     }

                     t.setBlock(Blocks.air);
                  } else if (mode == LayerMode.wallOnly) {
                     t.setBlock(a > 32 ? cWall : Blocks.air);
                  } else {
                     if (cFloor instanceof Floor) {
                        t.setFloor((Floor)cFloor);
                     }

                     float lum = 0.299F * (float)r + 0.587F * (float)g + 0.114F * (float)b;
                     t.setBlock(a > 32 && lum < 95.0F ? cWall : Blocks.air);
                  }

                  setExtra(t, extra);
               }
            }
         }

      });
   }

   private static void applyTerrainArt(Pixmap img, int w, int h, LayerMode mode, boolean useAll, ObjectSet<Block> allowed) {
      Seq<Floor> floors = new Seq();
      Seq<Block> walls = new Seq();
      Iterator var8 = Vars.content.blocks().iterator();

      while(true) {
         while(true) {
            Block b;
            do {
               do {
                  if (!var8.hasNext()) {
                     if (floors.isEmpty()) {
                        floors.add((Floor)Blocks.stone);
                     }

                     if (walls.isEmpty()) {
                        walls.add(Blocks.stoneWall);
                     }

                     Vars.editor.load(() -> {
                        for(int ty = 0; ty < h; ++ty) {
                           int sy = h - 1 - ty;

                           for(int x = 0; x < w; ++x) {
                              Tile t = Vars.editor.tile(x, ty);
                              if (t != null) {
                                 int rgba = img.getRaw(x, sy);
                                 int r = rgba >>> 24 & 255;
                                 int g = rgba >>> 16 & 255;
                                 int b = rgba >>> 8 & 255;
                                 int a = rgba & 255;
                                 t.setOverlay(Blocks.air);
                                 if (a < 25) {
                                    t.setBlock(Blocks.air);
                                 } else if (mode == LayerMode.floorOnly) {
                                    t.setFloor(bestFloor(floors, r, g, b));
                                    t.setBlock(Blocks.air);
                                 } else if (mode == LayerMode.wallOnly) {
                                    t.setBlock(bestBlock(walls, r, g, b));
                                 } else {
                                    Floor bf = bestFloor(floors, r, g, b);
                                    Block bw = bestBlock(walls, r, g, b);
                                    float df = colorDist(bf.mapColor, r, g, b);
                                    float dw = colorDist(bw.mapColor, r, g, b);
                                    t.setFloor(bf);
                                    t.setBlock(dw < df * 0.92F ? bw : Blocks.air);
                                 }
                              }
                           }
                        }

                     });
                     return;
                  }

                  b = (Block)var8.next();
               } while(!MapArtDialog.isEligibleTerrainBlock(b));
            } while(!useAll && !allowed.contains(b));

            if (b.isFloor() && b instanceof Floor) {
               floors.add((Floor)b);
            } else if (!b.isFloor() && b.isStatic()) {
               walls.add(b);
            }
         }
      }
   }

   private static Floor bestFloor(Seq<Floor> list, int r, int g, int b) {
      Floor best = (Floor)list.first();
      float min = Float.MAX_VALUE;

      for(int i = 0; i < list.size; ++i) {
         Floor f = (Floor)list.get(i);
         float d = colorDist(f.mapColor, r, g, b);
         if (d < min) {
            min = d;
            best = f;
         }
      }

      return best;
   }

   private static Block bestBlock(Seq<Block> list, int r, int g, int b) {
      Block best = (Block)list.first();
      float min = Float.MAX_VALUE;

      for(int i = 0; i < list.size; ++i) {
         Block bl = (Block)list.get(i);
         float d = colorDist(bl.mapColor, r, g, b);
         if (d < min) {
            min = d;
            best = bl;
         }
      }

      return best;
   }

   private static float colorDist(Color c, int r, int g, int b) {
      float cr = c.r * 255.0F - (float)r;
      float cg = c.g * 255.0F - (float)g;
      float cb = c.b * 255.0F - (float)b;
      return cr * cr * 0.3F + cg * cg * 0.59F + cb * cb * 0.11F;
   }
}
