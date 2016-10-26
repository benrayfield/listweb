package listweb.ui;
import static humanaicore.common.CommonFuncs.lg;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import listweb.Debug;
import listweb.Root;

/** listens (Consumer<String>) for changes to aSelectPtr.selectedName
and opens plain text editor for aSelectPtr.selectedName.def.
*/
public class DefPanel extends JPanel implements Consumer<String>, DocumentListener{
	
	public final String selectPtr;
	
	protected String selectedPtrSelectedName;
	
	//protected final JTextField tfName;
	protected final JTextArea taDef;
	
	protected final JScrollPane scroll;
	
	protected boolean listenToDocument = true;
	
	public DefPanel(String selectPtr){
		this.selectPtr = selectPtr;
		setLayout(new BorderLayout());
		//tfName = new JTextField();
		taDef = new JTextArea();
		taDef.setLineWrap(true);
		taDef.setWrapStyleWord(true);
		taDef.getDocument().addDocumentListener(this);
		//add(tfName, BorderLayout.NORTH);
		//tfName.setEditable(false);
		int v = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;
		int h = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
		add(scroll = new JScrollPane(taDef, v, h), BorderLayout.CENTER);
		Root.startListening(this, selectPtr);
	}

	public void accept(String aSelectPtr){
		if(aSelectPtr.equals(selectPtr)){
			listenToDocument = false;
			try{
				if(Debug.logSwingLock) lg("TREELOCK def");
				synchronized(getTreeLock()){
					String selectedName = (String) Root.get(selectPtr).get("selectedName");
					boolean diffName = selectedPtrSelectedName==null || !selectedPtrSelectedName.equals(selectedName);
					selectedPtrSelectedName = selectedName;
					if(selectedName == null){
						//tfName.setText("");
						taDef.setText("");
						taDef.setEnabled(false);
					}else{
						taDef.setEnabled(true);
						//tfName.setText(selectedName);
						String oldUiText = taDef.getText();
						String newDef = Root.def(selectedName);
						if(!oldUiText.equals(newDef)){
							taDef.setText(newDef);
							if(diffName){
								SwingUtilities.invokeLater(()->{
									scroll.getVerticalScrollBar().setValue(0);
								});
							}
						}
					}
				}
			}finally{
				listenToDocument = true;
				if(Debug.logSwingLock) lg("TREEUNLOCK def");
			}
		}
	}

	public void insertUpdate(DocumentEvent e){
		onEditDef();
	}

	public void removeUpdate(DocumentEvent e){
		onEditDef();
	}
	
	public void changedUpdate(DocumentEvent e){}
	
	protected void onEditDef(){
		if(listenToDocument && selectedPtrSelectedName != null){
			Root.setDef(selectedPtrSelectedName, taDef.getText());
		}
	}

}