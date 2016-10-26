/** Minimalist server wrapping your func of bytes or map in and out. Opensource MIT License. */
package occamserver;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/** TODO create Thread or use threadpool for each request that comes in,
so slow downloaders dont slow other web calls.

TODO generalize this using func of byte array in to byte array out.
*/
public class Occamserver implements Runnable, Closeable{
	
	protected BytesFunc func;
	public BytesFunc getFunc(){ return func; }
	public void setFunc(BytesFunc f){ func = f; }
	public void setFunc(MapFunc f){ func = new WrapMapFuncInHttpBytesFunc(f); }
	
	protected boolean startedClosing = false;
	protected ServerSocket serverSocket;
	
	public final int maxBytesPerRequest;
	
	public final double maxSecondsReceivingRequest;
	
	public static final int DEFAULT_PORT = 80;
	
	public static final int DEFAULT_MAX_BYTES_PER_REQUEST = 1<<24;
	
	public static final double DEFAULT_MAX_SECONDS_RECEIVING_REQUEST = 30;
	
	public final int port;
	
	public static final MapFunc EXAMPLE_MAPFUNC = new MapFunc(){
		public Map call(Map in){
			Map out = new HashMap();
			out.put("firstLine", "HTTP/1.1 200 OK");
			//byte[] or String
			out.put("content", "Occamserver. Time: "+System.currentTimeMillis()*.001+" seconds. Map received: "+in);
			return out;
		}
	};
	
