/*
 * variable_definition.c -- abstract syntax for variable definitions
 */

#include "variable_definition.h"
#include "type_expressions.h"
#include <util/memory.h>
#include <absyn/printing/printing_helpers.h>

VariableDefinition *newVariableDefinition(Position position, Identifier *name, TypeExpression *ty) {
    VariableDefinition *node = new(VariableDefinition);
    node->position = position;
    node->name = name;
    node->typeExpression = ty;
    return node;
}

void printVariableDefinition(FILE *out, int indentation, VariableDefinition *self) {
    indent(out, indentation);

    if (self == NULL) {
        fprintf(out, "NULL");
        return;
    }

    fprintf(out, "VariableDefinition(\n");
    printIdentifier(out, indentation + INDENTATION_INCREMENT, self->name);
    fprintf(out, ",\n");
    printTypeExpression(out, indentation + INDENTATION_INCREMENT, self->typeExpression);
    fprintf(out, ")");
}

