package hr.algebra.hockey.utils;

import hr.algebra.hockey.HockeyGameApplication;
import hr.algebra.hockey.Launcher;
import hr.algebra.hockey.controller.HockeyGameController;
import hr.algebra.hockey.engine.CollisionService;
import hr.algebra.hockey.engine.HockeyGameEngine;
import hr.algebra.hockey.exception.ChatActionException;
import hr.algebra.hockey.jndi.ConfigurationKey;
import hr.algebra.hockey.jndi.ConfigurationReader;
import hr.algebra.hockey.jndi.InitialDirContextCloseable;
import hr.algebra.hockey.model.GameState;
import hr.algebra.hockey.model.GameStatus;
import hr.algebra.hockey.model.HockeyMove;
import hr.algebra.hockey.model.HockeyMoveTag;
import hr.algebra.hockey.model.HockeyMoveType;
import hr.algebra.hockey.model.Player;
import hr.algebra.hockey.model.PlayerType;
import hr.algebra.hockey.model.Puck;
import hr.algebra.hockey.network.MultiplayerMessage;
import hr.algebra.hockey.network.SocketMultiplayerService;
import hr.algebra.hockey.rmi.ChatRemoteService;
import hr.algebra.hockey.rmi.ChatRemoteServiceImpl;
import hr.algebra.hockey.rmi.RmiServer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class DocumentationUtils {
    private static final Path DOCUMENTATION_PATH = Path.of("doc", "documentation.html");

    private static final List<Class<?>> DOCUMENTED_CLASSES = List.of(
            HockeyGameApplication.class,
            Launcher.class,
            HockeyGameController.class,
            HockeyGameEngine.class,
            CollisionService.class,
            ChatActionException.class,
            GameState.class,
            GameStatus.class,
            HockeyMove.class,
            HockeyMoveTag.class,
            HockeyMoveType.class,
            Player.class,
            PlayerType.class,
            Puck.class,
            MultiplayerMessage.class,
            SocketMultiplayerService.class,
            ChatRemoteService.class,
            ChatRemoteServiceImpl.class,
            RmiServer.class,
            ChatUtils.class,
            DialogUtils.class,
            GameSaveUtils.class,
            XmlUtils.class,
            DocumentationUtils.class
    );

    private DocumentationUtils() {
    }

    public static Path generateDocumentation() throws IOException {
        Files.createDirectories(DOCUMENTATION_PATH.getParent());
        Files.writeString(DOCUMENTATION_PATH, buildHtml(), StandardCharsets.UTF_8);
        return DOCUMENTATION_PATH;
    }

    private static String buildHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Ice Hockey Game Documentation</title>\n");
        html.append("<style>\n");
        html.append("body{font-family:Arial,sans-serif;background:#eef6fa;color:#132f43;margin:0;padding:32px;}\n");
        html.append("h1{margin-top:0;} h2{border-bottom:2px solid #1f77b4;padding-bottom:6px;}\n");
        html.append("section{background:white;border:1px solid #bed4df;border-radius:6px;margin:18px 0;padding:18px;}\n");
        html.append("table{border-collapse:collapse;width:100%;margin:10px 0 18px;}\n");
        html.append("th,td{border:1px solid #d4e2e8;padding:8px;text-align:left;vertical-align:top;}\n");
        html.append("th{background:#123047;color:white;} code{color:#a3262a;} .meta{color:#537082;}\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<h1>Ice Hockey Game Documentation</h1>\n");
        html.append("<p class=\"meta\">Generated with Java Reflection API on ")
                .append(escape(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .append(".</p>\n");

        for (Class<?> documentedClass : DOCUMENTED_CLASSES) {
            appendClassDocumentation(html, documentedClass);
        }

        html.append("</body>\n</html>\n");
        return html.toString();
    }

    private static void appendClassDocumentation(StringBuilder html, Class<?> documentedClass) {
        html.append("<section>\n");
        html.append("<h2>").append(escape(documentedClass.getName())).append("</h2>\n");
        html.append("<p><strong>Type:</strong> ").append(escape(typeName(documentedClass))).append("</p>\n");

        if (documentedClass.getSuperclass() != null) {
            html.append("<p><strong>Superclass:</strong> <code>")
                    .append(escape(documentedClass.getSuperclass().getName()))
                    .append("</code></p>\n");
        }

        Class<?>[] interfaces = documentedClass.getInterfaces();
        if (interfaces.length > 0) {
            html.append("<p><strong>Interfaces:</strong> ")
                    .append(Arrays.stream(interfaces).map(Class::getName).map(DocumentationUtils::escape).collect(Collectors.joining(", ")))
                    .append("</p>\n");
        }

        appendConstructors(html, documentedClass);
        appendFields(html, documentedClass);
        appendMethods(html, documentedClass);
        html.append("</section>\n");
    }

    private static void appendConstructors(StringBuilder html, Class<?> documentedClass) {
        Constructor<?>[] constructors = documentedClass.getDeclaredConstructors();
        Arrays.sort(constructors, Comparator.comparing(Constructor::toString));
        html.append("<h3>Constructors</h3>\n");
        html.append("<table><tr><th>Modifier</th><th>Signature</th></tr>\n");
        for (Constructor<?> constructor : constructors) {
            html.append("<tr><td>").append(escape(modifiers(constructor.getModifiers()))).append("</td><td><code>")
                    .append(escape(constructor.getName()))
                    .append("(")
                    .append(escape(parameterTypes(constructor.getParameterTypes())))
                    .append(")</code></td></tr>\n");
        }
        html.append("</table>\n");
    }

    private static void appendFields(StringBuilder html, Class<?> documentedClass) {
        Field[] fields = documentedClass.getDeclaredFields();
        Arrays.sort(fields, Comparator.comparing(Field::getName));
        html.append("<h3>Fields</h3>\n");
        html.append("<table><tr><th>Modifier</th><th>Type</th><th>Name</th></tr>\n");
        for (Field field : fields) {
            html.append("<tr><td>").append(escape(modifiers(field.getModifiers()))).append("</td><td><code>")
                    .append(escape(field.getType().getSimpleName()))
                    .append("</code></td><td>")
                    .append(escape(field.getName()))
                    .append("</td></tr>\n");
        }
        html.append("</table>\n");
    }

    private static void appendMethods(StringBuilder html, Class<?> documentedClass) {
        Method[] methods = documentedClass.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName).thenComparing(Method::toString));
        html.append("<h3>Methods</h3>\n");
        html.append("<table><tr><th>Modifier</th><th>Return</th><th>Signature</th></tr>\n");
        for (Method method : methods) {
            html.append("<tr><td>").append(escape(modifiers(method.getModifiers()))).append("</td><td><code>")
                    .append(escape(method.getReturnType().getSimpleName()))
                    .append("</code></td><td><code>")
                    .append(escape(method.getName()))
                    .append("(")
                    .append(escape(parameterTypes(method.getParameterTypes())))
                    .append(")</code></td></tr>\n");
        }
        html.append("</table>\n");
    }

    private static String typeName(Class<?> documentedClass) {
        if (documentedClass.isEnum()) {
            return "enum";
        }
        if (documentedClass.isInterface()) {
            return "interface";
        }
        return "class";
    }

    private static String parameterTypes(Class<?>[] parameterTypes) {
        return Arrays.stream(parameterTypes)
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
    }

    private static String modifiers(int modifiers) {
        String modifierText = Modifier.toString(modifiers);
        return modifierText.isBlank() ? "package-private" : modifierText;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
