package org.freedesktop.dbus.resolver;

public class DBusIntrospectArg extends DBusIntrospectMember {

	String type;
	String direction;

	public DBusIntrospectArg(String name, String type, String direction) {
		super(name);
		this.type = type;
		this.direction = direction;
	}

	@Override
	boolean isArgument() {
		return true;
	}

	public String getType() {
		return type;
	}

	public String getDirection() {
		return direction;
	}

}
