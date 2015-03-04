package de.bb.bejy.http;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

public class IncludeRequest extends ForwardRequest {

    public IncludeRequest(HttpServletRequest hr, RequestDispatcher rd) {
        super(hr, rd);
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.INCLUDE;
    }

}
