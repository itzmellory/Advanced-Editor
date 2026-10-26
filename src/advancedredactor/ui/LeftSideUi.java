package advancedredactor.ui;
import advancedredactor.LeftSideUtils.*;
import arc.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
public class LeftSideUi{
    public static void init(){
        StencilManager.init();
        Events.run(Trigger.update,()->{
            if(Vars.ui!=null&&Vars.ui.editor!=null&&Vars.ui.editor.isShown()){
                FogOfWarManager.attachOverlay();
                injectButtons(Vars.ui.editor);
            }
        });
    }
    private static void injectButtons(MapEditorDialog dialog){
        Table target=findCheckboxContainer(dialog);
        if(target==null)return;
        if(target.find("ar-stencil-btn")!=null)return;
        target.row();
        Button stencilBtn=target.button(b->{
            b.left();
            b.image(Icon.pasteSmall).size(20f).padRight(6f);
            b.add(Core.bundle.get("ar.stencil.button","Stencil")).growX().left();
        },Styles.flatt,StencilManager::showDialog).growX().height(34f).padTop(2f).get();
        stencilBtn.name="ar-stencil-btn";
        target.row();
        Button artBtn=target.button(b->{
            b.left();
            b.image(Icon.imageSmall).size(20f).padRight(6f);
            b.add(Core.bundle.get("ar.art.button","Map Art")).growX().left();
        },Styles.flatt,()->new MapArtDialog().show()).growX().height(34f).padTop(2f).get();
        artBtn.name="ar-art-btn";
        target.row();
        Button fogBtn=target.button(b->{
            b.left();
            b.image(Icon.eyeSmall).size(20f).padRight(6f);
            b.add(Core.bundle.get("ar.fog.button","Fog of War")).growX().left();
        },Styles.flatt,FogOfWarManager::showDialog).growX().height(34f).padTop(2f).get();
        fogBtn.name="ar-fog-btn";
        wrapLeftPanelScroll(dialog,target);
    }
    private static void wrapLeftPanelScroll(MapEditorDialog dialog,Table target){
        try{
            Element curr=target;
            Table leftColumn=null;
            while(curr!=null&&curr!=dialog){
                if(curr.parent instanceof Table&&curr.parent.getChildren().contains(dialog.getView(),true)){
                    if(curr instanceof Table)leftColumn=(Table)curr;
                    break;
                }
                curr=curr.parent;
            }
            if(leftColumn!=null&&!"ar-scroll-wrapped".equals(leftColumn.name)){
                leftColumn.name="ar-scroll-wrapped";
                Group parent=leftColumn.parent;
                if(parent instanceof Table){
                    Table pt=(Table)parent;
                    SnapshotSeq<Element> seq=new SnapshotSeq<>(pt.getChildren());
                    pt.clearChildren();
                    for(Element el:seq){
                        if(el==leftColumn){
                            ScrollPane sp=new ScrollPane(leftColumn,Styles.noBarPane);
                            sp.setScrollingDisabled(true,false);
                            sp.setOverscroll(false,false);
                            pt.add(sp).growY().top().left();
                        }else if(el==dialog.getView()){
                            pt.add(el).grow();
                        }else{
                            pt.add(el).growY();
                        }
                    }
                }
            }
        }catch(Throwable ignored){}
    }
    private static Table findCheckboxContainer(Group root){
        for(Element e:root.getChildren()){
            if(e instanceof CheckBox){
                if(e.parent instanceof Table)return (Table)e.parent;
            }
            if(e instanceof Group){
                Table found=findCheckboxContainer((Group)e);
                if(found!=null)return found;
            }
        }
        return null;
    }
}
