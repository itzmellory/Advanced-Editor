package advancedredactor.ui;
import advancedredactor.generation.AdvancedRotationFilter;
import advancedredactor.generation.AdvancedRuinFilter;
import advancedredactor.generation.AdvancedTerrainFilter;
import advancedredactor.generation.AdvancedWaterFilter;
import arc.Core;
import arc.Events;
import arc.scene.Element;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Reflect;
import java.util.Iterator;
import mindustry.Vars;
import mindustry.editor.MapGenerateDialog;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Tex;
import mindustry.maps.filters.GenerateFilter;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class GeneratorUI {
   private static final ObjectSet<BaseDialog> hookedDialogs = new ObjectSet();

   public static void init() {
      Events.run(Trigger.update, () -> {
         if (Core.scene != null) {
            Iterator var0 = Core.scene.root.getChildren().iterator();

            while(var0.hasNext()) {
               Element el = (Element)var0.next();
               if (el instanceof BaseDialog) {
                  BaseDialog dialog = (BaseDialog)el;
                  if (!hookedDialogs.contains(dialog) && isAddFilterDialog(dialog)) {
                     hookedDialogs.add(dialog);
                     injectCustomPanel(dialog);
                  }
               }
            }

         }
      });
   }

   private static boolean isAddFilterDialog(BaseDialog dialog) {
      String title = dialog.title.getText().toString();
      String addLocalized = Core.bundle.get("add", "Add...");
      if (!title.equals("Add...") && !title.equals(addLocalized)) {
         return false;
      } else {
         if (dialog.cont.getChildren().size == 1) {
            Element first = (Element)dialog.cont.getChildren().first();
            if (first instanceof ScrollPane) {
               return true;
            }

            if (first instanceof Table) {
               return ((Table)first).getChildren().size >= 5;
            }
         }

         return false;
      }
   }

   private static void injectCustomPanel(BaseDialog addDialog) {
      Element first = (Element)addDialog.cont.getChildren().first();
      addDialog.cont.clearChildren();
      Table wrapper = new Table();
      if (first instanceof ScrollPane) {
         ScrollPane vp = (ScrollPane)first;
         vp.setScrollingDisabled(true, false);
         wrapper.add(vp).padRight(15.0F).top().maxHeight((float)Core.graphics.getHeight() * 0.8F);
      } else {
         wrapper.add(first).padRight(15.0F).top();
      }

      Table customPanel = new Table(Tex.button);
      customPanel.margin(14.0F);
      customPanel.defaults().size(230.0F, 45.0F).pad(3.0F).left();
      customPanel.add("[accent]Advanced Redactor[]").height(28.0F).padBottom(6.0F).left().row();
      addFilterButton(customPanel, addDialog, new AdvancedTerrainFilter());
      addFilterButton(customPanel, addDialog, new AdvancedWaterFilter());
      addFilterButton(customPanel, addDialog, new AdvancedRuinFilter());
      addFilterButton(customPanel, addDialog, new AdvancedRotationFilter());
      ScrollPane pane = new ScrollPane(customPanel);
      pane.setScrollingDisabled(true, false);
      wrapper.add(pane).top().maxHeight((float)Core.graphics.getHeight() * 0.8F);
      addDialog.cont.add(wrapper);
   }

   private static void addFilterButton(Table parent, BaseDialog addDialog, GenerateFilter filterTemplate) {
      parent.button((b) -> {
         b.left().marginLeft(10.0F);
         b.add(String.valueOf(filterTemplate.icon())).padRight(8.0F);
         b.add(filterTemplate.name()).growX().left();
      }, Styles.flatt, () -> {
         try {
            GenerateFilter instance = (GenerateFilter)filterTemplate.getClass().getDeclaredConstructor().newInstance();
            instance.randomize();
            applyToGenerateDialog(instance);
            addDialog.hide();
         } catch (Exception var3) {
         }

      }).row();
   }

   private static void applyToGenerateDialog(GenerateFilter filter) {
      try {
         MapGenerateDialog gen = Vars.ui.editor.getGenerateDialog();
         Seq<GenerateFilter> filters = (Seq)Reflect.get(MapGenerateDialog.class, gen, "filters");
         if (filters != null) {
            filters.add(filter);
            Reflect.invoke(MapGenerateDialog.class, gen, "rebuildFilters", new Object[0], new Class[0]);
            Reflect.invoke(MapGenerateDialog.class, gen, "update", new Object[0], new Class[0]);
         }
      } catch (Exception var3) {
      }

   }
}
