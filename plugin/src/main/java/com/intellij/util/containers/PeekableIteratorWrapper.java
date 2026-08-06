package com.intellij.util.containers;

import consulo.util.collection.PeekableIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PeekableIteratorWrapper<T> implements PeekableIterator<T> {
    private final Iterator<? extends T> myIterator;
    private T myValue;
    private boolean myValidValue;

    public PeekableIteratorWrapper(Iterator<? extends T> iterator) {
        myIterator = iterator;
        advance();
    }

    private void advance() {
        if (myIterator.hasNext()) {
            myValue = myIterator.next();
            myValidValue = true;
        } else {
            myValue = null;
            myValidValue = false;
        }
    }

    @Override
    public boolean hasNext() {
        return myValidValue;
    }

    @Override
    public T peek() {
        if (!myValidValue) throw new NoSuchElementException();
        return myValue;
    }

    @Override
    public T next() {
        T result = peek();
        advance();
        return result;
    }
}
