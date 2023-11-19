package de.thm.mni.compilerbau.absyn;

import de.thm.mni.compilerbau.absyn.visitor.Visitor;
import de.thm.mni.compilerbau.table.Identifier;

public abstract class ListNode extends Node implements Iterable<Node>{
    //This is a linked list like implementation
    Node head;
    ListNode tail;
    public ListNode(Position position) {
        super(position);
        this.head = null;
        this.tail = null;
    }

    public ListNode(Position position, Node head, ListNode tail) {
        super(position);
        this.head = head;
        this.tail = tail;
    }

    @Override
    public String toString() {
        return formatAst("ListNode", head, tail);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
