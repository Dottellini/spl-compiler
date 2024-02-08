package de.thm.mni.compilerbau.phases._06_codegen;

import de.thm.mni.compilerbau.CommandLineOptions;
import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitor;
import de.thm.mni.compilerbau.phases._02_03_parser.Sym;
import de.thm.mni.compilerbau.phases._05_varalloc.VarAllocator;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.types.Type;
import de.thm.mni.compilerbau.utils.NotImplemented;
import de.thm.mni.compilerbau.utils.SplError;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * This class is used to generate the assembly code for the compiled program.
 * This code is emitted via the {@link CodePrinter} in the output field of this class.
 */
public class CodeGenerator {
    final CommandLineOptions options;
    final CodePrinter output;

    /**
     * Initializes the code generator.
     *
     * @param options The command line options passed to the compiler
     * @param output  The PrintWriter to the output file.
     */
    public CodeGenerator(CommandLineOptions options, PrintWriter output) throws IOException {
        this.options = options;
        this.output = new CodePrinter(output);
    }

    public void generateCode(Program program, SymbolTable table) {
        assemblerProlog();
        Visitor visitor = new CodeGeneratorVisitor(table);
        program.accept(visitor);
    }

    public class CodeGeneratorVisitor extends DoNothingVisitor {
        SymbolTable globalTable;
        SymbolTable localTable;
        Register currentRegister;
        String label;
        Type currentArrayType;
        int labelCount = 0;

        CodeGeneratorVisitor(SymbolTable globalTable) {
            this.globalTable = globalTable;
            this.currentRegister = Register.FIRST_FREE_USE;
        }

        String labelGenerator() {
            return "L" + labelCount++;
        }

        void checkRegAvailability(Register reg) {
            if(!reg.isFreeUse()) {
                throw SplError.RegisterOverflow();
            }
        }

        /////////VISITORS/////////

        public void visit(Program program) {
            program.definitions.forEach(definition -> definition.accept(this));
        }

        public void visit(CompoundStatement compoundStatement) {
            compoundStatement.statements.forEach(statement -> statement.accept(this));
        }

        public void visit(IntLiteral intLiteral) {
            checkRegAvailability(currentRegister);
            output.emitInstruction("add", currentRegister, Register.NULL, intLiteral.value);
            this.currentRegister = currentRegister.next();
        }

        public void visit(NamedVariable namedVariable) {
            VariableEntry entry = (VariableEntry) localTable.lookup(namedVariable.name);
            checkRegAvailability(currentRegister);
            output.emitInstruction("add", currentRegister, Register.FRAME_POINTER, entry.offset);

            if(entry.isReference) {
                output.emitInstruction("ldw", currentRegister, currentRegister, 0);
            }

            this.currentRegister = currentRegister.next();
        }
        public void visit(VariableExpression variableExpression) {
            variableExpression.variable.accept(this);
            output.emitInstruction("ldw", currentRegister.previous(), currentRegister.previous(), 0);
        }

        public void visit(UnaryExpression unaryExpression) {
            unaryExpression.operand.accept(this);

            switch (unaryExpression.operator) {
                case UnaryExpression.Operator.MINUS:
                    output.emitInstruction("sub", currentRegister, Register.NULL, currentRegister);
                    break;
            }
        }

        public void visit(BinaryExpression binaryExpression) {
            binaryExpression.leftOperand.accept(this);
            binaryExpression.rightOperand.accept(this);

            Register left = currentRegister.minus(2);
            Register right = currentRegister.minus(1);

            switch (binaryExpression.operator) {
                case BinaryExpression.Operator.ADD:
                    output.emitInstruction("add", left, left, right);
                    break;
                case BinaryExpression.Operator.SUB:
                    output.emitInstruction("sub", left, left, right);
                    break;
                case BinaryExpression.Operator.MUL:
                    output.emitInstruction("mul", left, left, right);
                    break;
                case BinaryExpression.Operator.DIV:
                    output.emitInstruction("div", left, left, right);
                    break;
                case BinaryExpression.Operator.EQU:
                    output.emitInstruction("beq", left, right, label);
                    break;
                case BinaryExpression.Operator.NEQ:
                    output.emitInstruction("bne", left, right, label);
                    break;
                case BinaryExpression.Operator.LST:
                    output.emitInstruction("blt", left, right, label);
                    break;
                case BinaryExpression.Operator.LSE:
                    output.emitInstruction("ble", left, right, label);
                    break;
                case BinaryExpression.Operator.GRT:
                    output.emitInstruction("bgt", left, right, label);
                    break;
                case BinaryExpression.Operator.GRE:
                    output.emitInstruction("bge", left, right, label);
                    break;
                case BinaryExpression.Operator.AND:
                    output.emitInstruction("and", left, left, right);
                    break;
                case BinaryExpression.Operator.OR:
                    output.emitInstruction("or", left, left, right);
                    break;
            }

            this.currentRegister = currentRegister.previous();
        }

        public void visit (AssignStatement assignStatement) {
            assignStatement.target.accept(this);
            assignStatement.value.accept(this);
            output.emitInstruction("stw", currentRegister.minus(1), currentRegister.minus(2), 0);
            this.currentRegister = currentRegister.minus(2);
        }

        public void visit(ArrayAccess arrayAccess) {
            arrayAccess.array.accept(this);
            arrayAccess.index.accept(this);
            System.out.println(arrayAccess);

            output.emitInstruction("add", currentRegister, Register.NULL, ((ArrayType)arrayAccess.array.dataType).arraySize);
            this.currentRegister = currentRegister.previous();

            output.emitInstruction("bgeu", currentRegister, currentRegister.next(), "_indexError");
            output.emitInstruction("mul", currentRegister, currentRegister, arrayAccess.dataType.byteSize);
            output.emitInstruction("add", currentRegister.previous(), currentRegister.previous(), currentRegister);

        }