	/** displays System.currentTimeMillis() in browser.
	Try holding F5/refresh button in Chrome to see it count many times per second.
	*/
	public static final BytesFunc EXAMPLE_BYTESFUNC = new BytesFunc(){
		public byte[] call(byte[] in){
			try{
				//These example headers copied from Wikipedia. TODO use as few headers as possible.
				final String n = "\r\n";
				final String responseText = "HTTP/1.1 200 OK"
				+n+"Date: Mon, 23 May 2005 22:38:34 GMT"
				+n+"Content-Type: text/html; charset=UTF-8"
				+n+"Content-Encoding: UTF-8"
				//TODO dont want to answer wrong +n+"Content-Length: 138"
				+n+"Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT"
				//+n+"Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)"
				//+n+"ETag: \"3f80f-1b6-3e1cb03b\""
				+n+"Accept-Ranges: bytes"
				+n+"Connection: close"
				+n+""
				+n+"<html>"
				+n+"<head>"
				+n+"  <title>An Example Page</title>"
				+n+"</head>"
				+n+"<body>";
				//+n+"  Hello World, this is a very simple HTML document.";
				//+n+"</body>"
				//+n+"</html>";
				String r = responseText+n+"<br>"+System.currentTimeMillis()+"</body></html>";
				return r.getBytes("UTF-8");
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
	};
	
	public Occamserver(MapFunc func){
		this(func, DEFAULT_PORT, DEFAULT_MAX_BYTES_PER_REQUEST, DEFAULT_MAX_SECONDS_RECEIVING_REQUEST);
	}
	
	public Occamserver(BytesFunc func){
		this(func, DEFAULT_PORT, DEFAULT_MAX_BYTES_PER_REQUEST, DEFAULT_MAX_SECONDS_RECEIVING_REQUEST);
	}
	
	public Occamserver(MapFunc func, int port){
		this(func, port, DEFAULT_MAX_BYTES_PER_REQUEST, DEFAULT_MAX_SECONDS_RECEIVING_REQUEST);
	}
	
	public Occamserver(BytesFunc func, int port){
		this(func, port, DEFAULT_MAX_BYTES_PER_REQUEST, DEFAULT_MAX_SECONDS_RECEIVING_REQUEST);
	}
	
	public Occamserver(MapFunc func, int port, int maxBytesPerRequest, double maxSecondsReceivingRequest){
		this(new WrapMapFuncInHttpBytesFunc(func), port, maxBytesPerRequest, maxSecondsReceivingRequest);
	}
	
	/** The BytesFunc is expected to include http headers like "HTTP/1.1 200 OK" */ 
	public Occamserver(BytesFunc func, int port, int maxBytesPerRequest, double maxSecondsReceivingRequest){
		setFunc(func);
		this.maxBytesPerRequest = maxBytesPerRequest;
		this.maxSecondsReceivingRequest = maxSecondsReceivingRequest;
		this.port = port;
	}
	
	/** starts waiting for connections, and creates thread for each. TODO threadpool. */
	public void run(){
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		try{
			serverSocket = new ServerSocket(port);
			//FIXME Close InputStream and OutputStream from serverSocket. I tried closing InputStream after reading from it but browser says connection was closed. So maybe need to wait until write all to outputstream or some delay after that? I hope its not another http call expected, since back and forth between client and server creates lag.
		}catch(IOException e){
			throw new RuntimeException(e);
		}
		while(!startedClosing){
			final Socket s;
			try{
				s = serverSocket.accept();
				Thread t = new Thread(){
					public void run(){
						OutputStream out = null;
						InputStream in = null;
						try{
							in = s.getInputStream();
							byte bytesIn[] = readFully(in, maxBytesPerRequest, maxSecondsReceivingRequest, false);
							//System.out.println("bytesInAsString["+new String(bytesIn,"UTF-8")+"]");
							byte bytesOut[] = func.call(bytesIn);
							//byte bytesOut[] = func.func(null);
							out = s.getOutputStream();
							out.write(bytesOut);
							//out.flush();
						}catch(Exception e){
							//TODO return http response code and an error message
							throw new RuntimeException(e);
						}finally{
							if(out != null) try{ out.close(); }catch(IOException e){}
							if(in != null) try{ in.close(); }catch(IOException e){}
						}
					}
				};
				t.setPriority(Thread.MAX_PRIORITY-1);
				t.start();
			}catch(IOException e){
				e.printStackTrace(System.err);
			}
		}
		try{
			close();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public void close() throws IOException{
		startedClosing = true;
		serverSocket.close();
		System.out.println("Closed "+this);
	}
	
	public String toString(){
		return "(Occamserver, port "+serverSocket.getLocalPort()+", func "+func+")";
	}
	
	public static byte[] readFully(InputStream in, int maxBytes, double maxSeconds, boolean close) throws IOException{
		//System.err.println("readFully start...");
		try{
			byte b[] = new byte[256];
			int siz = 0;
			long nanoStart = System.nanoTime();
			while(true){
				int remainingArrayCapacity = b.length-siz;
				if(remainingArrayCapacity == 0){ //enlarge array
					if(siz == maxBytes+1) throw new IOException("Exceeded max bytes "+maxBytes);
					byte b2[] = new byte[Math.min(maxBytes+1,b.length*2)];
					System.arraycopy(b, 0, b2, 0, siz);
					b = b2;
					remainingArrayCapacity = b.length-siz;
				}
				int nextByte = in.read();
				//System.err.print((char)nextByte);
				if(nextByte == -1) break;
				b[siz++] = (byte)nextByte;
				//System.out.print((char)nextByte);
				double duration = (System.nanoTime()-nanoStart)*1e-9;
				if(maxSeconds < duration) throw new IOException("Took longer than "+maxSeconds+" seconds");
				boolean isGet = 3<=siz && (b[0]=='G' && b[1]=='E' && b[2]=='T');
				if(isGet && 7<=siz && b[siz-4]=='\r' && b[siz-3]=='\n' && b[siz-2]=='\r' && b[siz-1]=='\n'){
					break; //stream may close or not, but this is end since its GET and theres no data to POST.
				}
			}
			byte b2[] = new byte[siz];
			System.arraycopy(b, 0, b2, 0, siz);
			//System.err.println("readFully end.");
			return b2;
		}finally{
			if(close) in.close();
		}
	}
	
	
	public static void main(String[] args){
		new Occamserver(EXAMPLE_MAPFUNC).run();
		//new Occamserver(EXAMPLE_BYTESFUNC).run();
	}
	

}