package listweb;
import static humanaicore.common.CommonFuncs.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import humanaicore.common.Files;
import humanaicore.common.Text;
import humanaicore.common.Time;
import humanaicore.err.Err;
import occamsjsonds.JsonDS;
import java.util.function.*;

public class Root{
	private Root(){}
	
	/** map of mindmapItemName (unescaped filename) to json value in that file. Infoflow both directions. */
	public static final NavigableMap<String,NavigableMap<String,Object>> listweb = new TreeMap();
	
	/** All names. The NavigableMap starts with few names because most are not loaded from harddrive.
	This SortedSet can only have names that dont exist if they're deleted since program last started.
	This is used for searching names without having to load them all from harddrive.
	This is a cache, so adding to this SortedSet is not an event that should fireListeners.
	*/
	public static final SortedSet<String> namesCache = new TreeSet();
	
	public static final int maxEscapedNameLen = 200; //TODO chars or bytes? Check at least Windows and Linux limits.
	
	/** Map of name to (last time saved to vervar). The var "last version" files are overwritten more often. *
	public static final NavigableMap<String,Double> cacheVervarModt = new TreeMap();
	*/
	
	/** TODO? Unlike the mindmap NavigableMap, metadata must be loaded for all mindmapItems,
	cached together in 1 file. It must be a cache, which means derivable from mindmap.
	Key is the same as in mindmap. Value is map with at least these keys:
	prilistListSize defStringSize
	TODO in later version, for maplists also include keys: econacycRelValue econacycCost.
	*
	public static final NavigableMap<String,NavigableMap<String,Object>> metadata = new TreeMap();
	*/
	
	/** Consumer<String> listens for any change of that mindmap name.
	FIXME instead of Set, use a SortedSet or TreeList since this software must be deterministic.
	*/
	private static final Map<String,Set<Consumer<String>>> mapStringToSetOfListener = new HashMap();
	
	private static final Map<Consumer<String>,Set<String>> mapListenerToSetOfString = new HashMap();
	
	/** names that need to be saved */
	private static final Set<String> modified = new HashSet<String>();
	
	/** Call this when change value of Root.mindmap key, including directly changing its deep contents.
	Does setModified then fireListeners. In a later version of this software,
	this will be obsolete because all data will be merkleForest which is immutable.
	*/
	public static synchronized void onChange(String name){
		setModified(name);
		fireListeners(name);
	}
	
	private static void setModified(String name){
		namesCache.add(name); //TODO Whats the smallest set of places this could be and catch all the names?
		if(Debug.logModified) lg("modified: "+name);
		//setModified(name, Time.time());
		NavigableMap map = get(name);
		Double uiTime = (Double) map.get("uiTime");
		double now = Time.time();
		if(uiTime == null || uiTime<now){
			//in case server receives version modified slightly out of sync so ahead of our time
			map.put("uiTime", now);
		}
		modified.add(name);
	}
	
	private static synchronized void fireListeners(String name){
		namesCache.add(name); //TODO Whats the smallest set of places this could be and catch all the names?
		Set<Consumer<String>> set = listeners(name);
		if(Debug.logSetOfListenersBeforeFiringEvent) lg("fireListeners about "+name+" to "+set);
		for(Consumer<String> listener : set){
			listener.accept(name);
		}
	}
	
	/** Vervar, not var.
	TODO store each option in def of some mindmapItem like name "acycOptionVervarSaveInterval"?
	If so, make sure to use permisvec to limit who and what values it can change.
	Def is the right place since its the only textfield displayed by default.
	Also add a field defRecogFunc, which permisvec will protect similarly.
	defRecogFunc defines allowed defs. Example: ["range" 60 3600].
	Example ["regex" "[1-9][0-9]{0,3}"] or ["and" ["regex" ".*blah.*"] ["not" ["regex" ".*xxyy.*"]]].
	*/
	public static double vervarSaveInterval(){
		//return 60; //TODO 15 mins
		return 60*15;
	}
	
	/** var, not vervar. */
	public static double varSaveInterval(){
		//return 20;
		return 60;
	}
	
