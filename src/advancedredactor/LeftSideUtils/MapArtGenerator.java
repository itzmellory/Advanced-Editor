package advancedredactor.LeftSideUtils;
import arc.files.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.editor.*;
import mindustry.editor.DrawOperation.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.logic.*;
public class MapArtGenerator{
    private static Seq<Floor> validFloors;
    private static Seq<Block> validWalls;
    private static Seq<Item> validItems;
    private static void ensureCache(){
        if(validFloors!=null)return;
        validFloors=new Seq<>();
        validWalls=new Seq<>();
        validItems=new Seq<>();
        for(Block b:Vars.content.blocks()){
            if(b.isHidden()||b.mapColor.a<0.5f)continue;
            if(b instanceof Floor&&!(b instanceof OverlayFloor)&&b!=Blocks.empty){
                validFloors.add((Floor)b);
            }else if(b instanceof StaticWall){
                validWalls.add(b);
            }
        }
        for(Item item:Vars.content.items()){
            if(!item.isHidden()){
                validItems.add(item);
            }
        }
    }
    public static void generate(Fi file,int targetW,int targetH,MapArtDialog.ArtStyle style,MapArtDialog.LayerMode layerMode,boolean clearBlocks){
        Pixmap source=new Pixmap(file);
        try{
            int startX=Math.max(0,(Vars.editor.width()-targetW)/2);
            int startY=Math.max(0,(Vars.editor.height()-targetH)/2);
            generateAt(source,startX,startY,targetW,targetH,style,layerMode,clearBlocks);
        }finally{
            source.dispose();
        }
    }
    public static void generateAt(Pixmap source,int startX,int startY,int targetW,int targetH,MapArtDialog.ArtStyle style,MapArtDialog.LayerMode layerMode,boolean clearBlocks){
        ensureCache();
        int mapW=Vars.editor.width();
        int mapH=Vars.editor.height();
        int drawW=Math.min(targetW,mapW-startX);
        int drawH=Math.min(targetH,mapH-startY);
        if(drawW<=0||drawH<=0)return;
        DrawOperation op=new DrawOperation(Vars.editor);
        Color temp=new Color();
        for(int x=0;x<drawW;x++){
            for(int y=0;y<drawH;y++){
                int tx=startX+x;
                int ty=startY+y;
                if(tx<0||ty<0||tx>=mapW||ty>=mapH)continue;
                int srcX=Mathf.clamp((int)((float)x/targetW*source.width),0,source.width-1);
                int srcY=Mathf.clamp((int)((float)(targetH-1-y)/targetH*source.height),0,source.height-1);
                temp.rgba8888(source.get(srcX,srcY));
                if(temp.a<0.15f)continue;
                Tile tile=Vars.editor.tile(tx,ty);
                if(tile==null)continue;
                if(style==MapArtDialog.ArtStyle.TERRAIN){
                    op.addOperation(TileOp.get((short)tx,(short)ty,(byte)OpType.floor.ordinal(),tile.floorID()));
                    if(clearBlocks||layerMode!=MapArtDialog.LayerMode.FLOORS_ONLY){
                        op.addOperation(TileOp.get((short)tx,(short)ty,(byte)OpType.block.ordinal(),tile.blockID()));
                    }
                    if(layerMode==MapArtDialog.LayerMode.FLOORS_ONLY){
                        Floor best=findClosestFloor(temp);
                        if(best!=null)tile.setFloor(best);
                        if(clearBlocks&&!tile.block().isStatic())tile.setBlock(Blocks.air);
                    }else if(layerMode==MapArtDialog.LayerMode.WALLS_ONLY){
                        Block best=findClosestWall(temp);
                        if(best!=null)tile.setBlock(best);
                    }else{
                        Floor bf=findClosestFloor(temp);
                        Block bw=findClosestWall(temp);
                        if(bf!=null&&bw!=null){
                            if(colorDist(temp,bf.mapColor)<=colorDist(temp,bw.mapColor)){
                                tile.setFloor(bf);
                                tile.setBlock(Blocks.air);
                            }else{
                                tile.setBlock(bw);
                            }
                        }
                    }
                }else if(style==MapArtDialog.ArtStyle.SORTER){
                    Item best=findClosestItem(temp);
                    if(best!=null&&Blocks.sorter!=null){
                        op.addOperation(TileOp.get((short)tx,(short)ty,(byte)OpType.block.ordinal(),tile.blockID()));
                        tile.setNet(Blocks.sorter,Team.sharded,0);
                        if(tile.build instanceof Sorter.SorterBuild){
                            ((Sorter.SorterBuild)tile.build).sortItem=best;
                        }
                    }
                }else if(style==MapArtDialog.ArtStyle.CANVAS){
                    op.addOperation(TileOp.get((short)tx,(short)ty,(byte)OpType.floor.ordinal(),tile.floorID()));
                    Floor best=findClosestFloor(temp);
                    if(best!=null)tile.setFloor(best);
                    if(clearBlocks)tile.setBlock(Blocks.air);
                }
            }
        }
        Vars.editor.addTileOp(op);
        Vars.editor.renderer.updateAll();
        FogOfWarManager.rebuild();
    }
    private static Floor findClosestFloor(Color c){
        Floor best=null;
        float minDist=Float.MAX_VALUE;
        for(Floor f:validFloors){
            float d=colorDist(c,f.mapColor);
            if(d<minDist){
                minDist=d;
                best=f;
            }
        }
        return best;
    }
    private static Block findClosestWall(Color c){
        Block best=null;
        float minDist=Float.MAX_VALUE;
        for(Block w:validWalls){
            float d=colorDist(c,w.mapColor);
            if(d<minDist){
                minDist=d;
                best=w;
            }
        }
        return best;
    }
    private static Item findClosestItem(Color c){
        Item best=null;
        float minDist=Float.MAX_VALUE;
        for(Item i:validItems){
            float d=colorDist(c,i.color);
            if(d<minDist){
                minDist=d;
                best=i;
            }
        }
        return best;
    }
    private static float colorDist(Color a,Color b){
        float dr=a.r-b.r;
        float dg=a.g-b.g;
        float db=a.b-b.b;
        return dr*dr*0.3f+dg*dg*0.59f+db*db*0.11f;
    }
}
