package listweb.ui;
import java.util.function.Consumer;

import javax.swing.JTextArea;
import javax.swing.JTextField;

import listweb.Root;

/** listens (Consumer<String>) for changes to aSelectPtr.selectedName
and displays it noneditable. This is normally used above a DefPanel
andOr other editors of properties of the selectedName.
*/
public class NameFieldNoneditable extends JTextArea implements Consumer<String>{
	
	public final String selectPtr;
	
	public NameFieldNoneditable(String selectPtr){
		setLineWrap(true);
		setWrapStyleWord(false);
		this.selectPtr = selectPtr;
		setEditable(false);
		Root.startListening(this, selectPtr);
	}

	public void accept(String aSelectPtr){
		if(aSelectPtr.equals(selectPtr)){
			String selectedName = (String) Root.get(selectPtr).get("selectedName");
			setText(selectedName==null ? "" : selectedName);
		}
	}

}