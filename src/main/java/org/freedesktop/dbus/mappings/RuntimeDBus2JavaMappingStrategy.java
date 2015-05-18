package org.freedesktop.dbus.mappings;

import java.util.Set;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusMemberName;
import org.freedesktop.dbus.DBusSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeDBus2JavaMappingStrategy implements DBus2JavaMappingStrategy {
    
    private static final Logger logger=LoggerFactory.getLogger(RuntimeDBus2JavaMappingStrategy.class);

    public RuntimeDBus2JavaMappingStrategy() {
        
    }
    
    @Override
    public Class<?> dbusName2JavaClass(String dbus_name) {
        Class<?> result = null;
        
        try {
            result = Class.forName(dbus_name);
        } catch (ClassNotFoundException e) {
            //If there is a direct match, try if is a signal, <classname>$signame ?
            logger.trace("trying to resolve dbus name {} to class member", dbus_name);
            int dot = dbus_name.lastIndexOf(".");
            
            if(dot > 0) {
                char[] member_name = dbus_name.toCharArray();
                member_name[dot] = '$';
                
                try {
                    result = Class.forName(String.valueOf(member_name));
                } catch (ClassNotFoundException e1) {
                    logger.trace("cannot resolve dbus name {} to class member", member_name);
                }
            }
        }
        
        return result;
    }

    @Override
    public String javaClass2DBusName(Class<?> c) {
        String result = null;
        
        if(DBusInterface.class.isAssignableFrom(c)) {
            DBusInterfaceName interfaceName = c.getAnnotation(DBusInterfaceName.class);
            
            if(interfaceName != null) {
                result = interfaceName.value();
            } else {
                result = c.getCanonicalName();
            }
            
        } else if(DBusSignal.class.isAssignableFrom(c)){
            DBusMemberName memberName = c.getAnnotation(DBusMemberName.class);
            
            if(memberName != null) {
                result = memberName.value();
            } else {
                result = c.getCanonicalName();
            }
        }
        
        return result;
    }

    @Override
    public void registerMappings(Set<Class<?>> interfaces) {}

}
