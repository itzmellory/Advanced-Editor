package advancedredactor.upperuiutils;
import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import advancedredactor.upperuiutils.LandCopyPast.*;
import java.io.*;
import java.util.zip.*;
public class TerrainSchematicsDialog{
    public static void show(){
        BaseDialog d=new BaseDialog(Core.bundle.get("upper.schematics","Terrain Schematics"));
        final String[] saveName={"Terrain_"+(Time.millis()/1000)%10000};
        Table top=new Table();
        top.defaults().pad(4f);
        top.add(Core.bundle.get("upper.schemname","Name:"));
        top.field(saveName[0],s->saveName[0]=s).width(200f);
        top.button(Core.bundle.get("upper.schemsave","Save to Schematics"),Icon.save,Styles.flatt,()->{
            if(LandCopyPast.clipboard==null){
                Vars.ui.showErrorMessage("Copy terrain first before saving!");
                return;
            }
            String name=saveName[0].trim();
            if(name.isEmpty())name="Terrain";
            saveToVanillaSchematics(LandCopyPast.clipboard,name);
            Vars.ui.showInfoFade("Saved to game Schematics!");
            d.hide();
        }).height(42f).disabled(b->LandCopyPast.clipboard==null).row();
        Table actions=new Table();
        actions.defaults().size(230f,46f).pad(4f);
        actions.button("Open Game Schematics",Icon.paste,Styles.flatt,()->{
            d.hide();
            Vars.ui.schematics.show();
        }).row();
        actions.button("Load Terrain From Schematics",Icon.download,Styles.flatt,()->{
            d.hide();
            showTerrainPickerFromSchematics();
        }).row();
        d.cont.add(top).padBottom(10f).row();
        d.cont.add(actions).row();
        d.addCloseButton();
        d.show();
    }
    public static void saveToVanillaSchematics(Clip c,String name){
        try{
            Seq<Stile> tiles=new Seq<>();
            for(int x=0;x<c.width;x++){
                for(int y=0;y<c.height;y++){
                    int idx=x+y*c.width;
                    if(c.isCenter[idx]&&c.blockIds[idx]>0){
                        Block b=Vars.content.block(c.blockIds[idx]);
                        if(b!=null&&b!=Blocks.air){
                            tiles.add(new Stile(b,x,y,null,c.rotations[idx]));
                        }
                    }
                }
            }
            if(tiles.isEmpty()){
                tiles.add(new Stile(Blocks.copperWall,0,0,null,(byte)0));
            }
            StringMap tags=new StringMap();
            tags.put("name",name);
            tags.put("description","Terrain Schematic ("+c.width+"x"+c.height+")");
            tags.put("ar_terrain",encodeClip(c));
            Schematic schem=new Schematic(tiles,tags,c.width,c.height);
            Vars.schematics.add(schem);
        }catch(Exception e){
            Vars.ui.showException(e);
        }
    }
    private static void showTerrainPickerFromSchematics(){
        BaseDialog d=new BaseDialog("Load Terrain Schematic");
        Table list=new Table();
        list.defaults().pad(4f).growX();
        int count=0;
        for(Schematic s:Vars.schematics.all()){
            if(s.tags!=null&&s.tags.containsKey("ar_terrain")){
                count++;
                Table row=new Table(Tex.button);
                row.margin(6f);
                row.add(s.name()+" ["+s.width+"x"+s.height+"]").left().growX().padRight(10f);
                row.button("Paste Terrain",Icon.ok,Styles.flatt,()->{
                    Clip loaded=decodeClip(s.tags.get("ar_terrain"));
                    if(loaded!=null){
                        if(LandCopyPast.clipboard!=null)LandCopyPast.clipboard.dispose();
                        LandCopyPast.clipboard=loaded;
                        LandCopyPast.setMode(Mode.paste);
                        d.hide();
                    }
                }).size(150f,38f);
                list.add(row).width(420f).row();
            }
        }
        if(count==0){
            for(Schematic s:Vars.schematics.all()){
                Table row=new Table(Tex.button);
                row.margin(6f);
                row.add(s.name()+" ["+s.width+"x"+s.height+"]").left().growX().padRight(10f);
                row.button("Load Blocks",Icon.ok,Styles.flatt,()->{
                    Clip loaded=clipFromStandardSchematic(s);
                    if(LandCopyPast.clipboard!=null)LandCopyPast.clipboard.dispose();
                    LandCopyPast.clipboard=loaded;
                    LandCopyPast.setMode(Mode.paste);
                    d.hide();
                }).size(150f,38f);
                list.add(row).width(420f).row();
            }
        }
        d.cont.pane(list).size(460f,380f);
        d.addCloseButton();
        d.show();
    }
    private static Clip clipFromStandardSchematic(Schematic s){
        Clip c=new Clip(s.width,s.height);
        for(int i=0;i<s.width*s.height;i++){
            c.floorIds[i]=-1;
            c.overlayIds[i]=-1;
        }
        for(Stile st:s.tiles){
            if(st.x>=0&&st.y>=0&&st.x<s.width&&st.y<s.height&&st.block!=null){
                int idx=st.x+st.y*s.width;
                c.blockIds[idx]=st.block.id;
                c.isCenter[idx]=true;
                c.rotations[idx]=st.rotation;
                c.teamIds[idx]=(byte)Vars.editor.drawTeam.id;
            }
        }
        return c;
    }
    private static String encodeClip(Clip c)throws Exception{
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(new DeflaterOutputStream(bos))){
            out.writeInt(c.width);
            out.writeInt(c.height);
            int len=c.width*c.height;
            for(int i=0;i<len;i++){
                out.writeUTF(c.floorIds[i]>=0&&Vars.content.block(c.floorIds[i])!=null?Vars.content.block(c.floorIds[i]).name:"");
                out.writeUTF(c.overlayIds[i]>=0&&Vars.content.block(c.overlayIds[i])!=null?Vars.content.block(c.overlayIds[i]).name:"");
                out.writeUTF(c.blockIds[i]>0&&Vars.content.block(c.blockIds[i])!=null?Vars.content.block(c.blockIds[i]).name:"");
                out.writeBoolean(c.isCenter[i]);
                out.writeByte(c.teamIds[i]);
                out.writeByte(c.rotations[i]);
                out.writeByte(c.datas[i]);
                out.writeByte(c.floorDatas[i]);
                out.writeByte(c.overlayDatas[i]);
                out.writeInt(c.extraDatas[i]);
            }
        }
        return new String(Base64Coder.encode(bos.toByteArray()));
    }
    private static Clip decodeClip(String b64){
        try{
            byte[] raw=Base64Coder.decode(b64);
            try(DataInputStream in=new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(raw)))){
                int w=in.readInt();
                int h=in.readInt();
                Clip c=new Clip(w,h);
                int len=w*h;
                for(int i=0;i<len;i++){
                    String fn=in.readUTF();
                    String on=in.readUTF();
                    String bn=in.readUTF();
                    c.floorIds[i]=fn.isEmpty()?-1:(Vars.content.block(fn)!=null?Vars.content.block(fn).id:-1);
                    c.overlayIds[i]=on.isEmpty()?-1:(Vars.content.block(on)!=null?Vars.content.block(on).id:0);
                    c.blockIds[i]=bn.isEmpty()?0:(Vars.content.block(bn)!=null?Vars.content.block(bn).id:0);
                    c.isCenter[i]=in.readBoolean();
                    c.teamIds[i]=in.readByte();
                    c.rotations[i]=in.readByte();
                    c.datas[i]=in.readByte();
                    c.floorDatas[i]=in.readByte();
                    c.overlayDatas[i]=in.readByte();
                    c.extraDatas[i]=in.readInt();
                }
                return c;
            }
        }catch(Exception e){
            Vars.ui.showException(e);
            return null;
        }
    }
}
