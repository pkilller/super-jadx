package jadx.core.dex.nodes;

import com.android.dex.ClassData.Field;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.VarInfo;
import jadx.core.dex.instructions.args.ArgType;

// TODO： 还未修改完成
public class VarNode extends LineAttrNode implements ICodeNode {

	private  MethodNode mth;
	private final VarInfo varInfo;

	private ArgType type;

	public VarNode(MethodNode mth, String varName, ArgType argType) {
		VarInfo varInfo = new VarInfo(mth.getMethodInfo(), varName, argType);
		this.mth = mth;
		this.varInfo = varInfo;
		this.type = varInfo.getType();
	}

	public VarInfo getVarInfo() {
		return varInfo;
	}

	@Override
	public AccessInfo getAccessFlags() {
		return null;
	}

	@Override
	public void setAccessFlags(AccessInfo accFlags) {

	}

	public String getName() {
		return varInfo.getName();
	}

	public String getAlias() {
		return varInfo.getAlias();
	}

	public ArgType getType() {
		return type;
	}

	public void setType(ArgType type) {
		this.type = type;
	}

	public MethodNode getMethod() {
		return mth;
	}

	@Override
	public String typeName() {
		return "var";
	}

	@Override
	public DexNode dex() {
		return mth.getParentClass().dex();
	}

	@Override
	public RootNode root() {
		return mth.getParentClass().root();
	}

	@Override
	public int hashCode() {
		return varInfo.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		VarNode other = (VarNode) obj;
		return varInfo.equals(other.varInfo);
	}

	@Override
	public String toString() {
		return varInfo.getDeclMethod() + "." + varInfo.getName() + " :" + type;
	}
}
