/** Ben F Rayfield offers this package of this software opensource GNU LGPL */
package humanaicore.common;

public class IntRange{
	
	public final int start, endExclusive;
	
	public IntRange(int start, int endExclusive){
		this.start = start;
		this.endExclusive = endExclusive;
	}
	
	public String toString(){
		return "[IntRange "+start+" "+endExclusive+"]";
	}

}
