# AutoRPC GWT Generator 

Annotation processor to generate [GWT RPC][gwt-rpc] Async and [RxJava][rxjava] interfaces. Just add `@AutoRpcGwt` to your 
services. The rx interfaces are only generated if the RxJava library is in the processor classpath. Example project 
[here][example].

To run the example project just execute `mvn gwt:devmode` from parent project.


## Download

Releases are deployed to [the Central Repository][dl].

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].


 [dl]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.intendia.gwt.autorpc%22
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
 [rxjava]: https://github.com/ReactiveX/RxJava
 [example]: https://github.com/intendia-oss/autorpc-gwt/tree/master/example
 [gwt-rpc]: http://www.gwtproject.org/doc/latest/tutorial/RPC.html
