package jp.ac.tsukuba.cs.ialab.pegast;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Grammar {
    final private Map<Peg.NonTerminal,Peg> rules;
    private Peg.NonTerminal start;
    public Grammar() {
        rules = new HashMap<Peg.NonTerminal,Peg>();
    }
    public Grammar addRule(Peg.NonTerminal nt, Peg body) {
        rules.put(nt, body);
        return this;
    }
    public Set<Peg.NonTerminal> nonTerminals() { return rules.keySet(); }
    public Peg getBody(Peg.NonTerminal nt) { return rules.get(nt); }
    public Grammar setStart(Peg.NonTerminal nt) { start = nt; return this; }
    public Peg.NonTerminal start() { return start; }
}