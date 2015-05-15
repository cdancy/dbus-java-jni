package org.freedesktop.dbus.resolver;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Mário Costa
 *
 */
public class DBusIntrospectInterface extends DBusIntrospectMember {

	List<DBusIntrospectSignal> memberSignals;

	List<DBusIntrospectMethod> memberMethods;

	List<DBusIntrospectInterface> memberInterfaces;

	List<DBusIntrospectProperty> memberProperties;

	public DBusIntrospectInterface(String name) {
		super(name);
	}

	public List<DBusIntrospectProperty> getMemberProperties() {
		return memberProperties;
	}

	public List<DBusIntrospectSignal> getMemberSignals() {
		return memberSignals;
	}

	public List<DBusIntrospectMethod> getMemberMethods() {
		return memberMethods;
	}

	public List<DBusIntrospectInterface> getMemberInterfaces() {
		return memberInterfaces;
	}

	@Override
	public boolean isSignal() {
		return false;
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	@Override
	public boolean isMethod() {
		return false;
	}

	@Override
	public boolean isProperty() {
		return false;
	}

	public void addMemberInterface(DBusIntrospectInterface interf) {
		if (memberInterfaces == null) {
			memberInterfaces = new ArrayList<DBusIntrospectInterface>();
		}
		memberInterfaces.add(interf);
	}

	public void addMemberProperty(DBusIntrospectProperty prop) {
		if (memberProperties == null) {
			memberProperties = new ArrayList<DBusIntrospectProperty>();
		}
		memberProperties.add(prop);
	}

	public void addMemberMethod(DBusIntrospectMethod member) {
		if (memberMethods == null) {
			memberMethods = new ArrayList<DBusIntrospectMethod>();
		}
		memberMethods.add(member);
	}

	public void addMemberSignal(DBusIntrospectSignal member) {
		if (memberSignals == null) {
			memberSignals = new ArrayList<DBusIntrospectSignal>();
		}
		memberSignals.add(member);
	}

}
