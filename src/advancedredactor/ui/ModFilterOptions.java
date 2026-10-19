package advancedredactor.ui;
import arc.Core;
import arc.func.*;
import arc.scene.Element;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
public class ModFilterOptions{
    public static final Boolf<Block> floorsOnly=b->b.isFloor()&&!b.isOverlay()&&b!=Blocks.air;
    public static final Boolf<Block> wallsOnly=b->!b.isFloor()&&!b.synthetic()&&b!=Blocks.air;
    public static final Boolf<Block> oresOnly=b->b.isOverlay()&&b!=Blocks.air;
    public static class SliderOption extends FilterOption{
        final String name;final Floatp getter;final Floatc setter;final float min,max,step;
        public SliderOption(String n,Floatp g,Floatc c,float mi,float ma,float s){name=n;getter=g;setter=c;min=mi;max=ma;step=s;}
        @Override public void build(Table t){
            Element b=t.add("@filter.option."+name).width(160f).left().style(Styles.outlineLabel).wrap().get();
            Label vl=new Label("",Styles.outlineLabel);vl.setAlignment(Align.right);
            Slider sl=new Slider(min,max,step,false);sl.setValue(getter.get());
            Runnable ut=()->{
                float v=getter.get();
                if(step>=1f)vl.setText(String.valueOf(Math.round(v)));else vl.setText(Strings.fixed(v,step<0.05f?2:1));
            };
            ut.run();
            sl.moved(v->{setter.get(v);ut.run();if(changed!=null)changed.run();});
            t.add(vl).right().padLeft(4f).row();
            t.add(sl).colspan(2).width(200f).padBottom(4f).row();
        }
    }
    public static class ToggleOption extends FilterOption{
        final String name;final Boolp getter;final Boolc setter;
        public ToggleOption(String n,Boolp g,Boolc c){name=n;getter=g;setter=c;}
        @Override public void build(Table t){
            t.add("@filter.option."+name).width(160f).left().style(Styles.outlineLabel).wrap();
            t.check("",getter.get(),v->{setter.get(v);if(changed!=null)changed.run();}).right().padBottom(4f).row();
        }
    }
    public static class BlockOption extends FilterOption{
        final String name;final Prov<Block> supplier;final Cons<Block> consumer;final Boolf<Block> filter;
        public BlockOption(String n,Prov<Block> s,Cons<Block> c,Boolf<Block> f){name=n;supplier=s;consumer=c;filter=f;}
        @Override public void build(Table t){
            t.button(b->b.image(supplier.get().uiIcon).size(24f).update(i->i.setDrawable(supplier.get().uiIcon)),()->{
                BaseDialog d=new BaseDialog("@filter.option."+name);
                d.cont.pane(pt->{
                    int i=0;for(Block bl:Vars.content.blocks()){
                        if(!filter.get(bl))continue;
                        pt.image(bl.uiIcon).size(32f).pad(3f).get().clicked(()->{consumer.get(bl);d.hide();if(changed!=null)changed.run();});
                        if(++i%10==0)pt.row();
                    }
                }).size(Math.min(Core.graphics.getWidth()*0.9f,420f),340f);
                d.addCloseButton();d.show();
            }).size(36f).pad(2f).left();
            t.add("@filter.option."+name).width(160f).left().padLeft(4f).style(Styles.outlineLabel).wrap().row();
        }
    }
}
