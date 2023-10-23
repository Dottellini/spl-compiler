/*
 * parameter_definition.h -- abstract syntax for parameter definitions
 */

#ifndef SPL_PARAMETER_DEFINITION_H
#define SPL_PARAMETER_DEFINITION_H

#include "_declarations.h"
#include "position.h"
#include <table/identifier.h>
#include <stdbool.h>

/**
 * This struct represents the local definition of a parameter in SPL.
 *
 * Parameter are declared in the parameter list of a procedure. They combine an identifier with
 * a type expression, expressing the parameters type and additionally have to store
 * whether the parameter is passed as a reference.
 * Parameters are only visible in the scope of their procedure.
 */
typedef struct parameter_definition {
    Position position;
    Identifier *name;
    TypeExpression *typeExpression;
    bool isReference;
} ParameterDefinition;

/**
 * Creates a new node representing the definition of a parameter in the head of a procedure.
 * @param line The position of the definition in the source code.
 * @param name The identifier of the declared parameter.
 * @param ty The type expression used to express the parameters type.
 * @param isRef A boolean value used to represent whether the parameter is passed as a reference.
 * @return A pointer to a newly created node.
 */
ParameterDefinition *newParameterDefinition(Position position, Identifier *name, TypeExpression *ty, bool isRef);

void printParameterDefinition(FILE *out, int indentation, ParameterDefinition *self);

#endif //SPL_PARAMETER_DEFINITION_H
