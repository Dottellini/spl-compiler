/*
 * lists.c -- Lists structures part of the abstract syntax
 */

#include <stddef.h>
#include <util/memory.h>
#include <util/list.h>
#include "lists.h"
#include "global_definitions.h"
#include "statements.h"
#include "parameter_definition.h"
#include "variable_definition.h"
#include "expressions.h"
#include "printing/printing_helpers.h"
#include "type_expressions.h"

DEFINE_EMPTY_LIST(emptyGlobalDefinitionList, GlobalDefinitionList)

DEFINE_EMPTY_LIST(emptyParameterList, ParameterDefinitionList)

DEFINE_EMPTY_LIST(emptyVariableList, VariableDefinitionList)

DEFINE_EMPTY_LIST(emptyStatementList, StatementList)

DEFINE_EMPTY_LIST(emptyExpressionList, ExpressionList)

DEFINE_LIST_CONSTRUCTOR(newGlobalDefinitionList, GlobalDefinitionList, GlobalDefinition)

DEFINE_LIST_CONSTRUCTOR(newParameterList, ParameterDefinitionList, ParameterDefinition)

DEFINE_LIST_CONSTRUCTOR(newVariableList, VariableDefinitionList, VariableDefinition)

DEFINE_LIST_CONSTRUCTOR(newStatementList, StatementList, Statement)

DEFINE_LIST_CONSTRUCTOR(newExpressionList, ExpressionList, Expression)

#define DEFINE_LIST_PRINTING_FUNCTION(name, list_type, element_printing_function)                   \
    void name (FILE *out, int indentation, bool isFirst, list_type *list) {                         \
        if (list != NULL && list->isEmpty) return;                                                  \
                                                                                                    \
        if (isFirst) fprintf(out, "\n");                                                            \
        else fprintf(out, ",\n");                                                                   \
                                                                                                    \
        if (list == NULL) {                                                                         \
            indent(out, indentation);                                                               \
            if (isFirst) fprintf(out, "LIST_NULL");                                                 \
            else fprintf(out, "TAIL_NULL");                                                         \
            return;                                                                                 \
        }                                                                                           \
                                                                                                    \
        element_printing_function(out, indentation, list->head);                                    \
        name(out, indentation, false, list->tail);                                                  \
    }

DEFINE_LIST_PRINTING_FUNCTION(printGlobalDefinitionList, GlobalDefinitionList, printGlobalDefinition)

DEFINE_LIST_PRINTING_FUNCTION(printVariableDefinitionList, VariableDefinitionList, printVariableDefinition)

DEFINE_LIST_PRINTING_FUNCTION(printParameterDefinitionList, ParameterDefinitionList, printParameterDefinition)

DEFINE_LIST_PRINTING_FUNCTION(printStatementList, StatementList, printStatement)

DEFINE_LIST_PRINTING_FUNCTION(printExpressionList, ExpressionList, printExpression)

DEFINE_LIST_SIZE(globalDefinitionListSize, GlobalDefinitionList)

DEFINE_LIST_SIZE(variableDefinitionListSize, VariableDefinitionList)

DEFINE_LIST_SIZE(parameterDefinitionListSize, ParameterDefinitionList)

DEFINE_LIST_SIZE(expressionListSize, ExpressionList)