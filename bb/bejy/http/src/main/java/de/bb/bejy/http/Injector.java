package de.bb.bejy.http;

import de.bb.util.LogFile;

public interface Injector {

    void inject(LogFile log, Object o) throws Exception;

}
