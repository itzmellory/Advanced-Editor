package advancedredactor.editor;
import arc.struct.IntSet;
public class ChunkedGridBits{
    private final IntSet bits=new IntSet();private final int width;
    public ChunkedGridBits(int width){this.width=width;}
    public void set(int x,int y){bits.add(x+y*width);}
    public void clear(int x,int y){bits.remove(x+y*width);}
    public boolean get(int x,int y){return bits.contains(x+y*width);}
    public IntSet.IntSetIterator iterator(){return bits.iterator();}
    public void clearAll(){bits.clear();}
}
