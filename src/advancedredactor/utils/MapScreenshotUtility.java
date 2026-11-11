package advancedredactor.utils;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import java.nio.*;
import java.text.*;
import java.util.*;
public class MapScreenshotUtility{
    private static boolean capturing;
    public static void capture(){
        if(capturing||Vars.world==null||Vars.world.width()<=0||Vars.world.height()<=0)return;
        capturing=true;
        Core.app.post(()->{
            FrameBuffer buffer=null;
            Pixmap pixmap=null;
            Pixmap flipped=null;
            float oldX=Core.camera.position.x,oldY=Core.camera.position.y,oldW=Core.camera.width,oldH=Core.camera.height;
            boolean wasHudShown=Vars.ui!=null&&Vars.ui.hudfrag!=null&&Vars.ui.hudfrag.shown;
            try{
                int width=Vars.world.width()*Vars.tilesize;
                int height=Vars.world.height()*Vars.tilesize;
                if(Vars.ui!=null&&Vars.ui.hudfrag!=null)Vars.ui.hudfrag.shown=false;
                buffer=new FrameBuffer(width,height);
                buffer.begin();
                Core.graphics.clear(0f,0f,0f,1f);
                if(Vars.state.isEditor()&&Vars.editor!=null&&Vars.editor.renderer!=null){
                    Draw.proj().setOrtho(0f,0f,width,height);
                    Draw.reset();
                    Vars.editor.renderer.draw(0f,0f,width,height);
                    Draw.flush();
                }else if(Vars.renderer!=null){
                    Core.camera.position.set(width/2f,height/2f);
                    Core.camera.width=width;
                    Core.camera.height=height;
                    Core.camera.update();
                    Draw.proj(Core.camera);
                    Vars.renderer.draw();
                    Draw.flush();
                }
                ByteBuffer pixels=Buffers.newByteBuffer(width*height*4);
                Core.gl.glReadPixels(0,0,width,height,GL20.GL_RGBA,GL20.GL_UNSIGNED_BYTE,pixels);
                pixels.position(0);
                pixmap=new Pixmap(pixels,width,height);
                flipped=pixmap.flipY();
                buffer.end();
                Fi folder=Core.settings.getDataDirectory().child("maps-screenshots");
                folder.mkdirs();
                String stamp=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                Fi file=folder.child("map_"+stamp+".png");
                int suffix=1;
                while(file.exists())file=folder.child("map_"+stamp+"_"+(suffix++)+".png");
                PixmapIO.writePng(file,flipped);
                if(Vars.ui!=null)Vars.ui.showInfoToast("Map screenshot saved: "+file.name(),3f);
            }catch(Throwable error){
                if(buffer!=null)try{buffer.end();}catch(Throwable ignored){}
                Log.err("Failed to capture map screenshot",error);
                if(Vars.ui!=null)Vars.ui.showInfoToast("Failed to save map screenshot.",3f);
            }finally{
                Core.camera.position.set(oldX,oldY);
                Core.camera.width=oldW;
                Core.camera.height=oldH;
                Core.camera.update();
                if(Vars.ui!=null&&Vars.ui.hudfrag!=null)Vars.ui.hudfrag.shown=wasHudShown;
                if(flipped!=null)flipped.dispose();
                if(pixmap!=null)pixmap.dispose();
                if(buffer!=null)buffer.dispose();
                capturing=false;
            }
        });
    }
}
