package advancedredactor.generation;
import arc.*;
import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import advancedredactor.ui.ModFilterOptions.*;
import static advancedredactor.ui.ModFilterOptions.*;
public class AdvancedTerrainFilter extends GenerateFilter{
    public Block floor=Blocks.stone;
    public Block wall=Blocks.stoneWall;
    public float scale=80f;
    public float octaves=4f;
    public float wallDensity=0.5f;
    public float tilt=0f;
    public boolean fillEdgeVoids=false;
    public float edgeThickness=22f;
    @Override public String name(){return Core.bundle.get("filter.advancedterrain.name","Advanced Terrain");}
    @Override public char icon(){return Iconc.blockStoneWall;}
    @Override public FilterOption[] options(){
        return new FilterOption[]{
            new BlockOption("floor",()->floor,b->floor=b,floorsOnly),
            new BlockOption("wall",()->wall,b->wall=b,wallsOnly),
            new SliderOption("scale",()->scale,f->scale=f,10f,400f,1f),
            new SliderOption("octaves",()->octaves,f->octaves=f,1f,8f,1f),
            new SliderOption("tilt",()->tilt,f->tilt=f,0f,360f,1f),
            new SliderOption("wallDensity",()->wallDensity,f->wallDensity=f,0f,1f,0.05f),
            new ToggleOption("fillEdgeVoids",()->fillEdgeVoids,b->fillEdgeVoids=b),
            new SliderOption("edgeThickness",()->edgeThickness,f->edgeThickness=f,5f,80f,1f)
        };
    }
    @Override public void apply(GenerateInput in){
        float rad=tilt*Mathf.degRad,cos=Mathf.cos(rad),sin=Mathf.sin(rad);
        float nx=in.x*cos-in.y*sin,ny=in.x*sin+in.y*cos;
        float noise=(float)Simplex.noise2d(seed,(int)octaves,0.5f,1f/scale,nx,ny);
        if(fillEdgeVoids){
            float dLeft=in.x;
            float dRight=in.width-1-in.x;
            float dBottom=in.y;
            float dTop=in.height-1-in.y;
            float minEdge=Math.min(Math.min(dLeft,dRight),Math.min(dBottom,dTop));
            float jag=(float)Simplex.noise2d(seed+99,3,0.6f,1f/(scale*0.45f),in.x,in.y)*edgeThickness*0.65f;
            float effectiveDist=minEdge+jag;
            if(effectiveDist<edgeThickness){
                float boost=Mathf.clamp(1f-(effectiveDist/edgeThickness))*1.35f;
                noise+=boost;
            }
        }
        in.floor=floor;
        in.block=(noise>(1f-wallDensity))?wall:Blocks.air;
    }
}
