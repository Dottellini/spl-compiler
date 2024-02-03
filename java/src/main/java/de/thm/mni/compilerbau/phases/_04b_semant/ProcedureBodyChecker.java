package de.thm.mni.compilerbau.phases._04b_semant;

import de.thm.mni.compilerbau.CommandLineOptions;
import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitor;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.table.SymbolTable;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.types.PrimitiveType;
import de.thm.mni.compilerbau.types.Type;

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
        NamedVariable currentArraySimpleVar;
        NamedVariable currentNamedVar;
        ProcedureEntry currentProcEntry;
        boolean hasMainBeenDeclared;
        int currentArrayType;
        int previousArrayType;
        int leftTypeOfAssign;
        int leftArraySize;
        int currentRow;
        boolean assignStmFlag;
        boolean assignLeftArray;
        boolean rightSideOfAssign;
        boolean isCurrentOpExpIntType;
        Type currentOpExpIntType;
        boolean leftArrayWithoutIndex;
        boolean isCurrentLeftOpExpIntType;
        Type currentLeftOpExpIntType;

        boolean isCurrentRightOpExpIntType;
        Type currentRightOpExpIntType;

        TypeAnalysisVisitor(SymbolTable table){
            this.globalTable = table;
            this.hasMainBeenDeclared = false;
            this.currentProcEntry = null;
            this.currentRow = 0;
            this.currentArrayType = 0;
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

            procedureDefinition.body.stream()
                    .filter(Objects::nonNull)
                    .forEach(s -> s.accept(this));
        }

        @Override
        public void visit(IntLiteral intLiteral) {
            this.currentOpExpIntType = PrimitiveType.intType;
            this.isCurrentOpExpIntType = true;
        }

        @Override
        public void visit(BinaryExpression binaryExpression) {
            binaryExpression.leftOperand.accept(this);
            isCurrentLeftOpExpIntType = isCurrentOpExpIntType;
            currentLeftOpExpIntType = currentOpExpIntType;


            binaryExpression.rightOperand.accept(this);
            isCurrentRightOpExpIntType = isCurrentOpExpIntType;
            currentRightOpExpIntType = currentOpExpIntType;

            if(currentRightOpExpIntType != currentLeftOpExpIntType) {
                binaryTypeMismatch(binaryExpression.operator, binaryExpression.leftOperand, binaryExpression.rightOperand);
            }
            isCurrentOpExpIntType = isCurrentLeftOpExpIntType;

            if(!isCurrentOpExpIntType) {//if(currentLeftOpExpIntType.byteSize != 4 || currentRightOpExpIntType.byteSize != 4){
                binaryTypeMismatch(binaryExpression.operator, currentLeftOpExpIntType, currentRightOpExpIntType);
            }
            currentOpExpIntType = currentRightOpExpIntType;
        }

        @Override
        public void visit(UnaryExpression unaryExpression) {
            unaryExpression.operand.accept(this);
            if(calculateTypeSize(currentOpExpIntType) != 0) {
                unaryTypeMismatch(unaryExpression.operator, currentOpExpIntType);
            }
        }

        @Override
        public void visit(NamedVariable namedVariable) {
            Entry entry = localTable.lookup(namedVariable.name);

            if(entry == null) undefinedIdentifier(namedVariable.name, namedVariable.position);

            VariableEntry variableEntry = (VariableEntry) entry;
            assert variableEntry != null;
            if(calculateTypeSize(variableEntry.type) == 0) {
                this.currentArraySimpleVar = namedVariable; //this.currentNamedVar = namedVariable;
            } else {
                if(rightSideOfAssign && assignLeftArray && currentArrayType == 0) {
                    indexTypeMismatch(variableEntry.type);
                }
                if(currentArrayType > calculateTypeSize(variableEntry.type)) {
                    invalidArrayAccess(variableEntry.type);
                }

                assignLeftArray = true;
                this.currentArraySimpleVar = namedVariable;
            }

            isCurrentOpExpIntType = calculateTypeSize(variableEntry.type) == 0;
            currentOpExpIntType = calculateTypeSize(variableEntry.type) == 0 ? variableEntry.type : currentOpExpIntType;
        }

        @Override
        public void visit(ArrayAccess arrayAccess) {
            currentArrayType++;
            arrayAccess.array.accept(this);
            arrayAccess.index.accept(this);
        }

        @Override
        public void visit(VariableExpression variableExpression) {
            //TODO: Schauen ob das richtig ist, idk
            if(variableExpression.variable instanceof ArrayAccess) {
                previousArrayType = currentArrayType;
                currentArrayType = 0;
            }
            variableExpression.variable.accept(this);
            VariableEntry entry = (VariableEntry) (localTable.lookup(currentArraySimpleVar.name));

            if(variableExpression.variable instanceof ArrayAccess) {
                int size = calculateTypeSize(entry.type);
                if(size != currentArrayType) {
                    //TODO: Error here!
                    invalidArrayAccess(entry.type);
                }
                isCurrentOpExpIntType = true;
                currentOpExpIntType = entry.type;
            }

            if(assignStmFlag){
                if(variableExpression.variable instanceof ArrayAccess && !assignLeftArray){
                    int currentType = calculateTypeSize(entry.type);

                    if(leftTypeOfAssign != (currentArrayType - currentType)){
                        assignmentTypeMismatch(entry.type, PrimitiveType.intType);
                    }
                }else if(leftTypeOfAssign != 0 && !assignLeftArray){
                    assignmentTypeMismatch(entry.type, new ArrayType(PrimitiveType.intType, leftArraySize));
                }
                if(leftArrayWithoutIndex && rightSideOfAssign){
                    assignmentTypeMismatch(entry.type, PrimitiveType.intType);
                }
            }
        }

        @Override
        public void visit(CallStatement callStatement) {
            Entry entry = globalTable.lookup(callStatement.procedureName);
            if (entry instanceof ProcedureEntry procEntry) {
                int argsSize = callStatement.arguments.size();
                this.currentProcEntry = procEntry;

                int expectedArgSize = procEntry.parameterTypes.size();
                if (argsSize != expectedArgSize) {
                    procArgumentCountMismatch(callStatement.procedureName, expectedArgSize, argsSize);
                }
            } else if (entry instanceof VariableEntry) {
                identifierNotAProcedure(callStatement.procedureName);
            } else { //Call was not a procedure
                identifierNotAProcedure(callStatement.procedureName);
            }

            int pos = 0;
            Node elem = null;

            for(ParameterType param : currentProcEntry.parameterTypes){ //iterating actual params
                int index = 0;
                for(Node n: callStatement.arguments) {
                    elem = n;
                    if(pos == index) {
                        break;
                    }
                    index++;
                }

                currentRow = callStatement.position.line;
                if(param.isReference){
                    if(elem instanceof IntLiteral || elem instanceof BinaryExpression){
                        argumentTypeMismatch(callStatement.procedureName, (pos + 1), param.type, PrimitiveType.intType);
                    }else if(elem instanceof VariableExpression){
                        VariableExpression varExp = (VariableExpression) elem;

                        if(varExp.variable instanceof ArrayAccess){
                            int paramSize = calculateTypeSize(param.type);
                            varExp.variable.accept(this);
                            VariableEntry varEntry = (VariableEntry)(localTable.lookup(currentArraySimpleVar.name));
                            int entrySize = calculateTypeSize(varEntry.type);
                            if(entrySize - currentArrayType != paramSize){
                                argumentTypeMismatch(callStatement.procedureName, (pos + 1), param.type, varEntry.type);
                            }
                        }
                    }
                }
                pos++;
            }
        }

        @Override
        public void visit(CompoundStatement compoundStatement) {
            List<Statement> statementList = compoundStatement.statements;
            for(Statement s: statementList) {
                s.accept(this);
            }
        }

        @Override
        public void visit(AssignStatement assignStatement) {
            currentArrayType = 0;
            assignStmFlag = true;
            assignStatement.value.accept(this);

            if(currentArrayType == 0  && assignStatement.target instanceof ArrayAccess) {
                leftArrayWithoutIndex = true;
            } else if(assignStatement.target instanceof ArrayAccess && currentArraySimpleVar != null){
                VariableEntry entry = (VariableEntry) (localTable.lookup(currentArraySimpleVar.name));
                int size = calculateTypeSize(entry.type);
                if(currentArrayType > size){
                    invalidArrayAccess(entry.type);
                }
            }

            if(assignStatement.target instanceof ArrayAccess){
                assert currentArraySimpleVar != null;
                VariableEntry entry = (VariableEntry) (localTable.lookup(currentArraySimpleVar.name));
                int size = calculateTypeSize(entry.type);
                if(currentArrayType > size && assignStmFlag || size != currentArrayType  && !assignStmFlag || currentArraySimpleVar == null){
                    invalidArrayAccess(entry.type);
                }

                isCurrentOpExpIntType = true;
                currentOpExpIntType = PrimitiveType.intType;

                leftTypeOfAssign = currentArrayType - 1;
            }else{
                if(assignLeftArray){
                    VariableEntry entry = (VariableEntry) (localTable.lookup(currentArraySimpleVar.name));
                    int size = calculateTypeSize(entry.type);
                    leftArraySize = size;
                    leftTypeOfAssign = size;
                }else{
                    leftTypeOfAssign = 0;
                }
            }

            rightSideOfAssign = true;
            assignStatement.value.accept(this);
            rightSideOfAssign = false;

            assignStmFlag = false;
            assignLeftArray = false;
            leftArrayWithoutIndex = false;
            leftTypeOfAssign = 0;
            currentArrayType = 0;

        }

        @Override
        public void visit(IfStatement ifStatement) {
            if(!(ifStatement.condition instanceof BinaryExpression)) {
                ifConditionTypeMismatch(ifStatement.condition);
            }

            this.currentRow = ifStatement.position.line;

            ifStatement.condition.accept(this);
            ifStatement.elsePart.accept(this);
            ifStatement.thenPart.accept(this);
        }

        @Override
        public void visit(WhileStatement whileStatement) {
            if(!(whileStatement.condition instanceof BinaryExpression)) {
                whileConditionTypeMismatch(whileStatement.condition);
            }

            this.currentRow = whileStatement.position.line;

            whileStatement.condition.accept(this);
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
        //TODO: Implement this error
        static void refArgMustBeVariable(Identifier procedure, int argNum) {
            System.err.println("Invalid argument for reference parameter in call to procedure '" + procedure + "': Argument " + argNum + " must be a variable.");
            System.exit(115);
        }
        static void procArgumentCountMismatch(Identifier procName, int expectedSize, int providedSize) {
            System.err.println("Argument count mismatch: Procedure '" + procName + "' expects + " + expectedSize + " arguments, but " + providedSize + " were provided.");
            System.exit(116);
        }
        static void binaryTypeMismatch(BinaryExpression.Operator binaryOperator, Expression type1, Expression type2) {
            System.err.println("Type mismatch in binary expression: Operator '" + binaryOperator + "' does not accept operands of types '" + type1 + "' and '" + type2 + "'.");
            System.exit(118);
        }
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

        private int calculateTypeSize(Type type) {
            int size = 1;

            if(type instanceof PrimitiveType){
                return 0;
            }else if(type instanceof ArrayType){
                Type arrayType = type;
                while(!(((ArrayType)arrayType).baseType instanceof PrimitiveType)){
                    arrayType = ((ArrayType)arrayType).baseType;
                    size++;
                }
            }
            return size;
        }

    }

}
