package humanaicore.common;
/** Ben F Rayfield offers HumanAiCore opensource GNU LGPL */

import java.security.SecureRandom;
import java.util.Random;

public class Rand{
	private Rand(){}
	
	public static final Random weakRand;
	public static final SecureRandom strongRand;
	static{
		strongRand = new SecureRandom();
		//TODO set seed as bigger byte array, more hashcodes to fill it maybe
		strongRand.setSeed(3+System.nanoTime()*49999+System.currentTimeMillis()*new Object().hashCode());
		weakRand = new Random(strongRand.nextLong());
	}

}
