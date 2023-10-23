/*
 * global_definitions.c -- abstract syntax for global definitions
 */

#include "global_definitions.h"
#include "lists.h"
#include "parameter_definition.h"
#include "variable_definition.h"
#include "statements.h"
#include "type_expressions.h"
#include <util/memory.h>
#include <absyn/printing/printing_helpers.h>

static GlobalDefinition *newGlobalDefinition(Position position, global_definition_kind kind, Identifier *name) {
    GlobalDefinition *node = new(GlobalDefinition);

    node->position = position;
    node->kind = kind;
    node->name = name;
    return node;
}

GlobalDefinition *newTypeDefinition(Position position, Identifier *name, TypeExpression *ty) {
    GlobalDefinition *node = newGlobalDefinition(position, DEFINITION_TYPEDEFINITION, name);
    node->u.typeDefinition.typeExpression = ty;
    return node;
}

GlobalDefinition *newProcedureDefinition(Position position, Identifier *name, ParameterDefinitionList *params, VariableDefinitionList *decls, StatementList *body) {
    GlobalDefinition *node = newGlobalDefinition(position, DEFINITION_PROCEDUREDEFINITION, name);
    node->u.procedureDefinition.parameters = params;
    node->u.procedureDefinition.variables = decls;
    node->u.procedureDefinition.body = body;
    return node;
}

static void printProcedureDefinition(FILE *out, int indentation, GlobalDefinition *self) {
    fprintf(out, "ProcedureDefinition(\n");
    printIdentifier(out, indentation + INDENTATION_INCREMENT, self->name);
    fprintf(out, ",\n");

    // Print Parameter List
    {
        indent(out, indentation + INDENTATION_INCREMENT);
        fprintf(out, "Parameters(");
        printParameterDefinitionList(out, indentation + 2 * INDENTATION_INCREMENT, true, self->u.procedureDefinition.parameters);
        fprintf(out, "),\n");
    }

    // Print Variable list
    {
        indent(out, indentation + INDENTATION_INCREMENT);
        fprintf(out, "Variables(");
        printVariableDefinitionList(out, indentation + 2 * INDENTATION_INCREMENT, true, self->u.procedureDefinition.variables);
        fprintf(out, "),\n");
    }

    // Print Body
    {
        indent(out, indentation + INDENTATION_INCREMENT);
        fprintf(out, "Body(");
        printStatementList(out, indentation + 2 * INDENTATION_INCREMENT, true, self->u.procedureDefinition.body);
        fprintf(out, ")");
    }

    // End of ProcedureDefinition
    fprintf(out, ")");
}

static void printTypeDefinition(FILE *out, int indentation, GlobalDefinition *globalDefinition) {
    fprintf(out, "TypeDefinition(\n");
    printIdentifier(out, indentation + INDENTATION_INCREMENT, globalDefinition->name);
    fprintf(out, ",\n");
    printTypeExpression(out, indentation + INDENTATION_INCREMENT, globalDefinition->u.typeDefinition.typeExpression);
    fprintf(out, ")");
}

void printGlobalDefinition(FILE *out, int indentation, GlobalDefinition *self) {
    indent(out, indentation);

    if (self == NULL) {
        fprintf(out, "NULL");
        return;
    }

    switch (self->kind) {
        case DEFINITION_PROCEDUREDEFINITION:
            printProcedureDefinition(out, indentation, self);
            break;
        case DEFINITION_TYPEDEFINITION:
            printTypeDefinition(out, indentation, self);
            break;
        default:
            printUnknownKind(out, self->kind);
    }
}
