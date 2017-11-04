package listweb.migration;

import static humanaicore.common.CommonFuncs.lg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
//import aod.Start;
import humanaicore.common.Files;
import humanaicore.common.Text;
import humanaicore.err.Err;
import listweb.Root;
import listweb.Util;

/** Mostly useful to Ben F Rayfield or anyone who happens to have HumanAiNet0.8 files.
Everyone else should start on the newer kind of files. This code will be removed eventually.
*/
public class MigrationFromOlderKindOfMindmapFiles{
	private MigrationFromOlderKindOfMindmapFiles(){}
	
	public static void main(String[] args) throws IOException{
		System.out.println(MigrationFromOlderKindOfMindmapFiles.class.getSimpleName()
			+". To avoid accidental big changes, starting which of these few things is not automatic. Type a command:");
		String command = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if("benRayfieldsImport2016FirstRun".equals(command)){
			benRayfieldsImport2016(false);
		}else if("benRayfieldsImport2016SecondRun".equals(command)){
			benRayfieldsImport2016(true);
		}else{
			System.out.println("Doing nothing.");
		}
	}

	/** secondRun==false imported but because of symmetry of existence in prilists,
	x being at the top of y's prilist meant y could be put near the top of x's prilist,
	depending which is imported first. After adding some new data then noticing that,
	I'm going to use secondRun==true to sort them. 
	*/
	public static void benRayfieldsImport2016(boolean secondRun){
		String paths[] = {
			//a 1 time import of my old mindmaps into the new system. Everyone else will probably
			//start on the new system. I've been using this kind of mindmap for 3 years (since 2013)
			//and its become a very important tool for me since my mind cant hold all this --Ben F Rayfield
			"c:/q25x/humanainet/todo/benfrayfieldTodo.humanainet",
			"c:/q25x/humanainet/mind/benfrayfieldMind.humanainet",
			"c:/q25x/humanainet/research/benfrayfieldResearch.humanainet",
			"c:/q25x/humanainet/talk/benfrayfieldTalk.humanainet",
			"c:/q25x/humanainet/file/benfrayfieldFile.humanainet",
			"c:/q25x/humanainet/public/benfrayfieldPublic.humanainet",
			"c:/q25x/humanainet/soc16/benfrayfieldSocrates2016.humanainet",
			"c:/Q24PathsNeedShortening/humanainet/socrates201411/benfrayfieldSocrates201411.humanainet",
			"c:/Q24PathsNeedShortening/humanainet/occam/benfrayfieldOccam.humanainet",
			"c:/q25x/humanainet/game/benfrayfieldGame.humanainet",
			"c:/q25x/humanainet/immut/benfrayfieldImmut.humanainet",
			"c:/q25x/humanainet/mousemoveai/benfrayfieldMousemoveai.humanainet",
			"c:/q25x/humanainet/Aod/benfrayfieldAod.humanainet",
			"c:/q25x/humanainet/lstm/benfrayfieldLstm.humanainet",
			"c:/q25x/humanainet/xorlisp/benfrayfieldXorlisp.humanainet",
			"c:/q25x/humanainet/meme/benfrayfieldMeme.humanainet",
			//others q25
			"c:/q25x/humanainet/politic/benfrayfieldPolitic.humanainet",
			"c:/q25x/humanainet/ethereum/benfrayfieldEthereum.humanainet",
			"c:/q25x/humanainet/tool/benfrayfieldTool.humanainet",
			"c:/q25x/humanainet/selfmodhtml/benfrayfieldSelfmodhtml.humanainet",
			"c:/q25x/humanainet/pow/benfrayfieldPow.humanainet",
			"c:/q25x/humanainet/mmgpaint/benfrayfieldMmgpaint.humanainet",
			"c:/q25x/humanainet/niche/benfrayfieldNiche.humanainet",
			"c:/q25x/humanainet/onion/benfrayfieldOnion.humanainet",
			"c:/q25x/humanainet/ipfs/benfrayfieldIpfs.humanainet",
			"c:/q25x/humanainet/job/benfrayfieldJob.humanainet",
			"c:/q25x/humanainet/entropy/benfrayfieldEntropy.humanainet",
			"c:/q25x/humanainet/biz/benfrayfieldBiz.humanainet",
			"c:/q25x/humanainet/brainporn/brainporn.humanainet",
			//others q24
			"c:/Q24PathsNeedShortening/humanainet/physics/benfrayfieldPhysics.humanainet",
			"c:/Q24PathsNeedShortening/humanainet/mvos (multiverseOS)/benfrayfieldMvos.humanainet",
			"c:/Q24PathsNeedShortening/humanainet/humanainetIntro/humanainetIntro.humanainet",
			"c:/Q24PathsNeedShortening/humanainet/humanianity/benfrayfieldHumanianity.humanainet",
			"c:/Q24PathsNeedShortening/humanainet/brains/benfrayfieldBrains.humanainet",
			"c:/Q24PathsNeedShortening/humanainet/debate/benfrayfieldDebate.humanainet",
			"c:/Q24PathsNeedShortening/humanainet/places/benfrayfieldPlace.humanainet",
			"c:/Q24PathsNeedShortening/humanainet/code/benfrayfieldCode.humanainet"
		};
		Map<String,List<String>> partialPrilists = new HashMap(); //used if secondRun
		Map<String,Set<String>> partialPrilistsAsSets = new HashMap(); //used if secondRun
		if(new HashSet(Arrays.asList(paths)).size() != paths.length)
			throw new Err("Duplicate path in: "+Arrays.asList(paths));
		for(String path : paths){
			if(!new File(path).isFile()) throw new Err("File not exist: "+path);
		}
		SortedSet<String> thingsToAddToPrilistPrilistAtEnd = new TreeSet();
		for(String path : paths){
			lg("importOldMindmap "+path);
			final String mindmapName = path.replaceAll(".*(/|\\\\)","").replace(".humanainet","");
			//UnaryOperator<String> defChanger =
			//	name->name.replaceAll("benfrayfield[A-Z][a-z]{3,15}\\.","").replace(" ... ","\r\n\r\n");
			UnaryOperator<String> defChanger = def->def.replace(" ... ","\r\n\r\n");
			
			//TODO
			//lgToUser("Import new files into startsWithA startsWitha startsWith+ etc, so changes arent so huge in files.");
			//lgToUser("windows cant store filename and Filename as 2 separate files");
			//lgToUser("what caused the ' ' mindmapItemName? Was it removing 'benfrayield[A-Z][a-z]{3,15}\\.'?");
			//lgToUser("Change prilistPrilist to the instance of mindmapName");
			//lgToUser("Add instance of mindmapName to prilistPrilist");
			//lgToUser("must parse def because def must use renamer because its not enough to remove 'benfrayield[A-Z][a-z]{3,15}\\.' since renamer also limits length");
			//lgToUser("What order should I import my old mindmaps?");
			//lgToUser("Append all changes to jsonLog, 1 json object per line, so mindmap can be rebuilt after errors, or mostly rebuilt even if theres errors in the jsonLog, using the last json object for each name.");
			
			UnaryOperator<String> renamer = (String name)->{
				//lg("Name: "+name);
				boolean addToPrilistPrilistAtEnd = false;
				if(name.charAt(0) == 'b'){
					//merging names like benfrayfieldTodo.nashEquilibrium
					//and benfrayfieldResearch.nashEquilibrium both to nashEquilibrium
					//lg("Name: "+name);
					String name2 = name.replaceAll("benfrayfield[A-Z][a-z]{3,15}\\.","");
					if(!name2.isEmpty()) name = name2;
					//lg("Name: "+name);
				}
				if(name.matches("benfrayfield[A-Z][a-z]{3,15}")){ //name of a mindmap, usually from another mindmap
					//lg("Name: "+name);
					name = "pointerAtMindmap_"+name;
					//lg("Name: "+name);
					addToPrilistPrilistAtEnd = true;
				}
				if("prilistPrilist".equals(name)){
					//Merging multiple old mindmaps. Each has a prilistPrilist. Rename that to mindmap's name.
					//The mindmap's name exists as a name in multiple mindmaps, so that will be merged.
					//Every name in old mindmaps that starts with "prilist" are sorted first,
					//so merge order between those 2 is correct within same mindmap,
					//but merge order would not be correct across multiple old mindmaps.
					//So have already renamed those.
					return mindmapName;
				}
				if(name.startsWith("prilist") && !name.equals("prilist") && !name.equals("prilists")
						&& !name.equals("prilistBasedMindmap") && !name.equals("prilistStack")){
					//merge prilistX with x, for all x
					//lg("Name: "+name);
					name = Character.toLowerCase(name.charAt(7))+name.substring(8);
					//lg("Name: "+name);
				}
				if(Util.charsLenOfEscapedName(name) > Root.maxEscapedNameLen){
					//lg("Name: "+name);
					//... is escaped to 9 bytes because of filenames . and ..
					if(name.length() > Root.maxEscapedNameLen-9){
						name = name.substring(0,Root.maxEscapedNameLen-9);
					}
					//TODO optimize by looping from end and only doing substring once
					while(Util.charsLenOfEscapedName(name) > Root.maxEscapedNameLen-9
							|| Character.isLowSurrogate(name.charAt(name.length()-1))){ //dont split unicode surrogate pair at end
						name = name.substring(0,name.length()-1);
						//lg("Popped char, name="+name);
					}
					name = name+"...";
					//lg("Name: "+name);
				}
				if(addToPrilistPrilistAtEnd) thingsToAddToPrilistPrilistAtEnd.add(name);
				return name;
			};
			String fileContent = Text.bytesToString(Files.read(new File(path)));
			if(secondRun){ //second run will sort prilists using partialPrilists each as a Comparator<String>
				updateComparators(renamer, fileContent, partialPrilists, partialPrilistsAsSets);
			}else{ //first run imports
				importOldMindmap(defChanger, renamer, fileContent);
				Root.addToEndOfPrilistIfNotExist(Root.rootName, mindmapName);
			}
			//TODO split big prilists which usually includes mindmapName (was prilistPrilist).
			//Should that be a separate func called after the import?
		}
		if(secondRun){
			Map<String,Comparator<String>> comparators = new HashMap();
			for(Map.Entry<String,List<String>> entry : partialPrilists.entrySet()){
				comparators.put(
					entry.getKey(),
					new Comparator<String>(){ //compare by position in list, and all others equal and earliest
						final List list = entry.getValue();
						public int compare(String x, String y){
							return list.indexOf(x)-list.indexOf(y); //-1 if not found sorts correct
						}
					}
				);
			}
			lg("Sorting...");
			//Root.sortPrilist("benfrayfieldTodo", comparators.get("benfrayfieldTodo"));
			for(String name : comparators.keySet()){
				lg("Sorting prilist of "+name);
				Root.sortPrilist(name, comparators.get(name));
			}
		}
		//if(1<2) throw new Todo();
		Root.updateRootChars();
		for(String name : thingsToAddToPrilistPrilistAtEnd){
			Root.addToEndOfPrilistIfNotExist(Root.rootName, name);
		}
		Root.saveChanges();
		//System.out.println("Prilist of lsTimeSequential: "+Root.prilist("lsTimeSequential"));
		//System.out.println("Def of lsTimeSequential: "+Root.def("lsTimeSequential"));
	}
	
