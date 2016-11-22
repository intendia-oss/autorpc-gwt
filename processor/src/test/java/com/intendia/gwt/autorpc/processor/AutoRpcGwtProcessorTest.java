package com.intendia.gwt.autorpc.processor;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceString;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import org.junit.Test;

public class AutoRpcGwtProcessorTest {

    @Test public void assert_processor_works() {
        assertAbout(javaSource())
                .that(forSourceString("shared.GreetingService", "package shared;\n"
                        + "\n"
                        + "import com.google.gwt.user.client.rpc.RemoteService;\n"
                        + "import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;\n"
                        + "import com.intendia.gwt.autorpc.async.AutoRpcGwt;\n"
                        + "\n"
                        + "@AutoRpcGwt @RemoteServiceRelativePath(\"greet\")\n"
                        + "public interface GreetingService extends RemoteService {\n"
                        + "    String greet(String name) throws IllegalArgumentException;\n"
                        + "    int time();\n"
                        + "    void noOp();\n"
                        + "}"))
                .withCompilerOptions("-AskipJavaLangImports")
                .processedWith(new AutoRpcGwtProcessor())
                .compilesWithoutError()
                .and().generatesSources(forSourceString("shared.GreetingServiceAsync", "package shared;\n"
                + "\n"
                + "import com.google.gwt.user.client.rpc.AsyncCallback;\n"
                + "\n"
                + "public interface GreetingServiceAsync {\n"
                + "    void greet(String name, AsyncCallback<String> callback);\n"
                + "    void time(AsyncCallback<Integer> callback);\n"
                + "    void noOp(AsyncCallback<Void> callback);\n"
                + "}"))
                .and().generatesSources(forSourceString("shared.GreetingServiceRx", "package shared;\n"
                + "\n"
                + "import com.intendia.gwt.autorpc.rx.SingleSubscriberAdapter;\n"
                + "import rx.Single;\n"
                + "import rx.SingleSubscriber;\n"
                + "\n"
                + "public class GreetingServiceRx {\n"
                + "    private final GreetingServiceAsync async;\n"
                + "    public GreetingServiceRx(GreetingServiceAsync async) { this.async = async; }\n"
                + "    public Single<String> greet(final String name) {\n"
                + "        return Single.create(new Single.OnSubscribe<String>() {\n"
                + "            @Override public void call(SingleSubscriber<? super String> s) {\n"
                + "                async.greet(name, new SingleSubscriberAdapter<>(s));\n"
                + "            }\n"
                + "        });\n"
                + "    }\n"
                + "    public Single<Integer> time() {\n"
                + "        return Single.create(new Single.OnSubscribe<Integer>() {\n"
                + "            @Override public void call(SingleSubscriber<? super Integer> s) {\n"
                + "                async.time(new SingleSubscriberAdapter<>(s));\n"
                + "            }\n"
                + "        });\n"
                + "    }\n"
                + "    public Single<Void> noOp() {\n"
                + "        return Single.create(new Single.OnSubscribe<Void>() {\n"
                + "            @Override public void call(SingleSubscriber<? super Void> s) {\n"
                + "                async.noOp(new SingleSubscriberAdapter<>(s));\n"
                + "            }\n"
                + "        });\n"
                + "    }\n"
                + "}"));
    }

}
