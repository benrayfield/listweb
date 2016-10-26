/** Minimalist server wrapping your func of bytes or map in and out. Opensource MIT License. */
package occamserver;
import java.util.Map;

/** A general func of Map to Map.
Example: In http, requires headers "firstLine" and "content"
*/
public interface MapFunc{
	
	public Map call(Map in);

}