package org.objectrepository.services;

import org.apache.commons.exec.CommandLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Commandizer
 * <p/>
 * Parses an Instruction type into "flat" command line parameters:
 * key value.
 * <p/>
 * The result is a shell command like:
 * /path/to/the/bash -l -c '/path/to/the/shell/script -key1 "value1" -key2 "value 2" -key3 "This key\'s \"escaped\" value"'
 * <p/>
 * A single quote may not occur between single quotes, even when preceded by a backslash.
 * Here we substitute the single quote with a double escaped quote.
 *
 * @author Jozsef Gabor Bone <bonej@ceu.hu>
 * @author Lucien van Wouw <lwo@iisg.nl>
 */

class Commandizer {

    static CommandLine makeCommand(String bash, String commandToRun, String message) {

        final boolean use_native_shell = (bash == null);
        final CommandLine command = (use_native_shell)
                ? new CommandLine(commandToRun)
                : new CommandLine(bash);

        if (!use_native_shell) {
            command.addArgument("-l", false);
            command.addArgument("-c", false);
            command.addArgument("'" + commandToRun, false);
        }

        command.addArgument( escaping(message), false);

        if (!use_native_shell)
            command.addArgument("'", false);

        return command;
    }

    /**
     * escaping
     * <p/>
     * Escapes key tokens of Linux ( we will target this OS )
     *
     * @param text String to normalize
     * @return The normalized string
     */
    private static String escaping(String text) {

        if (text == null || text.trim().isEmpty())
            return null;

        final List<Character> escapeChars = new ArrayList<>(2);
        escapeChars.add('$');
        escapeChars.add('\\');
        escapeChars.add('"');

        final List<Character> substituteWithQuote = new ArrayList<>(1);
        substituteWithQuote.add('\'');

        final StringBuilder sb = new StringBuilder(text.trim());
        for (int i = sb.length() - 1; i != -1; i--) {
            char c = sb.charAt(i);
            if (escapeChars.contains(c)) {
                sb.insert(i, "\\");
            } else if (substituteWithQuote.contains(c)) {
                sb.deleteCharAt(i);
                sb.insert(i, "\\\"");
            }
        }

        return sb.toString();
    }

}
