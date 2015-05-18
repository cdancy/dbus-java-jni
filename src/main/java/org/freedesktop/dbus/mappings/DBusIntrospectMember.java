package org.freedesktop.dbus.mappings;

public abstract class DBusIntrospectMember {

	String name;

	protected DBusIntrospectMember(String name) {
		this.name = name;
	}

	public String getName() {
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

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((name == null) ? 0 : name.hashCode());
	    return result;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    
	    if (obj == null)
		return false;
	    
	    if (getClass() != obj.getClass())
		return false;
	    
	    DBusIntrospectMember other = (DBusIntrospectMember) obj;
	    
	    if (name == null) {
		if (other.name != null)
		    return false;
	    } else if (!name.equals(other.name))
		return false;
	    
	    return true;
	}
	
	

}
