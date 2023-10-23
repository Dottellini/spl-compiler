/*
 * global_definitions.h -- abstract syntax for global definitions
 */

#ifndef SPL_GLOBAL_DEFINITIONS_H
#define SPL_GLOBAL_DEFINITIONS_H

#include "_declarations.h"
#include "position.h"
#include <table/identifier.h>
#include <stdio.h>

/**
 * Used to differentiate between global definition kinds.
 */
typedef enum {
    DEFINITION_TYPEDEFINITION,
    DEFINITION_PROCEDUREDEFINITION
} global_definition_kind;

/**
 * This struct represents a global definition in SPL.
 *
 * Global definitions are all definitions done in the global scope.
 * There are two kinds of global definitions:
 *
 * 1. Type definitions representing the definition of a new type. When declaring a new type it is necessary
 * to provide an identifier for the newly declared type and a type expression describing the type.
 *
 * 2. Procedure definitions representing the definition of a new procedure. When declaring a procedure
 * an identifier has to be provided, that is used as the identifier of the definition.
 * Additionally a procedure definition requires a list of parameters, a list of local variables and
 * a list of statements in the body of the procedure.
 */
typedef struct global_definition {
    Position position;
    global_definition_kind kind;
    Identifier *name;
    union {
        struct {
            TypeExpression *typeExpression;
        } typeDefinition;
        struct {
            ParameterDefinitionList *parameters;
            VariableDefinitionList *variables;
            StatementList *body;
        } procedureDefinition;
    } u;
} GlobalDefinition;

/**
 * Creates a new node representing a type definition.
 * @param line The position of the definition in the source code.
 * @param name The definitions identifier.
 * @param ty The type expression associated with this definition.
 * @return A pointer to a newly created node.
 */
GlobalDefinition *newTypeDefinition(Position position, Identifier *name, TypeExpression *ty);

/**
 * Creates a new node representing a procedure definition.
 * @param line The position of the definition in the source code.
 * @param name The procedures identifier.
 * @param params The procedures parameter list.
 * @param decls The procedures local variables.
 * @param body The statements in the body of the procedure.
 * @return A pointer to a newly created node.
 */
GlobalDefinition *newProcedureDefinition(Position position, Identifier *name, ParameterDefinitionList *params, VariableDefinitionList *decls, StatementList *body);

void printGlobalDefinition(FILE *out, int intendation, GlobalDefinition *self);

#endif //SPL_GLOBAL_DEFINITIONS_H
