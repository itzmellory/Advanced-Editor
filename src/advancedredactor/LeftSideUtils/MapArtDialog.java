/* Decompiler 478ms, total 1544ms, lines 425 */
package advancedredactor.LeftSideUtils;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.TextField.TextFieldFilter;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.struct.ObjectSet.ObjectSetIterator;
import arc.util.Scaling;
import arc.util.Strings;
import java.lang.reflect.Method;
import java.util.Iterator;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.Platform;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;

public class MapArtDialog {
   public static int artWidth = 200;
   public static int artHeight = 200;
   public static Fi selectedFile;
   public static Texture previewTexture;
   public static TextureRegion previewRegion;
   public static MapArtDialog.ArtStyle style;
   public static MapArtDialog.LayerMode layerMode;
   public static boolean useAllBlocks;
   public static final ObjectSet<Block> allowedBlocks;
   private static boolean initializedDefaults;

   public static void show() {
      if (!initializedDefaults) {
         initDefaultAllowedBlocks();
         initializedDefaults = true;
      }

      BaseDialog d = new BaseDialog(Core.bundle.get("editor.mapart.title", "Pixel Art Generator"));
      Table main = new Table();
      Runnable[] rebuild = new Runnable[1];
      rebuild[0] = () -> {
         main.clearChildren();
         main.defaults().pad(4.0F).left();
         if (previewRegion != null) {
            Table prevBox = new Table(Tex.pane);
            Image img = new Image(new TextureRegionDrawable(previewRegion));
            img.setScaling(Scaling.fit);
            prevBox.add(img).size(160.0F, 160.0F).pad(4.0F);
            main.add(prevBox).colspan(2).center().padBottom(6.0F).row();
         }

         main.add(Core.bundle.get("editor.mapart.width", "Width (1-3000):"));
         main.field(String.valueOf(artWidth), TextFieldFilter.digitsOnly, (s) -> {
            artWidth = Math.max(1, Math.min(3000, Strings.parseInt(s, artWidth)));
         }).width(150.0F).row();
         main.add(Core.bundle.get("editor.mapart.height", "Height (1-3000):"));
         main.field(String.valueOf(artHeight), TextFieldFilter.digitsOnly, (s) -> {
            artHeight = Math.max(1, Math.min(3000, Strings.parseInt(s, artHeight)));
         }).width(150.0F).row();
         Label fileLabel = new Label(selectedFile == null ? Core.bundle.get("editor.mapart.nofile", "No PNG selected") : selectedFile.name());
         fileLabel.setColor(selectedFile == null ? Color.lightGray : Color.lime);
         main.button(Core.bundle.get("editor.mapart.selectpng", "Choose PNG..."), Icon.fileImage, Styles.flatt, () -> {
            pickPNG((file) -> {
               Core.app.post(() -> {
                  selectedFile = file;

                  try {
                     Pixmap p = new Pixmap(file);
                     artWidth = Math.min(800, p.width);
                     artHeight = Math.min(800, p.height);
                     if (previewTexture != null) {
                        previewTexture.dispose();
                     }

                     previewTexture = new Texture(p);
                     previewRegion = new TextureRegion(previewTexture);
                     p.dispose();
                  } catch (Exception var3) {
                  }

                  rebuild[0].run();
               });
            });
         }).colspan(2).growX().height(42.0F).row();
         main.add(fileLabel).colspan(2).left().padBottom(6.0F).row();
         main.add(Core.bundle.get("editor.mapart.type", "Generation Type:")).left();
         main.button(style == MapArtDialog.ArtStyle.colored ? Core.bundle.get("editor.mapart.type.colored", "Colored Blocks") : Core.bundle.get("editor.mapart.type.terrain", "Vanilla Terrain"), Styles.flatt, () -> {
            style = style == MapArtDialog.ArtStyle.colored ? MapArtDialog.ArtStyle.terrain : MapArtDialog.ArtStyle.colored;
            rebuild[0].run();
         }).width(190.0F).height(36.0F).row();
         main.add(Core.bundle.get("editor.mapart.layers", "Layer Mode:")).left();
         main.button(getLayerModeName(layerMode), Styles.flatt, () -> {
            MapArtDialog.LayerMode[] vals = MapArtDialog.LayerMode.values();
            layerMode = vals[(layerMode.ordinal() + 1) % vals.length];
            rebuild[0].run();
         }).width(190.0F).height(36.0F).row();
         if (style == MapArtDialog.ArtStyle.terrain) {
            main.add(Core.bundle.get("editor.mapart.palette", "Block Palette:")).left();
            main.button(useAllBlocks ? Core.bundle.get("editor.mapart.palette.all", "All Blocks") : Core.bundle.get("editor.mapart.palette.custom", "Selected Only"), Styles.flatt, () -> {
               useAllBlocks = !useAllBlocks;
               rebuild[0].run();
            }).width(190.0F).height(36.0F).row();
            if (!useAllBlocks) {
               main.button(Core.bundle.get("editor.mapart.selectblocks", "Select Allowed Blocks") + " (" + allowedBlocks.size + ")", Icon.edit, Styles.flatt, () -> {
                  showBlockFilterDialog(rebuild[0]);
               }).colspan(2).growX().height(40.0F).row();
            }
         }

         main.button(Core.bundle.get("editor.mapart.generate", "Generate Art on Map"), Icon.ok, Styles.flatt, () -> {
            if (selectedFile == null) {
               Vars.ui.showErrorMessage(Core.bundle.get("editor.mapart.needfile", "Please select a PNG file first!"));
            } else if (style == MapArtDialog.ArtStyle.terrain && !useAllBlocks && allowedBlocks.isEmpty()) {
               Vars.ui.showErrorMessage("Select at least 1 allowed block!");
            } else {
               d.hide();
               Vars.ui.loadAnd(() -> {
                  MapArtGenerator.generate(selectedFile, artWidth, artHeight, style, layerMode, useAllBlocks, allowedBlocks);
               });
            }
         }).colspan(2).growX().height(46.0F).padTop(10.0F).row();
      };
      rebuild[0].run();
      d.cont.pane(main).maxHeight((float)Core.graphics.getHeight() * 0.82F);
      d.addCloseButton();
      d.show();
   }

