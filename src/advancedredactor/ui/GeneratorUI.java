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
    private static final Seq<Prov<GenerateFilter>> col1Filters=new Seq<>();
    private static final Seq<Prov<GenerateFilter>> col2Filters=new Seq<>();
    public static void init(){
        setupFilterLists();
        registerIntoVanillaArray();
        Events.run(Trigger.update,GeneratorUI::injectDialogColumns);
    }
    @SuppressWarnings("unchecked")
    private static void setupFilterLists(){
        col1Filters.clear();
        col2Filters.clear();
        String[] knownCol1={
            "AdvancedTerrainFilter",
            "CaveGeneratorFilter",
            "CliffGeneratorFilter",
            "ShorelineFilter",
            "BiomeGeneratorFilter",
            "JaggedTerrainFilter",
            "IslandFilter",
            "RiverGeneratorFilter"
        };
        for(String clsName:knownCol1){
            try{
                Class<?> cls=Class.forName("advancedredactor.generation."+clsName);
                if(GenerateFilter.class.isAssignableFrom(cls)){
                    col1Filters.add(()->{
                        try{
                            return(GenerateFilter)cls.getDeclaredConstructor().newInstance();
                        }catch(Exception e){
                            throw new RuntimeException(e);
                        }
                    });
                }
            }catch(Throwable ignored){}
        }
        col2Filters.add(MeteorImpactFilter::new);
        col2Filters.add(RichOreClusterFilter::new);
    }
    @SuppressWarnings("unchecked")
    private static void registerIntoVanillaArray(){
        try{
            Field f=Maps.class.getDeclaredField("allFilterTypes");
            f.setAccessible(true);
            Object val=f.get(null);
            if(val instanceof Prov[]){
                Prov<GenerateFilter>[] orig=(Prov<GenerateFilter>[])val;
                Seq<Prov<GenerateFilter>> combined=Seq.with(orig);
                for(Prov<GenerateFilter> p:col1Filters)combined.add(p);
                for(Prov<GenerateFilter> p:col2Filters)combined.add(p);
                try{
                    Field modField=Field.class.getDeclaredField("modifiers");
                    modField.setAccessible(true);
                    modField.setInt(f,f.getModifiers()&~Modifier.FINAL);
                }catch(Throwable ignored){}
                try{
                    Field accField=Field.class.getDeclaredField("accessFlags");
                    accField.setAccessible(true);
                    accField.setInt(f,f.getModifiers()&~Modifier.FINAL);
                }catch(Throwable ignored){}
                f.set(null,combined.toArray(Prov.class));
            }
        }catch(Throwable ignored){}
    }
    private static void injectDialogColumns(){
        if(Vars.ui==null||Core.scene==null)return;
        for(Element el:Core.scene.root.getChildren()){
            if(el instanceof BaseDialog){
                BaseDialog bd=(BaseDialog)el;
                if(bd.isShown()&&!"ar-gen-patched".equals(bd.name)){
                    if(isFilterSelectionDialog(bd)){
                        bd.name="ar-gen-patched";
                        buildColumns(bd);
                    }
                }
            }
        }
    }
    private static boolean isFilterSelectionDialog(BaseDialog bd){
        return hasText(bd.cont,Core.bundle.get("filter.scatter.name","Scatter"))&&
               hasText(bd.cont,Core.bundle.get("filter.noise.name","Noise"));
    }
    private static boolean hasText(Group g,String text){
        for(Element e:g.getChildren()){
            if(e instanceof Label&&((Label)e).getText().toString().contains(text))return true;
            if(e instanceof Group&&hasText((Group)e,text))return true;
        }
        return false;
    }
    private static void buildColumns(BaseDialog addDialog){
        try{
            if(col1Filters.size>0){
                Table col1=createColumnTable("[accent]Advanced Terrain[]",col1Filters,addDialog);
                addDialog.cont.add(col1).top().padLeft(8f).fillY();
            }
            if(col2Filters.size>0){
                Table col2=createColumnTable("[accent]Special Generators[]",col2Filters,addDialog);
                addDialog.cont.add(col2).top().padLeft(8f).fillY();
            }
            addDialog.cont.invalidateHierarchy();
            addDialog.pack();
        }catch(Throwable ignored){}
    }
    private static Table createColumnTable(String title,Seq<Prov<GenerateFilter>> filters,BaseDialog addDialog){
        Table col=new Table(Tex.button);
        col.margin(8f);
        col.top();
        col.defaults().growX().height(45f).pad(2f);
        col.add(title).padBottom(6f).row();
        for(Prov<GenerateFilter> prov:filters){
            GenerateFilter sample=prov.get();
            col.button(b->{
                b.left();
                b.marginLeft(6f);
                b.add(String.valueOf(sample.icon())).padRight(6f);
                b.add(sample.name()).growX().left();
            },Styles.flatt,()->{
                addFilterToEditor(prov.get());
                addDialog.hide();
            }).width(205f).row();
        }
        return col;
    }
    @SuppressWarnings("unchecked")
    private static void addFilterToEditor(GenerateFilter filter){
        try{
            Field gdField=MapEditorDialog.class.getDeclaredField("generateDialog");
            gdField.setAccessible(true);
            Object genDialog=gdField.get(Vars.ui.editor);
            if(genDialog==null)return;
            filter.randomize();
            Field filtersField=genDialog.getClass().getDeclaredField("filters");
            filtersField.setAccessible(true);
            Seq<GenerateFilter> activeFilters=(Seq<GenerateFilter>)filtersField.get(genDialog);
            if(activeFilters!=null){
                activeFilters.add(filter);
            }
            for(Method m:genDialog.getClass().getDeclaredMethods()){
                if(m.getName().equals("rebuildFilters")&&m.getParameterCount()==0){
                    m.setAccessible(true);
                    m.invoke(genDialog);
                }
            }
            for(Method m:genDialog.getClass().getDeclaredMethods()){
                if(m.getName().equals("update")&&m.getParameterCount()==0){
                    m.setAccessible(true);
                    m.invoke(genDialog);
                }
            }
        }catch(Throwable e){
            Vars.ui.showException(e);
        }
    }
}
