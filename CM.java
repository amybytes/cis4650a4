/*
  Created by: Ethan Rowan
  File Name: CM.java
*/

import java.io.*;
import absyn.*;

class CM {
    public static boolean hasArg(String[] args, String arg) {
        for (String a : args) {
            if (a.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static boolean showSyntaxTree(String[] args) {
        return hasArg(args, "-a");
    }

    public static boolean showSymbolTable(String[] args) {
        return hasArg(args, "-s");
    }

    static public void main(String args[]) {
        String inputFileName = args[0];
        String inputFilePrefix = inputFileName.split("[.]")[0];
        String syntaxTreeFile = showSyntaxTree(args) ? inputFilePrefix + ".abs" : null;
        String symbolTableFile = showSymbolTable(args) ? inputFilePrefix + ".sym" : null;

        /* Start the parser */
        try {
            parser p = new parser(new Lexer(new FileReader(inputFileName)));
            Absyn result = (Absyn) (p.parse().value);
            if (showSyntaxTree(args) && result != null) {
                ShowTreeVisitor visitor = new ShowTreeVisitor();
                visitor.showTree(result, syntaxTreeFile);
            }

            if (showSymbolTable(args) && result != null && result instanceof DecList) {
                SemanticAnalyzer semAnalyzer = new SemanticAnalyzer();
                semAnalyzer.analyze((DecList) result, symbolTableFile);
            }
        } catch (Exception e) {
            /* do cleanup here -- possibly rethrow e */
            e.printStackTrace();
        }
    }
}
