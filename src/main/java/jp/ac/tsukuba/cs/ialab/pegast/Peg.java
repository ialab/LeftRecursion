package jp.ac.tsukuba.cs.ialab.pegast;

import java.util.HashMap;
import java.util.regex.Pattern;

public abstract class Peg extends AST {
    protected Peg(String label, Peg...children) {
        super(label, children);
    }
    abstract public <T> T accept(PegVisitor<T> visitor);
    public interface PegVisitor<T> {
        T visit(Sequence s);
        T visit(Choice ch);
        T visit(Constant c);
        T visit(RegExp re);
        T visit(NonTerminal nt);
        T visit(Peg.Repeat r);
        T visit(Peg.NotPredicate n);
        // T visit(Peg peg);
    }
    @Override
    public String toString() { return accept(new PegToString()); }

    public static NotPredicate not(Peg child) {
	    return new NotPredicate(child);
	}
	public static Repeat repeat(Peg child) {
	    return new Repeat(child);
	}
	public static class Sequence extends Peg {
        private Sequence(Peg...children) {
            super("Sequence", children);
        }
        @Override
        public <T> T accept(PegVisitor<T> visitor) {
            return visitor.visit(this);
        }    
    }
    public static Sequence seq(Peg...children) { return new Sequence(children); }

    public static class Choice extends Peg {
        private Choice(Peg...children) {
            super("Choice", children);
        }
        @Override
        public <T> T accept(PegVisitor<T> visitor) {
            return visitor.visit(this);
        }    
   }
    public static Choice choice(Peg...children) { return new Choice(children); }

    public static class Constant extends Peg {
        final private String string;
        private Constant(String string) {
            super("Constant:\"" + string + "\"");
            this.string = string;
        }
        public String string() { return string; }
        @Override
        public <T> T accept(PegVisitor<T> visitor) {
            return visitor.visit(this);
        }    
    }
    public static Constant constant(String s) { return new Constant(s); }

    public static class RegExp extends Peg {
        final private Pattern pattern;
        private RegExp(String re) {
            super("RegExp:/" + re + "/");
            pattern = Pattern.compile(re);
        }
        public Pattern pattern() { return pattern; }
        @Override
        public <T> T accept(PegVisitor<T> visitor) {
            return visitor.visit(this);
        }    
    }
    public static RegExp regExp(String re) { return new RegExp(re); }

    public static class NonTerminal extends Peg {
        final private String name;
        private NonTerminal(String name) {
            super("NT:" + name);
            this.name = name;
        }
        public String name() { return name; }
        @Override
        public int hashCode() {
            return name.hashCode();
        }
        // 同じ名前のNonTerminalを重複して作らないようにする。
        // NonTerminal nt1, nt2を比較するには nt1 == nt2 ですむ。
        final private static HashMap<String,NonTerminal> table = new HashMap<String,NonTerminal>();
        public static NonTerminal intern(String name) {
            if (table.containsKey(name)) {
                return table.get(name);
            } else {
                NonTerminal newNT = new NonTerminal(name);
                table.put(name, newNT);
                return newNT;
            }
        }
        @Override
        public <T> T accept(PegVisitor<T> visitor) {
            return visitor.visit(this);
        }    
    }
    public static class Repeat extends Peg {
	    Repeat(Peg...children) {
	        super("Repeat", children);
	    }
	    @Override
	    public <T> T accept(PegVisitor<T> visitor) {
	        return ((Peg.PegVisitor<T>)visitor).visit(this);
	    }
	}
	public static class NotPredicate extends Peg {
	    NotPredicate(Peg...children) {
	        super("NotPredicate", children);
	    }
	    @Override
	    public <T> T accept(PegVisitor<T> visitor) {
	        return ((Peg.PegVisitor<T>)visitor).visit(this);
	    }
	}
	public static NonTerminal nt(String name) {return NonTerminal.intern(name); }    
}