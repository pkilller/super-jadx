package jadx.api;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.VarInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.VarNode;

import javax.annotation.Nullable;
import java.util.Map;

///  TODO:  还没修改完成， 从JavaFIeld修改而来
public final class JavaVar implements JavaNode {

	private JavaMethod mth;
	private final VarNode var;
	private final MethodNode mthN;

	public JavaVar(JavaMethod jmth, MethodNode mthNode, VarNode var) {
		this.mth = jmth;
		this.var = var;
		this.mthN = mthNode;
	}

	private void initJavaMethod() {
		if (mth == null) {
			mth = JadxDecompiler.instance.getJavaMethodByNode(mthN);
		}
	}

	public VarNode getVarNode() {
		return var;
	}

	public void setMethod(JavaMethod mth) { initJavaMethod(); this.mth = mth; }

	public JavaMethod getMethod() { initJavaMethod(); return mth;}

	@Override
	public String getName() {
		return var.getAlias();
	}

	@Override
	public String getFullName() {
		initJavaMethod();
		return mth.getFullName() + "()->" + getName() + ":" + getType();
	}

	@Override
	public String getRawFullName() {
		return var.getVarInfo().getRawFullId();
	}

	@Override
	public JavaClass getDeclaringClass() {
		initJavaMethod();
//		if (mth == null) {
//
//			Map<MethodNode, JavaMethod> mths = JadxDecompiler.instance.getMethodsMap();
//			for (MethodNode _mth : mths.keySet()) {
//				if (_mth.getMethodInfo().getRawFullId().equals(mthN.getMethodInfo().getRawFullId())) {
//					int aa = 1;
//				}
//			}
//			int a = 0;
//		}
		return mth.getDeclaringClass();
	}

	@Override
	public JavaClass getTopParentClass() {
		initJavaMethod();
		return mth.getTopParentClass();
	}

	public AccessInfo getAccessFlags() {
		initJavaMethod();
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
