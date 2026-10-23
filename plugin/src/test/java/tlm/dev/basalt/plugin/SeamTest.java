package tlm.dev.basalt.plugin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeamTest {

    private static final List<String> MODULES = List.of("api", "core", "nms", "plugin");
    private static final Set<String> SKIPPED_FOLDERS = Set.of("build", ".gradle", ".git", ".idea", ".kotlin", "run", "out");
    private static final List<String> SCANNED_EXTENSIONS = List.of(".java", ".kts", ".yml", ".yaml", ".properties");
    private static final String API_SOURCES = "api/src/main/java/";

    private static final Pattern MOJANG = Pattern.compile("net[.]minecraft[.]|org[.]bukkit[.]craftbukkit[.]|com[.]mojang[.]");
    private static final Pattern MOJANG_BUT_BRIGADIER = Pattern.compile("net[.]minecraft[.]|org[.]bukkit[.]craftbukkit[.]|com[.]mojang[.](?!brigadier[.])");
    private static final Pattern BUKKIT = Pattern.compile("org[.]bukkit\\b");
    private static final Map<String, Set<String>> FAMILIES = Map.of(
            "api", Set.of(),
            "core", Set.of("chunk", "entity", "interaction", "visibility"),
            "nms", Set.of("channel", "interaction", "packet", "view"),
            "plugin", Set.of("command", "facade", "interaction", "listener", "masking"));
    private static final Map<String, Set<String>> PLUMBING = Map.of(
            "api", Set.of(""),
            "core", Set.of("debug", "platform", "position"),
            "nms", Set.of("", "platform", "port"),
            "plugin", Set.of("", "config", "lifecycle"));
    private static final Set<String> COMPOSITION_ROOTS = Set.of("nms", "plugin");
    private static final Map<String, Pattern> HIGHER_MODULES = Map.of(
            "api", Pattern.compile("tlm[.]dev[.]basalt[.](core|nms|plugin)\\b"),
            "core", Pattern.compile("tlm[.]dev[.]basalt[.](nms|plugin)\\b"),
            "nms", Pattern.compile("tlm[.]dev[.]basalt[.]plugin\\b"));
    private static final Pattern WORK_MARKER = Pattern.compile("\\b(TO[D]O|FIX[M]E)\\b");
    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "\\bpublic\\s+(?:(?:static|final|abstract|sealed|non-sealed|strictfp)\\s+)*(?:class|interface|enum|record|@interface)\\s+(\\w+)");
    private static final Pattern ANNOTATIONS_ONLY = Pattern.compile("(?:\\s*@[\\w.]+(?:\\([^)]*\\))?)*\\s*");

    private static List<SourceFile> files;

    @BeforeAll
    static void readSources() throws IOException {
        Path root = Path.of(System.getProperty("basalt.root"));
        try (Stream<Path> paths = Files.walk(root)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> isScanned(root.relativize(path)))
                    .map(path -> SourceFile.read(root, path))
                    .toList();
        }
    }

    @Test
    void readsEveryModule() {
        for (String module : MODULES) {
            assertTrue(files.stream().anyMatch(file -> file.module().equals(module) && file.isJava()),
                    "no Java source found in " + module);
        }
    }

    @Test
    void mojangClassesStayInNms() {
        List<String> found = new ArrayList<>();
        found.addAll(occurrences(files.stream().filter(file -> !file.module().equals("nms") && !file.module().equals("plugin")), MOJANG));
        found.addAll(occurrences(files.stream().filter(file -> file.module().equals("plugin")), MOJANG_BUT_BRIGADIER));

        assertEquals(List.of(), found, "Mojang or CraftBukkit classes outside nms, Brigadier aside in plugin");
    }

    @Test
    void everyPackageIsAFamilyOrPlumbing() {
        List<String> found = new ArrayList<>();
        for (SourceFile file : mainSources()) {
            String segment = file.packageSegment();
            if (!FAMILIES.get(file.module()).contains(segment) && !PLUMBING.get(file.module()).contains(segment)) {
                found.add(file.path() + " in undeclared package '" + segment + "'");
            }
        }

        assertEquals(List.of(), found, "packages that are neither a family nor plumbing");
    }

    @Test
    void familiesNeverReachAnotherFamily() {
        List<String> found = new ArrayList<>();
        for (SourceFile file : mainSources()) {
            if (COMPOSITION_ROOTS.contains(file.module()) && file.packageSegment().isEmpty()) {
                continue;
            }
            for (String family : FAMILIES.get(file.module())) {
                if (!family.equals(file.packageSegment())) {
                    Pattern otherFamily = Pattern.compile("tlm[.]dev[.]basalt[.]" + file.module() + "[.]" + family + "\\b");
                    found.addAll(occurrences(Stream.of(file), otherFamily));
                }
            }
        }

        assertEquals(List.of(), found, "a family or plumbing package reaching a family of its module");
    }

    @Test
    void coreNeverTouchesBukkit() {
        List<String> found = occurrences(files.stream().filter(file -> file.module().equals("core")), BUKKIT);

        assertEquals(List.of(), found, "Bukkit in core");
    }

    @Test
    void modulesNeverReachAHigherModule() {
        List<String> found = new ArrayList<>();
        HIGHER_MODULES.forEach((module, higher) ->
                found.addAll(occurrences(files.stream().filter(file -> file.module().equals(module)), higher)));

        assertEquals(List.of(), found, "reference to a higher module");
    }

    @Test
    void onlyApiJavadocIsAComment() {
        List<String> found = new ArrayList<>();
        for (SourceFile file : files) {
            for (int offset : file.forbiddenCommentOffsets()) {
                found.add(file.path() + ":" + file.lineOf(offset));
            }
        }

        assertEquals(List.of(), found, "comments outside the api Javadoc");
    }

    @Test
    void noWorkMarkerIsLeft() {
        assertEquals(List.of(), occurrences(files.stream(), WORK_MARKER), "work markers left in the code");
    }

    @Test
    void everyPublicApiTypeHasJavadoc() {
        List<String> found = new ArrayList<>();
        for (SourceFile file : files) {
            if (!file.path().startsWith(API_SOURCES) || !file.isJava()) {
                continue;
            }
            Scan scan = Scan.of(file.text());
            Matcher declaration = PUBLIC_TYPE.matcher(scan.code());
            while (declaration.find()) {
                if (!scan.documents(file.text(), declaration.start())) {
                    found.add(file.path() + ":" + file.lineOf(declaration.start()) + " " + declaration.group(1));
                }
            }
        }

        assertEquals(List.of(), found, "public api types without Javadoc");
    }

    private static boolean isScanned(Path relative) {
        for (Path part : relative) {
            if (SKIPPED_FOLDERS.contains(part.toString())) {
                return false;
            }
        }
        String name = relative.getFileName().toString();
        return SCANNED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private static List<SourceFile> mainSources() {
        return files.stream().filter(file -> file.path().startsWith(file.module() + "/src/main/java/") && file.isJava()).toList();
    }

    private static List<String> occurrences(Stream<SourceFile> scanned, Pattern pattern) {
        List<String> found = new ArrayList<>();
        scanned.forEach(file -> {
            Matcher matcher = pattern.matcher(file.text());
            while (matcher.find()) {
                found.add(file.path() + ":" + file.lineOf(matcher.start()) + " " + matcher.group());
            }
        });
        return found;
    }

    private record SourceFile(String path, String module, String text) {

        static SourceFile read(Path root, Path file) {
            String path = root.relativize(file).toString().replace('\\', '/');
            String first = path.substring(0, Math.max(path.indexOf('/'), 0));
            try {
                return new SourceFile(path, MODULES.contains(first) ? first : "root", Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }

        boolean isJava() {
            return path.endsWith(".java");
        }

        String packageSegment() {
            String modulePackage = module + "/src/main/java/tlm/dev/basalt/" + module + "/";
            if (!path.startsWith(modulePackage)) {
                return "?";
            }
            String rest = path.substring(modulePackage.length());
            int slash = rest.indexOf('/');
            return slash < 0 ? "" : rest.substring(0, slash);
        }

        int lineOf(int offset) {
            int line = 1;
            for (int index = 0; index < offset; index++) {
                if (text.charAt(index) == '\n') {
                    line++;
                }
            }
            return line;
        }

        List<Integer> forbiddenCommentOffsets() {
            if (isJava() || path.endsWith(".kts")) {
                boolean javadocAllowed = path.startsWith(API_SOURCES);
                return Scan.of(text).comments().stream()
                        .filter(comment -> !(javadocAllowed && comment.isJavadoc()))
                        .map(Comment::start)
                        .toList();
            }
            if (path.endsWith(".properties")) {
                return lineStarts().stream().filter(this::isPropertiesComment).toList();
            }
            return lineStarts().stream().map(this::yamlCommentOffset).filter(offset -> offset >= 0).toList();
        }

        private List<Integer> lineStarts() {
            List<Integer> starts = new ArrayList<>();
            starts.add(0);
            for (int index = 0; index < text.length(); index++) {
                if (text.charAt(index) == '\n') {
                    starts.add(index + 1);
                }
            }
            return starts;
        }

        private boolean isPropertiesComment(int lineStart) {
            String line = text.substring(lineStart, lineEnd(lineStart)).strip();
            return line.startsWith("#") || line.startsWith("!");
        }

        private int yamlCommentOffset(int lineStart) {
            char quote = 0;
            for (int index = lineStart; index < lineEnd(lineStart); index++) {
                char current = text.charAt(index);
                if (quote != 0) {
                    quote = current == quote ? 0 : quote;
                } else if (current == '\'' || current == '"') {
                    quote = current;
                } else if (current == '#' && (index == lineStart || Character.isWhitespace(text.charAt(index - 1)))) {
                    return index;
                }
            }
            return -1;
        }

        private int lineEnd(int lineStart) {
            int end = text.indexOf('\n', lineStart);
            return end < 0 ? text.length() : end;
        }
    }

    private record Comment(int start, int end, String text) {

        boolean isJavadoc() {
            return text.startsWith("/**") && !text.startsWith("/**/");
        }
    }

    private record Scan(List<Comment> comments, String code) {

        static Scan of(String source) {
            StringBuilder code = new StringBuilder(source);
            List<Comment> comments = new ArrayList<>();
            int index = 0;
            while (index < source.length()) {
                int end;
                if (source.startsWith("\"\"\"", index)) {
                    end = closingIndex(source, index + 3, "\"\"\"", false);
                } else if (source.charAt(index) == '"' || source.charAt(index) == '\'') {
                    end = closingIndex(source, index + 1, String.valueOf(source.charAt(index)), true);
                } else if (source.startsWith("//", index)) {
                    int lineEnd = source.indexOf('\n', index);
                    end = lineEnd < 0 ? source.length() : lineEnd;
                    comments.add(new Comment(index, end, source.substring(index, end)));
                } else if (source.startsWith("/*", index)) {
                    int close = source.indexOf("*/", index + 2);
                    end = close < 0 ? source.length() : close + 2;
                    comments.add(new Comment(index, end, source.substring(index, end)));
                } else {
                    index++;
                    continue;
                }
                blank(code, index, end);
                index = end;
            }
            return new Scan(List.copyOf(comments), code.toString());
        }

        boolean documents(String source, int declaration) {
            Comment last = null;
            for (Comment comment : comments) {
                if (comment.end() <= declaration) {
                    last = comment;
                }
            }
            return last != null && last.isJavadoc()
                    && ANNOTATIONS_ONLY.matcher(source.substring(last.end(), declaration)).matches();
        }

        private static int closingIndex(String source, int from, String closer, boolean singleLine) {
            int index = from;
            while (index < source.length()) {
                char current = source.charAt(index);
                if (current == '\\') {
                    index += 2;
                } else if (singleLine && current == '\n') {
                    return index;
                } else if (source.startsWith(closer, index)) {
                    return index + closer.length();
                } else {
                    index++;
                }
            }
            return source.length();
        }

        private static void blank(StringBuilder code, int start, int end) {
            for (int index = start; index < end; index++) {
                if (code.charAt(index) != '\n') {
                    code.setCharAt(index, ' ');
                }
            }
        }
    }
}
