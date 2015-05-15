package org.freedesktop.dbus.resolver;

public class DBusIntrospectProperty extends DBusIntrospectMember {

	String type;
	String access;
	
	DBusIntrospectProperty(String name, String type, String access) {
		super(name);
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
		return false;
	}

	@Override
	boolean isProperty() {
		return true;
	}
	
	
}