	/** TODO in a future version of this software,
	each queuedFunc takes global state as param and returns next global state,
	but for now since that state is the mutable NavigableMap mindmap, param and return are null.
	TODO ForkJoinTask and ForkJoinPool?
	*/
	private static volatile Function queue = null;
	public static synchronized void queue(Function f){
		if(queue == null) queue = f;
		else queue = queue.andThen(f);
	}
	public static synchronized void runQueuedFuncs(){
		final Function runNow = queue;
		if(runNow != null){
			queue = null;
			runNow.apply(null); //TODO see comment on queue var
		}
	}
	
	public static final File rootDir = new File(Files.dirWhereThisProgramStarted,"acyc");
	
	/** files named by Util.escapeName(anyShortString) */
	public static final File jsonRootDir = new File(rootDir,"json");
	
	/** For later version when mapacyc either replaces or optionally is used with the json mindmap */
	public static final File mapacycRootDir = new File(rootDir,"mapacyc");
	
	/** For later version when derive acyc64 (Torrent Like Acyc Part Packet, tlapp) from mapacyc */
	public static final File acyc64RootDir = new File(rootDir,"acyc64");
	
	public static final File jsonVarDir = new File(jsonRootDir,"var"); //vars of 1 value each
	//public static File jsonVervarDir = new File(jsonRootDir,"vervar"); //versioned vars
	//"TODO Is eventLog a vervar? Will I use vervar with json. Not directly, because I'm waiting on mapacyc for that, which is binary, but can put json in a maplist with key 'json'"
	//public static File jsonEventDir = new File(jsonRootDir,"event");
	//public static File jsonEventLog = new File(jsonEventDir,"eventLog.jsonperline");
	//private static final OutputStream streamToEventLog; //TODO verify: closed when jvm closes
	static{
		jsonVarDir.mkdirs();
		System.out.println("varDir/mindmap: "+jsonVarDir);
		//jsonVervarDir.mkdirs();
		//System.out.println("vervarDir(mindmap version history): "+jsonVervarDir);
		//jsonEventDir.mkdirs();
		//System.out.println("eventObjectLog: "+jsonEventDir);
		/*try{
			streamToEventLog = new FileOutputStream(jsonEventLog,true);
		}catch (FileNotFoundException e){
			throw new Err(e);
		}
		System.out.println("eventLog(TODO for automatic recovery from data corruption): "+jsonEventLog);
		*/
	}
	
	public static final File jsonLockFile = new File(jsonRootDir,"lockSoOnly1CopyOfTheProgCanBeOpenAtATime_writeLockingIsOnlyNeededForJsonVersionSinceMapacycVersionWillUseImmutableMerkleData.lock");
	
	public static final String rootName = "prilistPrilist";
	
	/** Returns from memory if exists. Else loads from file. Else creates new. */
	public static NavigableMap<String,Object> get(String name){
		NavigableMap<String,Object> node = listweb.get(name);
		if(node == null){
			//boolean createInMemory = ignoreSaveCommandForTesting || !fileOfJsonVar(name).exists();
			File f = fileOfJsonVar(name);
			boolean createInMemory = !f.exists();
			//lg("In GET, file="+f+" not exist so will create in memory, name="+name);
			if(createInMemory){
				node = new TreeMap();
				node.put("prilist", new ArrayList());
				node.put("def", "");
				//FIXME also connect symmetricly with rootName
				//if(!ignoreSaveCommandForTesting) setModified(name);
				listweb.put(name, node);
				if(Debug.logModified) lg("modified by get: "+name);
				setModified(name);
			}else{
				load(name);
				node = listweb.get(name);
				saveVervarIfItsTimeAndVarModified(name); //happens less often than save var file, so prog often closes without this
			}
		}
		return node;
	}
	
	public static boolean nameExists(String name){
		return listweb.containsKey(name) || fileOfJsonVar(name).isFile();
	}
	
	public synchronized static void saveChanges(){
		//if(ignoreSaveCommandForTesting) return;
		String m[] = modified.toArray(new String[0]);
		if(Debug.logModified) lg("saveChanges? modified: "+Arrays.asList(m));
		else lg("saveChanges? countModified="+m.length);
		modified.clear();
		NavigableSet<String> couldntSave = new TreeSet();
		try{
			for(String name : m){
				try{
					save(name);
				}catch(Exception e){
					lg("Couldnt save (1 or both files) "+name+" so putting it back in modified set to try soon");
					couldntSave.add(name);
				}
			}
		}finally{
			modified.addAll(couldntSave);
		}
	}
	