	/** defChanger is first step on def. Renames in def and prilist. Prefixes def with oldName=theOldName\r\n if renamed. */
	public static void importOldMindmap(
			UnaryOperator<String> defChanger, UnaryOperator<String> renamer, String content){
		try{
			BufferedReader br = new BufferedReader(new StringReader(content));
			String line;
			String name = null;
			String appendFullOldNameToDef = null;
			while((line = br.readLine()) != null){
				//lg("Line: "+line);
				line = line.trim();
				if(line.equals("")){
					name = null;
				}else if(name == null){
					if(Text.splitByWhitespaceNoEmptyTokens(line).length != 1) throw new Err("Not a name: "+line);
					name = renamer.apply(line);
					if(!name.equals(line)){
						lg("ReNAME "+line+" to "+name);
					}
					appendFullOldNameToDef = name.equals(line) ? null : line;
				}else{
					if(line.startsWith(".def[") && line.endsWith("]")){
						line = line.replace(" ... ", "\r\n\r\n");
						String defStr = line.substring(".def[".length(), line.length()-"]".length()).trim();
						defStr = defChanger.apply(defStr);
						defStr = renameInDef(renamer, defStr);
						if(appendFullOldNameToDef != null) defStr = "oldName="+appendFullOldNameToDef+"\r\n"+defStr;
						if(!defStr.equals("")) addToDef(name, "\r\n\r\n--defsMergedAboveAndBelow--\r\n\r\n", defStr);
					}else if(line.startsWith(".prilist[") && line.endsWith("]")){
						String prilistStr = line.substring(".prilist[".length(), line.length()-"]".length());
						String prilist[] = Text.splitByWhitespaceNoEmptyTokens(prilistStr);
						for(String childName : prilist){
							childName = renamer.apply(childName);
							Root.addToEndOfPrilistIfNotExist(name, childName);
						}
					}else throw new Err("Not a def or prilist line: "+line);
				}
				if(Root.listweb.size()%1000==0) lg("Mindmap size: "+Root.listweb.size());
			}
		}catch(Exception e){
			throw new Err(e);
		}
	}
	
