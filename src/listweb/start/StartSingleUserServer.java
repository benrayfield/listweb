package listweb.start;
import static humanaicore.common.CommonFuncs.*;
import occamserver.MapFunc;
import occamserver.Occamserver;
import occamserver.WrapMapFuncInHttpBytesFunc;
import occamsjsonds.JsonDS;
import humanaicore.common.Files;
import humanaicore.common.Text;
import humanaicore.common.Time;
import humanaicore.err.Err;
import listweb.Root;
import listweb.Util;

import java.awt.Desktop;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Optionally, for using mindmap here from mobile phone browser,
so you can use and edit your mindmap while you're out,
as long as your computer doesnt go to sleep/off,
or could rent a server to be always on.
In theory many people could edit at once, if you give them (TODO) passwords,
and the mapacyc system (future version of this) will use all immutable merkle data
and publicKeys so will certainly scale to massivelyMultiplayer
such as a huge wiki mindmap or many small groups of people.
While this mindmap is an important tool I (Ben F Rayfield) use for organizing
my own ideas, I'm most interested in millions of people using it
to build software together with functions and data as immutable merkle.
*/
public class StartSingleUserServer{
	private StartSingleUserServer(){}
	
	//FIXME TODO after stop editing it much: static final byte[] htmlBytes = readHtmlFile();
	static byte[] readHtmlFile(){
		return Files.readFileRel("/data/listweb/mobileWebpage/index.html");
	}
	
	public static String urlSuffixFromFirstHttpLine(String line){
		return line.split("\\s+")[1];
	}
	
	public static String methodFromFromFirstHttpLine(String line){
		return line.split("\\s+")[0];
	}
	
	protected static Thread serverThread = null;
	
	public static void startServer(){
		if(serverThread == null){
			lg("Mindmap server starting at http://localhost:"+server.port+" starting at time "+Time.timeStr());
			serverThread = new Thread(server);
			serverThread.start();
		}
	}
	
	public static final boolean allowPut = false; //FIXME, not until password
	
