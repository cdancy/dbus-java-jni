package org.freedesktop.dbus.resolver;

import java.util.ArrayList;
import java.util.List;

public class DBusIntrospectSignal extends DBusIntrospectMember {
	List<DBusIntrospectArg> memberArguments;

	public List<DBusIntrospectArg> getMemberArguments() {
		return memberArguments;
	}

	public DBusIntrospectSignal(String name) {
		super(name);
	}

	@Override
	boolean isSignal() {
		return true;
	}

	public void addArgument(DBusIntrospectArg arg) {
		if (memberArguments == null) {
			memberArguments = new ArrayList<DBusIntrospectArg>();
		}
		memberArguments.add(arg);
	}

}
