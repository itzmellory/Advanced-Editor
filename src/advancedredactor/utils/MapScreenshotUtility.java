package advancedredactor.utils;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.graphics.g2d.*;
import arc.util.*;
import arc.util.Buffers;
import mindustry.*;
import java.nio.*;
import java.text.*;
import java.util.*;
public class MapScreenshotUtility{
    private static boolean capturing;
    public static void capture(){
        if(capturing||Vars.editor==null||Vars.editor.isLoading()||Vars.editor.width()<=0||Vars.editor.height()<=0)return;
        capturing=true;
        Core.app.post(()->{
            FrameBuffer buffer=null;
            Pixmap pixmap=null;
            Pixmap flipped=null;
            try{
                int width=Vars.editor.width()*Vars.tilesize;
                int height=Vars.editor.height()*Vars.tilesize;
                buffer=new FrameBuffer(width,height);
                buffer.begin();
                Core.graphics.clear(0f,0f,0f,0f);
                Draw.proj().setOrtho(0f,0f,width,height);
                Draw.reset();
                Vars.editor.renderer.draw(0f,0f,width,height);
                Draw.flush();
                ByteBuffer pixels=Buffers.newByteBuffer(width*height*4);
                Core.gl.glReadPixels(0,0,width,height,GL20.GL_RGBA,GL20.GL_UNSIGNED_BYTE,pixels);
                pixels.position(0);
                pixmap=new Pixmap(pixels,width,height);
                flipped=pixmap.flipY();
                buffer.end();
                Fi folder=Core.files.local("Map-Screenshots");
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
                if(flipped!=null)flipped.dispose();
                if(pixmap!=null)pixmap.dispose();
                if(buffer!=null)buffer.dispose();
                capturing=false;
            }
        });
    }
}
