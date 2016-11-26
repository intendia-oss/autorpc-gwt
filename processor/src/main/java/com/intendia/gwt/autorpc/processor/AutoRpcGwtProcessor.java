package com.intendia.gwt.autorpc.processor;

import static java.util.Collections.singleton;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic.Kind;

public class AutoRpcGwtProcessor extends AbstractProcessor {
    @VisibleForTesting final Set<Element> processed = new HashSet<>();

    @Override public Set<String> getSupportedOptions() { return singleton("debug"); }

    @Override public Set<String> getSupportedAnnotationTypes() { return SUPPORTED_ANNOTATIONS; }

    @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;
        annotations.stream().flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
                .filter(e -> e.getKind().isInterface() && e instanceof TypeElement).map(e -> (TypeElement) e)
                .filter(t -> t.getAnnotationMirrors().stream()
                        .map(a -> a.getAnnotationType().asElement().getSimpleName().toString())
                        .noneMatch("SkipRpcGwt"::equals))
                .filter(processed::add) // just in case some one add both annotations to the same interface
                .forEach(rpcService -> {
                    try {
                        process(rpcService);
                    } catch (Exception e) {
                        // We don't allow exceptions of any kind to propagate to the compiler
                        error("uncaught exception processing RPC service " + rpcService + ": " + e + "\n"
                                + Throwables.getStackTraceAsString(e));
                    }
                });
        return true;
    }

    private void process(TypeElement rpcService) throws Exception {
        ClassName rpcName = ClassName.get(rpcService);
        log("RPC service interface: " + rpcName);

        ClassName asyncName = ClassName.get(rpcName.packageName(), rpcName.simpleName() + "Async");
        log("Async service model: " + asyncName);

        ClassName rxName = ClassName.get(rpcName.packageName(), rpcName.simpleName() + "Rx");
        log("Rx service model: " + asyncName);

        TypeSpec.Builder asyncTypeBuilder = TypeSpec.interfaceBuilder(asyncName.simpleName())
                .addOriginatingElement(rpcService).addModifiers(Modifier.PUBLIC);

        boolean rx = rxInClasspath();
        TypeSpec.Builder rxTypeBuilder = rx ? TypeSpec.classBuilder(rxName.simpleName())
                .addOriginatingElement(rpcService).addModifiers(Modifier.PUBLIC) : null;

        if (rx) {
            rxTypeBuilder.addField(asyncName, ASYNC_FIELD, PRIVATE, FINAL);
            rxTypeBuilder.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC).addParameter(asyncName, ASYNC_FIELD)
                    .addStatement("this.async = async").build());
        }

        List<ExecutableElement> methods = rpcService.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD && e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(method -> !(method.getModifiers().contains(STATIC) || method.isDefault()))
                .collect(Collectors.toList());

        for (ExecutableElement method : methods) {
            String methodName = method.getSimpleName().toString();
            TypeName returnTypeName = TypeName.get(method.getReturnType());

            // from GreetingResponse greetServer(String name) throws IllegalArgumentException;

            // to async method
            MethodSpec.Builder asyncMethod = MethodSpec.methodBuilder(methodName)
                    .addModifiers(PUBLIC, ABSTRACT).returns(TypeName.VOID);
            getDoc(method).ifPresent(asyncMethod::addJavadoc);
            for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
                asyncMethod.addTypeVariable(TypeVariableName.get((TypeVariable) typeParameterElement.asType()));
            }
            for (VariableElement parameter : method.getParameters()) {
                TypeName type = TypeName.get(parameter.asType());
                String name = parameter.getSimpleName().toString();
                asyncMethod.addParameter(ParameterSpec.builder(type, name).build());
            }
            final TypeName returnType = returnTypeName.box();
            asyncMethod.addParameter(ParameterSpec
                    .builder(ParameterizedTypeName.get(AsyncCallback, returnType), "callback")
                    .build());
            asyncTypeBuilder.addMethod(asyncMethod.build());

            if (!rx) continue; // to rx method
            MethodSpec.Builder rxMethod = MethodSpec.methodBuilder(methodName).addModifiers(PUBLIC);
            getDoc(method).ifPresent(rxMethod::addJavadoc);
            for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
                rxMethod.addTypeVariable(TypeVariableName.get((TypeVariable) typeParameterElement.asType()));
            }
            // parameters
            String params = "";
            for (VariableElement parameter : method.getParameters()) {
                TypeName type = TypeName.get(parameter.asType());
                String name = parameter.getSimpleName().toString();
                params += name + ", ";
                rxMethod.addParameter(ParameterSpec.builder(type, name).addModifiers(FINAL).build());
            }
            rxMethod.addCode("return $T.create(new $T<$T>() {\n"
                            + "  @Override public void call($T<? super $T> s) {\n"
                            + "    $L.$L($Lnew $T<$T>() {\n"
                            + "      @Override public void onFailure($T caught) { s.onError(caught); }\n"
                            + "      @Override public void onSuccess($T result) { s.onSuccess(result); }\n"
                            + "    });\n"
                            + "  }\n"
                            + "});\n", Single, OnSubscribe, returnType, SingleSubscriber, returnType,
                    ASYNC_FIELD, methodName, params, AsyncCallback, returnType, Throwable.class, returnType);
            rxMethod.returns(ParameterizedTypeName.get(Single, returnType));
            rxTypeBuilder.addMethod(rxMethod.build());
        }

        Filer filer = processingEnv.getFiler();
        boolean skipJavaLangImports = processingEnv.getOptions().containsKey("skipJavaLangImports");
        JavaFile.builder(rpcName.packageName(), asyncTypeBuilder.build())
                .skipJavaLangImports(skipJavaLangImports).build().writeTo(filer);
        if (rx) JavaFile.builder(rpcName.packageName(), rxTypeBuilder.build())
                .skipJavaLangImports(skipJavaLangImports).build().writeTo(filer);
    }

    private Optional<String> getDoc(ExecutableElement method) {
        String docComment = processingEnv.getElementUtils().getDocComment(method);
        if (docComment == null || docComment.trim().isEmpty()) return Optional.empty();
        if (!docComment.endsWith("\n")) docComment += "\n";
        return Optional.of(docComment);
    }

    private boolean rxInClasspath() {
        try {
            return getClass().getClassLoader().loadClass("rx.Single") != null;
        } catch (ClassNotFoundException notFound) {
            return false;
        }
    }

    private void log(String msg) {
        if (processingEnv.getOptions().containsKey("debug")) {
            processingEnv.getMessager().printMessage(Kind.NOTE, msg);
        }
    }

    private void error(String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg);
    }

    private static final String ASYNC_FIELD = "async";
    private static final String GWT_RPC = "com.google.gwt.user.client.rpc";
    private static final String AutoRpcGwt = "com.intendia.gwt.autorpc.async.AutoRpcGwt";
    private static final String RemoteServiceRelativePath = GWT_RPC + ".RemoteServiceRelativePath";
    private static final ClassName AsyncCallback = ClassName.get(GWT_RPC, "AsyncCallback");
    private static final ClassName Single = ClassName.get("rx", "Single");
    private static final ClassName OnSubscribe = Single.nestedClass("OnSubscribe");
    private static final ClassName SingleSubscriber = ClassName.get("rx", "SingleSubscriber");
    private static final Set<String> SUPPORTED_ANNOTATIONS = ImmutableSet.of(RemoteServiceRelativePath, AutoRpcGwt);
}
