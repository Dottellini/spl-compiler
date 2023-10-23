/*
 * errors.h -- error reporting
 */

#ifndef SPL_ERRORS_H
#define SPL_ERRORS_H

#include <table/identifier.h>
#include <absyn/position.h>

#include <types/types.h>
#include "absyn/expressions.h"

/**
 * Displays an error to the user and aborts execution.
 *
 * Calling this function will exit the program with an exit code of 1.
 *
 * @param fmt A format string used to display the error message.
 * @param ... Additional parameters used by the format string.
 */
_Noreturn void error(char *fmt, ...);

/**
 * A function used to mark specific code blocks as "not implemented".
 * Calls to this function have to be removed from said code blocks and replaced
 * by correctly working code as part of the different assignments.
 */
_Noreturn void notImplemented();
_Noreturn void internalError();
_Noreturn void lexicalError(Position position, char character);
_Noreturn void syntaxError(Position position, char const *token, char const **expectedTokens, int expected_tokens_size);
_Noreturn void undefinedIdentifier(Position position, Identifier *name);
_Noreturn void notAType(Position position, Identifier *name);
_Noreturn void redefinitionOfIdentifier(Position position, Identifier *name);
_Noreturn void parameterMustBeReference(Position position, Identifier *name, Type *type);
_Noreturn void illegalAssignment(Position position, Type *left, Type *right);
_Noreturn void ifConditionMustBeBoolean(Position position, Type *actual);
_Noreturn void whileConditionMustBeBoolean(Position position, Type *actual);
_Noreturn void callOfNonProcedure(Position position, Identifier *name);
_Noreturn void argumentTypeMismatch(Position position, Identifier *name, int argumentIndex, Type *expected, Type *actual);
_Noreturn void argumentMustBeAVariable(Position position, Identifier *name, int argumentIndex);
_Noreturn void argumentCountMismatch(Position position, Identifier *name, int expected, int actual);
_Noreturn void binaryOperandTypeMismatch(Position position, BinaryOperator operator, Type *left, Type *right);
_Noreturn void unaryOperandTypeMismatch(Position position, UnaryOperator operator, Type *right);
_Noreturn void notAVariable(Position position, Identifier *name);
_Noreturn void indexingNonArray(Position position, Type *actual);
_Noreturn void indexTypeMismatch(Position position, Type *actual);
_Noreturn void mainIsMissing();
_Noreturn void mainIsNotAProcedure();
_Noreturn void mainMustNotHaveParameters();
_Noreturn void registerOverflow();

#endif //SPL_ERRORS_H
