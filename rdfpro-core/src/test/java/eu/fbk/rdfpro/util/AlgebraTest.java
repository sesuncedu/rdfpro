package eu.fbk.rdfpro.util;

import java.util.Arrays;

import org.junit.Test;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.impl.ListBindingSet;

public class AlgebraTest {

    @Test
    public void test() throws Throwable {
        final ValueExpr expr = Algebra.parseValueExpr("CONCAT(STR(ks:mint(rdf:type)), ?x)", null,
                Namespaces.DEFAULT.uriMap());
        System.out.println(Algebra.evaluateValueExpr(expr, new ListBindingSet(Arrays.asList("x"),
                Statements.VALUE_FACTORY.createLiteral("__test"))));
    }

}