	public static void updateComparators(UnaryOperator<String> renamer, String content,
			Map<String,List<String>> partialPrilists, Map<String,Set<String>> partialPrilistsAsSets){
		try{
			BufferedReader br = new BufferedReader(new StringReader(content));
			String line;
			String name = null;
			//String appendFullOldNameToDef = null;
			while((line = br.readLine()) != null){
				line = line.trim();
				if(line.equals("")){
					name = null;
				}else if(name == null){
					if(Text.splitByWhitespaceNoEmptyTokens(line).length != 1) throw new Err("Not a name: "+line);
					name = renamer.apply(line);
					if(!name.equals(line)){
						lg("ReNAME "+line+" to "+name);
					}
					//appendFullOldNameToDef = name.equals(line) ? null : line;
				}else{
					if(line.startsWith(".def[") && line.endsWith("]")){
						//ignore defs. Only reorder prilists.
					}else if(line.startsWith(".prilist[") && line.endsWith("]")){
						String prilistStr = line.substring(".prilist[".length(), line.length()-"]".length());
						String prilist[] = Text.splitByWhitespaceNoEmptyTokens(prilistStr);
						for(String childName : prilist){
							childName = renamer.apply(childName);
							
							if(!partialPrilistsAsSets.containsKey(name)){
								partialPrilists.put(name, new ArrayList());
								partialPrilistsAsSets.put(name, new HashSet());
							}
							if(!partialPrilistsAsSets.containsKey(childName)){
								partialPrilists.put(childName, new ArrayList());
								partialPrilistsAsSets.put(childName, new HashSet());
							}
							if(!partialPrilistsAsSets.get(name).contains(childName)){
								partialPrilists.get(name).add(childName);
								partialPrilistsAsSets.get(name).add(childName);
							}
							
							//Root.addToEndOfPrilistIfNotExist(name, childName);
						}
					}else throw new Err("Not a def or prilist line: "+line);
				}
			}
		}catch(Exception e){
			throw new Err(e);
		}
	}
	
