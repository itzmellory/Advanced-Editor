package advancedredactor.ui;
import arc.*;
import arc.func.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import advancedredactor.generation.*;
import java.lang.reflect.*;

public class GeneratorUI{
    private static boolean filtersRegistered = false;

    public static void init(){
        Events.run(Trigger.update, () -> {
            registerFilters();
            injectColumnsIfNeeded();
        });
    }

    @SuppressWarnings("unchecked")
    private static void registerFilters(){
        if(filtersRegistered || Vars.maps == null) return;
        try{
            Field f = Maps.class.getDeclaredField("allFilterTypes");
            f.setAccessible(true);
            Seq<Prov<GenerateFilter>> list = (Seq<Prov<GenerateFilter>>)f.get(Vars.maps);
            if(list != null){
                boolean hasOurs = false;
                for(Prov<GenerateFilter> p : list){
                    if(p.get() instanceof AdvancedTerrainFilter) hasOurs = true;
                }
                if(!hasOurs){
                    list.add(AdvancedTerrainFilter::new);
                    list.add(MeteorImpactFilter::new);
                    list.add(RichOreClusterFilter::new);
                }
                filtersRegistered = true;
            }
        }catch(Throwable e){
            Log.err("Advanced-Editor: Failed to register custom filters", e);
        }
    }

    private static void injectColumnsIfNeeded(){
        if(Vars.ui == null || Core.scene == null) return;
        for(Element el : Core.scene.root.getChildren()){
            if(el instanceof BaseDialog){
                BaseDialog bd = (BaseDialog)el;
                if(bd.isShown() && !"ar-gen-patched".equals(bd.name)){
                    if(isFilterAddDialog(bd)){
                        bd.name = "ar-gen-patched";
                        appendSpecialColumn(bd);
                    }
                }
            }
        }
    }

    private static boolean isFilterAddDialog(BaseDialog bd){
        return searchForText(bd.cont, Core.bundle.get("filter.scatter.name", "Scatter")) || 
               searchForText(bd.cont, Core.bundle.get("filter.noise.name", "Noise"));
    }

    private static boolean searchForText(Group g, String target){
        for(Element e : g.getChildren()){
            if(e instanceof Label){
                if(((Label)e).getText().toString().contains(target)) return true;
            }
            if(e instanceof Group && searchForText((Group)e, target)) return true;
        }
        return false;
    }

    private static void appendSpecialColumn(BaseDialog dialog){
        try{
            Object genDialog = Reflect.get(MapEditorDialog.class, Vars.ui.editor, "generateDialog");
            if(genDialog == null) return;
            
            Method addMethod = null;
            for(Method m : genDialog.getClass().getDeclaredMethods()){
                if(m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == GenerateFilter.class){
                    addMethod = m;
                    addMethod.setAccessible(true);
                    break;
                }
            }
            final Method finalAdd = addMethod;

            Table col = new Table(Tex.pane);
            col.margin(8f);
            col.top();
            col.defaults().growX().height(46f).pad(3f);
            col.add("[accent]Special Generators[]").padBottom(6f).row();

            Seq<Prov<GenerateFilter>> specials = Seq.with(
                MeteorImpactFilter::new,
                RichOreClusterFilter::new
            );

            for(Prov<GenerateFilter> prov : specials){
                GenerateFilter sample = prov.get();
                col.button(b -> {
                    b.left();
                    b.add(String.valueOf(sample.icon())).padRight(6f);
                    b.add(sample.name()).growX().left();
                }, Styles.flatt, () -> {
                    try{
                        if(finalAdd != null){
                            finalAdd.invoke(genDialog, prov.get());
                            dialog.hide();
                        }
                    }catch(Throwable ignored){}
                }).width(210f).row();
            }

            dialog.cont.add(col).top().padLeft(8f).fillY();
        }catch(Throwable ignored){}
    }
}
