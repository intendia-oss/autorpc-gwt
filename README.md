# AutoRPC GWT Generator

[![Maven Central][mavenbadge-svg]][mavenbadge]
[![Build Status][cibadge-svg]][cibadge]

Annotation processor to generate [GWT RPC][gwt-rpc] async interfaces and 
[RxJava][rxjava] adapter types. Add `autorpc-gwt-processor` to your classpath 
(not required at runtime) and your async interfaces will be automatically 
generated. Rx interfaces will be generated only if the RxJava library is in 
the classpath. 

Example project [here][example]. To run the example project just execute `mvn gwt:devmode` from parent project.

## Download

Releases are deployed to [the Central Repository][dl].

Snapshots of the development version are available in 
[Sonatype's `snapshots` repository][snap].

## Notes

The processor generates async interfaces for all classes annotated with 
`@RemoteServiceRelativePath`. This should works correctly almost always, but 
if you do not use this annotation you can use `@AutoRpcGwt` annotation included
in the `autorpc-gwt-annotations` project (in this case, you should add this 
project as a dependency). However, if what you want is to skip the generation 
of one of yours services, you can add the annotation `@SkipRpcGwt`. You can use 
whatever annotation that matches this name or just add a dependency to 
`autorpc-gwt-annotations` and use the provided one.

Async interfaces returns always `Request`. GWT RPC support `void`, `Request` 
and `RequestBuilder` as return types, but this libs has considered that there
are no advantages of returning `void` instead of `Request`. And returning
`Request` might be interesting in some situations like request cancellation 
(automatically done by the Rx adapter if the observable is unsubscribed).
`RequestBuilder` might be used in some cases to add some header, this return
type is not supported to simplify implementation, but you can (and, IMO should)
manipulate the `RequestBuilder` using a custom `RpcRequestBuilder` and 
overriding `doCreate` or `doFinish` like in this example.
```java
GreetingServiceAsync async = GWT.create(GreetingService.class);
((RemoteServiceProxy) async).setRpcRequestBuilder(new RpcRequestBuilder() {
    @Override protected void doFinish(RequestBuilder rb) {
        super.doFinish(rb);
        rb.setHeader("X-Custom-Header", "Hi!");
    }
});
```


 [dl]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.intendia.gwt.autorpc%22
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
 [rxjava]: https://github.com/ReactiveX/RxJava
 [example]: https://github.com/intendia-oss/autorpc-gwt/tree/master/example
 [gwt-rpc]: http://www.gwtproject.org/doc/latest/tutorial/RPC.html
 [mavenbadge]: https://maven-badges.herokuapp.com/maven-central/com.intendia.gwt.autorpc/autorpc-gwt-parent
 [mavenbadge-svg]: https://maven-badges.herokuapp.com/maven-central/com.intendia.gwt.autorpc/autorpc-gwt-parent/badge.svg
 [cibadge]: https://travis-ci.org/intendia-oss/rxjava-gwt
 [cibadge-svg]: https://travis-ci.org/intendia-oss/rxjava-gwt.svg
