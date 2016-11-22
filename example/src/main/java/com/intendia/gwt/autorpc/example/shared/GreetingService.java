package com.intendia.gwt.autorpc.example.shared;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.intendia.gwt.autorpc.async.AutoRpcGwt;

@AutoRpcGwt
@RemoteServiceRelativePath("greeting")
public interface GreetingService extends RemoteService {

    void ping();

    Greeting time();

    Greeting post(Greeting name);

    int divide(int x, int y);
}
