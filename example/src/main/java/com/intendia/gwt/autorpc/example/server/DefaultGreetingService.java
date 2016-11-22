package com.intendia.gwt.autorpc.example.server;

import static java.lang.Math.random;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.intendia.gwt.autorpc.example.shared.Greeting;
import com.intendia.gwt.autorpc.example.shared.GreetingService;
import java.util.Date;

public class DefaultGreetingService extends RemoteServiceServlet implements GreetingService {

    @Override public void ping() { System.out.println("pong"); }

    @Override public Greeting time() { return new Greeting(new Date().toString()); }

    @Override public Greeting post(Greeting name) {
        if (random() < .1) throw new IllegalStateException("ups…");
        else return new Greeting("hi " + name.getGreeting() + "!");
    }

    @Override public int divide(int x, int y) { return x / y; }

}
