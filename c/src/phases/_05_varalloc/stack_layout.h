#ifndef SPL_STACK_LAYOUT_H
#define SPL_STACK_LAYOUT_H

#include <util/memory.h>
#include <stdbool.h>

/**
 * This struct describes the stack frame layout of a procedure.
 * It contains the sizes of the various subareas.
 * There are also accompanying functions that provide additional information about the stack frame required to generate code for the procedure.
 */
typedef struct {
    int argumentAreaSize;
    int localVarAreaSize;
    int outgoingAreaSize;
    bool isOptimizedLeafProcedure;
} StackLayout;

/**
 * @return A newly created stack layout object
 */
StackLayout *newStackLayout();

/**
 * @return The total size of the stack frame described by this object.
 */
int getFrameSize(StackLayout *stackLayout);

/**
 * @return The offset (starting from the new stack pointer) where the old frame pointer is stored in this stack frame.
 */
int getOldFramePointerOffSet(StackLayout *stackLayout);

/**
 * @return The offset (starting from the new frame pointer) where the old return adress is stored in this stack frame.
 */
int getOldReturnAddressOffset(StackLayout *stackLayout);

#endif //SPL_STACK_LAYOUT_H
