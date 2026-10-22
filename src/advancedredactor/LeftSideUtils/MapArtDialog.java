package advancedredactor.LeftSideUtils;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import java.lang.reflect.*;
public class MapArtDialog{
    public enum ArtStyle{colored,terrain}
    public enum LayerMode{floorOnly,wallOnly,floorAndWall}
    public static int artWidth=200,artHeight=200;
    public static Fi selectedFile;
    public static Texture previewTexture;
    public static TextureRegion previewRegion;
    public static ArtStyle style=ArtStyle.colored;
    public static LayerMode layerMode=LayerMode.floorOnly;
    public static boolean useAllBlocks=true;
    public static final ObjectSet<Block> allowedBlocks=new ObjectSet<>();
    private static boolean initializedDefaults=false;
    public static void show(){
        if(!initializedDefaults){initDefaultAllowedBlocks();initializedDefaults=true;}
        BaseDialog d=new BaseDialog(Core.bundle.get("editor.mapart.title","Pixel Art Generator"));
        Table main=new Table();
        Runnable[] rebuild=new Runnable[1];
        rebuild[0]=()->{
            main.clearChildren();
            main.defaults().pad(4f).left();
            if(previewRegion!=null){
                Table prevBox=new Table(Tex.pane);
                Image img=new Image(new TextureRegionDrawable(previewRegion));
                img.setScaling(Scaling.fit);
                prevBox.add(img).size(160f,160f).pad(4f);
                main.add(prevBox).colspan(2).center().padBottom(6f).row();
            }
            main.add(Core.bundle.get("editor.mapart.width","Width (1-3000):"));
            main.field(String.valueOf(artWidth),TextField.TextFieldFilter.digitsOnly,s->artWidth=Math.max(1,Math.min(3000,Strings.parseInt(s,artWidth)))).width(150f).row();
            main.add(Core.bundle.get("editor.mapart.height","Height (1-3000):"));
            main.field(String.valueOf(artHeight),TextField.TextFieldFilter.digitsOnly,s->artHeight=Math.max(1,Math.min(3000,Strings.parseInt(s,artHeight)))).width(150f).row();
            Label fileLabel=new Label(selectedFile==null?Core.bundle.get("editor.mapart.nofile","No PNG selected"):selectedFile.name());
            fileLabel.setColor(selectedFile==null?Color.lightGray:Color.lime);
            main.button(Core.bundle.get("editor.mapart.selectpng","Choose PNG..."),Icon.fileImage,Styles.flatt,()->{
                pickPNG(file->Core.app.post(()->{
                    selectedFile=file;
                    try{
                        Pixmap p=new Pixmap(file);
                        artWidth=Math.min(800,p.width);
                        artHeight=Math.min(800,p.height);
                        if(previewTexture!=null)previewTexture.dispose();
                        previewTexture=new Texture(p);
                        previewRegion=new TextureRegion(previewTexture);
                        p.dispose();
                    }catch(Exception ignored){}
                    rebuild[0].run();
                }));
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
                    main.button(Core.bundle.get("editor.mapart.selectblocks","Select Allowed Blocks")+" ("+allowedBlocks.size+")",Icon.edit,Styles.flatt,()->showBlockFilterDialog(rebuild[0])).colspan(2).growX().height(40f).row();
                }
            }
            main.button(Core.bundle.get("editor.mapart.generate","Generate Art on Map"),Icon.ok,Styles.flatt,()->{
                if(selectedFile==null){Vars.ui.showErrorMessage(Core.bundle.get("editor.mapart.needfile","Please select a PNG file first!"));return;}
                if(style==ArtStyle.terrain&&!useAllBlocks&&allowedBlocks.isEmpty()){Vars.ui.showErrorMessage("Select at least 1 allowed block!");return;}
                d.hide();
                Vars.ui.loadAnd(()->MapArtGenerator.generate(selectedFile,artWidth,artHeight,style,layerMode,useAllBlocks,allowedBlocks));
            }).colspan(2).growX().height(46f).padTop(10f).row();
        };
        rebuild[0].run();
        d.cont.pane(main).maxHeight(Core.graphics.getHeight()*0.82f);
        d.addCloseButton();
        d.show();
    }
    private static void pickPNG(arc.func.Cons<Fi> cons){
        try{
            Class<?> fc=null;
            try{fc=Class.forName("mindustry.ui.FileChooser");}catch(Throwable ignored){}
            if(fc==null)fc=Class.forName("mindustry.ui.dialogs.FileChooser");
            Method op=null;
            for(Method m:fc.getMethods())if(m.getName().equals("open")){op=m;break;}
            if(op!=null){
                Object fci=op.getParameterTypes()[0]==String.class?op.invoke(null,"png"):op.invoke(null,(Object)new String[]{"png"});
                for(Method m:fci.getClass().getMethods())if(m.getName().equals("submit")){m.invoke(fci,cons);return;}
            }
        }catch(Throwable ignored){}
        try{
            for(Method m:mindustry.core.Platform.class.getMethods()){
                if(m.getName().equals("showFileChooser")){
                    if(m.getParameterTypes().length==3){m.invoke(Vars.platform,true,"png",cons);return;}
                    if(m.getParameterTypes().length==4){m.invoke(Vars.platform,true,"Select PNG","png",cons);return;}
                }
            }
        }catch(Throwable ignored){}
    }
    private static String getLayerModeName(LayerMode m){
        switch(m){
            case floorOnly:return Core.bundle.get("editor.mapart.layer.floor","Floor Only");
            case wallOnly:return Core.bundle.get("editor.mapart.layer.wall","Walls Only");
            default:return Core.bundle.get("editor.mapart.layer.both","Floor + Walls");
        }
    }
    public static void initDefaultAllowedBlocks(){
        allowedBlocks.clear();
        for(Block b:Vars.content.blocks())if(isEligibleTerrainBlock(b))allowedBlocks.add(b);
    }
    public static boolean isEligibleTerrainBlock(Block b){
        if(b==null||b==Blocks.air||b.synthetic()||b.isOverlay())return false;
        if(b.name.contains("colored"))return false;
        if(b instanceof Floor&&((Floor)b).isLiquid)return true;
        return(b.isFloor()||b.isStatic())&&b.mapColor!=null&&b.mapColor.a>0.5f&&b.inEditor;
    }
    private static void showBlockFilterDialog(Runnable onCloseUpdate){
        BaseDialog bd=new BaseDialog(Core.bundle.get("editor.mapart.selectblocks","Select Allowed Blocks"));
        Table grid=new Table();
        final String[] search={""};
        final int[] tab={0};
        Label countLabel=new Label("");
        Runnable updateCount=()->countLabel.setText("Selected: [accent]"+allowedBlocks.size+"[] blocks");
        Runnable rebuildGrid=()->{
            grid.clearChildren();
            updateCount.run();
            int cols=0;
            String q=search[0].toLowerCase().trim();
            for(Block b:Vars.content.blocks()){
                if(!isEligibleTerrainBlock(b))continue;
                if(tab[0]==1&&!b.isFloor())continue;
                if(tab[0]==2&&b.isFloor())continue;
                if(!q.isEmpty()&&!b.localizedName.toLowerCase().contains(q)&&!b.name.toLowerCase().contains(q))continue;
                Button btn=new Button(Styles.flatTogglet);
                boolean sel=allowedBlocks.contains(b);
                btn.setChecked(sel);
                Image iconImg=new Image(b.uiIcon);
                iconImg.setScaling(Scaling.fit);
                iconImg.setColor(sel?Color.white:Color.darkGray);
                Label checkMark=new Label(()->allowedBlocks.contains(b)?"[lime]✔[]":"");
                checkMark.setAlignment(Align.topRight);
                Stack st=new Stack(iconImg,checkMark);
                btn.add(st).size(34f);
                btn.clicked(()->{
                    if(allowedBlocks.contains(b))allowedBlocks.remove(b);
                    else allowedBlocks.add(b);
                    boolean ns=allowedBlocks.contains(b);
                    btn.setChecked(ns);
                    iconImg.setColor(ns?Color.white:Color.darkGray);
                    updateCount.run();
                });
                grid.add(btn).size(48f).pad(3f).tooltip(b.localizedName);
                if(++cols%8==0)grid.row();
            }
        };
        bd.cont.add(countLabel).padBottom(4f).row();
        bd.cont.table(tabs->{
            tabs.defaults().size(110f,36f).pad(2f);
            tabs.button("All",Styles.flatTogglet,()->{tab[0]=0;rebuildGrid.run();}).update(b->b.setChecked(tab[0]==0));
            tabs.button("Floors",Styles.flatTogglet,()->{tab[0]=1;rebuildGrid.run();}).update(b->b.setChecked(tab[0]==1));
            tabs.button("Walls",Styles.flatTogglet,()->{tab[0]=2;rebuildGrid.run();}).update(b->b.setChecked(tab[0]==2));
        }).row();
        bd.cont.table(top->{
            top.defaults().size(120f,36f).pad(2f);
            top.button("Select All",Styles.flatt,()->{
                for(Block b:Vars.content.blocks()){
                    if(!isEligibleTerrainBlock(b))continue;
                    if(tab[0]==1&&!b.isFloor())continue;
                    if(tab[0]==2&&b.isFloor())continue;
                    allowedBlocks.add(b);
                }
                rebuildGrid.run();
            });
            top.button("Clear All",Styles.flatt,()->{
                if(tab[0]==0)allowedBlocks.clear();
                else{
                    Seq<Block> rem=new Seq<>();
                    for(Block b:allowedBlocks){
                        if(tab[0]==1&&b.isFloor())rem.add(b);
                        if(tab[0]==2&&!b.isFloor())rem.add(b);
                    }
                    for(Block b:rem)allowedBlocks.remove(b);
                }
                rebuildGrid.run();
            });
        }).row();
        bd.cont.field("",s->{search[0]=s;rebuildGrid.run();}).width(340f).padBottom(6f).row();
        rebuildGrid.run();
        bd.cont.pane(grid).size(Math.min(Core.graphics.getWidth()*0.9f,460f),Math.min(Core.graphics.getHeight()*0.65f,340f));
        bd.hidden(onCloseUpdate);
        bd.addCloseButton();
        bd.show();
    }
}
