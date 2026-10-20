package advancedredactor.ui;
import arc.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import advancedredactor.generation.*;
public class GeneratorUI{
    private static final ObjectSet<Dialog> hookedDialogs=new ObjectSet<>();
    private static final ObjectSet<Table> hookedEditorTables=new ObjectSet<>();
    public static void init(){
        Events.run(Trigger.update,()->{
            if(Core.scene==null)return;
            for(Element el:Core.scene.root.getChildren()){
                if(el instanceof BaseDialog){
                    BaseDialog dialog=(BaseDialog)el;
                    if(!hookedDialogs.contains(dialog)&&isAddFilterDialog(dialog)){
                        hookedDialogs.add(dialog);
                        injectCustomPanel(dialog);
                    }
                }
            }
            if(Vars.ui!=null&&Vars.ui.editor!=null&&Vars.ui.editor.isShown()){
                injectBrushSizeField(Vars.ui.editor);
            }
        });
    }
    private static void injectBrushSizeField(Element root){
        if(root instanceof Table){
            Table table=(Table)root;
            SnapshotSeq<Element> children=table.getChildren();
            for(int i=0;i<children.size;i++){
                Element child=children.get(i);
                if(child instanceof Slider){
                    Slider slider=(Slider)child;
                    if(Math.abs(slider.getMaxValue()-(MapEditor.brushSizes.length-1))<0.1f&&!hookedEditorTables.contains(table)){
                        hookedEditorTables.add(table);
                        rebuildLeftBrushPanel(table,slider);
                        return;
                    }
                }
            }
        }
        if(root instanceof Group){
            for(Element e:((Group)root).getChildren()){
                injectBrushSizeField(e);
            }
        }
    }
    private static void rebuildLeftBrushPanel(Table parent,Slider brushSlider){
        try{
            Seq<Element> saved=new Seq<>();
            for(Element e:parent.getChildren())saved.add(e);
            parent.clearChildren();
            for(int i=0;i<saved.size;i++){
                Element el=saved.get(i);
                if(el==brushSlider||(el instanceof Stack&&((Stack)el).getChildren().contains(brushSlider))){
                    parent.add(el).growX().padTop(4f).row();
                    Table inputBox=new Table();
                    inputBox.left().padTop(4f).padBottom(6f);
                    inputBox.add(Core.bundle.get("editor.brushsize.input","Size:")).style(Styles.outlineLabel).padRight(4f);
                    TextField field=new TextField(Strings.fixed(Vars.editor.brushSize,1));
                    field.setFilter((f,c)->Character.isDigit(c)||c=='.');
                    field.changed(()->{
                        float val=Strings.parseFloat(field.getText(),-1f);
                        if(val>=0.5f&&val<=500f){
                            Vars.editor.brushSize=val;
                        }
                    });
                    brushSlider.moved(v->{
                        int idx=Math.max(0,Math.min(MapEditor.brushSizes.length-1,(int)v));
                        Vars.editor.brushSize=MapEditor.brushSizes[idx];
                        field.setText(Strings.fixed(Vars.editor.brushSize,1));
                    });
                    inputBox.add(field).width(75f).height(28f);
                    parent.add(inputBox).growX().padTop(2f).padBottom(6f).row();
                }else{
                    parent.add(el).growX().padTop(2f).row();
                }
            }
        }catch(Exception e){Log.err(e);}
    }
    private static boolean isAddFilterDialog(BaseDialog dialog){
        String title=dialog.title.getText().toString();
        String addLocalized=Core.bundle.get("editor.addfilter","Add...");
        if(!title.equals("Add...")&&!title.equals(addLocalized))return false;
        if(dialog.cont.getChildren().size==1&&dialog.cont.getChildren().first() instanceof Table){
            Table inner=(Table)dialog.cont.getChildren().first();
            return inner.getChildren().size>=10;
        }return false;
    }
    private static void injectCustomPanel(BaseDialog addDialog){
        Element first=addDialog.cont.getChildren().first();
        if(!(first instanceof Table))return;
        Table vanillaTable=(Table)first;
        addDialog.cont.clearChildren();
        Table wrapper=new Table();
        wrapper.add(vanillaTable).padRight(15f).top();
        Table customPanel=new Table(Tex.button);
        customPanel.margin(14f);
        customPanel.defaults().size(230f,45f).pad(3f).left();
        customPanel.add("[accent]Advanced Redactor[]").height(28f).padBottom(6f).left().row();
        addFilterButton(customPanel,addDialog,new AdvancedTerrainFilter());
        addFilterButton(customPanel,addDialog,new AdvancedWaterFilter());
        addFilterButton(customPanel,addDialog,new AdvancedRuinFilter());
        addFilterButton(customPanel,addDialog,new AdvancedBlendFilter());
        addFilterButton(customPanel,addDialog,new AdvancedCircuitFilter());
        addFilterButton(customPanel,addDialog,new AdvancedRotationFilter());
        wrapper.add(customPanel).top();
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
            }catch(Exception e){Log.err(e);}
        }).row();
    }
    @SuppressWarnings("unchecked")
    private static void applyToGenerateDialog(GenerateFilter filter){
        for(Element el:Core.scene.root.getChildren()){
            if(el instanceof MapGenerateDialog){
                MapGenerateDialog genDialog=(MapGenerateDialog)el;
                try{
                    Seq<GenerateFilter> filters=Reflect.get(MapGenerateDialog.class,genDialog,"filters");
                    if(filters!=null){
                        filters.add(filter);
                        Reflect.invoke(MapGenerateDialog.class,genDialog,"rebuildFilters",new Object[]{},new Class[]{});
                        Reflect.invoke(MapGenerateDialog.class,genDialog,"update",new Object[]{},new Class[]{});
                    }
                }catch(Exception e){Log.err("Failed to inject filter into MapGenerateDialog",e);}
                break;
            }
        }
    }
}
