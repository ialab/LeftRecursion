package jp.ac.tsukuba.cs.ialab.pegast;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Memo<T> {
    private Map<Peg.NonTerminal,Map<Integer,T>> memo;
    public Memo() {
        memo = new HashMap<Peg.NonTerminal,Map<Integer,T>>();
    }
    public T probe(Peg.NonTerminal nt, int pos) {
        if (memo.containsKey(nt)) {
            return memo.get(nt).get(pos);
        } else {
            return null;
        }
    }
    public void store(Peg.NonTerminal nt, int pos, T value) {
        Map<Integer,T> map = memo.get(nt);
        if (map == null) {
            memo.put(nt, map = new HashMap<Integer,T>());
        }
        map.put(pos, value);
    }
    public Set<Peg.NonTerminal> nonTerminals() { return memo.keySet(); }
    public Set<Integer> positions(Peg.NonTerminal key) { return memo.get(key).keySet(); }
}