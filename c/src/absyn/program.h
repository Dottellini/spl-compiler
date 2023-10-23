/*
 * program.h -- abstract syntax for programs
 */

#ifndef _PROGRAM_H
#define _PROGRAM_H

#include <stdio.h>
#include <absyn/printing/printing_helpers.h>
#include "lists.h"
#include "global_definitions.h"
#include "_declarations.h"
#include "position.h"

/**
 * The Program type represents the root of the AST.
 * It consists of a list containing all global definitions of a SPL program.
 */
typedef struct program {
    Position position;
    GlobalDefinitionList *definitions;
} Program;

/**
 * Creates a new node representing an entire program.
 *
 * @param line The position of the program in the source code.
 * @param definitions The list of global definitions in the program.
 * @return The pointer to a newly created node.
 */
Program *newProgram(Position position, GlobalDefinitionList *definitions);

void printProgram(FILE *out, int indentation, Program *program);

#endif //_PROGRAM_H
