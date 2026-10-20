package advancedredactor.ui;
import arc.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import advancedredactor.generation.*;
public class GeneratorUI{
    private static boolean injected=false;
    public static void init(){
        Events.run(Trigger.update,()->{
            if(injected||Vars.ui==null||Vars.ui.editor==null||Vars.ui.editor.generate==null)return;
            try{
                BaseDialog d=(BaseDialog)Reflect.get(Vars.ui.editor.generate,"addDialog");
                if(d!=null&&!d.cont.getChildren().isEmpty()){
                    Element first=d.cont.getChildren().first();
                    if(first instanceof Table){
                        injectCustomPanel(d,(Table)first);
                        injected=true;
                    }
                }
            }catch(Exception ignored){}
        });
    }
    private static void injectCustomPanel(BaseDialog addDialog,Table vanillaTable){
        addDialog.cont.clearChildren();
        Table wrapper=new Table();
        ScrollPane vp=new ScrollPane(vanillaTable);
        vp.setScrollingDisabled(true,false);
        wrapper.add(vp).padRight(15f).top().maxHeight(Core.graphics.getHeight()*0.8f);
        Table customPanel=new Table(Tex.button);
        customPanel.margin(14f);
        customPanel.defaults().size(230f,45f).pad(3f).left();
        customPanel.add("[accent]Advanced Redactor[]").height(28f).padBottom(6f).left().row();
        addFilterButton(customPanel,addDialog,new AdvancedTerrainFilter());
        addFilterButton(customPanel,addDialog,new AdvancedWaterFilter());
        addFilterButton(customPanel,addDialog,new AdvancedRuinFilter());
        addFilterButton(customPanel,addDialog,new AdvancedRotationFilter());
        ScrollPane pane=new ScrollPane(customPanel);
        pane.setScrollingDisabled(true,false);
        wrapper.add(pane).top().maxHeight(Core.graphics.getHeight()*0.8f);
        addDialog.cont.add(wrapper);
    }
    @SuppressWarnings("unchecked")
    private static void addFilterButton(Table parent,BaseDialog addDialog,GenerateFilter filterTemplate){
        parent.button(b->{
            b.left().marginLeft(10f);
            b.add(String.valueOf(filterTemplate.icon())).padRight(8f);
            b.add(filterTemplate.name()).growX().left();
        },Styles.flatt,()->{
            try{
                GenerateFilter instance=filterTemplate.getClass().getDeclaredConstructor().newInstance();
                instance.randomize();
                applyToGenerateDialog(instance);
                addDialog.hide();
            }catch(Exception ignored){}
        }).row();
    }
    @SuppressWarnings("unchecked")
    private static void applyToGenerateDialog(GenerateFilter filter){
        try{
            Seq<GenerateFilter> filters=Reflect.get(Vars.ui.editor.generate,"filters");
            if(filters!=null){
                filters.add(filter);
                Reflect.invoke(MapGenerateDialog.class,Vars.ui.editor.generate,"rebuildFilters",new Object[]{},new Class[]{});
                Reflect.invoke(MapGenerateDialog.class,Vars.ui.editor.generate,"update",new Object[]{},new Class[]{});
            }
        }catch(Exception ignored){}
    }
}
