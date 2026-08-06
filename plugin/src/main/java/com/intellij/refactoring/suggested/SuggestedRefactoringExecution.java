package com.intellij.refactoring.suggested;
import jakarta.annotation.Nonnull;
/** IntelliJ-compat stub — part of the suggested refactoring framework. */
public abstract class SuggestedRefactoringExecution {
    protected final SuggestedRefactoringSupport refactoringSupport;
    public SuggestedRefactoringExecution(@Nonnull SuggestedRefactoringSupport support) {
        this.refactoringSupport = support;
    }
    public Object prepareChangeSignature(@Nonnull SuggestedChangeSignatureData data) { return null; }
    public void performChangeSignature(@Nonnull SuggestedChangeSignatureData data, java.util.List<NewParameterValue> newParameterValues, Object preparedData) {}

    public interface NewParameterValue {
        class None implements NewParameterValue {}
        class AnyValue implements NewParameterValue {}
        class Expression implements NewParameterValue {
            public Object expression;
            public Expression() {}
            public Expression(Object expression) { this.expression = expression; }
            public Object getExpression() { return expression; }
        }
    }
}
