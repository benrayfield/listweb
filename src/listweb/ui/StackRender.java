package listweb.ui;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;

import listweb.Root;

public class StackRender implements ListCellRenderer{
	
	public final String stackName;
	
	public final Color background, textActive, textSelected, textGhost;
	
	public final boolean reverse;
	
	public StackRender(String stackName, boolean reverse, Color background, Color textActive, Color textSelected, Color textGhost){
		this.stackName = stackName;
		this.reverse = reverse;
		this.background = background;
		this.textActive = textActive;
		this.textSelected = textSelected;
		this.textGhost = textGhost;
	}
	
	public Component getListCellRendererComponent(
			JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
		String name = (String)value;
		int selectedIndex = list.getSelectedIndex();
		boolean selected = index==selectedIndex;
		boolean ghost = selectedIndex==-1 || (reverse ? index<selectedIndex : index>selectedIndex);
		JLabel label = new JLabel(name);
		//JLabel label = new JLabel("<html>"+Util.escapeToAppearAsPlainTextInHtml(name)+"</html>");
		/*JTextArea label = new JTextArea(name);
		label.setEditable(false);
		label.setFocusable(false);
		label.setLineWrap(true);
		label.setWrapStyleWord(false);
		*/
		label.setBackground(background);
		label.setForeground(selected ? textSelected : (ghost ? textGhost : textActive));
		return label;
	}

}
