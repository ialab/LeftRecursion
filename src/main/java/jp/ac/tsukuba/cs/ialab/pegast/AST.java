package jp.ac.tsukuba.cs.ialab.pegast;

import java.util.Arrays;
import java.util.List;

// 非常に単純なASTの実装。葉と中間ノードを区別せず、すべてのノードがchildrenリストを持つ。
// 葉は、単に子供の数が0個のノード。

public class AST {
    final private String label; // このノードのラベル
    final private List<AST> children; // 子供のASTのリスト
    final public static AST FAIL = new AST("FAIL");

    protected AST(final String label, final List<AST> children) {
        this.label = label;
        this.children = children;
    }

    protected AST(final String label, final AST... childrenArray) { // 可変個引数
        this(label, Arrays.asList(childrenArray));
    }

    public static AST ast(final String label, final List<AST> children) {
        return new AST(label, children);
    }

    public static AST ast(final String label, final AST... childrenArray) {
        return new AST(label, childrenArray);
    }

    public String label() {
        return label;
    }

    public List<AST> children() {
        return children;
    }

    public void addChild(final AST child) {
        children.add(child);
    }

    // 文字列表現
    // (ラベル 子1 子2 ...) とS式風に表す
    public String toString() {
        final StringBuilder builder = new StringBuilder("(" + label);
        for (final AST child : children()) {
            builder.append(" ");
            builder.append(child);
        }
        builder.append(")");
        return builder.toString();
    }

    private static class PPrintState {
        int currentColumn = 0;
        private void put(String s) {
            System.out.print(s);
            currentColumn += s.length();
        }
        private void flush() {
            System.out.println();
            currentColumn = 0;
        }
        private void indentTo(int targetColumn) {
            while (currentColumn < targetColumn) {
                put(" ");
            }
        }
        public int currentColumn() { return currentColumn; }
    }
    private void pprint(int targetColumn, PPrintState state) {
        state.indentTo(targetColumn);
        state.put("("); state.put(label);
        int nextIndent = state.currentColumn() + 1;
        boolean firstLine = true;
        for (AST c : children()) {
            if (! firstLine) {
                state.flush();
            }
            c.pprint(nextIndent, state);
            firstLine = false;
        }
        state.put(")");
    }
    public void prettyPrint() {
        PPrintState state = new PPrintState();
        pprint(0, state);
        state.flush();
    }
}