/*
 * Created By: Ethan Rowan
 * File Name: CodeGenerator.java
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import absyn.*;

public class CodeGenerator implements AbsynVisitor {
    private StringBuilder output;
    private int mainEntry, globalOffset;
    private int inputLoc, outputLoc;
    private int emitLoc = 0; // Current instruction being generated
    private int highEmitLoc = 0; // Next available space (for next instruction)

    private FunctionDec currentFunc = null;
    private Map<FunctionDec, Integer> fpOffsets; // Offsets for each function
    private List<Temp> temporaries; // Temporary memory addresses for calculations

    // Registers
    private int AC = 0;
    private int AC1 = 1;
    private int AC2 = 2;
    private int FP = 5;
    private int GP = 6;
    private int PC = 7;

    // Boolean constants
    private int TRUE = 1;
    private int FALSE = 0;

    public CodeGenerator() {
        output = new StringBuilder();
        fpOffsets = new HashMap<FunctionDec, Integer>();
        temporaries = new ArrayList<Temp>();
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
            emitRM(OpCode.LD, PC, -1, FP, "Return back to caller");
            emitComment("Output routine");
            outputLoc = emitLoc;
            emitRM(OpCode.ST, AC, -1, FP, "Store return address");
            emitRM(OpCode.LD, 0, -2, FP, "Load output value");
            emitRO(OpCode.OUT, 0, 0, 0, "Display output");
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
        String tempOut = null;
        if (type == OpType.RO) {
            tempOut = String.format("%3d: %5s  %d,%d,%d", emitLoc, code.toString(), r, s, t);
        } else if (type == OpType.RM) {
            int arg2 = isAbsolute ? s - (emitLoc + 1) : s;
            int arg3 = isAbsolute ? PC : t;
            tempOut = String.format("%3d: %5s  %d,%d(%d)", emitLoc, code.toString(), r, arg2, arg3);
        }
        output.append(tempOut);

        // Calculate uniform spacing between instruction and comment
        final int MAX_SPACING = 22;
        int spaces = Math.max(MAX_SPACING - tempOut.length(), 2);
        for (int i = 0; i < spaces; i++) {
            output.append(" ");
        }
        output.append(String.format("%s\n", c));
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
            int fpOffset = getFpOffset();
            varDec.offset = fpOffset;
            setFpOffset(fpOffset - size);
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

    private int getFpOffset() {
        return fpOffsets.getOrDefault(currentFunc, 0);
    }

    private void setFpOffset(int offset) {
        fpOffsets.put(currentFunc, offset);
    }

    private void setVarMemory(int val, int offset, boolean isLocal) {
        emitRM(OpCode.LDC, AC, val, 0, "Load constant into AC");
        emitRM(OpCode.ST, AC, offset, isLocal ? FP : GP, "Store result in memory");
    }

    private Temp getNextTempOffset() {
        temporaries.add(0, new Temp(getFpOffset() - temporaries.size() - 1, Temp.LOCAL_SCOPE));
        return temporaries.get(0);
    }

    private void disposeTemporaries(int amount) {
        while (amount-- > 0 && temporaries.size() > 0) {
            temporaries.remove(0);
        }
    }

    private void clearTemporaries() {
        temporaries.clear();
    }

    public void visit(NameTy type, int level, boolean isAddress) {
    }

    public void visit(IndexVar var, int offset, boolean isAddress) {
        var.index.accept(this, offset, false);
    }

    public void visit(SimpleVar var, int offset, boolean isAddress) {
    }

    public void visit(AssignExp exp, int offset, boolean isAddress) {
        exp.lhs.accept(this, offset - 1, false);
        exp.rhs.accept(this, offset - 2, isAddress);
        VarDec dec = (VarDec) exp.dtype;
        boolean isRightAddr = !(exp.rhs instanceof IntExp || exp.rhs instanceof BoolExp);

        emitComment("Evaluating assignment");

        exp.temp = exp.rhs.temp;
        emitRM(isRightAddr ? OpCode.LD : OpCode.LDC, AC, exp.rhs.temp.offset,
                exp.rhs.temp.scope == Temp.LOCAL_SCOPE ? FP : GP,
                "Loading right operand into AC");
        emitRM(OpCode.ST, AC, dec.offset, dec.nestLevel > 0 ? FP : GP, "Store final result");
    }

    public void visit(BoolExp exp, int offset, boolean isAddress) {
        int value = ((BoolExp) exp).value ? 1 : 0;
        System.out.println(value + " " + exp.value);
        exp.temp = new Temp(value, Temp.LOCAL_SCOPE);
        if (isAddress) {
            setVarMemory(value, offset, true);
        }
    }

    public void visit(CallExp exp, int offset, boolean isAddress) {
        int callerAddr = getCallerAddr(exp);
        int fpOffset = getFpOffset();

        emitComment("Call to " + exp.func);

        // Push arguments onto the stack
        int futureFpOffset = fpOffset - 2;
        ExpList args = exp.args;
        while (args != null && args.head != null) {
            args.head.accept(this, futureFpOffset--, true);
            args = args.tail;
        }

        emitRM(OpCode.ST, FP, fpOffset, FP, "Store old FP in stackframe");
        emitRM(OpCode.LDA, FP, fpOffset, FP, "Load new FP");

        emitRM(OpCode.LDA, AC, 1, PC, "Load return address into AC");
        emitRMAbs(OpCode.LDA, PC, callerAddr, "Jump to caller address");
        emitRM(OpCode.LD, FP, 0, FP, "Load old FP");

        // Store result in a new temporary
        Temp resultTemp = getNextTempOffset();
        emitRM(OpCode.ST, AC, resultTemp.offset, resultTemp.scope == Temp.LOCAL_SCOPE ? FP : GP,
                "Store result in a new temporary");
        exp.temp = resultTemp;

        // Store the returned value
        FunctionDec dec = (FunctionDec) exp.dtype;
        if (dec.result.type != NameTy.VOID && isAddress) {
            emitRM(OpCode.ST, AC, offset, FP, "Store return value from AC");
        }
    }

    public void visit(CompoundExp exp, int level, boolean isAddress) {
        exp.decs.accept(this, level + 1, false);
        exp.exps.accept(this, level + 1, false);
    }

    public void visit(IfExp exp, int offset, boolean isAddress) {
        exp.test.accept(this, offset, false);

        int testLoc = emitSkip(2);
        exp.thenpart.accept(this, offset, false);

        int thenLoc = emitSkip(0);
        int elseLocStart = emitSkip(1);
        exp.elsepart.accept(this, offset, false);

        int elseLocEnd = emitSkip(0);
        emitBackup(testLoc);
        emitRM(OpCode.LD, AC, exp.test.temp.offset, FP, "Load test into AC");
        emitRMAbs(OpCode.JEQ, AC, elseLocStart + 1, "Jump to elsepart if FALSE");
        emitBackup(elseLocStart);
        emitRMAbs(OpCode.LDA, PC, elseLocEnd, "Jump to end of IF block");
        emitRestore();
    }

    public void visit(IntExp exp, int offset, boolean isAddress) {
        int value = ((IntExp) exp).value;
        exp.temp = new Temp(value, Temp.LOCAL_SCOPE);
        if (isAddress) {
            setVarMemory(value, offset, true);
        }
    }

    public void visit(NilExp exp, int offset, boolean isAddress) {
    }

    public void visit(OpExp exp, int offset, boolean isAddress) {
        exp.left.accept(this, offset - 1, false);
        exp.right.accept(this, offset - 2, false);

        boolean isLeftAddr = !(exp.left instanceof IntExp || exp.left instanceof BoolExp);
        boolean isRightAddr = !(exp.right instanceof IntExp || exp.right instanceof BoolExp);

        emitComment("Evaluating " + exp.getDisplayOp() + " operation");

        // Load temporaries
        emitRM(isLeftAddr ? OpCode.LD : OpCode.LDC, AC1, exp.left.temp.offset,
                exp.left.temp.scope == Temp.LOCAL_SCOPE ? FP : GP, "Load left operand into AC1");
        emitRM(isRightAddr ? OpCode.LD : OpCode.LDC, AC2, exp.right.temp.offset,
                exp.right.temp.scope == Temp.LOCAL_SCOPE ? FP : GP, "Load right operand into AC2");

        switch (exp.op) {
            case OpExp.ADD:
                emitRO(OpCode.ADD, AC, AC1, AC2, "Perform addition");
                break;
            case OpExp.SUBT:
                emitRO(OpCode.SUB, AC, AC1, AC2, "Perform subtraction");
                break;
            case OpExp.DIV:
                // TODO: check for 0 divisor and trigger RUNTIME error
                emitRO(OpCode.DIV, AC, AC1, AC2, "Perform division");
                break;
            case OpExp.MULT:
                emitRO(OpCode.MUL, AC, AC1, AC2, "Perform multiplication");
                break;
            case OpExp.UMINUS:
                // TODO:
                break;
            case OpExp.LT:
                emitRO(OpCode.SUB, AC, AC1, AC2, "Subtract operands");
                emitRM(OpCode.JLT, AC, 2, PC, "Jump to TRUE if <");
                break;
            case OpExp.LTE:
                emitRO(OpCode.SUB, AC, AC1, AC2, "Subtract operands");
                emitRM(OpCode.JLE, AC, 2, PC, "Jump to TRUE if <=");
                break;
            case OpExp.GT:
                emitRO(OpCode.SUB, AC, AC1, AC2, "Subtract operands");
                emitRM(OpCode.JGT, AC, 2, PC, "Jump to TRUE if >");
                break;
            case OpExp.GTE:
                emitRO(OpCode.SUB, AC, AC1, AC2, "Subtract operands");
                emitRM(OpCode.JGE, AC, 2, PC, "Jump to TRUE if >=");
                break;
            case OpExp.EQUAL:
                emitRO(OpCode.SUB, AC, AC1, AC2, "Subtract operands");
                emitRM(OpCode.JEQ, AC, 2, PC, "Jump to TRUE if operands are equal");
                break;
            case OpExp.NEQUAL:
                emitRO(OpCode.SUB, AC, AC1, AC2, "Subtract operands");
                emitRM(OpCode.JNE, AC, 2, PC, "Jump to TRUE if operands are not equal");
                break;
            case OpExp.AND:
                emitRM(OpCode.JEQ, AC1, 1, PC, "Jump to FALSE if first operand != TRUE");
                emitRM(OpCode.JEQ, AC2, 0, PC, "Jump to FALSE if second operand != TRUE");
                break;
            case OpExp.OR:
                emitRM(OpCode.JNE, AC1, 3, PC, "Jump to TRUE if first operand == TRUE");
                emitRM(OpCode.JEQ, AC2, 2, PC, "Jump to TRUE if second operand == TRUE");
                break;
            case OpExp.BNOT:
                emitRM(OpCode.JEQ, AC1, 0, PC, "Jump to opposite value");
                break;
        }

        // Assign either 1 or 0 to AC if the operation is relational
        if (exp.isRelational()) {
            emitRM(OpCode.LDC, AC, FALSE, 0, "Load FALSE into AC");
            emitRM(OpCode.LDA, PC, 1, PC, "Unconditional jump");
            emitRM(OpCode.LDC, AC, TRUE, 0, "Load TRUE into AC");
        }

        // Store result in a new temporary
        Temp resultTemp = getNextTempOffset();
        emitRM(OpCode.ST, AC, resultTemp.offset, FP, "Store result in a new temporary");
        exp.temp = resultTemp;

        if (isAddress) {
            emitRM(OpCode.ST, AC, offset, FP, "Store final result");
            if (isLeftAddr || isRightAddr) {
            }
        }
    }

    public void visit(ReturnExp exp, int offset, boolean isAddress) {
        exp.exp.accept(this, offset, false);

        boolean isExpAddr = !(exp.exp instanceof IntExp || exp.exp instanceof BoolExp);

        if (!(exp.exp instanceof NilExp)) {
            emitRM(isExpAddr ? OpCode.LD : OpCode.LDC, AC, exp.exp.temp.offset, FP, "Load return value into AC");
        }
        emitRM(OpCode.LD, PC, -1, FP, "Return back to caller");
    }

    public void visit(VarExp exp, int offset, boolean isAddress) {
        exp.var.accept(this, offset, false);
        VarDec dec = (VarDec) exp.dtype;
        exp.temp = new Temp(dec.offset, dec.nestLevel > 0 ? Temp.LOCAL_SCOPE : Temp.GLOBAL_SCOPE);
        if (isAddress) {
            emitRM(OpCode.LD, AC, dec.offset, dec.nestLevel > 0 ? FP : GP, "Load var into AC");
            emitRM(OpCode.ST, AC, offset, FP, "Store var in new location");
        }
    }

    public void visit(WhileExp exp, int offset, boolean isAddress) {
        // int testLoc = emitLoc;
        // exp.test.accept(this, offset, false);
        // exp.body.accept(this, offset, false);

        // int testLoc = emitSkip(1);
        // // exp.thenpart.accept(this, offset, false);
        // emitRM(OpCode.ST, 0, 0, 0, "Inside thenpart");
        // int thenLoc = emitSkip(0);
        // emitBackup(testLoc);
        // emitRMAbs(OpCode.JNE, AC, thenLoc, "jump test");
        // emitRestore();

        int testStartLoc = emitSkip(0);
        exp.test.accept(this, offset, false);

        int testLoc = emitSkip(2);
        exp.body.accept(this, offset, false);
        // emitRM(OpCode.ST, 0, 0, 0, "Inside body");

        int thenLoc = emitSkip(0);
        int elseLocStart = emitSkip(0);
        // exp.elsepart.accept(this, offset, false);
        emitRMAbs(OpCode.LDA, PC, testStartLoc, "Jump to test");

        int locEnd = emitSkip(0);
        // emitBackup(testLoc);
        emitBackup(testLoc);
        emitRM(OpCode.LD, AC, exp.test.temp.offset, FP, "Load test into AC");
        emitRMAbs(OpCode.JEQ, AC, locEnd, "Jump to end of WHILE if FALSE");
        // emitRMAbs(OpCode.LDA, PC, locEnd, "Jump to end of IF block");
        emitRestore();
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
            currentFunc = dec;
            setFpOffset(-2);

            backpatch("Jump around function", () -> {
                emitRM(OpCode.ST, AC, -1, FP, "Store return address");
                dec.params.accept(this, level + 1, false);
                dec.body.accept(this, level, false);

                // Insert automatic return for void functions
                if (dec.getType().type == NameTy.VOID) {
                    emitRM(OpCode.LD, PC, -1, FP, "Return back to caller");
                }
            });

            currentFunc = null;
        }
    }

    public void visit(ArrayDec varDec, int level, boolean isAddress) {
        allocateVar(varDec, level, varDec.size);
    }

    public void visit(SimpleDec varDec, int level, boolean isAddress) {
        allocateVar(varDec, level, 1);
    }
}