	private static final boolean mindmapBugFileContentsCachedByOsSoDoesntSaveThemWhenItShould = true;
	
	/** Saves Util.mindmap.get(name) to file */
	public static synchronized void save(String name){
		//if(ignoreSaveCommandForTesting) return;
		NavigableMap<String,Object> node = listweb.get(name);
		if(node == null) throw new RuntimeException("Name not exist in memory: "+name);
		File f = fileOfJsonVar(name);
		byte data[] = Text.stringToBytes(JsonDS.toJson(node));
		if(!f.exists() || (mindmapBugFileContentsCachedByOsSoDoesntSaveThemWhenItShould || f.length() != data.length && !Files.bytesEqual(Files.read(f),data))){
			Files.overwrite(data, f);
		}else{
			lg("Not saving var file for name="+name+" because file content equals memory");
		}
		saveVervarIfItsTimeAndVarModified(name);
	}
	
	private static void saveVervarIfItsTimeAndVarModified(String name){
		//block of time theres allowed to be only 1 vervar save
		double now = Time.time();
		double blockStart = now - now%vervarSaveInterval();
		double v = lastVarSaveTime(name);
		double vv = lastVervarSaveTime(name);
		if(vv < blockStart && vv<v-5){
			File f = fileOfJsonVervar(name);
			String json = Util.jsonToSingleLine(JsonDS.toJson(eventViewOfVar(name)));
			byte data[] = Text.stringToBytes(json+"\r\n");
			Files.append(data, f);
			//lg("Saved version of "+name+" time="+Time.time());
		}
	}
	
	/** -Infinity if never saved. Var, not vervar. */
	public static double lastVarSaveTime(String name){
		File f = fileOfJsonVar(name);
		return f.isFile() ? f.lastModified()*.001 : -1./0;
	}
	
	/** -Infinity if never saved. Vervar, not var. */
	public static double lastVervarSaveTime(String name){
		File f = fileOfJsonVervar(name);
		return f.isFile() ? f.lastModified()*.001 : -1./0;
	}
	
	public static NavigableMap<String,Object> load(String name){
		File f = fileOfJsonVar(name);
		if(f.isDirectory()) throw new Err("For name="+name+" need to create file="+f+" but its a dir");
		if(!f.isFile()) throw new RuntimeException("File not exist ["+f+"] for name["+name+"]");
		NavigableMap<String,Object> node = (NavigableMap<String,Object>) JsonDS.parse(Text.bytesToString(Files.read(f)));
		namesCache.add(name); //TODO Whats the smallest set of places this could be and catch all the names?
		namesCache.addAll((List<String>)node.get("prilist")); //TODO Whats the smallest set of places this could be and catch all the names?
		Object prevValue = listweb.put(name,node);
		//lg("load("+name+") prevValue="+prevValue);
		if(prevValue != null){
			if(Debug.logModified) lg("modified by load: "+name);
			setModified(name);
		}
		fireListeners(name);
		return node;
	}
	
	/** creates empty Set<Consumer<String>> if not exist */
	protected static Set<Consumer<String>> listeners(String name){
		Set<Consumer<String>> set = mapStringToSetOfListener.get(name);
		if(set == null){
			set = new HashSet();
			mapStringToSetOfListener.put(name, set);
		}
		return set;
	}
	
	/** Creates empty Set<String> if not exist */
	protected static Set<String> listenees(Consumer<String> listener){
		Set<String> set = mapListenerToSetOfString.get(listener);
		if(set == null){
			set = new HashSet();
			mapListenerToSetOfString.put(listener, set);
		}
		return set;
	}
	
	/** Fires 1 event at listener for this name immediately to cover what was missed while not listening,
	and for all later changes.
	*/
	public static synchronized void startListening(Consumer<String> listener, String name){
		startListeningWithoutInstantEvent(listener, name);
		listener.accept(name); //first event
	}
	
	public static synchronized void startListeningWithoutInstantEvent(Consumer<String> listener, String name){
		if(Debug.logStartsAndStopsOfListening) lg("Start LISTEN to "+name+" WHO="+listener);
		listeners(name).add(listener);
		listenees(listener).add(name);
	}
	
