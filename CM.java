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

    public static boolean generateCode(String[] args) {
        return hasArg(args, "-c");
    }

    static public void main(String args[]) {
        String inputFileName = args[0];
        String inputFilePrefix = inputFileName.split("[.]")[0];
        String syntaxTreeFile = showSyntaxTree(args) ? inputFilePrefix + ".abs" : null;
        String symbolTableFile = showSymbolTable(args) ? inputFilePrefix + ".sym" : null;
        String codeGenFile = generateCode(args) ? inputFilePrefix + ".tm" : null;

        /* Start the parser */
        try {
            parser p = new parser(new Lexer(new FileReader(inputFileName)));
            Absyn result = (Absyn) (p.parse().value);
            ShowTreeVisitor treeVisitor = new ShowTreeVisitor();
            SemanticAnalyzer semAnalyzer = new SemanticAnalyzer();
            CodeGenerator codeGenerator = new CodeGenerator();

            if (result != null) {
                if (showSyntaxTree(args)) {
                    treeVisitor.showTree(result, syntaxTreeFile);
                }

                if ((showSymbolTable(args) || generateCode(args)) && result instanceof DecList) {
                    semAnalyzer.analyze((DecList) result, symbolTableFile);
                }

                if (generateCode(args) && semAnalyzer.getNumErrors() == 0) {
                    codeGenerator.generate(result, codeGenFile);
                } else if (semAnalyzer.getNumErrors() > 0) {
                    System.out.println("\nErrors in semantic analysis. Aborting code generation.");
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error: Failed to open the file \"" + inputFileName + "\".");
        } catch (Exception e) {
            /* do cleanup here -- possibly rethrow e */
            e.printStackTrace();
        }
    }
}
