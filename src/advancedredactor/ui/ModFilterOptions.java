package advancedredactor.ui;
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
public class ModFilterOptions {
    public static final Boolf<Block> floorsOnly = b -> b.isFloor() && !b.isOverlay() && b != Blocks.air;
    public static final Boolf<Block> wallsOnly = b -> !b.isFloor() && !b.synthetic() && b != Blocks.air;
    public static final Boolf<Block> oresOnly = b -> b.isOverlay() && b != Blocks.air;
    public static class SliderOption extends FilterOption {
        final String name;
        final Floatp getter;
        final Floatc setter;
        final float min, max, step;
        public SliderOption(String name, Floatp getter, Floatc setter, float min, float max, float step) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
        }
        @Override
        public void build(Table table) {
            Element base = table.add("@filter.option." + name).growX().left().style(Styles.outlineLabel).get();
            if (base instanceof Label) ((Label) base).setWrap(true);
            Label valueLabel = new Label("", Styles.outlineLabel);
            valueLabel.setAlignment(Align.right);
            Slider slider = new Slider(min, max, step, false);
            slider.setValue(getter.get());
            Runnable updateText = () -> {
                float val = getter.get();
                if (step >= 1f) valueLabel.setText(String.valueOf(Math.round(val)));
                else valueLabel.setText(Strings.fixed(val, step < 0.05f ? 2 : 1));
            };
            updateText.run();
            slider.moved(val -> {
                setter.get(val);
                updateText.run();
                if (changed != null) changed.run();
            });
            table.add(valueLabel).right().padLeft(4f).row();
            table.add(slider).colspan(2).growX().padBottom(4f).row();
        }
    }
    public static class ToggleOption extends FilterOption {
        final String name;
        final Boolp getter;
        final Boolc setter;
        public ToggleOption(String name, Boolp getter, Boolc setter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
        }
        @Override
        public void build(Table table) {
            Element base = table.add("@filter.option." + name).growX().left().style(Styles.outlineLabel).get();
            if (base instanceof Label) ((Label) base).setWrap(true);
            table.check("", getter.get(), val -> {
                setter.get(val);
                if (changed != null) changed.run();
            }).right().padBottom(4f).row();
        }
    }
    public static class BlockOption extends FilterOption {
        final String name;
        final Prov<Block> supplier;
        final Cons<Block> consumer;
        final Boolf<Block> filter;
        public BlockOption(String name, Prov<Block> supplier, Cons<Block> consumer, Boolf<Block> filter) {
            this.name = name;
            this.supplier = supplier;
            this.consumer = consumer;
            this.filter = filter;
        }
        @Override
        public void build(Table table) {
            table.button(b -> b.image(supplier.get().uiIcon).size(24f).update(i -> i.setDrawable(supplier.get().uiIcon)), () -> {
                BaseDialog dialog = new BaseDialog("@filter.option." + name);
                dialog.cont.pane(t -> {
                    int i = 0;
                    for (Block block : Vars.content.blocks()) {
                        if (!filter.get(block)) continue;
                        t.image(block.uiIcon).size(32f).pad(3f).get().clicked(() -> {
                            consumer.get(block);
                            dialog.hide();
                            if (changed != null) changed.run();
                        });
                        if (++i % 10 == 0) t.row();
                    }
                }).size(420f, 340f);
                dialog.addCloseButton();
                dialog.show();
            }).size(36f).pad(2f).left();
            Element lbl = table.add("@filter.option." + name).growX().left().padLeft(4f).style(Styles.outlineLabel).get();
            if (lbl instanceof Label) ((Label) lbl).setWrap(true);
            table.row();
        }
    }
}
