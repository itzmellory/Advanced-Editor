package advancedredactor.ui;
import arc.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
public class ModFilterOptions{
    public static final Boolf<Block> floorsOnly=b->(b instanceof Floor&&!(b instanceof OverlayFloor))&&!headless()&&Core.atlas.isFound(b.fullIcon);
    public static final Boolf<Block> wallsOnly=b->(!b.synthetic()&&!(b instanceof Floor)&&!headless()&&Core.atlas.isFound(b.fullIcon)&&b.inEditor);
    public static final Boolf<Block> oresOnly=b->b instanceof OverlayFloor&&!headless()&&Core.atlas.isFound(b.fullIcon);
    private static boolean headless(){return Vars.headless;}
    public static class SliderOption extends FilterOption{
        public final String name;
        public final Floatp getter;
        public final Floatc setter;
        public final float min,max,step;
        public SliderOption(String n,Floatp g,Floatc s,float mn,float mx,float st){
            name=n;getter=g;setter=s;min=mn;max=mx;step=st;
        }
        @Override public void build(Table table){
            table.add("@filter.option."+name).growX().left().style(Styles.outlineLabel);
            Label val=table.label(()->Strings.autoFixed(getter.get(),2)).right().style(Styles.outlineLabel).get();
            table.row();
            table.slider(min,max,step,getter.get(),f->{
                setter.get(f);
                val.setText(Strings.autoFixed(f,2));
                changed.run();
            }).colspan(2).growX().row();
        }
    }
    public static class ToggleOption extends FilterOption{
        public final String name;
        public final Boolp getter;
        public final Boolc setter;
        public ToggleOption(String n,Boolp g,Boolc s){
            name=n;getter=g;setter=s;
        }
        @Override public void build(Table table){
            table.check("@filter.option."+name,getter.get(),b->{
                setter.get(b);
                changed.run();
            }).colspan(2).growX().left().padTop(4f).padBottom(4f).row();
        }
    }
    public static class BlockOption extends FilterOption{
        public final String name;
        public final Prov<Block> supplier;
        public final Cons<Block> consumer;
        public final Boolf<Block> filter;
        public BlockOption(String n,Prov<Block> s,Cons<Block> c,Boolf<Block> f){
            name=n;supplier=s;consumer=c;filter=f;
        }
        @Override public void build(Table table){
            table.button(b->b.image(supplier.get().uiIcon).update(i->i.setDrawable(supplier.get().uiIcon)).size(24f),()->{
                BaseDialog dialog=new BaseDialog("@filter.option."+name);
                dialog.cont.pane(t->{
                    int i=0;
                    for(Block block:Vars.content.blocks()){
                        if(!filter.get(block))continue;
                        t.image(block.uiIcon).size(32f).pad(3f).tooltip(block.localizedName).get().clicked(()->{
                            consumer.get(block);
                            dialog.hide();
                            changed.run();
                        });
                        if(++i%10==0)t.row();
                    }
                });
                dialog.addCloseButton();
                dialog.show();
            }).pad(4f).margin(8f);
            table.add("@filter.option."+name).left().growX().style(Styles.outlineLabel).row();
        }
    }
}
