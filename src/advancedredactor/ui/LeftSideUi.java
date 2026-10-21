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
import mindustry.ui.*;
import advancedredactor.LeftSideUtils.*;
public class LeftSideUi{
    private static final ObjectSet<Table> hookedBrushTables=new ObjectSet<>();
    private static final ObjectSet<Table> hookedMidTables=new ObjectSet<>();
    public static void init(){
        Events.run(Trigger.update,()->{
            if(Vars.ui==null||Vars.ui.editor==null)return;
            if(!Vars.ui.editor.isShown()){
                hookedBrushTables.clear();
                hookedMidTables.clear();
                return;
            }
            injectLeftControls(Vars.ui.editor);
        });
    }
    private static void injectLeftControls(Element root){
        if(root instanceof Slider){
            Slider sl=(Slider)root;
            if(Math.abs(sl.getMaxValue()-(MapEditor.brushSizes.length-1))<0.1f){
                if(sl.parent instanceof Stack&&sl.parent.parent instanceof Table){
                    Table brushTable=(Table)sl.parent.parent;
                    if(!hookedBrushTables.contains(brushTable)){
                        hookedBrushTables.add(brushTable);
                        rebuildBrushBox(brushTable,(Stack)sl.parent,sl);
                    }
                    if(brushTable.parent instanceof Table){
                        Table midTable=(Table)brushTable.parent;
                        if(!hookedMidTables.contains(midTable)){
                            hookedMidTables.add(midTable);
                            appendBottomUtils(midTable);
                        }
                    }
                }
            }
        }
        if(root instanceof Group){
            for(Element e:((Group)root).getChildren()){
                injectLeftControls(e);
            }
        }
    }
    private static void rebuildBrushBox(Table parent,Stack stack,Slider slider){
        try{
            Seq<Element> saved=new Seq<>();
            for(Element e:parent.getChildren())saved.add(e);
            parent.clearChildren();
            for(int i=0;i<saved.size;i++){
                Element el=saved.get(i);
                if(el==stack){
                    parent.add(el).growX().padTop(4f).row();
                    Table inputBox=new Table();
                    inputBox.left();
                    inputBox.add(Core.bundle.get("editor.brushsize.input","Brush:")).style(Styles.outlineLabel).padRight(4f);
                    TextField field=new TextField(Strings.fixed(Vars.editor.brushSize,1));
                    field.setFilter((f,c)->Character.isDigit(c)||c=='.');
                    field.changed(()->{
                        float val=Strings.parseFloat(field.getText(),-1f);
                        if(val>=0.5f&&val<=500f)Vars.editor.brushSize=val;
                    });
                    slider.moved(v->{
                        int idx=Math.max(0,Math.min(MapEditor.brushSizes.length-1,(int)v));
                        Vars.editor.brushSize=MapEditor.brushSizes[idx];
                        field.setText(Strings.fixed(Vars.editor.brushSize,1));
                    });
                    inputBox.add(field).width(80f).height(32f);
                    parent.add(inputBox).growX().padTop(4f).padBottom(6f).row();
                }else{
                    parent.add(el).growX().row();
                }
            }
        }catch(Exception ignored){}
    }
    private static void appendBottomUtils(Table mid){
        try{
            mid.row();
            Table utils=new Table();
            utils.defaults().growX().height(40f).padTop(3f).padBottom(3f);
            utils.button(Core.bundle.get("editor.stencil.btn","Stencil"),Icon.imageSmall,Styles.flatt,StencilManager::showDialog).row();
            utils.button(Core.bundle.get("editor.mapart.btn","Pixel Art"),Icon.terrain,Styles.flatt,MapArtDialog::show).row();
            mid.add(utils).growX().padTop(4f).row();
        }catch(Exception ignored){}
    }
}
