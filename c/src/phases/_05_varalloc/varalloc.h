/*
 * varalloc.h -- variable allocation
 */

#ifndef _VARALLOC_H_
#define _VARALLOC_H_

#include "command_line_options.h"

#define REF_BYTE_SIZE    4    /* size of an address in bytes */

/**
 * This class is used to calculate the memory needed for variables and stack frames
 * of the currently compiled SPL program.
 *
 * Those values have to be stored in their corresponding fields in the ProcedureEntry, VariableEntry or
 * ParameterTypes structs.
 *
 * @param program The program for which the variables have to be allocated.
 * @param globalTable The symbol table for the current program.
 * @param options The command line options passed to the compiler
 */
void allocVars(Program *program, SymbolTable *globalTable, CommandLineOptions* options);

#endif /* _VARALLOC_H_ */
