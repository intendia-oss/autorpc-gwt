package com.intendia.gwt.autorpc.processor;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaFileObjects.forSourceString;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import org.junit.Test;

public class AutoRpcGwtProcessorTest {

    @Test public void assert_processor_works() {
        AutoRpcGwtProcessor processor = new AutoRpcGwtProcessor();
        assertAbout(javaSource()).that(forSourceString("shared.GreetingService", "package shared;\n"
                + "\n"
                + "import com.google.gwt.user.client.rpc.RemoteService;\n"
                + "import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;\n"
                + "import com.intendia.gwt.autorpc.annotations.AutoRpcGwt;\n"
                + "\n"
                + "@AutoRpcGwt @RemoteServiceRelativePath(\"greet\")\n"
                + "public interface GreetingService extends RemoteService {\n"
                + "    String greet(String name) throws IllegalArgumentException;\n"
                + "    int time();\n"
                + "    void noOp();\n"
                + "}"))
                .withCompilerOptions("-AskipJavaLangImports")
                .processedWith(processor)
                .compilesWithoutError()
                .and().generatesSources(forSourceString("shared.GreetingServiceAsync", "package shared;\n"
                + "\n"
                + "import com.google.gwt.http.client.Request;\n"
                + "import com.google.gwt.user.client.rpc.AsyncCallback;\n"
                + "import javax.annotation.Generated;\n"
                + "\n"
                + "@Generated(\"com.intendia.gwt.autorpc.processor.AutoRpcGwtProcessor\")\n"
                + "public interface GreetingServiceAsync {\n"
                + "    Request greet(String name, AsyncCallback<String> callback);\n"
                + "    Request time(AsyncCallback<Integer> callback);\n"
                + "    Request noOp(AsyncCallback<Void> callback);\n"
                + "}"))
                .and().generatesSources(forSourceString("shared.GreetingServiceRx", "package shared;\n"
                + "\n"
                + "import com.google.gwt.http.client.Request;\n"
                + "import com.google.gwt.user.client.rpc.AsyncCallback;\n"
                + "import javax.annotation.Generated;\n"
                + "import rx.Single;\n"
                + "import rx.SingleSubscriber;\n"
                + "import rx.functions.Action0;\n"
                + "import rx.subscriptions.Subscriptions;\n"
                + "\n"
                + "@Generated(\"com.intendia.gwt.autorpc.processor.AutoRpcGwtProcessor\")\n"
                + "public class GreetingServiceRx {\n"
                + "    private final GreetingServiceAsync async$;\n"
                + "    public GreetingServiceRx(GreetingServiceAsync async) { this.async$ = async; }\n"
                + "    public Single<String> greet(final String name) {\n"
                + "        return Single.create(new Single.OnSubscribe<String>() {\n"
                + "            @Override public void call(final SingleSubscriber<? super String> s$) {\n"
                + "                final Request r$ = async$.greet(name, new AsyncCallback<String>() {\n"
                + "                    @Override public void onFailure(Throwable caught) { s$.onError(caught); }\n"
                + "                    @Override public void onSuccess(String result) { s$.onSuccess(result); }\n"
                + "                });\n"
                + "                s$.add(Subscriptions.create(new Action0() { @Override public void call() { r$.cancel(); } }));\n"
                + "            }\n"
                + "        });\n"
                + "    }\n"
                + "    public Single<Integer> time() {\n"
                + "        return Single.create(new Single.OnSubscribe<Integer>() {\n"
                + "            @Override public void call(final SingleSubscriber<? super Integer> s$) {\n"
                + "                final Request r$ = async$.time(new AsyncCallback<Integer>() {\n"
                + "                    @Override public void onFailure(Throwable caught) { s$.onError(caught); }\n"
                + "                    @Override public void onSuccess(Integer result) { s$.onSuccess(result); }\n"
                + "                });\n"
                + "                s$.add(Subscriptions.create(new Action0() { @Override public void call() { r$.cancel(); } }));\n"
                + "            }\n"
                + "        });\n"
                + "    }\n"
                + "    public Single<Void> noOp() {\n"
                + "        return Single.create(new Single.OnSubscribe<Void>() {\n"
                + "            @Override public void call(final SingleSubscriber<? super Void> s$) {\n"
                + "                final Request r$ = async$.noOp(new AsyncCallback<Void>() {\n"
                + "                    @Override public void onFailure(Throwable caught) { s$.onError(caught); }\n"
                + "                    @Override public void onSuccess(Void result) { s$.onSuccess(result); }\n"
                + "                });\n"
                + "                s$.add(Subscriptions.create(new Action0() { @Override public void call() { r$.cancel(); } }));\n"
                + "            }\n"
                + "        });\n"
                + "    }\n"
                + "}"));
        assertThat(processor.processed).hasSize(1);
    }

    @Test public void assert_skip_works() {
        final AutoRpcGwtProcessor processor = new AutoRpcGwtProcessor();
        assertAbout(javaSource()).that(forSourceString("shared.GreetingService", "package shared;\n"
                + "\n"
                + "import com.google.gwt.user.client.rpc.RemoteService;\n"
                + "import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;\n"
                + "import com.intendia.gwt.autorpc.annotations.SkipRpcGwt;\n"
                + "\n"
                + "@SkipRpcGwt @RemoteServiceRelativePath(\"greet\")\n"
                + "public interface GreetingService extends RemoteService {\n"
                + "}"))
                .processedWith(processor)
                .compilesWithoutError();
        assertThat(processor.processed).hasSize(0);
    }
}
