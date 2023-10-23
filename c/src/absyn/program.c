/*
 * program.c -- abstract syntax for programs
 */

#include <util/memory.h>
#include "program.h"

Program *newProgram(Position position, GlobalDefinitionList *definitions) {
    Program *program = new(Program);
    program->position = position;
    program->definitions = definitions;
    return program;
}

void printProgram(FILE *out, int indentation, Program *program) {
    indent(out, indentation);

    if (program == NULL) {
        fprintf(out, "NULL");
        return;
    }

    fprintf(out, "Program(");
    printGlobalDefinitionList(out, indentation + INDENTATION_INCREMENT, true, program->definitions);
    fprintf(out, ")\n");

    fflush(out);
}
