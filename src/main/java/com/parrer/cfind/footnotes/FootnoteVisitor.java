package com.parrer.cfind.footnotes;

public interface FootnoteVisitor {
    void visit(FootnoteBlock node);

    void visit(Footnote node);
}
