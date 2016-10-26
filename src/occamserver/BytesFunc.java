/** Minimalist server wrapping your func of bytes or map in and out. Opensource MIT License. */
package occamserver;

/** Immutable. Param and return byte arrays must not be modified
by this or caller since they may be reused for efficiency.
*/
public interface BytesFunc{

	public byte[] call(byte[] in);

}
