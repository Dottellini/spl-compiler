package de.thm.mni.compilerbau.phases._04a_tablebuild;

import de.thm.mni.compilerbau.CommandLineOptions;
import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitor;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.Type;
import de.thm.mni.compilerbau.types.ArrayType;

import java.util.*;

/**
 * This class is used to create and populate a {@link SymbolTable} containing entries for every symbol in the currently
 * compiled SPL program.
 * Every declaration of the SPL program needs its corresponding entry in the {@link SymbolTable}.
 * <p>
 * Calculated {@link Type}s can be stored in and read from the dataType field of the {@link Expression},
 * {@link TypeExpression} or {@link Variable} classes.
 */
public class TableBuilder {

    SymbolTable globalTable;
    Map<Identifier, Entry> tableMapForPrinting = new HashMap<Identifier, Entry>();
    private final CommandLineOptions options;

    public TableBuilder(CommandLineOptions options) {
        this.options = options;
    }

    public SymbolTable buildSymbolTable(Program program) {
        this.globalTable = TableInitializer.initializeGlobalTable();
        Visitor visitor = new TableVisitor();

        program.accept(visitor);

        //TODO: level 0 always empty
        if(options.phaseOption == CommandLineOptions.PhaseOption.TABLES) {
            for (Map.Entry<Identifier, Entry> entry : tableMapForPrinting.entrySet()) {
                Identifier key = entry.getKey();
                ProcedureEntry value = (ProcedureEntry) entry.getValue();
                printSymbolTableAtEndOfProcedure(key, value);
            }
            /*Iterator<Map.Entry<Identifier, Entry>> it = tableMapForPrinting.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Identifier, Entry> pair = it.next();
                printSymbolTableAtEndOfProcedure(pair.getKey(), (ProcedureEntry) pair.getValue());
                it.remove();
            }*/
        }

        return globalTable;
    }


    class TableVisitor extends DoNothingVisitor {
        Type type;
        SymbolTable currentTable;
        List<ParameterType> paramTypeList;

        public void visit(Program prog) {
            for(GlobalDefinition g: prog.definitions) {
                if(g instanceof TypeDefinition){
                    g.accept(this);
                } else if (g instanceof  ProcedureDefinition) {
                    g.accept(this);
                }
            }

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
            List<ParameterType> pList = new ArrayList<>();
            this.paramTypeList = pList;

            //Traverse parameters
            List<ParameterDefinition> params = procDef.parameters;
            for(ParameterDefinition p: params) {
                p.typeExpression.accept(this);
            }

            //Traverse Variables
            List<VariableDefinition> vList = procDef.variables;
            for(VariableDefinition v: vList) {
                v.typeExpression.accept(this);
            }

            List<Statement> sList = procDef.body;
            for(Statement s: sList) {
                s.accept(this); //TODO: Visit method for statement
            }

            for(ParameterType param : paramTypeList){
                if(param.type.byteSize != 0 && !param.isReference){
                    System.err.println("Error: parameter must be a reference parameter in line " + procDef.position.line);
                    System.exit(1);
                }
            }

            //Add entry to global table
            ProcedureEntry procEntry = new ProcedureEntry(currentTable, paramTypeList);
            globalTable.enter(procDef.name, procEntry);

            tableMapForPrinting.put(procDef.name, procEntry);
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

            typeDef.typeExpression.accept(this);
            globalTable.enter(typeDef.name, new TypeEntry(type)); //TODO: You could add an SplError Object to be thrown
        }

        public void visit(VariableDefinition varDef) {
            //Check if variable was already created with that name
            /*if(globalTable.lookup(varDef.name) != null){
                System.err.println("Error: Variable " + varDef.name + " in line " + varDef.position.line + " was already created");
                System.exit(1);
            }*/
            varDef.typeExpression.accept(this);
            currentTable.enter(varDef.name, new VariableEntry(type, false));
        }

        public void visit(ArrayTypeExpression arrType) {
            arrType.baseType.accept(this);
            this.type = new ArrayType(type, arrType.arraySize);
        }

        public void visit(ParameterDefinition paramDef) {
            //TODO: Check for double parameters
            if(currentTable.lookup(paramDef.name) != null) {
                System.err.println("Error: Parameter " + paramDef.name + " in line " + paramDef.position.line + " was already created");
                System.exit(1);
            }

            paramDef.typeExpression.accept(this);
            this.paramTypeList.add(new ParameterType(type, paramDef.isReference));
            currentTable.enter(paramDef.name, new VariableEntry(type, paramDef.isReference));
        }

        public void visit(NamedTypeExpression nameType) {
            Entry entry = globalTable.lookup(nameType.name);

            if(entry instanceof VariableEntry){
                type = ((VariableEntry) entry).type;
            }else if(entry instanceof TypeEntry){
                type = ((TypeEntry) entry).type;
            }else{
                System.err.println("Error: undefined type '" + nameType.name + "' in line " + nameType.position.line);
                System.exit(1);
            }
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
