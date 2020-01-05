package jadx.gui.ui;

import jadx.api.*;
import jadx.gui.ui.codearea.RenameAction;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.List;

public class RenameDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 5763493590584039096L;
	RenameAction mAction;
	JTextField mNewName;
	JTextField mOrigName;
	JavaNode node;

	public RenameDialog(RenameAction action, String nodeName, JavaNode node) {
		initUI(nodeName, node);
		this.mAction = action;
	}

	public final void initUI(String nodeName, JavaNode node) {
		Font font = new Font("Serif", Font.BOLD, 13);
		this.node = node;

		URL logoURL = getClass().getResource("/logos/jadx-logo-48px.png");
		Icon logo = new ImageIcon(logoURL, "jadx logo");

		JLabel labelTargetLabel = new JLabel("Target: ");
		labelTargetLabel.setFont(font);
		labelTargetLabel.setAlignmentX(0.5f);

		JLabel labelTarget = new JLabel(nodeName);
		labelTarget.setFont(font);
		labelTarget.setAlignmentX(0.5f);

		JLabel labelOrigName = new JLabel("Original: ");
		labelOrigName.setFont(font);
		labelOrigName.setAlignmentX(0.5f);
		mOrigName = new JTextField();
		mOrigName.setFont(font);
		mOrigName.setAlignmentX(0.5f);
		mOrigName.setColumns(30);

		JLabel labelNewName = new JLabel("New: ");
		labelNewName.setFont(font);
		labelNewName.setAlignmentX(0.5f);
		mNewName = new JTextField();
		mNewName.setFont(font);
		mNewName.setAlignmentX(0.5f);
		mNewName.setColumns(30);

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(labelTargetLabel);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(labelTarget);

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(labelOrigName);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(mOrigName);

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(labelNewName);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(mNewName);

		JButton ok = new JButton(NLS.str("tabs.ok"));
		ok.addActionListener(this);
		ok.setAlignmentX(0.5f);

		Container contentPane = getContentPane();
		contentPane.add(panel, BorderLayout.PAGE_START);
		contentPane.add(ok, BorderLayout.PAGE_END);

		UiUtils.setWindowIcons(this);

		setModalityType(ModalityType.APPLICATION_MODAL);

		setTitle("Rename: " + nodeName);
		pack();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);

		initNameField();

	}

	void initNameField() {
		String newname = null;
		String origname = null;
		if (node instanceof JavaMethod) {
			newname = ((JavaMethod)node).getMethodNode().getMethodInfo().getAlias();
			origname = ((JavaMethod)node).getMethodNode().getMethodInfo().getName();
		} else if (node instanceof JavaField) {
			newname = ((JavaField)node).getFieldNode().getFieldInfo().getAlias();
			origname = ((JavaField)node).getFieldNode().getFieldInfo().getName();
		} else if (node instanceof JavaClass) {
			newname = ((JavaClass)node).getClassNode().getClassInfo().getAliasShortName();
			origname = ((JavaClass)node).getClassNode().getClassInfo().getShortName();
			for (CodePosition codePos : ((JavaClass)node).getUsageMap().keySet()) {
				JavaNode _node = ((JavaClass)node).getUsageMap().get(codePos);
			}
		} else if (node instanceof JavaVar) {
			newname = ((JavaVar)node).getVarNode().getVarInfo().getAlias();
			origname = ((JavaVar)node).getVarNode().getVarInfo().getName();
		}
		mNewName.setText(newname);
		mOrigName.setEditable(false);

		mOrigName.setText(origname);
		mNewName.requestFocus(true);
		mNewName.selectAll();
	}

	void setNewName(JavaNode node, String newName) {
		if (node instanceof JavaClass) {
			mAction.setDeobfuscatName(node, newName);
		} else if (node instanceof JavaMethod) {
			// rename override methods
			if (!((JavaMethod)node).getMethodNode().isConstructor() && ((JavaMethod)node).getMethodNode().isVirtual()) {
				List<JavaMethod> overrideMethods = mAction.getOverrideMethods(((JavaMethod)node));
				if (overrideMethods != null) {
					for (JavaMethod mth : overrideMethods) {
						mAction.setDeobfuscatName(mth, newName);
					}
				}
			} else {
				mAction.setDeobfuscatName(node, newName);
			}
		} else if (node instanceof JavaField) {
			mAction.setDeobfuscatName(node, newName);
		} else if (node instanceof JavaVar) {
			mAction.setDeobfuscatName(node, newName);
		}

		mAction.reGenerateClassesCode();
	}

	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		String newName = mNewName.getText();
		setNewName(node, newName);
		dispose();
	}
}
