package org.freedesktop.dbus.mappings;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusMemberName;
import org.freedesktop.dbus.DBusSignal;

import static org.freedesktop.dbus.Gettext._;

/**
 * This aims to solve the following issue:
 * 
 * DBus Java mappings can have name clashes, from the generated class.
 * 
 * Consider:
 * 
 * Case 1:
 * 
 * <node> <interface name="org.example.Interf1"> </interface> </node>
 * 
 * <node><interface name="org.example.Interf1.AnotherInterf"></interface>
 * </node>
 * 
 * 1.1 - When generating the code, for case 1, a java interface class with name
 * org.example.Interf1 would be generated, representing DBus interface
 * "org.example.Interf1"
 * 
 * 1.2 - When generating the code, for case 2, a java interface
 * org.example.Interf1.AnotherInterf, would be generated for DBus interface
 * "org.example.Interf1.AnotherInterf".
 * 
 * The later is incompatible, since it would be an interface "AnotherInterf"
 * under package "org.example.Interf1" and would clash with the interface of
 * (1).
 * 
 * So the interface must be renamed, and a mapping must be established.
 * 
 * Case 2:
 * 
 * <node> <interface name="org.example.Interf1"> <signal
 * name="Interf1">...</signal> </interface> </node>
 * 
 * 2.1 - When generating code, would result in a java interface
 * org.example.Interf1, and an inner class for the signal Interf1, which is not
 * compatible with java code, due to shadowing of the top level interface.
 * 
 * So a mapping for the member must be established.
 * 
 * Case 3:
 * 
 * Java to DBus:
 * 
 * Runtime type inference and proxy instantiation.
 * 
 * At runtime, DBus java implementation tries solve the issues above using
 * runtime, annotations. When requesting a remote object reference, the DBus
 * interface name to be requested, if the interface is annotated the name is
 * extracted from the annotation (DBusInterfaceName), or the java interface,
 * name via Class.getName().
 * 
 * The same is used, for signals (DBusMemberName)
 * 
 * DBus to Java:
 * 
 * There is an issue, regarding this mapping. Mapping, for remote objects, is
 * retrieved from the DBus interfaces, provided from the introspect invocation.
 * The issue, is that, we must map, a DBus name to a java class/member, and if,
 * such class in java, is annotated with a different a DBusInterfaceName, there
 * is no means to find the class which is annotated, so additional, mappings
 * should be provided, to aid this mapping.
 * 
 * Consider an interface, with a method, m1, which returns an object path:
 * 
 * At the java interface, the library should check, which are the interfaces
 * exported, by the object, referenced by the reference path, create a proxy
 * with those interfaces, and register the object as an imported object, and
 * return a generic DBisInterface, able to be casted to any of its provided
 * interfaces.
 * 
 * Another, way, is just to consider, the return type, declared in the interface
 * and use that one (this one, limits the expression of DBus, preventing one
 * object to export several interfaces ... )
 * 
 * @author Mario Costa
 *
 */
public class DefaultDBus2JavaMappingStrategy implements DBus2JavaMappingStrategy {

    Map<String, Class<?>> mappings = new HashMap<String, Class<?>>();

    private DBus2JavaMappingStrategy fallbackStategy;
    
    public DefaultDBus2JavaMappingStrategy() {
        fallbackStategy = new RuntimeDBus2JavaMappingStrategy();
    }
    
    /**
     * Registers the classes, for map scanning. This will, scan the class, for
     * annotations, and register the annotations, for reverse mapping.
     * 
     * @param interfaces
     */
    public void registerMappings(Set<Class<?>> interfaces) {
        for (Class<?> interf : interfaces) {
            registerMappings(interf);
        }
    }

    /**
     * 
     * @param interf
     */
    public void registerMappings(Class<?> interf) {
        if(DBusInterface.class.isAssignableFrom(interf)) {
            DBusInterfaceName interfaceName = interf.getAnnotation(DBusInterfaceName.class);
        
            if(interfaceName != null) {
                registerMapping(interfaceName.value(), interf);
            } else {
                registerMapping(interf.getCanonicalName(), interf);
            }
            
            registerMembers(interf);
                
        } else {
            throw new java.lang.IllegalArgumentException(_("Interface must, extend DBusInterface."));
        } 
    }

    private void registerMapping(String dbus_name, Class<?> interf) {
        mappings.put(dbus_name, interf);
    }

    /**
     * Register interface members, which are, signals, or properties.
     * 
     * @param interf
     */
    private void registerMembers(Class<?> interf) {
        Class<?>[] members = interf.getDeclaredClasses();

        for (Class<?> member : members) {
            // if its a DBus signal
            if (DBusSignal.class.isAssignableFrom(member)) {
                DBusMemberName memberName = member.getAnnotation(DBusMemberName.class);

                if (memberName != null) {
                    registerMapping(memberName.value(), member);
                } else {
                    registerMapping(member.getCanonicalName().replace("$", "."), member);
                }
            } // TODO: add here support for properties !? else if()
        }
    }

    @Override
    public Class<?> dbusName2JavaClass(String dbus_name) {
        Class<?> result = null;
        
        if(mappings.containsKey(dbus_name)) {
            result = mappings.get(dbus_name);
        } else {
            result = fallbackStategy.dbusName2JavaClass(dbus_name);
        }
        
        return result;
    }

    @Override
    public String javaClass2DBusName(Class<?> c) {
        return fallbackStategy.javaClass2DBusName(c);
    }

}
