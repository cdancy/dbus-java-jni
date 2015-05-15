package org.freedesktop.dbus.resolver;

import java.util.ArrayList;
import java.util.List;

public class DBusIntrospectMethod extends DBusIntrospectMember {

	List<DBusIntrospectArg> memberArguments;

	public List<DBusIntrospectArg> getMemberArguments() {
		return memberArguments;
	}

	public DBusIntrospectMethod(String name) {
		super(name);
	}

	public void addArgument(DBusIntrospectArg arg) {
		if (memberArguments == null) {
			memberArguments = new ArrayList<DBusIntrospectArg>();
		}
		memberArguments.add(arg);
	}

	@Override
	boolean isSignal() {
		return false;
	}

	@Override
	boolean isInterface() {
		return false;
	}

	@Override
	boolean isMethod() {
		return true;
	}

	@Override
	boolean isProperty() {
		return false;
	}

}
