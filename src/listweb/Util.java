package listweb;
import static humanaicore.common.CommonFuncs.*;
import humanaicore.common.Text;
import humanaicore.err.Err;

public class Util{
	private Util(){}
	
	public static final String progName = "listweb";
	public static final String licenseName = "GNU GPL 2+";

	public static String unescapeName(String escaped){
		return unescape(escaped,true);
	}
	
	public static String unescapeUrl(String escaped){
		return unescape(escaped,false);
	}
	
	public static String unescape(String escaped, boolean dropUnderscores){
		byte b[] = Text.stringToBytes(escaped);
		//each escape shortens by 2. Each _ is removed, but you can include _ as %5f
		byte b2[] = new byte[b.length];
		int b2Siz = 0;
		for(int i=0; i<b.length; i++){
			if(b[i] == '%'){
				b2[b2Siz++] = (byte)(Text.hexDigitToInt(b[++i])<<4 | Text.hexDigitToInt(b[++i]));
			}else if(!dropUnderscores || b[i] != '_'){
				b2[b2Siz++] = b[i];
			}
		}
		byte b3[] = new byte[b2Siz];
		System.arraycopy(b2, 0, b3, 0, b2Siz);
		return Text.bytesToString(b3);
		
		/*try{
			escaped = escaped.replace("_",""); //remove what precedes capitals.
			return URLDecoder.decode(escaped, "UTF-8"); //ERROR changes "+" to " "
		}catch (UnsupportedEncodingException e){
			throw new RuntimeException(e);
		}*/
	}
	
	/** All these are replaced urlEscaping (but on these chars), such as "_" becomes "%5f".
	Also, capitals must be preceded by "_" since names are caseSensitive
	but must also work in fileSystems that check filename equality caseInsensitive (Windows).
	*/
	public static final String escapedChars = "%\\/:;\r\n?*\"<>|._";
	
	public static String escapeName(String name){
		String escaped = escapeNameNoThrow(name);
		//TODO does any relevant filesystem measure in bytes instead of chars? What max len per path part?
		if(Root.maxEscapedNameLen < escaped.length())
			throw new Err("EscapedName too long: escaped="+escaped+" escapedLen="+escaped.length()+" name="+name);
		return escaped;
	}
	
	/** escapeName is the normal way. This is used when choosing how to rename when its too long. */
	public static String escapeNameNoThrow(String name){
		if(!name.equals(name.trim())){
			throw new Err("Name has leading or trailing whitespace["+name+"]");
		}
		//TODO optimize
		for(int i=0; i<escapedChars.length(); i++){
			name = name.replace(escapedChars.substring(i,i+1),escapeFor(escapedChars.charAt(i)));
		}
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<name.length(); i++){
			char c = name.charAt(i);
			if(Character.isUpperCase(c)) sb.append('_');
			sb.append(c);
		}
		String escaped = sb.toString();
		return escaped;
	}
	
	/** This is used when ready to shorten a name if its too long,
	such as when importing data and copying the full name into def if shortened.
	TODO optimize by adding 2 len for each escapedChars found.
	*/
	public static int charsLenOfEscapedName(String name){
		return escapeNameNoThrow(name).length();
	}
	
	public static int byteLenOfEscapedName(String name){
		return Text.stringToBytes(name).length;
	}

	static String escapeFor(char c){
		String s = Integer.toHexString(c);
		if(s.length() > 2) throw new RuntimeException("TODO urlescape multibyte char: "+c);
		if(s.length() == 1) s = "0"+s;
		return "%"+s;
	}

	/** True if both are null, false if 1 is null, else x.equals(y) */
	public static boolean equals(Object x, Object y){
		return x==null ? y==null : x.equals(y);
	}

	/** If it contains tags, they must be as plain text */
	public static String escapeToAppearAsPlainTextInHtml(String s){
		return s.replace("&","&amp;").replace("<","&gt;").replace(">","&lt;"); //FIXME what are the others?
	}
	
	public static String jsonToSingleLine(String json){
		return json.replaceAll("(\\r|\\n|\\r\\n)\\t*", "");
	}
	
	public static void main(String[] args){
		String s = "abc"+escapedChars+"def";
		lg("testName:"+s);
		String escaped = escapeName(s);
		lg("escaped: "+escaped);
		String unescaped = unescapeName(escaped);
		lg("unescaped: "+unescaped);
		if(!s.equals(unescaped)) throw new Err("Escape or unescape broken");
	}

}