/*
 * _declarations.h -- Some shared declarations needed across many absyn files
 */

#ifndef SPL_DECLARATIONS_H
#define SPL_DECLARATIONS_H

/**
 * This file contains declarations for the various structs forming the abstract syntax tree for spl.
 * Due to the fact that C requires all structs to be declared before being used, each header file would have to declare all structs it uses.
 * To avoid repetition, all declarations are placed in this header file.
 * All other header files defining the structures then include this file to have all declarations available.
 */

/* Declare abstract syntax structures */
typedef struct expression Expression;
typedef struct global_definition GlobalDefinition;
typedef struct variable Variable;
typedef struct program Program;
typedef struct statement Statement;
typedef struct type_expression TypeExpression;
typedef struct parameter_definition ParameterDefinition;
typedef struct variable_definition VariableDefinition;

/* Declare list structures used in the abstract syntax */
typedef struct global_definition_list GlobalDefinitionList;
typedef struct parameter_list ParameterDefinitionList;
typedef struct variable_definition_list VariableDefinitionList;
typedef struct statement_list StatementList;
typedef struct expression_list ExpressionList;

#endif //SPL_DECLARATIONS_H
