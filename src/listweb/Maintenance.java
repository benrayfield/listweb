package listweb;
import static humanaicore.common.CommonFuncs.*;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/** TODO automaticly schedule maintenance for 3am any day it hasnt been done for a week,
or maybe should offer 2 ways to close the program: instant and afterMaintenance.
*/
public class Maintenance{
	private Maintenance(){}
	
	/** Expensive op, may take minutes or hours depending on how much data you have.
	Call this sometimes to try to fix possible dataCorruption as best it can be fixed,
	especially x being in y's prilist but y not being in x's prilist.
	Does not (TODO yet?) do anything about vervar files that may contain unparseable lines.
	*/
	public static void maintenance(){
		loadAndSaveAllVarFilesToMakeExistenceInPrilistSymmetric();
	}
	
	static boolean startedMakeSymmetric;
	
	/** If x is in y's prilist, y must be in x's prilist, and can differ in list position.
	If any are found missing, adds them at bottom of relevant prilist.
	*/
	public static synchronized void loadAndSaveAllVarFilesToMakeExistenceInPrilistSymmetric(){
		if(startedMakeSymmetric) return;
		try{
			startedMakeSymmetric = true;
			lg("START loadAndSaveAllVarFilesToMakeExistenceInPrilistSymmetric...");
			Root.loadAllFiles();
			SortedSet<String> namesInNonsymmetricPairs = new TreeSet();
			long countChanges = 0;
			for(String name : Root.listweb.keySet()){
				lg("Checking "+name);
				for(String name2 : (List<String>)Root.prilist(name)){ //name.prilist contains name2
					if(!Root.prilist(name2).contains(name)){ //name2.prilist not contains name
						countChanges++;
						namesInNonsymmetricPairs.add(name);
						namesInNonsymmetricPairs.add(name2);
						lg("Nonsymmetric prilist, add "+name+" to "+name2+"'s prilist");
						Root.addToEndOfPrilistIfNotExist(name2, name);
					}
				}
			}
			lg("END loadAndSaveAllVarFilesToMakeExistenceInPrilistSymmetric, made "+countChanges
				+" changes in these "+namesInNonsymmetricPairs.size()+" names: "+namesInNonsymmetricPairs);
		}finally{
			startedMakeSymmetric = false;
		}
	}

}
