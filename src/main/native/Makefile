JAVAC?=javac
JAVADOC?=javadoc
JAR?=jar
JAVAH?=javah
GCJ?=gcj
CC?=gcc
LD?=gcc
JPPFLAGS+=-C -P
CFLAGS+=-Wall -Os -pedantic -Werror
CSTD?=-std=c99
CSHAREFLAG+=-fpic -fno-stack-protector
GCJJNIFLAG=-fjni
JVERCFLAGS+=-source 1.5
JCFLAGS+=
INCLUDES+=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
JAVADOCFLAGS?=-quiet -author -link http://java.sun.com/j2se/1.4.2/docs/api/
CLASSPATH=../../../target/classes

LDVER?=$(shell ld -v | cut -d' ' -f1)
UNAME?=$(shell uname -s)
ARCH=$(shell uname -i | awk '{ if(index($$1,"64") !=0) {print "amd64"} else print $$1}')

ifeq ($(LDVER),GNU)
LDSHAREFLAGS+=-fpic -shared
else
LDSHAREFLAGS+=-lc
endif

#search JDK if not set
ifndef JDK_HOME
  JDK_HOME=$(shell readlink -f `which javac` | sed "s:/bin/javac::")
endif

all: clean libunix-java-$(ARCH).so maven

clean:
	rm -f *.so *.h

%-$(ARCH).o: %.c %.h
	$(CC) $(CFLAGS) $(CSTD) $(CSHAREFLAG) $(INCLUDES) -c -o $@ $<

lib%-$(ARCH).so: %-$(ARCH).o
	$(CC) $(LDFLAGS) $(LDSHAREFLAGS) -o $@ $<

unix-java.h: 
	$(JAVAH) -classpath $(CLASSPATH) -o $@ cx.ath.matthew.unix.UnixServerSocket cx.ath.matthew.unix.UnixSocket cx.ath.matthew.unix.USInputStream cx.ath.matthew.unix.USOutputStream

maven:
	cp *.so $(CLASSPATH)
