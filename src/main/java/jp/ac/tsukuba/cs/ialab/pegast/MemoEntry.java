package jp.ac.tsukuba.cs.ialab.pegast;

public class MemoEntry {
    private int pos;
    private AST ans;
    public MemoEntry(AST ans, int pos) {
        this.pos = pos; this.ans = ans;
    }
    public int pos() { return pos; }
    public void setPos(int pos) { this.pos = pos; }
    public AST ans() { return ans; }
    public void setAns(AST ans) { this.ans = ans; }
}