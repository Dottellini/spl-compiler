/*
 * lists.h -- Lists structures part of the abstract syntax
 */

#ifndef SPL_LISTS_H
#define SPL_LISTS_H

#include <table/identifier.h>
#include "_declarations.h"
#include <stdbool.h>
#include <stdio.h>

typedef struct global_definition_list {
    bool isEmpty;
    GlobalDefinition *head;
    struct global_definition_list *tail;
} GlobalDefinitionList;

typedef struct parameter_list {
    bool isEmpty;
    ParameterDefinition *head;
    struct parameter_list *tail;
} ParameterDefinitionList;

typedef struct variable_definition_list {
    bool isEmpty;
    VariableDefinition *head;
    struct variable_definition_list *tail;
} VariableDefinitionList;

typedef struct statement_list {
    bool isEmpty;
    Statement *head;
    struct statement_list *tail;
} StatementList;

typedef struct expression_list {
    bool isEmpty;
    Expression *head;
    struct expression_list *tail;
} ExpressionList;

/**
 * @return An empty list for global definitions.
 */
GlobalDefinitionList *emptyGlobalDefinitionList(void);

/**
 * Creates a new GlobalDefinitionList by prepending a new head to an existing list.
 * Does not modify the existing list.
 * @param head The head of the new list.
 * @param tail The tail of the new list.
 * @return A list containing the new head as well as the old list as its tail.
 */
GlobalDefinitionList *newGlobalDefinitionList(GlobalDefinition *head, GlobalDefinitionList *tail);

/**
 * @return An empty list for variables.
 */
VariableDefinitionList *emptyVariableList(void);

/**
 * Creates a new VariableDefinitionList by prepending a new head to an existing list.
 * Does not modify the existing list.
 * @param head The head of the new list.
 * @param tail The tail of the new list.
 * @return A list containing the new head as well as the old list as its tail.
 */
VariableDefinitionList *newVariableList(VariableDefinition *head, VariableDefinitionList *tail);

/**
 * @return An empty list for parameters.
 */
ParameterDefinitionList *emptyParameterList(void);

/**
 * Creates a new ParameterDefinitionList by prepending a new head to an existing list.
 * Does not modify the existing list.
 * @param head The head of the new list.
 * @param tail The tail of the new list.
 * @return A list containing the new head as well as the old list as its tail.
 */
ParameterDefinitionList *newParameterList(ParameterDefinition *head, ParameterDefinitionList *tail);

/**
 * Returns an empty list for statements.
 */
StatementList *emptyStatementList(void);

/**
 * Creates a new StatementList by prepending a new head to an existing list.
 * Does not modify the existing list.
 * @param head The head of the new list.
 * @param tail The tail of the new list.
 * @return A list containing the new head as well as the old list as its tail.
 */
StatementList *newStatementList(Statement *head, StatementList *tail);

/**
 * @return An empty list for expressions.
 */
ExpressionList *emptyExpressionList(void);

/**
 * Creates a new ExpressionList by prepending a new head to an existing list.
 * Does not modify the existing list.
 * @param head The head of the new list.
 * @param tail The tail of the new list.
 * @return A list containing the new head as well as the old list as its tail.
 */
ExpressionList *newExpressionList(Expression *head, ExpressionList *tail);

void printGlobalDefinitionList(FILE *out, int indentation, bool isFirst, GlobalDefinitionList *list);

void printParameterDefinitionList(FILE *out, int indentation, bool isFirst, ParameterDefinitionList *list);

void printVariableDefinitionList(FILE *out, int indentation, bool isFirst, VariableDefinitionList *list);

void printStatementList(FILE *out, int indentation, bool isFirst, StatementList *list);

void printExpressionList(FILE *out, int indentation, bool isFirst, ExpressionList *list);

int globalDefinitionListSize(GlobalDefinitionList* list);

int variableDefinitionListSize(VariableDefinitionList* list);

int parameterDefinitionListSize(ParameterDefinitionList* list);

int expressionListSize(ExpressionList* list);

#endif //SPL_LISTS_H
