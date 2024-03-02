/*
  Created by: Ethan Rowan
  File Name: CM.java
*/

import java.io.*;
import absyn.*;

class CM {
    public static boolean showSyntaxTree(String[] args) {
        for (String arg : args) {
            if (arg.equals("-a")) {
                return true;
            }
        }
        return false;
    }

    static public void main(String args[]) {
        /* Start the parser */
        try {
            parser p = new parser(new Lexer(new FileReader(args[0])));
            Absyn result = (Absyn) (p.parse().value);
            if (showSyntaxTree(args) && result != null) {
                System.out.println("The abstract syntax tree is:");
                AbsynVisitor visitor = new ShowTreeVisitor();
                result.accept(visitor, 0);
            }
        } catch (Exception e) {
            /* do cleanup here -- possibly rethrow e */
            e.printStackTrace();
        }
    }
}
