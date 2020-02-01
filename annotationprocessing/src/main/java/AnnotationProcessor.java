import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("ToString")
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (TypeElement annotation : annotations) {

            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            Map<Boolean, List<Element>> annotatedMethods = annotatedElements.stream().collect(Collectors.partitioningBy(element -> element.getSimpleName().toString().startsWith("get")));

            List<Element> getters = annotatedMethods.get(true);
            List<Element> otherMethods = annotatedMethods.get(false);

            otherMethods.forEach(element -> messager.printMessage(Diagnostic.Kind.ERROR, "@ToString must be applied to a getXxx method", element));

            if (getters.isEmpty()) {
                continue;
            }

            String className = ((TypeElement) getters.get(0).getEnclosingElement()).getQualifiedName().toString();

            List<String> stringGetters = getters.stream().map(getter -> getter.getSimpleName().toString()).collect(Collectors.toList());
            try {
                writeBuilderFile(className, stringGetters);
            } catch (IOException e) {
                error(annotation, e.getMessage(),stringGetters);
            }

        }
        return true;
    }


    private void writeBuilderFile(String className, List<String> getters) throws IOException {

        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }

        String toStringsClassName = "ToStrings";

        JavaFileObject builderFile = filer.createSourceFile(toStringsClassName);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }

            out.print("public class ");
            out.print(toStringsClassName);
            out.println(" {");
            out.println();

            out.print(" public static String toString(Cat cat){");
            out.println();
            out.print(" return ");

            String result = getters.stream().map(m -> "cat." + m + "()").collect(Collectors.joining("+\",\"+"));
            out.println(result + ";");

            out.println("    }");
            out.println("}");

        }
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }
}
