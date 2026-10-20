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
import mindustry.ui.*;
import advancedredactor.editor.*;
public class LeftSideUi{
    private static final ObjectSet<Table> hookedTables=new ObjectSet<>();
    public static void init(){
        Events.run(Trigger.update,()->{
            if(Vars.ui==null||Vars.ui.editor==null)return;
            if(!Vars.ui.editor.isShown()){
                hookedTables.clear();
                return;
            }
            injectBrushSizeField(Vars.ui.editor);
        });
    }
    private static void injectBrushSizeField(Element root){
        if(root instanceof Slider){
            Slider sl=(Slider)root;
            if(Math.abs(sl.getMaxValue()-(MapEditor.brushSizes.length-1))<0.1f){
                if(sl.parent instanceof Stack&&sl.parent.parent instanceof Table){
                    Table parentTable=(Table)sl.parent.parent;
                    if(!hookedTables.contains(parentTable)){
                        hookedTables.add(parentTable);
                        rebuildLeftBrushPanel(parentTable,(Stack)sl.parent,sl);
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
    private static void rebuildLeftBrushPanel(Table parent,Stack stack,Slider slider){
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
                    inputBox.add(Core.bundle.get("editor.brushsize.input","Size:")).style(Styles.outlineLabel).padRight(4f);
                    TextField field=new TextField(Strings.fixed(Vars.editor.brushSize,1));
                    field.setFilter((f,c)->Character.isDigit(c)||c=='.');
                    field.changed(()->{
                        float val=Strings.parseFloat(field.getText(),-1f);
                        if(val>=0.5f&&val<=500f){
                            Vars.editor.brushSize=val;
                            EditorTools.brushSize=Math.max(1,Math.round(val));
                        }
                    });
                    slider.moved(v->{
                        int idx=Math.max(0,Math.min(MapEditor.brushSizes.length-1,(int)v));
                        Vars.editor.brushSize=MapEditor.brushSizes[idx];
                        EditorTools.brushSize=Math.max(1,Math.round(Vars.editor.brushSize));
                        field.setText(Strings.fixed(Vars.editor.brushSize,1));
                    });
                    inputBox.add(field).width(80f).height(32f);
                    parent.add(inputBox).growX().padTop(4f).padBottom(8f).row();
                }else{
                    parent.add(el).growX().row();
                }
            }
        }catch(Exception ignored){}
    }
}
