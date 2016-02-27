package com.googlecode.lanterna.terminal.swing;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.screen.TabBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by Martin on 2016-02-21.
 */
class VirtualTerminal2 {
    private final TextBuffer2 regularTextBuffer;
    private final TextBuffer2 privateModeTextBuffer;
    private final TreeSet<TerminalPosition> dirtyTerminalCells;

    private TextBuffer2 currentTextBuffer;
    private TerminalSize terminalSize;

    // Position is stored in "global coordinates", where 0x0 is the top-left corner of the scrollback buffer
    private TerminalPosition cursorPosition;

    VirtualTerminal2(TerminalSize initialTerminalSize) {
        this.regularTextBuffer = new TextBuffer2();
        this.privateModeTextBuffer = new TextBuffer2();
        this.dirtyTerminalCells = new TreeSet<TerminalPosition>();

        // Start with regular mode
        this.currentTextBuffer = regularTextBuffer;
        this.terminalSize = initialTerminalSize;
        this.cursorPosition = TerminalPosition.TOP_LEFT_CORNER;
    }

    synchronized TerminalSize getTerminalSize() {
        return terminalSize;
    }

    synchronized void setTerminalSize(TerminalSize newSize) {
        // TODO: do we need to do this in the new implementation?
        //if(terminalSize.getRows() < newSize.getRows()) {
        //    cursorPosition = cursorPosition.withRelativeRow(newSize.getRows() - size.getRows());
        //}
        this.terminalSize = newSize;
        correctCursor();
    }

    synchronized void putCharacter(TextCharacter terminalCharacter) {
        if(terminalCharacter.getCharacter() == '\n') {
            moveCursorToNextLine();
        }
        else if(terminalCharacter.getCharacter() == '\t') {
            int nrOfSpaces = TabBehaviour.ALIGN_TO_COLUMN_4.getTabReplacement(cursorPosition.getColumn()).length();
            for(int i = 0; i < nrOfSpaces && cursorPosition.getColumn() < terminalSize.getColumns() - 1; i++) {
                putCharacter(terminalCharacter.withCharacter(' '));
            }
        }
        else {
            currentTextBuffer.setCharacter(cursorPosition.getRow(), cursorPosition.getColumn(), terminalCharacter);

            //Advance cursor
            cursorPosition = cursorPosition.withRelativeColumn(TerminalTextUtils.isCharCJK(terminalCharacter.getCharacter()) ? 2 : 1);
            if(cursorPosition.getColumn() >= terminalSize.getColumns()) {
                moveCursorToNextLine();
            }
            // TODO: ensure there is enough lines here!
        }
    }

    synchronized TerminalPosition getCursorPosition() {
        if(terminalSize.getRows() >= currentTextBuffer.getLineCount()) {
            return cursorPosition;
        }
        return cursorPosition.withRelativeRow(-(currentTextBuffer.getLineCount() - terminalSize.getRows()));
    }

    synchronized TerminalPosition getTranslatedCursorPosition() {
        //TODO: Implementation
        return TerminalPosition.TOP_LEFT_CORNER;
    }

    synchronized void switchToPrivateMode() {
        //TODO: Implementation
        currentTextBuffer = privateModeTextBuffer;
    }

    synchronized void switchToNormalMode() {
        //TODO: Implementation
        currentTextBuffer = regularTextBuffer;
    }

    synchronized void clear() {
        //TODO: Implementation
        currentTextBuffer.clear();
    }

    synchronized void setCursorPosition(TerminalPosition cursorPosition) {
        this.cursorPosition = translateCursorSpaceToGlobalSpace(cursorPosition);
    }

    synchronized void setCursorAndPutCharacter(TerminalPosition terminalPosition, TextCharacter textCharacter) {
        //TODO: Implementation
    }

    synchronized TextCharacter getCharacter(TerminalPosition position) {
        TerminalPosition globalPosition = translateCursorSpaceToGlobalSpace(position);
        return currentTextBuffer.getCharacter(globalPosition.getRow(), globalPosition.getColumn());
    }

    synchronized List<TextCharacter>[] getLines() {
        //TODO: Implementation
        return new ArrayList[0];
    }

    private TerminalPosition translateCursorSpaceToGlobalSpace(TerminalPosition cursorSpacePosition) {
        if(terminalSize.getRows() >= currentTextBuffer.getLineCount()) {
            return cursorSpacePosition;
        }
        return cursorSpacePosition.withRelativeColumn(currentTextBuffer.getLineCount() - terminalSize.getRows());
    }

    private void correctCursor() {
        this.cursorPosition =
                new TerminalPosition(
                        Math.max(cursorPosition.getColumn(), 0),
                        Math.max(cursorPosition.getRow(), 0));
        this.cursorPosition = cursorPosition.withColumn(Math.min(cursorPosition.getColumn(), terminalSize.getColumns() - 1));
    }

    private void moveCursorToNextLine() {
        cursorPosition = cursorPosition.withColumn(0).withRelativeRow(1);
        if(cursorPosition.getRow() >= terminalSize.getRows()) {
            cursorPosition = cursorPosition.withRelativeRow(-1);
            if(currentTextBuffer == regularTextBuffer) {
                currentTextBuffer.newLine();
            }
        }
    }
}