package de.thm.mni.compilerbau.phases._05_varalloc;

import de.thm.mni.compilerbau.CommandLineOptions;
import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitor;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.utils.*;
import java_cup.runtime.Symbol;

import java.util.*;
import java.util.stream.IntStream;

/**
 * This class is used to calculate the memory needed for variables and stack frames of the currently compiled SPL program.
 * Those value have to be stored in their corresponding fields in the {@link ProcedureEntry}, {@link VariableEntry} and
 * {@link ParameterType} classes.
 */
public class VarAllocator {
    public static final int REFERENCE_BYTESIZE = 4;
    SymbolTable globalTable;
    boolean showVarAlloc;
    ArrayList<String> predefinedProcedures;
    int listIndex;
    int resultSize;

    private final CommandLineOptions options;

    /**
     * @param options The options passed to the compiler
     */
    public VarAllocator(CommandLineOptions options) {
        this.options = options;
    }

    public void allocVars(Program program, SymbolTable table) {
        this.showVarAlloc = options.phaseOption == CommandLineOptions.PhaseOption.VARS;
        //TODO (assignment 5): Allocate stack slots for all parameters and local variables
        this.globalTable = table;
        this.predefinedProcedures = new ArrayList<>();
        this.predefinedProcedures.addAll(List.of("printi", "printc", "readi", "readc", "exit", "time", "clearAll", "setPixel", "drawLine", "drawCircle"));

        Visitor firstRoundVisitor = new AllocatorVisitor(table);
        program.accept(firstRoundVisitor);

        Visitor secondRoundVisitor = new AllocatorVisitorSecondIteration(table);
        program.accept(secondRoundVisitor);

        if (showVarAlloc) formatVars(program, table);
    }

    class AllocatorVisitor extends DoNothingVisitor {
        SymbolTable globalTable;
        SymbolTable localTable;
        ArrayList<Integer> valueOffsetList;

        AllocatorVisitor(SymbolTable globalTable) {
            this.valueOffsetList = new ArrayList<>();
            this.globalTable = globalTable;
        }
        @Override
        public void visit(Program program) {
            program.definitions.stream().filter(d -> d instanceof ProcedureDefinition).forEach(p -> p.accept(this));
        }

        public void visit(IfStatement ifStatement) {
            ifStatement.thenPart.accept(this);
            ifStatement.elsePart.accept(this);
        }

        public void visit(WhileStatement whileStatement) {
            whileStatement.body.accept(this);
        }

        public void visit(CallStatement callStatement) {

        }

        public void visit(ProcedureDefinition procedureDefinition) {
            ProcedureEntry entry = (ProcedureEntry) globalTable.lookup(procedureDefinition.name);
            this.localTable = entry.localTable;
            int size = 0;
            entry.stackLayout.argumentAreaSize = 0;
            for(ParameterType p: entry.parameterTypes) {
                p.offset = entry.stackLayout.argumentAreaSize;
                valueOffsetList.add(p.offset);

                if(p.isReference) {
                    size = REFERENCE_BYTESIZE;
                } else {
                    size = p.type.byteSize;
                }

                entry.stackLayout.argumentAreaSize += size;
            }

            procedureDefinition.parameters.forEach(p -> {
                p.accept(this);
                listIndex++;
            });

            resultSize = 0;
            procedureDefinition.variables.forEach(v -> {
                v.accept(this);
            });
            resultSize *= (-1);
            entry.stackLayout.localVarAreaSize = resultSize;
        }

        public void visit(VariableDefinition variableDefinition) {
            VariableEntry entry = (VariableEntry) localTable.lookup(variableDefinition.name);
            resultSize -= entry.type.byteSize;
            entry.offset = resultSize;
        }

        public void visit(ParameterDefinition parameterDefinition) {
            VariableEntry entry = (VariableEntry) localTable.lookup(parameterDefinition.name);
            entry.offset = valueOffsetList.get(listIndex);
        }
    }

    private class AllocatorVisitorSecondIteration extends DoNothingVisitor {
        SymbolTable globalTable;
        int callStatementSize = 0;
        int counter = 0;

        AllocatorVisitorSecondIteration(SymbolTable globalTable) {
            this.globalTable = globalTable;
        }

        public void visit(Program program) {
            program.definitions.forEach(p -> p.accept(this));
        }

        public void visit(ProcedureDefinition procedureDefinition) {
            ProcedureEntry procedureEntry = (ProcedureEntry) globalTable.lookup(procedureDefinition.name);
            this.counter = 0;
            this.callStatementSize = 0;
            procedureDefinition.body.forEach(s -> s.accept(this));

            if(this.counter != 0) {
                procedureEntry.stackLayout.outgoingAreaSize = this.callStatementSize;
            } else {
                procedureEntry.stackLayout.outgoingAreaSize = -1;
            }
        }

        public void visit(CallStatement callStatement) {
            this.counter++;
            Entry entry = globalTable.lookup(callStatement.procedureName);
            int size = ((ProcedureEntry) entry).stackLayout.argumentAreaSize;
            if(this.callStatementSize < size && size != 0) {
                this.callStatementSize = size;
            }
        }

        public void visit(IfStatement ifStatement) {
            ifStatement.thenPart.accept(this);
            ifStatement.elsePart.accept(this);
        }