	public static synchronized void stopListening(Consumer<String> listener, String name){
		if(Debug.logStartsAndStopsOfListening) lg("Stop LISTEN to "+name+" WHO="+listener);
		listeners(name).remove(listener);
		listenees(listener).remove(name);
		//TODO remove up to 1 empty set from both maps
	}
	
	public static synchronized void stopListening(Consumer<String> listener){
		String names[] = listenees(listener).toArray(new String[0]);
		for(String name : names) stopListening(listener, name);
	}
	
	public static synchronized void stopListening(String name){
		Consumer<String> listeners[] = listeners(name).toArray(new Consumer[0]);
		for(Consumer<String> listener : listeners) stopListening(listener, name);
	}
	
	public static File fileOfJsonVar(String name){
		return new File( new File(jsonVarDir,Util.escapeName(name.substring(0,1))),
			Util.escapeName(name)+".json" );
	}
	
	public static File fileOfJsonVervar(String name){
		return new File( new File(jsonVarDir,Util.escapeName(name.substring(0,1))),
			Util.escapeName(name)+".jsonperline" );
	}
	
	/** returns true if changed */
	static boolean putAtTopOfPrilist_noEvent(String parent, String child){
		NavigableMap<String,Object> node = get(parent);
		List prilist = (List) node.get("prilist");
		int i = prilist.indexOf(child);
		boolean change = i!=0;
		if(change){
			if(Debug.logModified) lg("modified by putAtTopOfPrilist_noEvent: "+parent);
			setModified(parent);
			if(i != -1) prilist.remove(i);
			prilist.add(0, child);
		}
		return change;
	}
	
	/** removes each from the other's prilist */
	public static void unpair(String x, String y){
		//remove and do event if they exist
		boolean xRemovedY = prilist(x).remove(y);
		boolean yRemovedX = prilist(y).remove(x);
		if(xRemovedY) onChange(x);
		if(yRemovedX) onChange(y);
	}
	
	/** moves if exists. else adds */
	public static void putAtTopOfPrilist(String parent, String child){
		boolean changedParent = putAtTopOfPrilist_noEvent(parent, child);
		//child is parent
		boolean changedChild = addToEndOfPrilistIfNotExist_noEvent(child, parent);
		if(changedParent) fireListeners(parent);
		if(changedChild) fireListeners(child);
	}
	
	/** returns true if changed */
	static boolean addToEndOfPrilistIfNotExist_noEvent(String parent, String child){
		NavigableMap<String,Object> node = get(parent);
		List prilist = (List) node.get("prilist");
		boolean change = !prilist.contains(child);
		if(change){
			prilist.add(child);
			//if(!ignoreSaveCommandForTesting) setModified(parent);
			if(Debug.logModified) lg("modified by addToEndOfPrilistIfNotExist_noEvent: "+parent);
			setModified(parent);
		}
		return change;
	}
	
	public static void addToEndOfPrilistIfNotExist(String parent, String child){
		boolean changedParent = addToEndOfPrilistIfNotExist_noEvent(parent, child);
		//child is parent
		boolean changedChild = addToEndOfPrilistIfNotExist_noEvent(child, parent);
		if(changedParent) fireListeners(parent);
		if(changedChild) fireListeners(child);
	}
	
	public static List prilist(String name){
		return (List) get(name).get("prilist");
	}
	
	/*private static void setModified(String name, double time){
		mindmap.get(name).put("modt", time);
		modified.add(name);
	}*/
	
	public static void setPrilist(String name, List prilist){
		NavigableMap<String,Object> node = get(name);
		if(Debug.logModified) lg("modified by setPrilist: "+name);
		setModified(name);
		node.put("prilist", prilist);
		fireListeners(name);
	}
	
	public static String def(String name){
		return (String) get(name).get("def");
	}
	
	public static void setDef(String name, String def){
		NavigableMap<String,Object> node = get(name);
		setModified(name);
		node.put("def", def);
		fireListeners(name);
	}
	
