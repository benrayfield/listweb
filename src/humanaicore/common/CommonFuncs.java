/** Ben F Rayfield offers HumanAiCore opensource GNU LGPL */
package humanaicore.common;
//import humanaicore.jselfmodify.JSelfModify;
//import humanaicore.xob.XobUtil;

/** Use the following line at the top of each source code file:
import static commonfuncs.CommonFuncs.*;
<br><br>
This is more like functional programming than object oriented.
*/
public class CommonFuncs{
	private CommonFuncs(){}
	
	public static void lg(String line){
		System.out.println(line);
	}
	
	public static void lgToUser(String line){
		System.err.println(line);
	}
	
	/** Since JSelfModify doesnt have a User system yet, just room for expansion,
	this function uses JSelfModify.rootUser as is normally done.
	WARNING: This kind of thing will need to be redesigned securely when we start having usernames.
	*
	public static Object jsmGet(String path){
		try{
			return JSelfModify.root.get(JSelfModify.rootUser, path);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	/** See WARNING in jsmGet *
	public static void jsmPut(String path, Object value){
		try{
			JSelfModify.root.put(JSelfModify.rootUser, path, value);
		}catch(Exception e){
			throw new RuntimeException(e);
		}	
	}
	
	/** See WARNING in jsmGet *
	public static boolean jsmExist(String path){
		try{
			return JSelfModify.root.exist(JSelfModify.rootUser, path);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}*/
	
	/** returns a debug string summarizing a pointer into an acyc as an object *
	public static String str(int pointerIntoAcyc){
		return XobUtil.describeGlobal(pointerIntoAcyc);
	}*/


}