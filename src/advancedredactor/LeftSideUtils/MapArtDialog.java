package advancedredactor.LeftSideUtils;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
public class MapArtDialog{
    public enum ArtStyle{colored,terrain}
    public enum LayerMode{floorOnly,wallOnly,floorAndWall}
    public static int artWidth=200,artHeight=200;
    public static Fi selectedFile;
    public static ArtStyle style=ArtStyle.colored;
    public static LayerMode layerMode=LayerMode.floorOnly;
    public static boolean useAllBlocks=true;
    public static final ObjectSet<Block> allowedBlocks=new ObjectSet<>();
    public static void show(){
        if(allowedBlocks.isEmpty())initDefaultAllowedBlocks();
        BaseDialog d=new BaseDialog(Core.bundle.get("editor.mapart.title","Pixel Art Generator"));
        Table main=new Table();
        Runnable[] rebuild=new Runnable[1];
        rebuild[0]=()->{
            main.clearChildren();
            main.defaults().pad(4f).left();
            main.add(Core.bundle.get("editor.mapart.width","Width (1-3000):"));
            main.field(String.valueOf(artWidth),TextField.TextFieldFilter.digitsOnly,s->artWidth=Math.max(1,Math.min(3000,Strings.parseInt(s,artWidth)))).width(150f).row();
            main.add(Core.bundle.get("editor.mapart.height","Height (1-3000):"));
            main.field(String.valueOf(artHeight),TextField.TextFieldFilter.digitsOnly,s->artHeight=Math.max(1,Math.min(3000,Strings.parseInt(s,artHeight)))).width(150f).row();
            Label fileLabel=new Label(selectedFile==null?Core.bundle.get("editor.mapart.nofile","No PNG selected"):selectedFile.name());
            fileLabel.setColor(selectedFile==null?Color.lightGray: Color.lime);
            main.button(Core.bundle.get("editor.mapart.selectpng","Choose PNG..."),Icon.fileImage,Styles.flatt,()->{
                Vars.platform.showFileChooser(true,"png",file->{
                    Core.app.post(()->{
                        selectedFile=file;
                        try{
                            Pixmap p=new Pixmap(file);
                            artWidth=Math.min(800,p.width);
                            artHeight=Math.min(800,p.height);
                            p.dispose();
                        }catch(Exception ignored){}
                        rebuild[0].run();
                    });
                });
            }).colspan(2).growX().height(42f).row();
            main.add(fileLabel).colspan(2).left().padBottom(6f).row();
            main.add(Core.bundle.get("editor.mapart.type","Generation Type:")).left();
            main.button(style==ArtStyle.colored?Core.bundle.get("editor.mapart.type.colored","Colored Blocks"):Core.bundle.get("editor.mapart.type.terrain","Vanilla Terrain"),Styles.flatt,()->{
                style=(style==ArtStyle.colored)?ArtStyle.terrain:ArtStyle.colored;
                rebuild[0].run();
            }).width(190f).height(36f).row();
            main.add(Core.bundle.get("editor.mapart.layers","Layer Mode:")).left();
            main.button(getLayerModeName(layerMode),Styles.flatt,()->{
                LayerMode[] vals=LayerMode.values();
                layerMode=vals[(layerMode.ordinal()+1)%vals.length];
                rebuild[0].run();
            }).width(190f).height(36f).row();
            if(style==ArtStyle.terrain){
                main.add(Core.bundle.get("editor.mapart.palette","Block Palette:")).left();
                main.button(useAllBlocks?Core.bundle.get("editor.mapart.palette.all","All Blocks"):Core.bundle.get("editor.mapart.palette.custom","Selected Only"),Styles.flatt,()->{
                    useAllBlocks=!useAllBlocks;
                    rebuild[0].run();
                }).width(190f).height(36f).row();
                if(!useAllBlocks){
                    main.button(Core.bundle.get("editor.mapart.selectblocks","Select Allowed Blocks..."),Icon.edit,Styles.flatt,MapArtDialog::showBlockFilterDialog).colspan(2).growX().height(40f).row();
                }
            }
            main.button(Core.bundle.get("editor.mapart.generate","Generate Art on Map"),Icon.ok,Styles.flatt,()->{
                if(selectedFile==null){
                    Vars.ui.showErrorMessage(Core.bundle.get("editor.mapart.needfile","Please select a PNG file first!"));
                    return;
                }
                d.hide();
                Vars.ui.loadAnd(()->MapArtGenerator.generate(selectedFile,artWidth,artHeight,style,layerMode,useAllBlocks,allowedBlocks));
            }).colspan(2).growX().height(46f).padTop(10f).row();
        };
        rebuild[0].run();
        d.cont.pane(main).maxHeight(Core.graphics.getHeight()*0.82f);
        d.addCloseButton();
        d.show();
    }
    private static String getLayerModeName(LayerMode m){
        switch(m){
            case floorOnly:return Core.bundle.get("editor.mapart.layer.floor","Floor Only");
            case wallOnly:return Core.bundle.get("editor.mapart.layer.wall","Walls Only");
            default:return Core.bundle.get("editor.mapart.layer.both","Floor + Walls");
        }
    }
    public static void initDefaultAllowedBlocks(){
        for(Block b:Vars.content.blocks()){
            if(isEligibleTerrainBlock(b))allowedBlocks.add(b);
        }
    }
    public static boolean isEligibleTerrainBlock(Block b){
        if(b==null||b==Blocks.air||b.synthetic()||b.isOverlay())return false;
        if(b.name.contains("colored"))return false;
        if(b instanceof Floor&&((Floor)b).isLiquid)return true;
        return(b.isFloor()||b.isStatic())&&b.mapColor!=null&&b.mapColor.a>0.5f&&b.inEditor;
    }
    private static void showBlockFilterDialog(){
        BaseDialog bd=new BaseDialog(Core.bundle.get("editor.mapart.selectblocks","Select Allowed Blocks"));
        Table grid=new Table();
        final String[] search={""};
        Runnable rebuildGrid=()->{
            grid.clearChildren();
            int cols=0;
            String q=search[0].toLowerCase().trim();
            for(Block b:Vars.content.blocks()){
                if(!isEligibleTerrainBlock(b))continue;
                if(!q.isEmpty()&&!b.localizedName.toLowerCase().contains(q)&&!b.name.toLowerCase().contains(q))continue;
                Button btn=grid.button(bt->bt.image(b.uiIcon).size(28f),Styles.flati,()->{
                    if(allowedBlocks.contains(b))allowedBlocks.remove(b);
                    else allowedBlocks.add(b);
                }).size(40f).pad(2f).tooltip(b.localizedName).get();
                btn.update(()->btn.setColor(allowedBlocks.contains(b)?Color.white:Color.darkGray));
                if(++cols%10==0)grid.row();
            }
        };
        bd.cont.table(top->{
            top.button("All",Styles.flatt,()->{for(Block b:Vars.content.blocks())if(isEligibleTerrainBlock(b))allowedBlocks.add(b);}).size(100f,34f).pad(2f);
            top.button("Clear",Styles.flatt,allowedBlocks::clear).size(100f,34f).pad(2f);
        }).row();
        bd.cont.field("",s->{search[0]=s;rebuildGrid.run();}).width(320f).padBottom(6f).row();
        rebuildGrid.run();
        bd.cont.pane(grid).size(Math.min(Core.graphics.getWidth()*0.9f,480f),Math.min(Core.graphics.getHeight()*0.7f,360f));
        bd.addCloseButton();
        bd.show();
    }
          }
