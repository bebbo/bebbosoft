# de.bb.util
My collection of useful Java classes. Most of them were created in the 90's using Java 1.1.

The Java API doc is there: https://bebbosoft.de/java/apidoc/

## ByteRef
A wrapper around a byte array which can be handled like a string. And it's not immutable (keep in mind if it is used in HashMaps), this eases parsing/splitting the content of a ByteRef. So this snippet removes the content up to the next ':' from buffer and returns that part without the delimiter:
```
ByteRef buffer = ...;
ByteRef quali = buffer.nextWord(':');
```
If buffer contains `"foo:bar"` then quali becomes `"foo""` and buffer becomes `"bar"`

To extend the buffer with new data, the method `update(InputStream is)` can be used, which returns `null` if the buffer did not change...

## IniFile
Manage the good old Windows ini files with sections and content. It mirrors the Windows API functions to handle ini files.

## XmlFile
Similar API as `IniFile` but manages a XML files. It does not support external entities, which I consider is a good thing, since it keeps the system closed.

A section here is a path to a XML tag somewhere in the XML tree.

I'm using it to read configurations, process and transform files or even as simple database as in the LDAPServer.

using that simple xml file:
```
<log timeout="50">
	<formatter name="f1" class="de.bb.log.Formatter" dateFormat="yyyy-MM-dd HH:mm:ss.SSS"
		format="%d %p [%t] %C - %m" escape="false" />
	<appender name="a1" formatter="f1" class="de.bb.log.FileAppender"
		append="false" bufferSize="8192" baseName="logs/a1" dateFormat="_yyyyMMdd" 
		appendDateAfterClose="true"/>
	<formatter name="xml" class="de.bb.log.Formatter" dateFormat="yyyy-MM-dd HH:mm:ss.SSS"
		format='&lt;%p time="%d" thread="%t" logger="%C"&gt;%m&lt;/%p&gt;' escape="true" />
	<appender name="x1" formatter="xml" class="de.bb.log.FileAppender"
		append="false" bufferSize="8192" baseName="logs/x1" dateFormat="_yyyyMMdd" 
		appendDateAfterClose="true" />
	<logger name="" level="ERROR" appender="x1,a1" />
</log>
```

the XmlFile way would be:

```
			final XmlFile xml = new XmlFile();
			xml.read(is); // read the InputStream
      
      // read the attribute timeout, using "100" as default value
      final String stimeout = xml.getString("/log", "timeout", "100"); 
      ...
			// loop over the formattes
			for (final Iterator<String> i = xml.sections("/log/formatter"); i.hasNext();) {
				final String key = i.next(); // key is the path a nested tag
				final Map<String, String> attributes = xml.getAttributes(key);
        ...
      }
```

## SingleMap and MultiMap
Similar as the java TreeMap with some differences:
- implemented using an AVL Tree
- supports `headMap`, `subMap` and `tailMap` with values which may not exists in the Map
- MultiMap supports inserting many values for the same key.

## LRUCache
The cache you always wanted. No size limit and still memory friendly: 
Only a distinct percentage is kept as hard references. All other values are tracked using a `WeakReference`.

It implements the `Map` interface.

The main difference to many other caches (which IMHO aren't caches) is:
    you may get `null` if you lookup a value, 
    even if you put it just before. It's a real cache.

## TimedLRUCache
Similar as LRUCache with an additional maximum life time.