	public static String renameInDef(UnaryOperator<String> renamer, String def){
		List<String> tokens = Text.parseNaturalLanguageAndSymbolsUrlsAndNumbers(def);
		//lg("Def: "+def);
		//lg("Def tokens: "+tokens);
		StringBuilder sb = new StringBuilder();
		for(String token : tokens){
			if(token.trim().isEmpty()){
				sb.append(token);
			}else{
				String r = renamer.apply(token);
				if(!r.equals(token)){
					//lg("Renamed "+token+" to "+r);
					if(token.startsWith("/") || token.toLowerCase().startsWith("c:") || Text.isURL(token)){
						sb.append("( "+token+" )"); //also include full length in parans if its a file path or url
						//lg("also include full length in parans if its a file path or url: "+token);
					}
				}
				sb.append(r);
			}
		}
		String newDef = sb.toString();
		/*if(!def.equals(newDef)){
			lg("Def: "+def);
			lg("Def tokens: "+tokens);
			lg("Changed def: "+newDef);
		}*/
		return newDef;
	}
	
	/** If merging multiple defs, adds sep between them, else just sets it to def */
	public static void addToDef(String parent, String sep, String def){
		String oldDef = Root.def(parent);
		Root.setDef(parent, oldDef.equals("") ? def : oldDef+sep+def);
		//Root.fireListeners(parent);
	}

}
