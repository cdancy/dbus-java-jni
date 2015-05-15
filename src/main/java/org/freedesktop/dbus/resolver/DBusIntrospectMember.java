package org.freedesktop.dbus.resolver;

public abstract class DBusIntrospectMember {

	String name;

	protected DBusIntrospectMember(String name) {
		this.name = name;
	}

	String getName() {
		return name;
	}

	boolean isSignal() {
		return false;
	}

	boolean isInterface() {
		return false;
	}

	boolean isMethod() {
		return false;
	}

	boolean isProperty() {
		return false;
	}

	boolean isArgument() {
		return false;
	}

}
