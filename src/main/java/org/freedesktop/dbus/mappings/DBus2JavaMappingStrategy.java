package org.freedesktop.dbus.mappings;

import java.util.Set;


/**
 * This interface implements the strategy for mapping between java classes and dbus names.
 * 
 * dbus names, includes, interface, signal ... 
 * 
 * @author Mario Costa
 *
 */
public interface DBus2JavaMappingStrategy {
    
    /**
     * Registers the classes, for map scanning. This will, scan the class, for
     * annotations, and register the annotations, for reverse mapping.
     * 
     * @param interfaces
     */    
    void registerMappings(Set<Class<?>> interfaces);
    
    /**
     * Returns the java class mapped from the dbus_name.
     * 
     * @param dbus_name
     * @return The java class.
     */
    Class<?> dbusName2JavaClass(String dbus_name);
    
    /**
     * Returns the dbus name, given the java class.
     * @param c
     * @return
     */
    String javaClass2DBusName(Class<?> c);
    
}