	/** use server.setFunc(MapFunc) to change its behaviors */
	public static final Occamserver server = new Occamserver(
		new MapFunc(){
			public Map call(Map in){
				String inFirstLine = WrapMapFuncInHttpBytesFunc.asString(in.get("firstLine"));
				String relUrl = urlSuffixFromFirstHttpLine(inFirstLine);
				String httpMethod = methodFromFromFirstHttpLine(inFirstLine).toUpperCase();
				Map out = new HashMap();
				String name = Util.unescapeName(relUrl.substring(1));
				if(name.isEmpty()){
					out.put("firstLine", "HTTP/1.1 200 OK");
					out.put("Content-Type", "text/html; charset=UTF-8");
					//out.put("content", "Smartblob sever. Time: "+Time.time()); //byte[] or String
					//FIXME TODO out.put("content", htmlBytes);
					out.put("content", readHtmlFile()); //so can edit the file without restarting
				}else{
					if("GET".equals(httpMethod)){ //read name's json content
						out.put("firstLine", "HTTP/1.1 200 OK");
						String json = Root.nameExists(name)
							? JsonDS.toJson(Root.get(name))
							: "{}";
						out.put("Content-Type", "application/json; charset=UTF-8");
						out.put("content", json); //byte[] or String
					}else if(allowPut && "PUT".equals(httpMethod)){ //write name's json content
						Object content = in.get("content");
						if(content == null){
							out.put("firstLine", "HTTP/1.1 406 You gave no content to HTTP PUT at name: "+name);
							//out.put("Content-Type", "text/plain; charset=UTF-8");
							//out.put("content", "You gave no content to HTTP PUT at name: "+name); //byte[] or String
						}else{
							//FIXME occamserver should handle escaping/unescaping if its part of http protocol,
							//but not if js escaped it before sending. If so, send as Blob.
							String json = content instanceof String
								? (String)content
								: Text.bytesToString((byte[])content);
							json = Util.unescapeUrl(json);
							NavigableMap<String,Object> map = (NavigableMap) JsonDS.parse(json);
							Double localUiTime = (Double) Root.get(name).get("uiTime");
							if(localUiTime == null){
								localUiTime = Time.time();
								Root.get(name).put("uiTime", 0);
							}
							Double remoteUiTime = (Double) map.get("uiTime");
							if(remoteUiTime == null || remoteUiTime <= localUiTime){
								out.put("firstLine", "HTTP/1.1 409 Json map must include key \"uiTime\" after "+localUiTime
										+" which is last modify time of name: "+name);
								//out.put("Content-Type", "text/plain; charset=UTF-8");
								//out.put("content", "Json map must include key \"uiTime\" after "+localUiTime
								//	+" which is last modify time of name: "+name); //byte[] or String
							}else{
								Root.listweb.put(name, map);
								Root.onChange(name);
								out.put("firstLine", "HTTP/1.1 200 OK");
							}
						}
					}
				}
				return out;
				
				/*
				String s = "/b/name/";
				String sAjax = "/listweb/post/";
				if(relUrl.startsWith(sAjax)){
					Object content = in.get("content");
					if(content == null){
						out.put("Content-Type", "text/plain; charset=UTF-8");
						out.put("content", "You must POST a map of mindmap name to new value, where each value includes key uiTime."); //byte[] or String
					}else{
						String json = content instanceof String
							? (String)content
							: Text.bytesToString((byte[])content);
						json = Util.unescapeName(json); //urlunescape
						lg("mindmapAjax call: "+json);
						NavigableMap<String,NavigableMap<String,Object>> map = (NavigableMap) JsonDS.parse(json);
						//FIXME allow append version to versioned mindmap, but for now (at least until password and crypt) just read
						NavigableMap<String,NavigableMap<String,Object>> mapRet = new TreeMap();
						for(String name : map.keySet()){
							//FIXME: This time needs to be only when user changes it, not the current time,
							//before expanding to a multi user system,
							//but must sacrifice being able to edit nonversioned file for any name
							//without also setting time in it,
							//unless version file ignores that and always uses current time.
							//
							//I'm creating field "uiTime" which tries to sync with
							//but is less trusted than verTime (version file time).
							if(!Root.get(name).containsKey("uiTime")){
								Root.set(name, "uiTime", Time.time());
							}
							mapRet.put(name, Root.get(name));
						}
						String jsonRet = JsonDS.toJson(mapRet);
						lg("mindmapAjax answers: "+jsonRet);
						//out.put("Content-Type", "application/json; charset=UTF-8");
						out.put("Connection", "close");
						out.put("Content-Type", "text/plain; charset=UTF-8");
						out.put("abc", "def");
						out.put("abc2", "def2");
						out.put("abc3", "def3");
						out.put("content", jsonRet); //byte[] or String
					}
				}else if(relUrl.startsWith(s)){
					relUrl = relUrl.substring(s.length());
					String name;
					try{
						name = URLDecoder.decode(relUrl,"UTF-8");
					}catch(UnsupportedEncodingException e){ throw new Err(e); }
					//Check Root.nameExists(name) so dont allow remote user to create empty name in GET
					String json = Root.nameExists(name)
						? JsonDS.toJson(Root.get(name))
						: "{}";
					out.put("Content-Type", "text/plain; charset=UTF-8");
					out.put("content", json); //byte[] or String
				}else{
					out.put("Content-Type", "text/html; charset=UTF-8");
					//out.put("content", "Smartblob sever. Time: "+Time.time()); //byte[] or String
					//FIXME TODO out.put("content", htmlBytes);
					out.put("content", readHtmlFile()); //so can edit the file without restarting
				}
				return out;
				*/
			}
		},
		//I like this number because 1/e = 0.11111...^100000... in binary.
		(int)((1<<16)/Math.E) //port, first 16 binary digits of 1/e
	);
	
	public static void main(String[] args){
		lg(StartSingleUserServer.class.getName());
		startServer();
		try{
			Desktop.getDesktop().browse(new URI("http://localhost:"+server.port));
		}catch(Exception e){ throw new Err(e); }
	}

}
