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
    @SuppressWarnings("unchecked")
    public static void init(){
        registerFilters();
        Events.run(Trigger.update,GeneratorUI::injectColumnsIfNeeded);
    }
    @SuppressWarnings("unchecked")
    private static void registerFilters(){
        try{
            Field f=Maps.class.getDeclaredField("allFilterTypes");
            f.setAccessible(true);
            Seq<Prov<GenerateFilter>> list=(Seq<Prov<GenerateFilter>>)f.get(null);
            if(list!=null){
                list.add(AdvancedTerrainFilter::new);
                list.add(MeteorImpactFilter::new);
                list.add(RichOreClusterFilter::new);
            }
        }catch(Throwable ignored){}
    }
    private static void injectColumnsIfNeeded(){
        if(Vars.ui==null)return;
        Scene scene=Core.scene;
        if(scene==null)return;
        for(Element el:scene.root.getChildren()){
            if(el instanceof BaseDialog){
                BaseDialog bd=(BaseDialog)el;
                if(bd.isShown()&&!"ar-gen-patched".equals(bd.name)){
                    if(containsFilterButtons(bd.cont)){
                        bd.name="ar-gen-patched";
                        appendSpecialColumn(bd);
                    }
                }
            }
        }
    }
    private static boolean containsFilterButtons(Group g){
        for(Element e:g.getChildren()){
            if(e instanceof Label){
                String txt=((Label)e).getText().toString();
                if(txt.contains(Core.bundle.get("filter.advancedterrain.name","Advanced Terrain")))return true;
            }
            if(e instanceof Group&&containsFilterButtons((Group)e))return true;
        }
        return false;
    }
    private static void appendSpecialColumn(BaseDialog dialog){
        try{
            Object genDialog=Reflect.get(MapEditorDialog.class,Vars.ui.editor,"generateDialog");
            if(genDialog==null)return;
            Method addMethod=null;
            for(Method m:genDialog.getClass().getDeclaredMethods()){
                if(m.getParameterTypes().length==1&&m.getParameterTypes()[0]==GenerateFilter.class){
                    addMethod=m;
                    addMethod.setAccessible(true);
                    break;
                }
            }
            final Method finalAdd=addMethod;
            Table col=new Table(Tex.pane);
            col.margin(8f);
            col.top();
            col.defaults().growX().height(46f).pad(3f);
            col.add("[accent]Special Generators[]").padBottom(6f).row();
            Prov<GenerateFilter>[] specials=new Prov[]{
                MeteorImpactFilter::new,
                RichOreClusterFilter::new
            };
            for(Prov<GenerateFilter> prov:specials){
                GenerateFilter sample=prov.get();
                col.button(b->{
                    b.left();
                    b.add(String.valueOf(sample.icon())).padRight(6f);
                    b.add(sample.name()).growX().left();
                },Styles.flatt,()->{
                    try{
                        if(finalAdd!=null){
                            finalAdd.invoke(genDialog,prov.get());
                            dialog.hide();
                        }
                    }catch(Throwable ignored){}
                }).width(210f).row();
            }
            dialog.cont.add(col).top().padLeft(8f).fillY();
        }catch(Throwable ignored){}
    }
}
