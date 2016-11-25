package com.intendia.gwt.autorpc.example.shared;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.intendia.gwt.autorpc.async.AutoRpcGwt;
import java.util.ArrayList;

@AutoRpcGwt @RemoteServiceRelativePath("greeting")
public interface GreetingService extends RemoteService {

    void ping();

    /** Returns server time. */
    Greeting time();

    /** Responds with a greeting message using your name. */
    Greeting post(Greeting name);

    /**
     * Divides {@code x} by {@code y}.
     *
     * @param x dividend
     * @param y divisor
     */
    int divide(int x, int y);

    <T extends Number> ArrayList<T> acc(T a, T b);
}
