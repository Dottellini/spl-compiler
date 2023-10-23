/*
 * expressions.c -- abstract syntax for expressions
 */

#include "expressions.h"
#include "variables.h"
#include "lists.h"

#include <stddef.h>
#include <util/memory.h>
#include <absyn/printing/printing_helpers.h>
#include <assert.h>

bool isOperatorArithmetic(BinaryOperator operator) {
    switch (operator) {
        case ABSYN_OP_ADD:
        case ABSYN_OP_SUB:
        case ABSYN_OP_MUL:
        case ABSYN_OP_DIV:
            return true;
        default:
            return false;
    }
}

bool isOperatorEquality(BinaryOperator operator){
    switch (operator) {
        case ABSYN_OP_EQU:
        case ABSYN_OP_NEQ:
            return true;
        default:
            return false;
    }
}

bool isOperatorComparison(BinaryOperator operator){
    switch (operator) {
        case ABSYN_OP_EQU:
        case ABSYN_OP_NEQ:
        case ABSYN_OP_GRT:
        case ABSYN_OP_GRE:
        case ABSYN_OP_LST:
        case ABSYN_OP_LSE:
            return true;
        default:
            return false;
    }
}

BinaryOperator flipComparisonOperator(BinaryOperator operator) {
    switch (operator) {
        case ABSYN_OP_EQU:
            return ABSYN_OP_NEQ;
        case ABSYN_OP_NEQ:
            return ABSYN_OP_EQU;
        case ABSYN_OP_LST:
            return ABSYN_OP_GRE;
        case ABSYN_OP_LSE:
            return ABSYN_OP_GRT;
        case ABSYN_OP_GRT:
            return ABSYN_OP_LSE;
        case ABSYN_OP_GRE:
            return ABSYN_OP_LST;
        default:
            assert(false);
    }
}

Expression *newExpression(Position position, expression_kind kind) {
    Expression *node = new(Expression);

    node->position = position;
    node->kind = kind;
    node->dataType = NULL;
    node->ershovNumber = -1;
    return node;
}

Expression *newBinaryExpression(Position position, BinaryOperator op, Expression *left, Expression *right) {
    Expression *node = newExpression(position, EXPRESSION_BINARYEXPRESSION);
    node->u.binaryExpression.operator = op;
    node->u.binaryExpression.leftOperand = left;
    node->u.binaryExpression.rightOperand = right;
    return node;
}

Expression *newUnaryExpression(Position position, UnaryOperator op, Expression *operand) {
    Expression *node = newExpression(position, EXPRESSION_UNARYEXPRESSION);
    node->u.unaryExpression.operator = op;
    node->u.unaryExpression.operand = operand;
    return node;
}

Expression *newVariableExpression(Position position, Variable *var) {
    Expression *node = newExpression(position, EXPRESSION_VARIABLEEXPRESSION);
    node->u.variableExpression.variable = var;
    return node;
}

Expression *newIntLiteral(Position position, int val) {
    Expression *node = newExpression(position, EXPRESSION_INTLITERAL);
    node->u.intLiteral.value = val;
    return node;
}

static void printBinaryOperator(FILE *out, int indentation, BinaryOperator operator) {
    static const char *BINARY_OPERATOR_NAMES[] = {
            "EQU",
            "NEQ",
            "LST",
            "LSE",
            "GRT",
            "GRE",
            "ADD",
            "SUB",
            "MUL",
            "DIV",
    };

    indent(out, indentation);
    fprintf(out, "%s", BINARY_OPERATOR_NAMES[operator]);
}

static void printUnaryOperator(FILE *out, int indentation, UnaryOperator operator) {
    static const char *UNARY_OPERATOR_NAMES[] = {
            "MINUS",
    };

    indent(out, indentation);
    fprintf(out, "%s", UNARY_OPERATOR_NAMES[operator]);
}

void printExpression(FILE *out, int indentation, Expression *expression) {
    indent(out, indentation);
    if (expression == NULL) {
        fprintf(out, "NULL");
        return;
    }

    switch (expression->kind) {
        case EXPRESSION_BINARYEXPRESSION:
            fprintf(out, "BinaryExpression(\n");
            printBinaryOperator(out, indentation + INDENTATION_INCREMENT, expression->u.binaryExpression.operator);
            fprintf(out, ",\n");

            printExpression(out, indentation + INDENTATION_INCREMENT, expression->u.binaryExpression.leftOperand);
            fprintf(out, ",\n");

            printExpression(out, indentation + INDENTATION_INCREMENT, expression->u.binaryExpression.rightOperand);
            break;
        case EXPRESSION_UNARYEXPRESSION:
            fprintf(out, "UnaryExpression(\n");
            printUnaryOperator(out, indentation + INDENTATION_INCREMENT, expression->u.unaryExpression.operator);
            fprintf(out, ",\n");

            printExpression(out, indentation + INDENTATION_INCREMENT, expression->u.unaryExpression.operand);
            break;
        case EXPRESSION_INTLITERAL:
            fprintf(out, "IntLiteral(\n");
            printInteger(out, indentation + INDENTATION_INCREMENT, expression->u.intLiteral.value);
            break;
        case EXPRESSION_VARIABLEEXPRESSION:
            fprintf(out, "VariableExpression(\n");
            printVariable(out, indentation + INDENTATION_INCREMENT, expression->u.variableExpression.variable);
            break;
        default:
            printUnknownKind(out, expression->kind);
    }

    fprintf(out, ")");
}
