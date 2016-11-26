package com.intendia.gwt.autorpc.example.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasKeyUpHandlers;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.intendia.gwt.autorpc.example.shared.Greeting;
import com.intendia.gwt.autorpc.example.shared.GreetingService;
import com.intendia.gwt.autorpc.example.shared.GreetingServiceAsync;
import com.intendia.gwt.autorpc.example.shared.GreetingServiceRx;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

public class ExampleEntryPoint implements EntryPoint {
    private Action1<Throwable> err = e -> GWT.log("exception: " + e, e);

    public void onModuleLoad() {
        TextBox name = append(new TextBox());
        HTML out = append(new HTML());

        GreetingServiceAsync async = GWT.create(GreetingService.class);
        GreetingServiceRx rx = new GreetingServiceRx(async);
        Observable.merge(valueChange(name), keyUp(name))
                .map(e -> name.getValue())
                .switchMap(q -> rx.post(new Greeting(q))
                        .map(Greeting::getGreeting)
                        .onErrorReturn(Throwable::toString)
                        .toObservable())
                .forEach(out::setHTML);
        name.setValue("ping", true);

        append("-- Static tests --");
        rx.ping().subscribe(n -> append("rx.ping pong"), err);
        rx.time().subscribe(n -> append("rx.time response: " + n.getGreeting()), err);
    }

    private static void append(String text) { append(new Label(text)); }

    private static <T extends IsWidget> T append(T w) { RootPanel.get().add(w); return w; }

    private static Observable<KeyUpEvent> keyUp(HasKeyUpHandlers source) {
        return Observable.create(s -> register(s, source.addKeyUpHandler(s::onNext)));
    }

    public static <T> Observable<ValueChangeEvent<T>> valueChange(HasValueChangeHandlers<T> source) {
        return Observable.create(s -> register(s, source.addValueChangeHandler(s::onNext)));
    }

    private static void register(Subscriber s, HandlerRegistration handlerRegistration) {
        s.add(Subscriptions.create(handlerRegistration::removeHandler));
    }
}
