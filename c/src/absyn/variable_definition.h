/*
 * variable_definition.h -- abstract syntax for variable definitions
 */

#ifndef _VARIABLE_DEFINITION_H
#define _VARIABLE_DEFINITION_H

#include "_declarations.h"
#include "position.h"
#include <table/identifier.h>
#include <stdio.h>

/**
 * This struct represents the local definition of a variable in SPL.
 *
 * Variables are declared inside a procedure and combine an identifier with a type expression,
 * expressing the variables type.
 * Variables are only visible in the local scope of their procedure.
 */
typedef struct variable_definition {
    Position position;
    Identifier *name;
    TypeExpression *typeExpression;
} VariableDefinition;

/**
 * Creates a new node representing the definition of a local variable in a procedures body.
 * @param line The position of the definition in the source code.
 * @param name The identifier of the declared local variable.
 * @param ty The type expression used to express the type of the local variable.
 * @return A pointer to a newly created node.
 */
VariableDefinition *newVariableDefinition(Position position, Identifier *name, TypeExpression *ty);

void printVariableDefinition(FILE *out, int indentation, VariableDefinition *self);

#endif //_VARIABLE_DEFINITION_H
