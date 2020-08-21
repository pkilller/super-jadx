package jadx.api;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.VarInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.VarNode;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class JavaVar implements JavaNode {

	private JavaMethod mth;
	private final VarNode var;

	public JavaVar(JavaMethod jmth, VarNode var) {
		this.mth = jmth;
		this.var = var;
	}

	public VarNode getVarNode() {
		return var;
	}

	public void setMethod(JavaMethod mth) {
		this.mth = mth;
	}

	public JavaMethod getMethod() {
		return mth;
	}

	@Override
	public void setName(String name) {
		var.getVarInfo().setAlias(name);
	}

	@Override
	public String getName() {
		return var.getAlias();
	}

	@Override
	public String getFullName() {
		return mth.getFullName() + "()->" + getName() + ":" + getType();
	}

	@Override
	public String getRawFullName() {
		return var.getVarInfo().getRawFullId();
	}

	@Override
	public String getAliasFullName() {
		return var.getMethod().getMethodInfo().getAliasFullId() + "()->" + var.getAlias() + ":" + getType();
	}

	@Override
	public JavaClass getDeclaringClass() {
		return mth.getDeclaringClass();
	}

	@Override
	public JavaClass getTopParentClass() {
		return mth.getTopParentClass();
	}

	public AccessInfo getAccessFlags() {
		return mth.getAccessFlags();
	}

	public ArgType getType() {
		return ArgType.tryToResolveClassAlias(var.dex(), var.getType());
	}

	public int getDecompiledLine() {
		return var.getDecompiledLine();
	}

	@Override
	public int hashCode() {
		return var.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JavaVar && var.equals(((JavaVar) o).var);
	}

	@Override
	public String toString() {
		return var.toString();
	}
}
