package advancedredactor.upperuiutils;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import advancedredactor.upperuiutils.LandCopyPast.*;
import java.io.*;
public class TerrainSchematicsDialog{
    public static Fi dir(){
        Fi d=Core.settings.getDataDirectory().child("advancedredactor-terrain-schematics");
        d.mkdirs();
        return d;
    }
    public static void show(){
        BaseDialog d=new BaseDialog(Core.bundle.get("upper.schematics","Terrain Schematics"));
        Table list=new Table();
        final String[] saveName={"terrain_"+Time.millis()/1000};
        Runnable[] rebuild=new Runnable[1];
        rebuild[0]=()->{
            list.clearChildren();
            list.defaults().pad(4f).left();
            Fi[] files=dir().list(".tmsch");
            if(files.length==0){
                list.add(Core.bundle.get("upper.noschematics","No saved terrain schematics.")).color(Color.lightGray).pad(12f).row();
            }
            for(Fi f:files){
                Table row=new Table(Tex.pane);
                row.margin(6f);
                row.add(f.nameWithoutExtension()).growX().left().padRight(10f);
                row.button(Icon.download,Styles.flati,()->{
                    Clip loaded=loadClip(f);
                    if(loaded!=null){
                        if(LandCopyPast.clipboard!=null)LandCopyPast.clipboard.dispose();
                        LandCopyPast.clipboard=loaded;
                        LandCopyPast.setMode(Mode.paste);
                        d.hide();
                    }
                }).size(40f).tooltip("Load into Paste");
                row.button(Icon.trash,Styles.flati,()->{
                    f.delete();
                    rebuild[0].run();
                }).size(40f).padLeft(4f).tooltip("Delete");
                list.add(row).growX().width(380f).row();
            }
        };
        Table top=new Table();
        top.defaults().pad(4f);
        top.add(Core.bundle.get("upper.schemname","Name:"));
        top.field(saveName[0],s->saveName[0]=s).width(200f);
        top.button(Core.bundle.get("upper.schemsave","Save Clipboard"),Icon.save,Styles.flatt,()->{
            if(LandCopyPast.clipboard==null){
                Vars.ui.showErrorMessage("Copy terrain first before saving!");
                return;
            }
            String clean=saveName[0].trim().replaceAll("[^a-zA-Z0-9_\\-]","_");
            if(clean.isEmpty())clean="terrain";
            saveClip(LandCopyPast.clipboard,dir().child(clean+".tmsch"));
            rebuild[0].run();
        }).height(40f).disabled(b->LandCopyPast.clipboard==null);
        d.cont.add(top).row();
        rebuild[0].run();
        d.cont.pane(list).size(420f,340f).padTop(6f);
        d.addCloseButton();
        d.show();
    }
    public static void saveClip(Clip c,Fi file){
        try(DataOutputStream out=new DataOutputStream(file.write(false,8192))){
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
        }catch(Exception e){
            Vars.ui.showException(e);
        }
    }
    public static Clip loadClip(Fi file){
        try(DataInputStream in=new DataInputStream(file.read(8192))){
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
        }catch(Exception e){
            Vars.ui.showException(e);
            return null;
        }
    }
        }
