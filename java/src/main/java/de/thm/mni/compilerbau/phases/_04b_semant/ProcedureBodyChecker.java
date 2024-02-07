package de.thm.mni.compilerbau.phases._04b_semant;

import com.sun.jdi.connect.Connector;
import de.thm.mni.compilerbau.CommandLineOptions;
import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitor;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.table.SymbolTable;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.types.PrimitiveType;
import de.thm.mni.compilerbau.types.Type;
import java_cup.runtime.Symbol;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * This class is used to check if the currently compiled SPL program is semantically valid.
 * The body of each procedure has to be checked, consisting of {@link Statement}s, {@link Variable}s and {@link Expression}s.
 * Each node has to be checked for type issues or other semantic issues.
 * Calculated {@link Type}s can be stored in and read from the dataType field of the {@link Expression} and {@link Variable} classes.
 */
public class ProcedureBodyChecker {

    private final CommandLineOptions options;

    public ProcedureBodyChecker(CommandLineOptions options) {
        this.options = options;
    }

    public void checkProcedures(Program program, SymbolTable globalTable) {
        Visitor visitor = new TypeAnalysisVisitor(globalTable);
        program.accept(visitor);

        //throw new NotImplemented();
    }

    class TypeAnalysisVisitor extends DoNothingVisitor {
        SymbolTable globalTable;
        SymbolTable localTable;
        Type type;

        boolean hasMainBeenDeclared = false;

        TypeAnalysisVisitor(SymbolTable globalTable) {
            this.globalTable = (SymbolTable) globalTable;
        }

        @Override
        public void visit(Program program) {
            program.definitions.stream()
                    .filter(d -> d instanceof ProcedureDefinition)
                    .forEach(p -> p.accept(this));

            if(!hasMainBeenDeclared) mainIsMissing();

        }

        @Override
        public void visit(ProcedureDefinition procedureDefinition) {
            ProcedureEntry procEntry = (ProcedureEntry) globalTable.lookup(procedureDefinition.name);
            this.localTable = procEntry.localTable;

            if(procedureDefinition.name.equals(new Identifier("main"))) {
                hasMainBeenDeclared = true;
                if(!procedureDefinition.parameters.isEmpty()) {
                    mainMustNotHaveParameters();
                }
            }

            procedureDefinition.parameters.stream()
                    .filter(Objects::nonNull)
                    .forEach(s -> s.accept(this));

            procedureDefinition.variables.stream()
                    .filter(Objects::nonNull)
                    .forEach(s -> s.accept(this));

            procedureDefinition.body.stream()
                    .filter(Objects::nonNull)
                    .forEach(s -> s.accept(this));
        }

        public void visit(AssignStatement assignStatement) {
            Type left, right;

            assignStatement.target.accept(this);
            left = this.type;
            assignStatement.value.accept(this);
            right = this.type;

            if(left != right) {
                assignmentTypeMismatch(right, left);
            } else if(!(left instanceof PrimitiveType)) {
                assignmentTypeMismatch(right, left);
            }
        }

        public void visit(NamedVariable namedVariable) {
            Entry entry = localTable.lookup(namedVariable.name);
            if(entry == null) {
                undefinedIdentifier(namedVariable.name, namedVariable.position);
            }

            if(entry instanceof VariableEntry) {
                this.type = ((VariableEntry) entry).type;
            } else {
                identifierNotRefered(namedVariable.name);
            }
        }

        public void visit(IntLiteral intLiteral) {
            this.type = PrimitiveType.intType;
        }

        public void visit(ArrayAccess arrayAccess) {
            arrayAccess.index.accept(this);
            if(!(this.type instanceof PrimitiveType)) {
                indexTypeMismatch(this.type);
            }
            arrayAccess.array.accept(this);
            arrayAccess.dataType = type;
            if(!(this.type instanceof ArrayType)) {
                invalidArrayAccess(this.type);
            }
            this.type = ((ArrayType) this.type).baseType;
            arrayAccess.dataType = this.type;
        }

        public void visit(CompoundStatement compoundStatement) {
            compoundStatement.statements.stream()
                    .filter(Objects::nonNull)
                    .forEach(s -> s.accept(this));
        }

