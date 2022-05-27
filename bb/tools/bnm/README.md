# de.bb.tools.bnm - BNM is Not Maven

BMN is a replacement for maven which I wrote years ago, because I was not content how maven worked.
And even today I still prefer using BNM over Maven.

- BNM has few dependencies and does not download that much
- BNM operates multithreaded and fast
- my current working tree can't be built using maven (mvn-3.6.3 at the time ov writing) because maven tries to download excluded dependencies...
  which fails for some org.eclipse.platform stuff.
  
To use BNM you need the main class and a wrapper script. The archive of the current version is here: https://bebbosoft.de/repo/de/bb/tools/bnm/launcher/0.2.7/launcher-0.2.7.jar

Once you checked out the root project and it's submodules simply run
```
bnm install
```
to build the project tree.

There's also an Eclipse plugin which integrates BNM into Eclipse.


## see also
https://bebbosoft.de/java/bnm/index.wiki
