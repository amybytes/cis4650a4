/*
 * Created By: Ethan Rowan
 * File Name: CodeGenerator.java
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import absyn.*;

public class CodeGenerator implements AbsynVisitor {
    private StringBuilder output;
    private int mainEntry, globalOffset;
    private int inputLoc, outputLoc;
    private int emitLoc = 0; // Current instruction being generated
    private int highEmitLoc = 0; // Next available space (for next instruction)
    private int fpOffset;

    // Registers
    private int AC = 0;
    private int AC1 = 1;
    private int FP = 5;
    private int GP = 6;
    private int PC = 7;

    public CodeGenerator() {
        output = new StringBuilder();
    }

    public void generate(Absyn tree, String outputFile) {
        generatePrelude();
        generateIORoutines();

        tree.accept(this, 0, false);

        generateFinale();
        if (writeFile(outputFile)) {
            System.out.println("Generated code written to \"" + outputFile + "\".");
        }
    }

    private boolean writeFile(String outputFile) {
        try {
            FileOutputStream outputStream = new FileOutputStream(new File(outputFile));
            outputStream.write(output.toString().getBytes());
            outputStream.close();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to write generated code to \"" + outputFile + "\".");
            return false;
        }
    }

    private void generatePrelude() {
        emitComment("Prelude");
        emitRM(OpCode.LD, GP, 0, AC, "Load GP with max address");
        emitRM(OpCode.LDA, FP, 0, GP, "Copy GP to FP");
        emitRM(OpCode.ST, AC, 0, AC, "Clear memory location 0");
    }

    private void backpatch(String jmpMsg, Runnable code) {
        int savedLocStart = emitSkip(1);
        code.run();
        int savedLocEnd = emitSkip(0);
        emitBackup(savedLocStart);
        emitRMAbs(OpCode.LDA, PC, savedLocEnd, jmpMsg);
        emitRestore();
    }

    private void generateIORoutines() {
        emitComment("Input routine");
        backpatch("Jump over I/O routines", () -> {
            inputLoc = emitLoc;
            emitRM(OpCode.ST, AC, -1, FP, "Store return address");
            emitRO(OpCode.IN, 0, 0, 0, "Get input");
            // emitRM(OpCode.LD, FP, 0, FP, "Load old FP");
            emitRM(OpCode.LD, PC, -1, FP, "Return back to caller");
            emitComment("Output routine");
            outputLoc = emitLoc;
            emitRM(OpCode.ST, AC, -1, FP, "Store return address");
            emitRM(OpCode.LD, 0, -2, FP, "Load output value");
            emitRO(OpCode.OUT, 0, 0, 0, "Display output");
            // emitRM(OpCode.LD, FP, 0, FP, "Load old FP");
            emitRM(OpCode.LD, PC, -1, FP, "Return back to caller");
        });
    }

    private void generateFinale() {
        emitComment("Finale");
        emitRM(OpCode.ST, FP, globalOffset, FP, "Push old frame pointer");
        emitRM(OpCode.LDA, FP, globalOffset, FP, "Push frame");
        emitRM(OpCode.LDA, AC, 1, PC, "Load AC with return pointer");
        emitRMAbs(OpCode.LDA, PC, mainEntry, "Jump to main location");
        emitRM(OpCode.LD, FP, 0, FP, "Pop frame");
        emitRO(OpCode.HALT, 0, 0, 0, "");
    }

    private void emitRO(OpCode code, int r, int s, int t, String c) {
        emit(code, OpType.RO, r, s, t, c, false);
    }

    private void emitRM(OpCode code, int r, int d, int s, String c) {
        emit(code, OpType.RM, r, d, s, c, false);
    }

    private void emitRMAbs(OpCode code, int r, int a, String c) {
        emit(code, OpType.RM, r, a, 0, c, true);
    }

    private int emitSkip(int distance) {
        int i = emitLoc;
        emitLoc += distance;
        if (highEmitLoc < emitLoc) {
            highEmitLoc = emitLoc;
        }
        return i;
    }

    private void emitBackup(int loc) {
        if (loc > highEmitLoc) {
            emitComment("BUG in emitBackup!!");
        }
        emitLoc = loc;
    }

    private void emitRestore() {
        emitLoc = highEmitLoc;
    }

    private void emitComment(String c) {
        output.append(String.format("* %s\n", c));
    }

    private void emit(OpCode code, OpType type, int r, int s, int t, String c, boolean isAbsolute) {
        if (type == OpType.RO) {
            output.append(String.format("%3d: %5s  %d,%d,%d", emitLoc, code.toString(), r, s, t));
        } else if (type == OpType.RM) {
            int arg2 = isAbsolute ? s - (emitLoc + 1) : s;
            int arg3 = isAbsolute ? PC : t;
            output.append(String.format("%3d: %5s  %d,%d(%d)", emitLoc, code.toString(), r, arg2, arg3));
        }
        output.append(String.format("\t%s\n", c));
        emitLoc++;

        if (highEmitLoc < emitLoc) {
            highEmitLoc = emitLoc;
        }
    }

    private void allocateVar(VarDec varDec, int level, int size) {
        varDec.nestLevel = level;
        if (level == 0) {
            emitComment("Allocating variable " + varDec.name);
            varDec.offset = globalOffset;
            globalOffset -= size;
        } else {
            emitComment("Processing local var: " + varDec.name);
            varDec.offset = fpOffset--;
        }
    }

    private int getCallerAddr(CallExp exp) {
        if (exp.func.equals("input")) {
            return inputLoc;
        } else if (exp.func.equals("output")) {
            return outputLoc;
        }

        if (exp.dtype != null) {
            return ((FunctionDec) exp.dtype).funaddr;
        }
        return 0;
    }

    public void visit(NameTy type, int level, boolean isAddress) {
    }

    public void visit(IndexVar var, int level, boolean isAddress) {
        var.index.accept(this, level, false);
    }

    public void visit(SimpleVar var, int level, boolean isAddress) {
    }

    public void visit(AssignExp exp, int level, boolean isAddress) {
        exp.lhs.accept(this, level, false);
        exp.rhs.accept(this, level, false);
    }

    public void visit(BoolExp exp, int level, boolean isAddress) {
    }

    public void visit(CallExp exp, int level, boolean isAddress) {
        // TODO: change fpOffset to equal the negative number of arguments pushed onto
        // the stack
        fpOffset = -1;

        int callerAddr = getCallerAddr(exp);

        emitRM(OpCode.ST, FP, fpOffset - 2, FP, "Store old FP in stackframe");
        emitRM(OpCode.LDA, FP, fpOffset - 2, FP, "Load new FP");

        // TODO: push computed arguments onto stack and remove test and test2 arguments
        exp.args.accept(this, level, false);
        emitRM(OpCode.LDC, AC1, 123, 0, "test");
        emitRM(OpCode.ST, AC1, -2, FP, "test2");

        emitRM(OpCode.LDA, AC, 1, PC, "Load return address into AC");
        emitRMAbs(OpCode.LDA, PC, callerAddr, "Jump to caller address");
        emitRM(OpCode.LD, FP, 0, FP, "Load old FP");
    }

    public void visit(CompoundExp exp, int level, boolean isAddress) {
        exp.decs.accept(this, level + 1, false);
        exp.exps.accept(this, level + 1, false);
    }

    public void visit(IfExp exp, int level, boolean isAddress) {
        exp.test.accept(this, level, false);
        exp.thenpart.accept(this, level, false);
        exp.elsepart.accept(this, level, false);
    }

    public void visit(IntExp exp, int level, boolean isAddress) {
    }

    public void visit(NilExp exp, int level, boolean isAddress) {
    }

    public void visit(OpExp exp, int level, boolean isAddress) {
        exp.left.accept(this, level, false);
        exp.right.accept(this, level, false);
    }

    public void visit(ReturnExp exp, int level, boolean isAddress) {
        exp.exp.accept(this, level, false);
    }

    public void visit(VarExp exp, int level, boolean isAddress) {
        exp.var.accept(this, level, false);
    }

    public void visit(WhileExp exp, int level, boolean isAddress) {
        exp.test.accept(this, level, false);
        exp.body.accept(this, level, false);
    }

    public void visit(ExpList expList, int level, boolean isAddress) {
        while (expList != null && expList.head != null) {
            expList.head.accept(this, level, false);
            expList = expList.tail;
        }
    }

    public void visit(DecList decList, int level, boolean isAddress) {
        while (decList != null && decList.head != null) {
            decList.head.accept(this, level, false);
            decList = decList.tail;
        }
    }

    public void visit(VarDecList varDecList, int level, boolean isAddress) {
        while (varDecList != null && varDecList.head != null) {
            varDecList.head.accept(this, level, false);
            varDecList = varDecList.tail;
        }
    }

    public void visit(FunctionDec dec, int level, boolean isAddress) {
        boolean isPrototype = dec.body instanceof NilExp;

        if (!isPrototype) {
            if (dec.func.equals("main")) {
                mainEntry = emitLoc + 1;
            }

            emitComment("Processing function: " + dec.func);
            dec.funaddr = emitLoc + 1;

            backpatch("Jump around function", () -> {
                // int previousSP = fpOffset;

                emitRM(OpCode.ST, AC, -1, FP, "Store return address");

                dec.params.accept(this, level + 1, false);
                dec.body.accept(this, level, false);

                // TODO: handle this stuff in return exp?
                // fpOffset = previousSP;
                emitRM(OpCode.LD, PC, -1, FP, "Return back to caller");
            });
        }
    }

    public void visit(ArrayDec varDec, int level, boolean isAddress) {
        allocateVar(varDec, level, varDec.size);
    }

    public void visit(SimpleDec varDec, int level, boolean isAddress) {
        allocateVar(varDec, level, 1);
    }
}
