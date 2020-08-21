package jadx.core.dex.info;

import com.android.dex.FieldId;
import jadx.core.codegen.TypeGen;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;

import java.util.Objects;

public final class VarInfo {

	private final MethodInfo declMethod;
	private final String name;
	private final ArgType type;
	private String alias;
	private boolean aliasFromPreset;

	public VarInfo(MethodInfo declMethod, String name, ArgType type) {
		this.declMethod = declMethod;
		this.name = name;
		this.type = type;
		this.alias = name;
	}


	public String getName() {
		return name;
	}

	public ArgType getType() {
		return type;
	}

	public MethodInfo getDeclMethod() {
		return declMethod;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public boolean hasAlias() {
		return !Objects.equals(name, alias);
	}

	public String getFullId() {
		return declMethod.getFullName() + "->" + alias + ':' + TypeGen.signature(type);
	}

	public String getRawFullId() {
		return declMethod.getRawFullId() + "->" + name + ':' + TypeGen.signature(type);
	}

	public boolean isRenamed() {
		return !name.equals(alias);
	}

	public boolean equalsNameAndType(VarInfo other) {
		return name.equals(other.name) && type.equals(other.type);
	}

	public void setAliasFromPreset(boolean value) {
		aliasFromPreset = value;
	}

	public boolean getAliasFromPreset() {
		return aliasFromPreset;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		VarInfo fieldInfo = (VarInfo) o;
		return name.equals(fieldInfo.name)
				&& type.equals(fieldInfo.type)
				&& declMethod.equals(fieldInfo.declMethod);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + type.hashCode();
		result = 31 * result + declMethod.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return declMethod + "()." + name + ' ' + type;
	}
}