        public void visit(CallStatement callStatement) {
            Entry entry = localTable.lookup(callStatement.procedureName);
            if(entry == null) {
                undefinedIdentifier(callStatement.procedureName, callStatement.position);
            }

            if(entry instanceof ProcedureEntry) {
                Iterator<Expression> ArgsIt = callStatement.arguments.iterator();
                Iterator<ParameterType> ParamsIt = ((ProcedureEntry) entry).parameterTypes.iterator();
                int i = 0;
                while(ArgsIt.hasNext() && ParamsIt.hasNext()){
                    i++;
                    Node a = ArgsIt.next();
                    ParameterType p = ParamsIt.next();
                    if(p.isReference && !(a instanceof VariableExpression)){
                        refArgMustBeVariable(callStatement.procedureName, i);
                    } else if(a instanceof VariableExpression) {
                        a.accept(this);
                        if(p.type != type) {
                            argumentTypeMismatch(callStatement.procedureName, i, p.type, type);
                        }
                    }
                }
                if(ArgsIt.hasNext()){ /*called with to much*/
                    procArgumentCountMismatch(callStatement.procedureName, i, callStatement.arguments.size());
                } else if(ParamsIt.hasNext()) { /*called with to few*/
                    procArgumentCountMismatch(callStatement.procedureName, i, callStatement.arguments.size());
                }
            } else {
                identifierNotAProcedure(callStatement.procedureName);
            }
        }

        public void visit(VariableExpression variableExpression) {
            variableExpression.variable.accept(this);
            variableExpression.dataType = variableExpression.variable.dataType;
        }

        public void visit(BinaryExpression binaryExpression) {
            binaryExpression.leftOperand.accept(this);
            Type left = this.type;
            binaryExpression.rightOperand.accept(this);
            Type right = this.type;

            if(left != right) {
                binaryTypeMismatch(binaryExpression.operator, left, right);
            }

            switch (binaryExpression.operator) {
                case BinaryExpression.Operator.EQU:
                case BinaryExpression.Operator.NEQ:
                case BinaryExpression.Operator.LST:
                case BinaryExpression.Operator.LSE:
                case BinaryExpression.Operator.GRT:
                case BinaryExpression.Operator.GRE:
                    if (left != PrimitiveType.intType) {
                        binaryTypeMismatch(binaryExpression.operator, left, right);
                    }
                    type = PrimitiveType.boolType;
                    break;
                case BinaryExpression.Operator.ADD:
                case BinaryExpression.Operator.SUB:
                case BinaryExpression.Operator.MUL:
                case BinaryExpression.Operator.DIV:
                    if (left != PrimitiveType.intType) {
                        binaryTypeMismatch(binaryExpression.operator, left, right);
                    }
                    type = PrimitiveType.intType;
                    break;
                default:
                    binaryTypeMismatch(binaryExpression.operator, left, right);
            }
        }

        public void visit(UnaryExpression unaryExpression) {
            unaryExpression.operand.accept(this);
            Type operandType = this.type;

            switch (unaryExpression.operator) {
                case UnaryExpression.Operator.MINUS:
                    if (operandType != PrimitiveType.intType) {
                        unaryTypeMismatch(unaryExpression.operator, operandType);
                    }
                    type = PrimitiveType.intType;
                    break;
                default:
                    unaryTypeMismatch(unaryExpression.operator, operandType);
            }
        }

        public void visit(IfStatement ifStatement) {
            ifStatement.condition.accept(this);
            if(this.type != PrimitiveType.boolType) {
                ifConditionTypeMismatch(ifStatement.condition);
            }
            type = null;
            ifStatement.thenPart.accept(this);
            if(ifStatement.elsePart != null) {
                ifStatement.elsePart.accept(this);
            }
        }

        public void visit(WhileStatement whileStatement) {
            whileStatement.condition.accept(this);
            if(this.type != PrimitiveType.boolType) {
                whileConditionTypeMismatch(whileStatement.condition);
            }
            type = null;
            whileStatement.body.accept(this);
        }