   private static void pickPNG(Cons<Fi> cons) {
      try {
         Class fc = null;

         try {
            fc = Class.forName("mindustry.ui.FileChooser");
         } catch (Throwable var8) {
         }

         if (fc == null) {
            fc = Class.forName("mindustry.ui.dialogs.FileChooser");
         }

         Method op = null;
         Method[] var3 = fc.getMethods();
         int var4 = var3.length;

         int var5;
         for(var5 = 0; var5 < var4; ++var5) {
            Method m = var3[var5];
            if (m.getName().equals("open")) {
               op = m;
               break;
            }
         }

         if (op != null) {
            Object fci = op.getParameterTypes()[0] == String.class ? op.invoke((Object)null, "png") : op.invoke((Object)null, new String[]{"png"});
            Method[] var15 = fci.getClass().getMethods();
            var5 = var15.length;

            for(int var17 = 0; var17 < var5; ++var17) {
               Method m = var15[var17];
               if (m.getName().equals("submit")) {
                  m.invoke(fci, cons);
                  return;
               }
            }
         }
      } catch (Throwable var10) {
      }

      try {
         Method[] var11 = Platform.class.getMethods();
         int var12 = var11.length;

         for(int var14 = 0; var14 < var12; ++var14) {
            Method m = var11[var14];
            if (m.getName().equals("showFileChooser")) {
               if (m.getParameterTypes().length == 3) {
                  m.invoke(Vars.platform, true, "png", cons);
                  return;
               }

               if (m.getParameterTypes().length == 4) {
                  m.invoke(Vars.platform, true, "Select PNG", "png", cons);
                  return;
               }
            }
         }
      } catch (Throwable var9) {
      }

   }

   private static String getLayerModeName(MapArtDialog.LayerMode m) {
      switch(m) {
      case floorOnly:
         return Core.bundle.get("editor.mapart.layer.floor", "Floor Only");
      case wallOnly:
         return Core.bundle.get("editor.mapart.layer.wall", "Walls Only");
      default:
         return Core.bundle.get("editor.mapart.layer.both", "Floor + Walls");
      }
   }

   public static void initDefaultAllowedBlocks() {
      allowedBlocks.clear();
      Iterator var0 = Vars.content.blocks().iterator();

      while(var0.hasNext()) {
         Block b = (Block)var0.next();
         if (isEligibleTerrainBlock(b)) {
            allowedBlocks.add(b);
         }
      }

   }

   public static boolean isEligibleTerrainBlock(Block b) {
      if (b != null && b != Blocks.air && !b.synthetic() && !b.isOverlay()) {
         if (b.name.contains("colored")) {
            return false;
         } else if (b instanceof Floor && ((Floor)b).isLiquid) {
            return true;
         } else {
            return (b.isFloor() || b.isStatic()) && b.mapColor != null && b.mapColor.a > 0.5F && b.inEditor;
         }
      } else {
         return false;
      }
   }

