package listweb.ui;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/** Set whole list at once with event at end, which DefaultListModel is very slow at. */
public class WholeListAtOnceModel implements ListModel{

	protected List immutableList = Collections.EMPTY_LIST;
	
	protected final Set<ListDataListener> listDataListeners = new HashSet();
	
	public int getSize(){
		return immutableList.size();
	}

	public Object getElementAt(int i){
		return immutableList.get(i);
	}

	public void addListDataListener(ListDataListener x){
		listDataListeners.add(x);
	}

	public void removeListDataListener(ListDataListener x){
		listDataListeners.remove(x);
	}
	
	public List getImmutableContent(){
		return immutableList;
	}
	
	/** copies list */
	public void setContent(List c){
		immutableList = Collections.unmodifiableList(new ArrayList(c));
		ListDataEvent e = new ListDataEvent("unknownWhichObjectItCameFrom",
			ListDataEvent.CONTENTS_CHANGED, 0, getSize()-1);
		for(ListDataListener x : listDataListeners){
			x.contentsChanged(e);
		}
	}

}