        //-----Errors------
        static void undefinedIdentifier(Identifier identifier, Position p) {
            System.err.println("Identifier '" + identifier + "' is not defined. In Line: " + p.line);
            System.exit(101);
        }
        static void identifierNotRefered(Identifier identifier) { //In Tablebuilder
            System.err.println("Identifier '" + identifier + "' does not refer to a type.");
            System.exit(102);
        }
        static void identifierAlreadyInScope(Identifier identifier) { //In Tablebuilder
            System.err.println("Identifier '" + identifier + "' is already defined in this scope.");
            System.exit(103);
        }
        static void nonReferenceHasReferenceType(Identifier identifier, Type type) { //In Tablebuilder
            System.err.println("Non-reference parameter '"+ identifier + "' has type '" + type + "', which can only be passed by reference.");
            System.exit(104);
        }
        static void assignmentTypeMismatch(Type type1, Type type2) {
            System.err.println("A value of type '" + type1 + "' can not be assigned to variable of type '" + type2 + "'.");
            System.exit(108);
        }
        static void ifConditionTypeMismatch(Expression type) {
            System.err.println("'if' condition expected to be of type 'boolean', but is of type '" + type + "'.");
            System.exit(110);
        }
        static void whileConditionTypeMismatch(Expression type) {
            System.err.println("while' condition expected to be of type 'boolean', but is of type '" + type + "'.");
            System.exit(111);
        }
        static void identifierNotAProcedure(Identifier identifier) {
            System.err.println("Identifier '" + identifier + "' does not refer to a procedure.");
            System.exit(113);
        }
        static void argumentTypeMismatch(Identifier procedure, int argNum, Type expectedArgType, Type givenArgType) {
            System.err.println("Argument type mismatch in call of procedure '" + procedure + "'. Argument " + argNum + " is expected to have type '" + expectedArgType + "', but has type '" + givenArgType + "'.");
            System.exit(114);
        }
        static void refArgMustBeVariable(Identifier procedure, int argNum) {
            System.err.println("Invalid argument for reference parameter in call to procedure '" + procedure + "': Argument " + argNum + " must be a variable.");
            System.exit(115);
        }
        static void procArgumentCountMismatch(Identifier procName, int expectedSize, int providedSize) {
            System.err.println("Argument count mismatch: Procedure '" + procName + "' expects + " + expectedSize + " arguments, but " + providedSize + " were provided.");
            System.exit(116);
        }
        /*static void binaryTypeMismatch(BinaryExpression.Operator binaryOperator, Expression type1, Expression type2) {
            System.err.println("Type mismatch in binary expression: Operator '" + binaryOperator + "' does not accept operands of types '" + type1 + "' and '" + type2 + "'.");
            System.exit(118);
        }*/
        static void binaryTypeMismatch(BinaryExpression.Operator binaryOperator, Type type1, Type type2) {
            System.err.println("Type mismatch in binary expression: Operator '" + binaryOperator + "' does not accept operands of types '" + type1 + "' and '" + type2 + "'.");
            System.exit(118);
        }
        static void unaryTypeMismatch(UnaryExpression.Operator operator, Type type) {
            System.err.println("Type mismatch in unary expression: Operator '" + operator + "' does not accept operand of type '" + type + "'.");
            System.exit(119);
        }
        static void identNotaVariable(Identifier identifier) {
            System.err.println("Identifier '" + identifier + "' does not refer to a variable.");
            System.exit(122);
        }
        static void invalidArrayAccess(Type type) {
            System.err.println("Type mismatch: Invalid array access operation on non-array variable of type '" + type + "'.");
            System.exit(123);
        }
        static void indexTypeMismatch(Type type) {
            System.err.println("Type mismatch: Array index expected to be of type 'int', but is type '" + type + "'.");
            System.exit(124);
        }
        static void mainIsMissing() {
            System.err.println("Procedure 'main' is missing.");
            System.exit(125);
        }
        static void mainNotAProcedure() { //In TableBuilder
            System.err.println("Identifier 'main' does not refer to a procedure");
            System.exit(126);
        }
        static void mainMustNotHaveParameters() {
            System.err.println("Procedure 'main' must not have any parameters");
            System.exit(127);
        }

    }

}
