import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    private static final int HTML_BUF = 4096;
    private static final Locale LOCALE = Locale.forLanguageTag("es-ES");
    private static final Set<String> SIGLAS = new HashSet<>(Arrays.asList("HTML", "ONU", "NASA"));

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java Main <fichero>");
            System.exit(1);
        }

        String inputPath = args[0];
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            System.err.println("No se puede abrir el fichero: " + inputPath);
            System.exit(1);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
                PrintWriter html = new PrintWriter(
                        new OutputStreamWriter(new FileOutputStream("salida.html"), StandardCharsets.UTF_8), true)) {

            escribirCabecera(html);

            StringBuilder buffer = new StringBuilder();
            String line;
            boolean capNextParrafo = true;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (buffer.length() > 0) {
                        // Quitar salto final si existe (equivalente al idx-- del C)
                        if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == '\n') {
                            buffer.setLength(buffer.length() - 1);
                        }
                        procesarParrafo(html, buffer.toString(), capNextParrafo);
                        buffer.setLength(0);
                        capNextParrafo = true;
                    }
                } else {
                    buffer.append(line).append('\n');
                }
            }

            if (buffer.length() > 0) {
                if (buffer.charAt(buffer.length() - 1) == '\n')
                    buffer.setLength(buffer.length() - 1);
                procesarParrafo(html, buffer.toString(), capNextParrafo);
            }

            escribirPie(html);
        } catch (IOException e) {
            System.err.println("Error de E/S: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void escribirCabecera(PrintWriter html) {
        html.printf("<!DOCTYPE html>%n<html lang=\"es\">%n<head>%n");
        html.printf("<meta charset=\"UTF-8\">%n");
        html.printf("<title>Comparación Original vs Convertido</title>%n");
        html.printf("<style>%n");
        html.printf("table { border-collapse: collapse; width: 100%%; margin:%n20px; }%n");
        html.printf("th, td { border: 1px solid #999; padding: 10px;%nvertical-align: top; text-align: left; }%n");
        html.printf("th { background-color: #0055aa; color: white;%nfont-weight: bold; }%n");
        html.printf(".negrita { font-weight: bold; color: #0055aa; }%n");
        html.printf(".original { color: #666; }%n");
        html.printf("</style>%n</head>%n<body>%n<table>%n");
        html.printf("<tr><th>Original</th><th>Convertido</th></tr>%n");
    }

    private static void escribirPie(PrintWriter html) {
        html.printf("%n");
        html.println("</table>");
        html.println("</body>");
        html.println("</html>");
    }

    private static void procesarParrafo(PrintWriter html, String parrafo, boolean capNextInicial) {
        String originalHtml = htmlEscape(parrafo);
        String convertido = convertirAHtml(parrafo, capNextInicial);
        html.printf("<tr><td class=\"original\">%n%s</td><td>%s</td></tr>%n", originalHtml, convertido);
    }

    private static boolean esMayusculaTotal(String s) {
        boolean hasLetters = false;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            if (Character.isLetter(ch)) {
                hasLetters = true;
                if (!Character.isUpperCase(ch))
                    return false;
            }
        }
        return hasLetters;
    }

    private static boolean palabraTieneLetras(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i)))
                return true;
        }
        return false;
    }

    private static boolean esSiglaReservada(String s) {
        String up = s.toUpperCase(Locale.ROOT);
        return SIGLAS.contains(up);
    }

    private static String aMinusculas(String s) {
        return s.toLowerCase(LOCALE);
    }

    private static String capitalizar(String s) {
        if (s.isEmpty())
            return s;
        StringBuilder r = new StringBuilder(s.length());
        boolean firstDone = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (!firstDone && Character.isLetter(ch)) {
                r.append(Character.toUpperCase(ch));
                firstDone = true;
            } else if (Character.isLetter(ch)) {
                r.append(Character.toLowerCase(ch));
            } else {
                r.append(ch);
            }
        }
        return r.toString();
    }

    private static void procesarBloqueMayusculas(StringBuilder dest, String bloque, boolean capitalizarPrimera) {
        StringBuilder conv = new StringBuilder(bloque.length());
        boolean primera = false;
        for (int i = 0; i < bloque.length(); i++) {
            char ch = bloque.charAt(i);
            if (Character.isLetter(ch)) {
                if (!primera && capitalizarPrimera) {
                    conv.append(Character.toUpperCase(ch));
                    primera = true;
                } else {
                    conv.append(Character.toLowerCase(ch));
                }
            } else {
                conv.append(ch);
            }
        }
        dest.append("<span class=\"negrita\">").append(htmlEscape(conv.toString())).append("</span>");
    }

    private static boolean esDelimitador(char ch) {
        return Character.isWhitespace(ch) || ch == '.' || ch == '!' || ch == '?' || ch == ',' || ch == ';' || ch == ':'
                || ch == '\n' || ch == '\u00A1' /* ¡ */ || ch == '\u00BF' /* ¿ */;
    }

    private static boolean esFinDeFrase(char ch) {
        return ch == '.' || ch == '!' || ch == '?' || ch == '\u00A1' || ch == '\u00BF';
    }

    private static String convertirAHtml(String src, boolean capNextInicial) {
        StringBuilder dest = new StringBuilder(HTML_BUF);
        int i = 0, start = 0;
        boolean enBloque = false;
        int bloqueStart = -1;
        boolean bloqueCap = false;
        boolean capNext = capNextInicial;

        int j = 0;
        while (j < src.length() && Character.isWhitespace(src.charAt(j)))
            j++;
        if (j < src.length()) {
            char c0 = src.charAt(j);
            if (c0 == '\u00A1' || c0 == '\u00BF')
                capNext = true;
        }

        while (i < src.length()) {
            char ch = src.charAt(i);
            if (esDelimitador(ch)) {
                if (i > start) {
                    String palabra = src.substring(start, i);
                    if (esMayusculaTotal(palabra)) {
                        if (esSiglaReservada(palabra)) {
                            if (enBloque) {
                                String bloque = src.substring(bloqueStart, start);
                                procesarBloqueMayusculas(dest, bloque, bloqueCap);
                                enBloque = false;
                            }
                            dest.append(htmlEscape(palabra));
                        } else if (!enBloque) {
                            enBloque = true;
                            bloqueStart = start;
                            bloqueCap = capNext;
                        }
                    } else if (palabraTieneLetras(palabra)) {
                        if (enBloque) {
                            String bloque = src.substring(bloqueStart, start);
                            procesarBloqueMayusculas(dest, bloque, bloqueCap);
                            enBloque = false;
                        }
                        String out = capNext ? capitalizar(palabra) : aMinusculas(palabra);
                        dest.append(htmlEscape(out));
                    } else {
                        if (!enBloque)
                            dest.append(htmlEscape(palabra));
                    }
                    capNext = false;
                }

                if (!enBloque)
                    dest.append(htmlEscape(Character.toString(ch)));

                if (esFinDeFrase(ch)) {
                    capNext = true;
                    if (enBloque) {
                        String bloque = src.substring(bloqueStart, i + 1);
                        procesarBloqueMayusculas(dest, bloque, bloqueCap);
                        enBloque = false;
                        bloqueStart = -1;
                    }
                }
                start = i + 1;
            }
            i++;
        }

        if (i > start) {
            String palabra = src.substring(start, i);
            if (esMayusculaTotal(palabra) && !esSiglaReservada(palabra)) {
                if (!enBloque) {
                    enBloque = true;
                    bloqueStart = start;
                    bloqueCap = capNext;
                }
                String bloque = src.substring(bloqueStart, i);
                procesarBloqueMayusculas(dest, bloque, bloqueCap);
            } else if (palabraTieneLetras(palabra)) {
                if (enBloque) {
                    String bloque = src.substring(bloqueStart, start);
                    procesarBloqueMayusculas(dest, bloque, bloqueCap);
                    enBloque = false;
                }
                String out = capNext ? capitalizar(palabra) : aMinusculas(palabra);
                dest.append(htmlEscape(out));
            } else if (!enBloque) {
                dest.append(htmlEscape(palabra));
            } else {
                String bloque = src.substring(bloqueStart, i);
                procesarBloqueMayusculas(dest, bloque, bloqueCap);
            }
        }

        return dest.toString();
    }

    private static String htmlEscape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\n':
                    out.append("<br>\n");
                    break;
                default:
                    out.append(ch);
            }
        }
        return out.toString();
    }
}
