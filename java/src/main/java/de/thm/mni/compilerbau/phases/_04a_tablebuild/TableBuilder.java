package de.thm.mni.compilerbau.phases._04a_tablebuild;

import de.thm.mni.compilerbau.CommandLineOptions;
import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitable;
import de.thm.mni.compilerbau.absyn.visitor.Visitor;
import de.thm.mni.compilerbau.phases._02_03_parser.Sym;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.PrimitiveType;
import de.thm.mni.compilerbau.types.Type;
import de.thm.mni.compilerbau.utils.NotImplemented;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.utils.SplError;
import java_cup.runtime.Symbol;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is used to create and populate a {@link SymbolTable} containing entries for every symbol in the currently
 * compiled SPL program.
 * Every declaration of the SPL program needs its corresponding entry in the {@link SymbolTable}.
 * <p>
 * Calculated {@link Type}s can be stored in and read from the dataType field of the {@link Expression},
 * {@link TypeExpression} or {@link Variable} classes.
 */
public class TableBuilder {

    SymbolTable globalTable = TableInitializer.initializeGlobalTable();
    private final CommandLineOptions options;

    public TableBuilder(CommandLineOptions options) {
        this.options = options;
    }

    //TODO; Stackoverflow irgendwo und ausgabe einbauen
    public SymbolTable buildSymbolTable(Program program) {
        //TODO (assignment 4a): Initialize a symbol table with all predefined symbols and fill it with user-defined symbol

        Visitor visitor = new TableVisitor();

        program.accept(visitor);

        throw new NotImplemented();
    }


    class TableVisitor extends DoNothingVisitor {
        Type type;
        SymbolTable currentTable;
        List<ParameterType> paramTypeList;

        public void visit(Program prog) {
            for(GlobalDefinition g: prog.definitions) {
                if(g instanceof TypeDefinition){
                    g.accept(this);
                }
            }
            for(GlobalDefinition g: prog.definitions) {
                if(g instanceof ProcedureDefinition){
                    g.accept(this);
                }
            }
        }
        public void visit(TypeDefinition typeDef) {
            //Check if type was already created with that name
            /*if(globalTable.lookup(typeDef.name) != null){
                System.err.println("Error: Type " + typeDef.name + " in line " + typeDef.position.line + " was already created");
                System.exit(1);
            }

            //Check if type is called main
            if(typeDef.name.equals(new Identifier("main"))){
                System.err.println("Error: 'main' must be a procedure");
                System.exit(1);
            }*/

            typeDef.accept(this);
            globalTable.enter(typeDef.name, new TypeEntry(type)); //TODO: You could add an SplError Object to be thrown
        }

        public void visit(VariableDefinition varDef) {
            //Check if variable was already created with that name
            /*if(globalTable.lookup(varDef.name) != null){
                System.err.println("Error: Variable " + varDef.name + " in line " + varDef.position.line + " was already created");
                System.exit(1);
            }*/
            varDef.accept(this);
            currentTable.enter(varDef.name, new VariableEntry(type, false));
        }

        public void visit(ArrayTypeExpression arrType) {
            arrType.accept(this);
            this.type = new ArrayType(type, arrType.arraySize);
        }

        public void visit(ProcedureDefinition procDef) {
            //Check if procedure was already created with that name
            /*if (globalTable.lookup(procDef.name) != null) {
                System.err.println("Error: Variable " + procDef.name + " in line " + procDef.position.line + " was already created");
                System.exit(1);
            }*/

            //Create level 0 table
            this.currentTable = new SymbolTable(globalTable);

            //Create Parameterlist
            List<ParameterType> pList = new LinkedList<>();
            this.paramTypeList = pList;

            //Traverse parameters
            List<ParameterDefinition> params = procDef.parameters;
            for(ParameterDefinition p: params) {
                p.accept(this);
            }

            //Traverse Variables
            List<VariableDefinition> vList = procDef.variables;
            for(VariableDefinition v: vList) {
                v.accept(this);
            }

            //Add entry to global table
            globalTable.enter(procDef.name, new ProcedureEntry(currentTable, pList));

            List<Statement> sList = procDef.body;
            for(Statement s: sList) {
                s.accept(this); //TODO: Visit method for statement
            }
        }

        public void visit(ParameterDefinition paramDef) {
            paramDef.accept(this);
            this.paramTypeList.add(new ParameterType(type, paramDef.isReference));
            currentTable.enter(paramDef.name, new VariableEntry(type, paramDef.isReference));
        }
    }

    /**
     * Prints the local symbol table of a procedure together with a heading-line
     * NOTE: You have to call this after completing the local table to support '--tables'.
     *
     * @param name  The name of the procedure
     * @param entry The entry of the procedure to print
     */
    static void printSymbolTableAtEndOfProcedure(Identifier name, ProcedureEntry entry) {
        System.out.format("Symbol table at end of procedure '%s':\n", name);
        System.out.println(entry.localTable.toString());
    }
}
