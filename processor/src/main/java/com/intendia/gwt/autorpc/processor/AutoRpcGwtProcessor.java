package com.intendia.gwt.autorpc.processor;

import static java.util.Collections.singleton;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.base.Throwables;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.intendia.gwt.autorpc.async.AutoRpcGwt;
import com.intendia.gwt.autorpc.rx.SingleSubscriberAdapter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic.Kind;
import rx.Single;
import rx.SingleSubscriber;

public class AutoRpcGwtProcessor extends AbstractProcessor {
    private static final ClassName AsyncCallback = ClassName.get(AsyncCallback.class);
    private static final ClassName Single = ClassName.get(rx.Single.class);

    @Override public Set<String> getSupportedOptions() { return singleton("debug"); }

    @Override public Set<String> getSupportedAnnotationTypes() { return singleton(AutoRpcGwt.class.getName()); }

    @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;
        roundEnv.getElementsAnnotatedWith(AutoRpcGwt.class).stream()
                .filter(e -> e.getKind().isInterface() && e instanceof TypeElement).map(e -> (TypeElement) e)
                .forEach(rpcService -> {
                    try {
                        processRestService(rpcService);
                    } catch (Exception e) {
                        // We don't allow exceptions of any kind to propagate to the compiler
                        error("uncaught exception processing RPC service " + rpcService + ": " + e + "\n"
                                + Throwables.getStackTraceAsString(e));
                    }
                });
        return true;
    }

    private void processRestService(TypeElement rpcService) throws Exception {
        ClassName rpcName = ClassName.get(rpcService);
        log("RPC service interface: " + rpcName);

        ClassName asyncName = ClassName.get(rpcName.packageName(), rpcName.simpleName() + "Async");
        log("Async service model: " + asyncName);

        ClassName rxName = ClassName.get(rpcName.packageName(), rpcName.simpleName() + "Rx");
        log("Rx service model: " + asyncName);

        TypeSpec.Builder asyncTypeBuilder = TypeSpec.interfaceBuilder(asyncName.simpleName())
                .addOriginatingElement(rpcService).addModifiers(Modifier.PUBLIC);

        TypeSpec.Builder rxTypeBuilder = TypeSpec.classBuilder(rxName.simpleName())
                .addOriginatingElement(rpcService).addModifiers(Modifier.PUBLIC);

        final String asyncField = "async";
        rxTypeBuilder.addField(asyncName, asyncField, PRIVATE, FINAL);
        rxTypeBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC).addParameter(asyncName, asyncField)
                .addStatement("this.async = async").build());

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

            // to rx method
            MethodSpec.Builder rxBuilder = MethodSpec.methodBuilder(methodName).addModifiers(PUBLIC);
            for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
                rxBuilder.addTypeVariable(TypeVariableName.get((TypeVariable) typeParameterElement.asType()));
            }
            // parameters
            String params = "";
            for (VariableElement parameter : method.getParameters()) {
                TypeName type = TypeName.get(parameter.asType());
                String name = parameter.getSimpleName().toString();
                params += name + ", ";
                rxBuilder.addParameter(ParameterSpec.builder(type, name).addModifiers(FINAL).build());
            }
            rxBuilder.addCode("return $T.create(new $T<$T>() {\n"
                            + "  @Override public void call($T<? super $T> s) {\n"
                            + "    $L.$L($Lnew $T<>(s));\n"
                            + "  }\n"
                            + "});\n", Single, Single.OnSubscribe.class, returnType, SingleSubscriber.class, returnType,
                    asyncField, methodName, params, SingleSubscriberAdapter.class);
            rxBuilder.returns(ParameterizedTypeName.get(Single, returnType));
            rxTypeBuilder.addMethod(rxBuilder.build());
        }

        Filer filer = processingEnv.getFiler();
        boolean skipJavaLangImports = processingEnv.getOptions().containsKey("skipJavaLangImports");
        JavaFile.builder(rpcName.packageName(), asyncTypeBuilder.build())
                .skipJavaLangImports(skipJavaLangImports).build().writeTo(filer);
        JavaFile.builder(rpcName.packageName(), rxTypeBuilder.build())
                .skipJavaLangImports(skipJavaLangImports).build().writeTo(filer);
    }

    private void log(String msg) {
        if (processingEnv.getOptions().containsKey("debug")) {
            processingEnv.getMessager().printMessage(Kind.NOTE, msg);
        }
    }

    private void error(String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg);
    }
}
