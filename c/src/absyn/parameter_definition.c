/*
 * parameter_definition.c -- abstract syntax for parameter definitions
 */

#include <absyn/printing/printing_helpers.h>
#include <util/memory.h>
#include "parameter_definition.h"
#include "type_expressions.h"

ParameterDefinition *newParameterDefinition(Position position, Identifier *name, TypeExpression *ty, bool isRef) {
    ParameterDefinition *node = new(ParameterDefinition);
    node->position = position;
    node->name = name;
    node->typeExpression = ty;
    node->isReference = isRef;
    return node;
}

void printParameterDefinition(FILE *out, int indentation, ParameterDefinition *self) {
    indent(out, indentation);

    if (self == NULL) {
        fprintf(out, "NULL");
        return;
    }

    fprintf(out, "ParameterDefinition(\n");
    printIdentifier(out, indentation + INDENTATION_INCREMENT, self->name);
    fprintf(out, ",\n");
    printTypeExpression(out, indentation + INDENTATION_INCREMENT, self->typeExpression);
    fprintf(out, ",\n");
    printBoolean(out, indentation + INDENTATION_INCREMENT, self->isReference);
    fprintf(out, ")");
}
