package advancedredactor.ui;
import advancedredactor.LeftSideUtils.*;
import arc.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.game.EventType.Trigger;
import mindustry.gen.*;
import mindustry.ui.*;
public class LeftSideUi{
    private static final ObjectSet<Table> hookedBrushTables=new ObjectSet<>();
    private static final ObjectSet<Table> hookedMidTables=new ObjectSet<>();
    public static void init(){
        StencilManager.init();
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
            Slider slider=(Slider)root;
            if(Math.abs(slider.getMaxValue()-(MapEditor.brushSizes.length-1))<0.1f&&slider.parent instanceof Stack&&slider.parent.parent instanceof Table){
                Table brushTable=(Table)slider.parent.parent;
                if(!hookedBrushTables.contains(brushTable)){
                    hookedBrushTables.add(brushTable);
                    rebuildBrushBox(brushTable,(Stack)slider.parent,slider);
                }
                if(brushTable.parent instanceof Table){
                    Table midTable=(Table)brushTable.parent;
                    if(!hookedMidTables.contains(midTable)){
                        hookedMidTables.add(midTable);
                        appendBottomUtils(midTable);
                        wrapLeftPanelScroll(Vars.ui.editor,midTable);
                    }
                }
            }
        }
        if(root instanceof Group){
            Seq<Element> children=((Group)root).getChildren();
            for(int i=0;i<children.size;i++)injectLeftControls(children.get(i));
        }
    }
    private static void rebuildBrushBox(Table parent,Stack stack,Slider slider){
        try{
            Seq<Element> saved=new Seq<>();
            for(Element element:parent.getChildren())saved.add(element);
            parent.clearChildren();
            for(int i=0;i<saved.size;i++){
                Element element=saved.get(i);
                if(element==stack){
                    parent.add(element).growX().row();
                    Table inputBox=new Table();
                    inputBox.left();
                    inputBox.add(Core.bundle.get("editor.brushsize.input","Brush:")).style(Styles.outlineLabel).padRight(4f);
                    TextField field=new TextField(Strings.fixed(Vars.editor.brushSize,1));
                    field.setFilter((f,c)->Character.isDigit(c)||c=='.');
                    field.changed(()->{
                        float value=Strings.parseFloat(field.getText(),-1f);
                        if(value>=1f&&value<=500f)Vars.editor.brushSize=value;
                    });
                    slider.moved(value->{
                        int index=Math.max(0,Math.min(MapEditor.brushSizes.length-1,(int)value));
                        Vars.editor.brushSize=MapEditor.brushSizes[index];
                        field.setText(Strings.fixed(Vars.editor.brushSize,1));
                    });
                    inputBox.add(field).width(80f).height(32f);
                    parent.add(inputBox).growX().row();
                }else{
                    parent.add(element).growX().row();
                }
            }
        }catch(Exception ignored){}
    }
    private static void appendBottomUtils(Table mid){
        try{
            mid.row();
            Table utils=new Table();
            utils.defaults().growX().height(40f);
            utils.button(Core.bundle.get("editor.stencil.btn","Stencil"),Icon.pasteSmall,Styles.flatt,StencilManager::showDialog).row();
            utils.button(Core.bundle.get("editor.mapart.btn","Pixel Art"),Icon.imageSmall,Styles.flatt,()->new MapArtDialog().show()).row();
            mid.add(utils).growX().row();
        }catch(Exception ignored){}
    }
    private static void wrapLeftPanelScroll(MapEditorDialog dialog,Table target){
        try{
            Element current=target;
            Table leftColumn=null;
            while(current!=null&&current!=dialog){
                if(current.parent instanceof Table&&((Table)current.parent).getChildren().contains(dialog.getView(),true)){
                    if(current instanceof Table)leftColumn=(Table)current;
                    break;
                }
                current=current.parent;
            }
            if(leftColumn==null||"ar-scroll-wrapped".equals(leftColumn.name))return;
            leftColumn.name="ar-scroll-wrapped";
            Group parent=leftColumn.parent;
            if(!(parent instanceof Table))return;
            Table parentTable=(Table)parent;
            float panelWidth=Math.min(280f,Math.max(190f,Core.graphics.getWidth()*0.24f));
            Seq<Element> children=new Seq<>();
            for(Element element:parentTable.getChildren())children.add(element);
            parentTable.clearChildren();
            for(Element element:children){
                if(element==leftColumn){
                    element.setWidth(panelWidth);
                    ScrollPane scroll=new ScrollPane(leftColumn,Styles.noBarPane);
                    scroll.setScrollingDisabled(true,false);
                    scroll.setOverscroll(false,false);
                    scroll.setFadeScrollBars(false);
                    scroll.setForceScroll(false,true);
                    parentTable.add(scroll).growY().top().left().width(panelWidth).maxWidth(panelWidth);
                }else if(element==dialog.getView()){
                    parentTable.add(element).grow();
                }else{
                    parentTable.add(element).growY();
                }
            }
        }catch(Throwable ignored){}
    }
}