	/** Creates if not exist.
	The "views" field is a map of name2 (normally those in my prilist) to view options
	for seeing/editing name2 in context of name, such as name is a stack (JList)
	that comes with a prilist editor (JList) of the name selected in stack,
	and view options include scroll position and whats selected in name2.prilist.
	This allows the same name2 to be in multiple JLists at once,
	such as to view far parts of a big list and drag between them.
	*/
	public static NavigableMap<String,NavigableMap<String,Object>> views(String name){
		NavigableMap n = get(name);
		NavigableMap v = (NavigableMap) n.get("views");
		if(v == null){
			n.put("views", v = new TreeMap());
			fireListeners(name);
		}else{
			if(prilist(name).size()*2 < v.size()) removeViewsAboutNamesNotInPrilist(name);
		}
		return v;
	}
	
	public static NavigableMap<String,Object> view(String from, String to){
		NavigableMap<String,NavigableMap<String,Object>> views = views(from);
		NavigableMap<String,Object> view = views.get(to);
		if(view == null){
			views.put(to, view = new TreeMap());
			fireListeners(from); //Not an event for (name)to because its (name)from's data about (name)to
		}
		return view;
	}
	
	/** Similar to garbcol. This removes views that are not useful since they're not in prilist.
	This is called automaticly by views(String) funcs when theres more than twice as many views as prilist.
	*/
	public static void removeViewsAboutNamesNotInPrilist(String from){
		Set<String> p = new HashSet(prilist(from));
		//avoid infinite loop when views(String) calls this
		NavigableMap<String,NavigableMap<String,Object>> views = (NavigableMap) get(from).get("views");
		NavigableMap<String,Object> v = views.get(from);
		if(v != null){
			Iterator<String> iter = v.keySet().iterator();
			while(iter.hasNext()) if(!p.contains(iter.next())) iter.remove();
		}
	}	
	
	/** All names must connect to the name that is their first char, so they all are reachable at
	least 1 way. This func scans dirs in Root.jsonVarDir for names not yet in their first letter,
	such as "hello" and "h" in eachother's prilists. If x is in y's prilist, then y must be in x's prilist.
	The "%" dir contains names whose first char is escaped, including if their first char is actually "%".
	Also updates Root.rootName to contain all those chars. All updates keep their position if exist.
	*/
	public static void updateRootChars(){
		lg("Start updateRootChars reading filenames");
		SortedSet<String> rootCharNames = new TreeSet();
		for(String name : listweb.keySet().toArray(new String[0])){
			String rootCharName = name.substring(0,1);
			rootCharNames.add(rootCharName);
			addToEndOfPrilistIfNotExist(rootCharName, name);
		}
		for(File dir : jsonVarDir.listFiles()){
			if(dir.isDirectory()){
				String filenames[] = dir.list();
				Arrays.sort(filenames); //this software must be deterministic
				for(String filename : filenames){
					if(filename.endsWith(".json")){
						String name = Util.unescapeName(filename.substring(0, filename.length()-".json".length()));
						String rootCharName = name.substring(0,1);
						rootCharNames.add(rootCharName);
						//System.err.println("This is causing everything to be marked as modified since we havent loaded all the files (nor should we) so connecting them creates new prilist and def, which probably causes problem when GET them later since wont be loaded from file. This need some redesign.");
						addToEndOfPrilistIfNotExist(rootCharName, name);
					}
				}
			}			
		}
		for(String rootCharName : rootCharNames){
			addToEndOfPrilistIfNotExist(Root.rootName, rootCharName);
			putAtTopOfPrilist("firstCharNames", rootCharName);
		}
		putAtTopOfPrilist(Root.rootName, "firstCharNames");
		lg("End updateRootChars reading filenames");
	}
	
	public static boolean isFirstCharName(String name){
		if(name.length() == 1){
			if(Character.isSurrogate(name.charAt(0))) throw new Err(
				"Unclosed unicode low surrogate: (char)"+(int)name.charAt(0));
			return true;
		}else if(name.length() == 2){
			//FIXME updateRootChars and escapes dont work with this yet
			//FIXME Check for surrogates in wrong order or not matching other places in name,
			//such as high then low, and other places in this software.
			return Character.isLowSurrogate(name.charAt(0))
				&& Character.isHighSurrogate(name.charAt(1));
		}else{
			return false;
		}
	}
	
