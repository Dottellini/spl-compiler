/*
 * tablebuild.h -- symbol table creation
 */

#ifndef _TABLEBUILD_H
#define _TABLEBUILD_H

#include <table/table.h>
#include "absyn/absyn.h"
#include "command_line_options.h"

/**
 * This function is used to create and populate a table containing entries for every symbol in the currently
 * compiled SPL program.
 * Every definition of the SPL program needs its corresponding entry in the table.
 *
 * Types calculated in this function can be stored in the type field of Expressions, Variables and TypeExpressions.
 *
 * @param program The program for which the table has to be built.
 * @param options The command line options passed to the compiler.
 */
SymbolTable *buildSymbolTable(Program *program, CommandLineOptions* options);

/**
 * Prints the local symbol table of a procedure together with a heading-line
 * NOTE: You have to call this after completing the local table to support '--tables'.
 * @param name           The name of the procedure
 * @param procedureEntry The entry of the procedure to print
 */
void printSymbolTableAtEndOfProcedure(Identifier *name, Entry *procedureEntry);

#endif //_TABLEBUILD_H