        public void visit(WhileStatement whileStatement) {
            whileStatement.body.accept(this);
        }

        public void visit(CompoundStatement compoundStatement) {
            compoundStatement.statements.forEach(s -> s.accept(this));
        }



    }

    /**
     * Formats and prints the variable allocation to a human-readable format
     * The stack layout
     *
     * @param program The abstract syntax tree of the program
     * @param table   The symbol table containing all symbols of the spl program
     */
    private static void formatVars(Program program, SymbolTable table) {
        program.definitions.stream().filter(dec -> dec instanceof ProcedureDefinition).map(dec -> (ProcedureDefinition) dec).forEach(procDec -> {
            ProcedureEntry entry = (ProcedureEntry) table.lookup(procDec.name);

            var isLeafOptimized = false; // This is a remainder from a bonus assignment, but I refuse to adjust this entire mess of a method
            var varparBasis = (isLeafOptimized ? "SP" : "FP");

            AsciiGraphicalTableBuilder ascii = new AsciiGraphicalTableBuilder();
            ascii.line("...", AsciiGraphicalTableBuilder.Alignment.CENTER);

            {
                final var zipped = IntStream.range(0, procDec.parameters.size()).boxed()
                        .map(i -> new Pair<>(procDec.parameters.get(i), new Pair<>(((VariableEntry) entry.localTable.lookup(procDec.parameters.get(i).name)), entry.parameterTypes.get(i))))
                        .sorted(Comparator.comparing(p -> Optional.ofNullable(p.second.first.offset).map(o -> -o).orElse(Integer.MIN_VALUE)));

                zipped.forEach(v -> {
                    boolean consistent = Objects.equals(v.second.first.offset, v.second.second.offset);

                    ascii.line("par " + v.first.name.toString(), "<- " + varparBasis + " + " +
                                    (consistent
                                            ? Objects.toString(v.second.first.offset, "NULL")
                                            : String.format("INCONSISTENT(%s/%s)", Objects.toString(v.second.first.offset, "NULL"), Objects.toString(v.second.second.offset, "NULL"))),
                            AsciiGraphicalTableBuilder.Alignment.LEFT);
                });
            }

            ascii.sep("BEGIN", "<- " + varparBasis);
            if (!procDec.variables.isEmpty()) {
                procDec.variables.stream()
                        .map(v -> new AbstractMap.SimpleImmutableEntry<>(v, ((VariableEntry) entry.localTable.lookup(v.name))))
                        .sorted(Comparator.comparing(e -> Try.execute(() -> -e.getValue().offset).getOrElse(0)))
                        .forEach(v -> ascii.line("var " + v.getKey().name.toString(),
                                "<- " + varparBasis + " - " + Optional.ofNullable(v.getValue().offset).map(o -> -o).map(Objects::toString).orElse("NULL"),
                                AsciiGraphicalTableBuilder.Alignment.LEFT));

                if (!isLeafOptimized) ascii.sep("");
            }

            if (isLeafOptimized) ascii.close("END");
            else {
                ascii.line("Old FP",
                        "<- SP + " + Try.execute(entry.stackLayout::oldFramePointerOffset).map(Objects::toString).getOrElse("UNKNOWN"),
                        AsciiGraphicalTableBuilder.Alignment.LEFT);

                ascii.line("Old Return",
                        "<- FP - " + Try.execute(() -> -entry.stackLayout.oldReturnAddressOffset()).map(Objects::toString).getOrElse("UNKNOWN"),
                        AsciiGraphicalTableBuilder.Alignment.LEFT);

                if (entry.stackLayout.outgoingAreaSize == null || entry.stackLayout.outgoingAreaSize > 0) {

                    ascii.sep("outgoing area");

                    if (entry.stackLayout.outgoingAreaSize != null) {
                        var max_args = entry.stackLayout.outgoingAreaSize / 4;

                        for (int i = 0; i < max_args; ++i) {
                            ascii.line(String.format("arg %d", max_args - i),
                                    String.format("<- SP + %d", (max_args - i - 1) * 4),
                                    AsciiGraphicalTableBuilder.Alignment.LEFT);
                        }
                    } else {
                        ascii.line("UNKNOWN SIZE", AsciiGraphicalTableBuilder.Alignment.LEFT);
                    }
                }

                ascii.sep("END", "<- SP");
                ascii.line("...", AsciiGraphicalTableBuilder.Alignment.CENTER);
            }

            System.out.printf("Variable allocation for procedure '%s':\n", procDec.name);
            System.out.printf("  - size of argument area = %s\n", Objects.toString(entry.stackLayout.argumentAreaSize, "NULL"));
            System.out.printf("  - size of localvar area = %s\n", Objects.toString(entry.stackLayout.localVarAreaSize, "NULL"));
            System.out.printf("  - size of outgoing area = %s\n", Objects.toString(entry.stackLayout.outgoingAreaSize, "NULL"));
            System.out.printf("  - frame size = %s\n", Try.execute(entry.stackLayout::frameSize).map(Objects::toString).getOrElse("UNKNOWN"));
            System.out.println();
            if (isLeafOptimized) System.out.println("  Stack layout (leaf optimized):");
            else System.out.println("  Stack layout:");
            System.out.println(ascii.toString().indent(4));
            System.out.println();
        });
    }
}
