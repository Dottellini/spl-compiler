/*
 * varalloc.c -- variable allocation
 */

#include <absyn/absyn.h>
#include <table/table.h>
#include <limits.h>
#include <string.h>
#include <util/ascii_table.h>
#include <util/string_ops.h>
#include <util/errors.h>
#include "varalloc.h"

DEFINE_LIST_TO_ARRAY(variableDefinitionListToArray, VariableDefinitionList, VariableDefinition)

DEFINE_LIST_GET(parameterDefinitionListGet, ParameterDefinitionList, ParameterDefinition)

DEFINE_LIST_GET(parameterTypeListGet, ParameterTypeList, ParameterType)

/**
 * Formats the variables of a procedure to a human readable format and prints it.
 * @param procDec       The procedure Definition
 * @param globalTable   The global symbol table
 */
static void showProcedureVarAlloc(GlobalDefinition *procDec, SymbolTable *globalTable) {
    Entry *entry = lookup(globalTable, procDec->name);
    SymbolTable *localTable = entry->u.procEntry.localTable;
    StackLayout *stackLayout = entry->u.procEntry.stackLayout;

    AsciiGraphicalTableBuilder *builder = newAsciiGraphicalTableBuilder();
    line(builder, ALIGN_CENTER, "...", "");

    char const *varparBase = stackLayout->isOptimizedLeafProcedure ? "SP" : "FP";

    //parameters
    {
        int sz = parameterDefinitionListSize(procDec->u.procedureDefinition.parameters);
        bool *index_ar = (bool *) allocate(sz * sizeof(bool *));

        for (int i = 0; i < sz; ++i) index_ar[i] = true;

        // This loop repeatedly picks the parameter with the highest offset from the list and then removes it until no more elements are available.
        // It essentially is a selection sort without the "sort" part, i.e. just a selection.
        while (true) {
            int maxOffsetIndex = -1;
            int maxOffset = INT_MIN;

            for (int i = 0; i < sz; ++i) {
                if (!index_ar[i]) continue;

                int offset = lookup(entry->u.procEntry.localTable,
                                    parameterDefinitionListGet(procDec->u.procedureDefinition.parameters,
                                                                i)->name)->u.varEntry.offset;

                if (maxOffsetIndex == -1 || offset > maxOffset) {
                    maxOffsetIndex = i;
                    maxOffset = offset;
                }
            }

            if (maxOffsetIndex < 0) break;
            int index = maxOffsetIndex;
            index_ar[maxOffsetIndex] = false;

            ParameterDefinition *parDec = parameterDefinitionListGet(procDec->u.procedureDefinition.parameters,
                                                                       index);
            ParameterType *parType = parameterTypeListGet(entry->u.procEntry.parameterTypes, index);
            Entry *parEntry = lookup(localTable, parDec->name);

            bool consistent = parEntry->u.varEntry.offset == parType->offset;
            String formattedOffset = consistent
                                     ? formatInt(parEntry->u.varEntry.offset, "NULL")
                                     : format("INCONSISTENT(%s/%s)", formatInt(parEntry->u.varEntry.offset, "NULL"),
                                              formatInt(parType->offset, "NULL"));

            line(builder, ALIGN_LEFT, format("par %s", parDec->name->string),
                 format("<- %s + %s", varparBase, formattedOffset));
        }

        release(index_ar);
    }

    sep(builder, ALIGN_CENTER, "BEGIN", format("<- %s", varparBase));

    //variables
    if (!procDec->u.procedureDefinition.variables->isEmpty) {
        int sz = variableDefinitionListSize(procDec->u.procedureDefinition.variables);

        VariableDefinition **ar = (VariableDefinition **) (allocate(sz * sizeof(VariableDefinition *)));
        variableDefinitionListToArray(ar, procDec->u.procedureDefinition.variables);

        // This loop repeatedly picks the variable with the highest offset from the list and then removes it until no more elements are available.
        // It essentially is a selection sort without the "sort" part, i.e. just a selection.
        while (true) {
            int maxOffsetIndex = -1;
            int maxOffset = INT_MIN;

            //Find the index of the variable with the biggest offset;
            for (int i = 0; i < sz; ++i) {
                if (ar[i] == NULL) continue;

                int offset = lookup(localTable, ar[i]->name)->u.varEntry.offset;

                if (maxOffsetIndex == -1 || offset > maxOffset) {
                    maxOffset = offset;
                    maxOffsetIndex = i;
                }
            }

            if (maxOffsetIndex < 0) break;

            Entry *varEntry = lookup(localTable, ar[maxOffsetIndex]->name);

            int offset = varEntry->u.varEntry.offset;
            line(builder, ALIGN_LEFT, format("var %s", ar[maxOffsetIndex]->name->string),
                 format("<- %s - %s", varparBase, offset == INT_MIN ? "NULL" : formatInt(-offset, "NULL")));
            ar[maxOffsetIndex] = NULL; //Remove from list
        }

        release(ar);

        if (!stackLayout->isOptimizedLeafProcedure) sep(builder, ALIGN_CENTER, "", "");
    }


    if (stackLayout->isOptimizedLeafProcedure) close(builder, "END", "");
    else {
        line(builder, ALIGN_LEFT, "Old FP",
             format("<- SP + %s", formatInt(getOldFramePointerOffSet(stackLayout), "UNKNOWN")));

        int oldReturn = getOldReturnAddressOffset(stackLayout);
        line(builder, ALIGN_LEFT, "Old Return",
             format("<- FP - %s", oldReturn == INT_MIN ? "UNKNOWN" : formatInt(-oldReturn, "UNKNOWN")));

        //outgoing area
        if (stackLayout->outgoingAreaSize == INT_MIN || stackLayout->outgoingAreaSize > 0) {
            sep(builder, ALIGN_CENTER, "outgoing area", "");

            if (stackLayout->outgoingAreaSize != INT_MIN) {
                int max_args = stackLayout->outgoingAreaSize / 4;

                for (int i = 0; i < max_args; ++i) {
                    line(builder, ALIGN_LEFT, format("arg %d", max_args - i),
                         format("<- SP + %d", (max_args - i - 1) * 4));
                }

            } else {
                line(builder, ALIGN_LEFT, "UNKNOWN SIZE", "");
            }
        }

        sep(builder, ALIGN_CENTER, "END", "<- SP");
        line(builder, ALIGN_CENTER, "...", "");
    }

    printf("Variable allocation for procedure '%s':\n", procDec->name->string);
    printf("  - size of argument area = %s\n", formatInt(stackLayout->argumentAreaSize, "NULL"));
    printf("  - size of localvar area = %s\n", formatInt(stackLayout->localVarAreaSize, "NULL"));
    printf("  - size of outgoing area = %s\n", formatInt(stackLayout->outgoingAreaSize, "NULL"));
    printf("  - frame size = %s\n", formatInt(getFrameSize(stackLayout), "UNKNOWN"));
    printf("\n");
    if (stackLayout->isOptimizedLeafProcedure) printf("  Stack layout (leaf optimized):\n");
    else printf("  Stack layout:\n");
    printTable(builder, stdout, 4);
    printf("\n\n");
}

/**
  * Formats the variable allocation to a human readable format and prints it.
  *
  * @param program      The abstract syntax tree of the program
  * @param globalTable  The symbol table containing all symbols of the spl program
  */
static void showVarAllocation(Program *program, SymbolTable *globalTable) {
    GlobalDefinitionList *definitionList = program->definitions;

    while (!definitionList->isEmpty) {
        if (definitionList->head->kind == DEFINITION_PROCEDUREDEFINITION) {
            showProcedureVarAlloc(definitionList->head, globalTable);
        }

        definitionList = definitionList->tail;
    }
}

void allocVars(Program *program, SymbolTable *globalTable, CommandLineOptions *options) {
    //TODO (assignment 5): Allocate stack slots for all parameters and local variables

    notImplemented();

    if (options->phaseOption == PHASE_VARS) showVarAllocation(program, globalTable);
}
