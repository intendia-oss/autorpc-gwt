package com.intendia.gwt.autorpc.rx;

import com.google.gwt.user.client.rpc.AsyncCallback;
import rx.SingleSubscriber;

public class SingleSubscriberAdapter<T> implements AsyncCallback<T> {
    private final SingleSubscriber<? super T> adaptee;

    public SingleSubscriberAdapter(SingleSubscriber<? super T> adaptee) {
        this.adaptee = adaptee;
    }

    @Override public void onFailure(Throwable throwable) {
        adaptee.onError(throwable);
    }

    @Override public void onSuccess(T result) {
        adaptee.onSuccess(result);
    }
}
