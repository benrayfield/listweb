package listweb.ui;
import static humanaicore.common.CommonFuncs.lg;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import listweb.Debug;

public class AddTextRemove extends JPanel{
	
	protected final JTextField textfield;
	
	public final Consumer<String> onAdd, onRemove;
	
	public AddTextRemove(){
		this((String s)->{}, "", (String s)->{});
	}
	
	public AddTextRemove(Consumer<String> onAdd, String firstText, Consumer<String> onRemove){
		this.onAdd = onAdd;
		this.onRemove = onRemove;
		JButton remove = new JButton(new AbstractAction("-"){
			public void actionPerformed(ActionEvent e){
				doRemove();
			}
		});
		textfield = new JTextField(firstText);
		JButton add = new JButton(new AbstractAction("+"){
			public void actionPerformed(ActionEvent e){
				doAdd();
			}
		});
		setLayout(new BorderLayout());
		textfield.addKeyListener(new KeyListener(){
			public void keyTyped(KeyEvent e){}
			public void keyReleased(KeyEvent e){}
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_ENTER){
					doAdd();
				}
			}
		});
		add(add, BorderLayout.WEST);
		add(textfield, BorderLayout.CENTER);
		add(remove, BorderLayout.EAST);
	}
	
	public void write(String s){
		if(Debug.logSwingLock) lg("TREELOCK addTextRemove");
		synchronized(getTreeLock()){
			textfield.setText(s);
		}
		if(Debug.logSwingLock) lg("TREEUNLOCK addTextRemove");
	}
	
	public String read(){
		return textfield.getText();
	}
	
	public void doAdd(){
		System.out.println("Add "+read());
		onAdd.accept(textfield.getText());
		write(""); //Empty textfield when add its contents into prilist
	}
	
	public void doRemove(){
		System.out.println("Remove "+read());
		onRemove.accept(textfield.getText());
		//This would prevent next lower prilist item from appearing in textfield: write("");
	}

}
