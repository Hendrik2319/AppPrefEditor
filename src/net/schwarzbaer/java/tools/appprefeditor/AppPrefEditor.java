package net.schwarzbaer.java.tools.appprefeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.schwarzbaer.gui.StandardMainWindow;

public class AppPrefEditor {

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
//		int i = 0;
//		showPrefs(i++,Preferences.userNodeForPackage(net.schwarzbaer.java.tools.bumpmappingtest.BumpMappingTest.class));
//		//showPrefs(i++,Preferences.systemNodeForPackage(net.schwarzbaer.java.tools.bumpmappingtest.BumpMappingTest.class));
//		//showPrefs(i++,Preferences.userNodeForPackage(net.schwarzbaer.java.tools.bumpmappingtest.BumpMappingTest.PolarTextOverlay.class));
//		//showPrefs(i++,Preferences.userNodeForPackage(net.schwarzbaer.java.tools.bumpmappingtest.Dummy.class));
//		showPrefs(i++,Preferences.userRoot());
//		try {
//			showPrefs(i++,Preferences.systemRoot());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		new PreferencesView().createGUI(Preferences.userRoot(),"userRoot");
	}

	@SuppressWarnings("unused")
	private static void showPrefs(int index, Preferences prefs) {
		showPrefs(System.out, index, prefs);
	}
	private static void showPrefs(PrintStream out, int index, Preferences prefs) {
		out.printf("Preferences[%d]%n",index);
		showPrefs(out, prefs);
		out.println("[END]");
		out.println();
	}
	private static void showPrefs(PrintStream out, Preferences prefs) {
		if (prefs==null)
			out.printf("<null>%n");
		else {
			String[] keys = null;
			out.printf("Name: \"%s\"%n",prefs.name());
			out.printf("AbsolutePath: \"%s\"%n",prefs.absolutePath());
			out.printf("IsUserNode: %s%n",prefs.isUserNode());
			
			try { out.printf("ChildrenNames: %s%n",Arrays.toString(prefs.childrenNames())); }
			catch (BackingStoreException e) { out.printf("ChildrenNames: <BackingStoreException>%n"); e.printStackTrace(); }
			
			try { out.printf("Keys: %s%n",Arrays.toString(keys = prefs.keys())); }
			catch (BackingStoreException e) { out.printf("Keys: <BackingStoreException>%n"); e.printStackTrace(); }
			
			if (keys!=null)
				for (String key:keys) {
					String value = prefs.get(key,"<unknown value>");
					out.printf("    Value[\"%s\"]: \"%s\"%n", key, value);
				}
		}
	}
	
	private static String prefToString(Preferences prefs) {
		ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(byteArrayOS);
		showPrefs(out, prefs);
		out.flush();
		return byteArrayOS.toString();
	}

	static class PreferencesView {

		private StandardMainWindow mainwindow = null;
		private ValueListModel valueListModel = null;

		void createGUI(Preferences preferences, String rootLabel) {
			
			JTree tree = new JTree(new PreferencesTreeNode(null,rootLabel,preferences));
			JScrollPane treeScrollPane = new JScrollPane(tree);
			treeScrollPane.setBorder(BorderFactory.createTitledBorder("Preferences Tree"));
			treeScrollPane.setPreferredSize(new Dimension(300,500));
			
			JTextArea textArea = new JTextArea();
			JScrollPane textScrollPane = new JScrollPane(textArea);
			textScrollPane.setBorder(BorderFactory.createTitledBorder("Selected Preferences"));
			textScrollPane.setPreferredSize(new Dimension(300,300));
			
			JList<ValueListModel.Entry> valueList = new JList<>();
			JScrollPane listScrollPane = new JScrollPane(valueList);
			listScrollPane.setPreferredSize(new Dimension(300,200));
			
			JButton removeValueBtn,editValueBtn,renameValueBtn;
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill=GridBagConstraints.BOTH;
			c.weightx=0;
			buttonPanel.add(removeValueBtn = createButton("Remove Value",false, e->removeValue(valueList.getSelectedValue())),c);
			buttonPanel.add(  editValueBtn = createButton(  "Edit Value",false, e->  editValue(valueList.getSelectedValue())),c);
			buttonPanel.add(renameValueBtn = createButton("Rename Value",false, e->renameValue(valueList.getSelectedValue())),c);
			c.weightx=1;
			buttonPanel.add(new JLabel(),c);
			
			valueList.addListSelectionListener(e -> {
				ValueListModel.Entry selectedValue = valueList.getSelectedValue();
				removeValueBtn.setEnabled(selectedValue!=null);
				editValueBtn  .setEnabled(selectedValue!=null);
				renameValueBtn.setEnabled(selectedValue!=null);
			});
			
			tree.addTreeSelectionListener(e -> {
				PreferencesTreeNode selectedTreeNode = null;
				TreePath path = e.getPath();
				if (path!=null) {
					Object obj = path.getLastPathComponent();
					if (obj instanceof PreferencesTreeNode)
						selectedTreeNode = (PreferencesTreeNode) obj;
				}
				showValues(textArea,selectedTreeNode);
				setValues(valueList,selectedTreeNode);
				removeValueBtn.setEnabled(false);
				editValueBtn  .setEnabled(false);
				renameValueBtn.setEnabled(false);
			});
			
			JPanel valuePanel = new JPanel(new BorderLayout(3,3));
			valuePanel.setBorder(BorderFactory.createTitledBorder("Preferences Values"));
			valuePanel.add(listScrollPane,BorderLayout.CENTER);
			valuePanel.add(buttonPanel,BorderLayout.SOUTH);
			
			JSplitPane rightPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
			rightPanel.setTopComponent(textScrollPane);
			rightPanel.setBottomComponent(valuePanel);
			rightPanel.setResizeWeight(0.5);
			
			JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.setLeftComponent(treeScrollPane);
			contentPane.setRightComponent(rightPanel);
			contentPane.setResizeWeight(0.5);
			
			mainwindow = new StandardMainWindow("Application Preferences Editor");
			mainwindow.startGUI(contentPane);
			
			for (int i=0; i<tree.getRowCount(); i++)
				tree.expandRow(i);
		}

		private void renameValue(ValueListModel.Entry entry) {
			String newName = JOptionPane.showInputDialog(mainwindow, "Change name of \""+entry.key+"\":", entry.key);
			if (newName==null) return;
			valueListModel.changeName(entry,newName);
		}

		private void editValue(ValueListModel.Entry entry) {
			String newValue = JOptionPane.showInputDialog(mainwindow, "Set value of \""+entry.key+"\":", entry.value);
			if (newValue==null) return;
			valueListModel.changeValue(entry,newValue);
		}

		private void removeValue(ValueListModel.Entry entry) {
			int result = JOptionPane.showConfirmDialog(mainwindow, "Do you want to remove \""+entry.key+"\"?", "Remove Entry", JOptionPane.YES_NO_CANCEL_OPTION);
			if (result!=JOptionPane.YES_OPTION) return;
			valueListModel.remove(entry);
		}

		private JButton createButton(String title, boolean enabled, ActionListener al) {
			JButton comp = new JButton(title);
			if (al!=null) comp.addActionListener(al);
			comp.setEnabled(enabled);
			return comp;
		}
		
		private void setValues(JList<ValueListModel.Entry> valueList, PreferencesTreeNode treeNode) {
			if (treeNode!=null && treeNode.preferences!=null)
				try {
					valueListModel = new ValueListModel(treeNode.preferences,treeNode.preferences.keys());
					valueList.setModel(valueListModel);
					return;
				}
				catch (BackingStoreException e) {}
			valueListModel = null;
			valueList.setModel(new DefaultListModel<>());
		}

		private void showValues(JTextArea textArea, PreferencesTreeNode treeNode) {
			textArea.setText("");
			if (treeNode==null) return;
			if (treeNode.preferences==null)
				textArea.setText("defined as child in parent node\r\nbut has no preferences node");
			else
				textArea.setText(prefToString(treeNode.preferences));
		}
		
		private static class ValueListModel implements ListModel<ValueListModel.Entry> {
			
			private static class Entry {
				private String key,value;
				public Entry(String key, String value) {
					this.key = key;
					this.value = value;
				}
				@Override public String toString() { return String.format("Value[\"%s\"]: \"%s\"", key, value); }
				
			}
			
			private final Vector<ListDataListener> listDataListeners;
			private final Vector<Entry> entries;
			private final Preferences preferences;
			
			ValueListModel(Preferences preferences, String[] keys) {
				this.preferences = preferences;
				entries = new Vector<>();
				for (String key:keys) {
					String value = preferences.get(key,null);
					entries.add( new Entry(key,value) );
				}
				listDataListeners = new Vector<>(); 
			}

			public void changeName(Entry entry, String newName) {
				int i = entries.indexOf(entry);
				if (i<0) return;
				preferences.remove(entry.key);
				entry.key = newName;
				preferences.put(entry.key,entry.value);
				notifyListeners(i, ListDataEvent.CONTENTS_CHANGED, (l,e)->l.contentsChanged(e));
			}

			public void changeValue(Entry entry, String newValue) {
				int i = entries.indexOf(entry);
				if (i<0) return;
				entry.value = newValue;
				preferences.put(entry.key,entry.value);
				notifyListeners(i, ListDataEvent.CONTENTS_CHANGED, (l,e)->l.contentsChanged(e));
			}

			public void remove(Entry entry) {
				int i = entries.indexOf(entry);
				if (i<0) return;
				entries.remove(entry);
				preferences.remove(entry.key);
				notifyListeners(i, ListDataEvent.INTERVAL_REMOVED, (l,e)->l.intervalRemoved(e));
			}

			private void notifyListeners(int index, int eventType, BiConsumer<ListDataListener, ListDataEvent> listenerAction) {
				ListDataEvent e = new ListDataEvent(this, eventType, index,index);
				listDataListeners.forEach(l->listenerAction.accept(l,e));
			}

			@Override public int getSize() { return entries.size(); }
			@Override public Entry getElementAt(int index) { if (index<0 || entries.size()<=index) return null; return entries.get(index); }
			
			@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
			@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l); }
		}

		private static class PreferencesTreeNode implements TreeNode {

			private final PreferencesTreeNode parent;
			private final String name;
			private final Preferences preferences;
			private final Vector<PreferencesTreeNode> children;
			
			public PreferencesTreeNode(PreferencesTreeNode parent, String name, Preferences preferences) {
				this.parent = parent;
				this.name = name;
				this.preferences = preferences;
				children = new Vector<>();
				if (this.preferences!=null)
					try {
						String[] childrenNames = this.preferences.childrenNames();
						for (String childName:childrenNames) {
							Preferences pref = null;
							if (this.preferences.nodeExists(childName)) pref = this.preferences.node(childName);
							children.add(new PreferencesTreeNode(this,childName,pref));
						}
					} catch (BackingStoreException e) {
						e.printStackTrace();
					}
			}
			@Override public String toString() { return name; }

			@Override public TreeNode getChildAt(int childIndex) {
				if (childIndex<0 || children.size()<=childIndex) return null;
				return children.get(childIndex);
			}
			
			@SuppressWarnings("rawtypes")
			@Override public Enumeration children() { return children.elements(); }
			@Override public int getChildCount() { return children.size(); }
			@Override public TreeNode getParent() { return parent; }
			@Override public int getIndex(TreeNode node) { return children.indexOf(node); }
			@Override public boolean getAllowsChildren() { return true; }
			@Override public boolean isLeaf() { return children.isEmpty(); }
		}
	
	}
}