        public void visit(WhileStatement whileStatement) {
            String test = labelGenerator();
            String loop = labelGenerator();
            String end = labelGenerator();

            label = loop;
            output.emitLabel(test);

            whileStatement.condition.accept(this);

            output.emitInstruction("j", end);
            output.emitLabel(loop);

            whileStatement.body.accept(this);

            output.emitInstruction("j", test);
            output.emitLabel(end);

            /*
            output.emitInstruction("bge", currentRegister, currentRegister.next(), end);

            this.currentRegister = currentRegister.previous();
            whileStatement.body.accept(this);

            output.emitInstruction("j", loop);
            output.emitLabel(end);*/
        }

        public void visit(IfStatement ifStatement) {
            String elseLabel = labelGenerator();
            String endLabel = labelGenerator();
            label = elseLabel;

            ifStatement.condition.accept(this);

            if(ifStatement.elsePart != null) {
                ifStatement.elsePart.accept(this);
            }

            output.emitInstruction("j", endLabel);
            output.emitLabel(elseLabel);

            ifStatement.thenPart.accept(this);
            output.emitLabel(endLabel);
/*
            ifStatement.condition.accept(this);
            output.emitInstruction("bge", currentRegister, currentRegister.next(), elseLabel);

            this.currentRegister = currentRegister.previous();
            ifStatement.thenPart.accept(this);
            output.emitInstruction("j", endLabel);
            output.emitLabel(elseLabel);
            ifStatement.elsePart.accept(this);
            output.emitLabel(endLabel);*/
        }

        public void visit(CallStatement callStatement) {
            int counter = 0;
            String procName = callStatement.procedureName.toString();

            List<ParameterType> paramList = ((ProcedureEntry) globalTable.lookup(callStatement.procedureName)).parameterTypes;

            for(Expression arg : callStatement.arguments) {
                ParameterType param = paramList.get(counter);
                if(param.isReference) {
                    ((VariableExpression) arg).variable.accept(this);
                } else {
                    arg.accept(this);
                }

                output.emitInstruction("stw", currentRegister.previous(), Register.STACK_POINTER, param.offset);
                counter++;
                this.currentRegister = currentRegister.previous();
            }

            output.emitInstruction("jal", procName);
        }

        public void visit(ProcedureDefinition procedureDefinition) {
            int returnByteSize = VarAllocator.REFERENCE_BYTESIZE;
            int frameByteSize = VarAllocator.REFERENCE_BYTESIZE;
            int frameSize;
            int oldFrameOffset;
            int oldReturnOffset;

            ProcedureEntry procedureEntry = (ProcedureEntry) globalTable.lookup(procedureDefinition.name);

            this.localTable = procedureEntry.localTable;
            this.label = procedureDefinition.name.toString();

            //output.emit("");
            output.emitExport(label);
            output.emitLabel(label);

            // Prolog
            //Calculate FrameSize to allocate Frame
            frameSize = procedureEntry.stackLayout.localVarAreaSize + frameByteSize + returnByteSize;
            frameSize += (procedureEntry.stackLayout.outgoingAreaSize < 0) ? 0 : procedureEntry.stackLayout.outgoingAreaSize;
            output.emitInstruction("sub", Register.STACK_POINTER, Register.STACK_POINTER, frameSize);

            //Save oldFP
            oldFrameOffset = returnByteSize;
            oldFrameOffset += (procedureEntry.stackLayout.outgoingAreaSize < 0) ? 0 : procedureEntry.stackLayout.outgoingAreaSize;
            output.emitInstruction("stw", Register.FRAME_POINTER, Register.STACK_POINTER, oldFrameOffset);

            //Set FP
            output.emitInstruction("add", Register.FRAME_POINTER, Register.STACK_POINTER, frameSize);

            //Save oldReturn
            oldReturnOffset = procedureEntry.stackLayout.localVarAreaSize + frameByteSize + returnByteSize;
            output.emitInstruction("stw", Register.RETURN_ADDRESS, Register.FRAME_POINTER, -oldReturnOffset);

            // Body
            //procedureDefinition.parameters.forEach(parameter -> parameter.accept(this));
            //procedureDefinition.variables.forEach(variable -> variable.accept(this));
            procedureDefinition.body.forEach(statement -> statement.accept(this));

            // Epilog
            if(procedureEntry.stackLayout.outgoingAreaSize >= 0) {
                output.emitInstruction("ldw", Register.RETURN_ADDRESS, Register.FRAME_POINTER, -oldReturnOffset);
            }
            output.emitInstruction("ldw", Register.FRAME_POINTER, Register.STACK_POINTER, oldFrameOffset);
            output.emitInstruction("add", Register.STACK_POINTER, Register.STACK_POINTER, frameSize);
            output.emitInstruction("jr", Register.RETURN_ADDRESS);
        }

    }

    /**
     * Emits needed import statements, to allow usage of the predefined functions and sets the correct settings
     * for the assembler.
     */
    private void assemblerProlog() {
        output.emitImport("printi");
        output.emitImport("printc");
        output.emitImport("readi");
        output.emitImport("readc");
        output.emitImport("exit");
        output.emitImport("time");
        output.emitImport("clearAll");
        output.emitImport("setPixel");
        output.emitImport("drawLine");
        output.emitImport("drawCircle");
        output.emitImport("_indexError");
        output.emit("");
        output.emit("\t.code");
        output.emit("\t.align\t4");
    }
}