	public static void updateNameCacheFromContentsOfFirstChars(){
		prilist(rootName); //firstCharNames should be in rootName. Put them in namesCache
		for(String name : namesCache.toArray(new String[0])){
			if(isFirstCharName(name)){
				//Fast because normally loads file for name, but not the names in its prilist
				namesCache.addAll(prilist(name));
			}
		}
	}
	
	public static void set(String name, String keyInName, Object valueOrNull){
		NavigableMap m = get(name);
		//Some funcs rely on this func to call listeners every time, so caller should check if value changed if(Util.equals(m.get(keyInName),valueOrNull)) return; //set x to x
		m.put(keyInName, valueOrNull);
		fireListeners(name);
	}
	
	/** For saving to that name's vervar file. Its [time, name, value]. */
	public static List eventViewOfVar(String name){
		return Arrays.asList(Time.time(), name, get(name));
	}
	
	public static void sortPrilist(String name, Comparator<String> c){
		//TODO When mapacyc upgrade, everything will be immutable so will have to replace it instead of sorting mutable
		Collections.sort(prilist(name), c);
		onChange(name);
	}
	
	/*public static void appendToEventLogForName(String name){
		NavigableMap node = get(name);
		NavigableMap event = new TreeMap();
		event.put("time", Time.time()); //will be sorted before val, so near start of each line.
		event.put("key", name);
		event.put("val", node);
		//TODO If JsonDS supported null, setting val to null could mean delete (should it?),
		//but in this early version of the software there are no deletes.
		//Should the lack of a val key mean its deleted?
		appendToEventLog(event);
	}
	
	/*public static void appendToEventLog(NavigableMap jsonMap){
		String json = JsonDS.toJson(jsonMap);
		json = Util.jsonToSingleLine(json);
		appendLineToEventLog.accept(json);
	}
	
	private static final Consumer<String> appendLineToEventLog = (String line)->{
		if(line.contains("\r") || line.contains("\n")) throw new Err("Multiline["+line+"]");
		byte b[] = Text.stringToBytes(line);
		try{
			streamToEventLog.write(b);
			streamToEventLog.write('\r');
			streamToEventLog.write('\n');
		}catch(Exception e){
			throw new Err(e);
		}
	};*/
	
	static volatile boolean startedClosing;
	
	private static final Object inCaseTheEarlierGrimReaper_whoWhoMayHaveMoreAccessToFilesEtc_fails = new Object(){
		protected void finalize() throws Throwable{
			onClosingProg();
		}
	};
	
	/** Slow. Only needed during maintenance and renames.
	TODO in a later version, if total files get too big,
	divide into n subsets and only have 2 in memory at a time.
	*/
	public static void loadAllFiles(){
		updateRootChars();
		for(String name : listweb.keySet().toArray(new String[0])){
			get(name);
		}
	}
	
	public static synchronized void onClosingProg(){
		if(startedClosing) return;
		startedClosing = true;
		lg("Closing "+Util.progName);
		try{
			while(!modified.isEmpty()){ //if save fails, keep trying forever so person may come find the problem and solve it instead of losing their data
				try{
					saveChanges();
				}catch(Throwable t){
					t.printStackTrace(System.err);
					lg("Failed to save all, so cant close prog, time="+Time.time());
				}
			}
		}finally{
			if(jsonLockFile.isFile()) jsonLockFile.delete(); //in case kill prog instead of letting it die on its own
		}
	}
	
	public static void main(String[] args){
		String s = "$~x\\/:;\r\nhello";
		String s2 = Util.unescapeName(Util.escapeName(s));
		System.out.println("equals?="+s.equals(s2));
		listweb.put("test xyz", (NavigableMap) JsonDS.parse("{\"prilist\":[\"abc\", \"def\", 32.4], \"def\": \"blah blah\"}"));
		save("test xyz");
		System.out.println(load("test xyz"));
	}
	
	public static void clearStack(String stackName, String nameShouldBeAtStackFloor){
		List<String> backedStack = prilist(stackName);
		for(int i=backedStack.size()-1; i>0; i--){
			String name = backedStack.get(i);
			if(!name.equals(nameShouldBeAtStackFloor)){
				unpair(stackName, name);
			}
		}
		putAtTopOfPrilist(stackName, nameShouldBeAtStackFloor);
	}

}