package com.intellij.refactoring.suggested;
public class SignatureChangePresentationModel {
    public enum Effect { None, Added, Removed, Modified, Moved }
    public static class TextFragment {
        public final String text;
        public final Effect effect;
        public TextFragment() { this(null, Effect.None); }
        public TextFragment(String text, Effect effect) { this.text = text; this.effect = effect; }
        public static class Leaf extends TextFragment {
            public Leaf() {}
            public Leaf(String text) { super(text, Effect.None); }
            public Leaf(String text, Effect effect) { super(text, effect); }
        }
        public static class Group extends TextFragment {
            public Group() {}
        }
    }
}