   private static void showBlockFilterDialog(Runnable onCloseUpdate) {
      BaseDialog bd = new BaseDialog(Core.bundle.get("editor.mapart.selectblocks", "Select Allowed Blocks"));
      Table grid = new Table();
      String[] search = new String[]{""};
      int[] tab = new int[]{0};
      Label countLabel = new Label("");
      Runnable updateCount = () -> {
         countLabel.setText("Selected: [accent]" + allowedBlocks.size + "[] blocks");
      };
      Runnable rebuildGrid = () -> {
         grid.clearChildren();
         updateCount.run();
         int cols = 0;
         String q = search[0].toLowerCase().trim();
         Iterator var6 = Vars.content.blocks().iterator();

         while(true) {
            Block b;
            do {
               do {
                  do {
                     do {
                        if (!var6.hasNext()) {
                           return;
                        }

                        b = (Block)var6.next();
                     } while(!isEligibleTerrainBlock(b));
                  } while(tab[0] == 1 && !b.isFloor());
               } while(tab[0] == 2 && b.isFloor());
            } while(!q.isEmpty() && !b.localizedName.toLowerCase().contains(q) && !b.name.toLowerCase().contains(q));

            Button btn = new Button(Styles.flatTogglet);
            boolean sel = allowedBlocks.contains(b);
            btn.setChecked(sel);
            Image iconImg = new Image(b.uiIcon);
            iconImg.setScaling(Scaling.fit);
            iconImg.setColor(sel ? Color.white : new Color(0.2F, 0.2F, 0.2F, 0.7F));
            Label checkMark = new Label(() -> {
               return allowedBlocks.contains(b) ? "[lime]✔[]" : "";
            });
            checkMark.setAlignment(18);
            Stack st = new Stack(new Element[]{iconImg, checkMark});
            btn.add(st).size(34.0F);
            btn.clicked(() -> {
               if (allowedBlocks.contains(b)) {
                  allowedBlocks.remove(b);
               } else {
                  allowedBlocks.add(b);
               }

               boolean ns = allowedBlocks.contains(b);
               btn.setChecked(ns);
               iconImg.setColor(ns ? Color.white : new Color(0.2F, 0.2F, 0.2F, 0.7F));
               updateCount.run();
            });
            grid.add(btn).size(48.0F).pad(3.0F).tooltip(b.localizedName);
            ++cols;
            if (cols % 8 == 0) {
               grid.row();
            }
         }
      };
      bd.cont.add(countLabel).padBottom(4.0F).row();
      bd.cont.table((tabs) -> {
         tabs.defaults().size(110.0F, 36.0F).pad(2.0F);
         tabs.button("All", Styles.flatTogglet, () -> {
            tab[0] = 0;
            rebuildGrid.run();
         }).update((b) -> {
            b.setChecked(tab[0] == 0);
         });
         tabs.button("Floors", Styles.flatTogglet, () -> {
            tab[0] = 1;
            rebuildGrid.run();
         }).update((b) -> {
            b.setChecked(tab[0] == 1);
         });
         tabs.button("Walls", Styles.flatTogglet, () -> {
            tab[0] = 2;
            rebuildGrid.run();
         }).update((b) -> {
            b.setChecked(tab[0] == 2);
         });
      }).row();
      bd.cont.table((top) -> {
         top.defaults().size(120.0F, 36.0F).pad(2.0F);
         top.button("Select All", Styles.flatt, () -> {
            Iterator var2 = Vars.content.blocks().iterator();

            while(true) {
               Block b;
               do {
                  do {
                     do {
                        if (!var2.hasNext()) {
                           rebuildGrid.run();
                           return;
                        }

                        b = (Block)var2.next();
                     } while(!isEligibleTerrainBlock(b));
                  } while(tab[0] == 1 && !b.isFloor());
               } while(tab[0] == 2 && b.isFloor());

               allowedBlocks.add(b);
            }
         });
         top.button("Clear All", Styles.flatt, () -> {
            if (tab[0] == 0) {
               allowedBlocks.clear();
            } else {
               Seq<Block> rem = new Seq();
               ObjectSetIterator var3 = allowedBlocks.iterator();

               Block b;
               while(var3.hasNext()) {
                  b = (Block)var3.next();
                  if (tab[0] == 1 && b.isFloor()) {
                     rem.add(b);
                  }

                  if (tab[0] == 2 && !b.isFloor()) {
                     rem.add(b);
                  }
               }

               Iterator var5 = rem.iterator();

               while(var5.hasNext()) {
                  b = (Block)var5.next();
                  allowedBlocks.remove(b);
               }
            }

            rebuildGrid.run();
         });
      }).row();
      bd.cont.field("", (s) -> {
         search[0] = s;
         rebuildGrid.run();
      }).width(340.0F).padBottom(6.0F).row();
      rebuildGrid.run();
      bd.cont.pane(grid).size(Math.min((float)Core.graphics.getWidth() * 0.9F, 460.0F), Math.min((float)Core.graphics.getHeight() * 0.65F, 340.0F));
      bd.hidden(onCloseUpdate);
      bd.addCloseButton();
      bd.show();
   }

   static {
      style = MapArtDialog.ArtStyle.colored;
      layerMode = MapArtDialog.LayerMode.floorOnly;
      useAllBlocks = true;
      allowedBlocks = new ObjectSet();
      initializedDefaults = false;
   }

   public static enum LayerMode {
      floorOnly,
      wallOnly,
      floorAndWall;

      // $FF: synthetic method
      private static MapArtDialog.LayerMode[] $values() {
         return new MapArtDialog.LayerMode[]{floorOnly, wallOnly, floorAndWall};
      }
   }

   public static enum ArtStyle {
      colored,
      terrain;

      // $FF: synthetic method
      private static MapArtDialog.ArtStyle[] $values() {
         return new MapArtDialog.ArtStyle[]{colored, terrain};
      }
   }
                    }
