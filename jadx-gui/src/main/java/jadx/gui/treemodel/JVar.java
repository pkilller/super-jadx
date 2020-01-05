package jadx.gui.treemodel;

import jadx.api.JavaNode;
import jadx.api.JavaVar;
import jadx.gui.utils.UiUtils;

import javax.swing.*;

public class JVar extends JNode {
	private static final long serialVersionUID = 1712572192106257359L;

	private final transient JavaVar var;
	private final transient JClass jParent;

	public JVar(JavaVar javaVar, JClass jClass) {
		this.var = javaVar;
		this.jParent = jClass;
	}

	@Override
	public JavaNode getJavaNode() {
		return var;
	}

	@Override
	public JClass getJParent() {
		return jParent;
	}

	@Override
	public JClass getRootClass() {
		return jParent.getRootClass();
	}

	@Override
	public int getLine() {
		return var.getDecompiledLine();
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String makeString() {
		return UiUtils.typeFormat(var.getName(), var.getType());
	}

	@Override
	public String makeLongString() {
		return UiUtils.typeFormat(var.getFullName(), var.getType());
	}

	@Override
	public int hashCode() {
		return var.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JVar && var.equals(((JVar) o).var);
	}
}
