/*
 * RDFpro - An extensible tool for building stream-oriented RDF processing libraries.
 * 
 * Written in 2014 by Francesco Corcoglioniti <francesco.corcoglioniti@gmail.com> with support by
 * Marco Rospocher, Marco Amadori and Michele Mostarda.
 * 
 * To the extent possible under law, the author has dedicated all copyright and related and
 * neighboring rights to this software to the public domain worldwide. This software is
 * distributed without any warranty.
 * 
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package eu.fbk.rdfpro;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

/**
 * Test case for {@link eu.fbk.rdfpro.base.FilterProcessorOld}.
 *
 * @author Michele Mostarda (mostarda@fbk.eu)
 */
public class TransformProcessorTest {

    @Test
    public void testFilter() throws RDFHandlerException {

        // rdfpro @read dbpedia.abox.nt.gz @rdfs dbpedia.tbox.owl @transform '+p rdf:type
        // rdfs:label' @mapreduce -u -e '+o dbo:Company' 's' @transform '+p rdfs:label' @write
        // labels.nt.gz

        URI dboCompany = new URIImpl("http://dbpedia.org/ontology/Company");

        RDFSource aboxSource = RDFSources.read(true, true, null, null, "dbpedia.tbox.owl");
        RDFSource tboxSource = RDFSources.read(true, true, null, null, "dbpedia.abox.nt.gz");

        RDFHandler labelsSink = RDFHandlers.write(null, 0, "labels.nt.gz");

        final RDFProcessor processor = RDFProcessors.sequence( //
                RDFProcessors.rdfs(aboxSource, null, false, false), //
                RDFProcessors.transform(Transformer.filter((final Statement s) -> {
                    final URI p = s.getPredicate();
                    return p.equals(RDF.TYPE) || p.equals(RDFS.LABEL);
                })), //
                RDFProcessors.mapReduce(Mapper.select("s"), Reducer.filter(Reducer.IDENTITY, (
                        final Statement s) -> s.getObject().equals(dboCompany), null), true));

        processor.apply(tboxSource, labelsSink, 1);

        // Create a filter which matches any subject and object with predicates of statements
        // contained into the file.nt.
        final RDFProcessor filter = RDFProcessors.parse(true, "@filter 'sou +[./file.nt]p -*'");
        Assert.assertEquals(0, filter.getExtraPasses());
        final int[] flags = new int[2];
        final List<Statement> statements = new ArrayList<>();
        final RDFHandler handler = filter.wrap(new RDFHandler() {

            @Override
            public void startRDF() throws RDFHandlerException {
                flags[0]++;
            }

            @Override
            public void endRDF() throws RDFHandlerException {
                flags[1]++;
            }

            @Override
            public void handleNamespace(final String pref, final String name)
                    throws RDFHandlerException {
                throw new IllegalStateException();
            }

            @Override
            public void handleStatement(final Statement s) throws RDFHandlerException {
                statements.add(s);
            }

            @Override
            public void handleComment(final String comment) throws RDFHandlerException {
                throw new IllegalStateException();
            }
        });

        final ValueFactory factory = new ValueFactoryImpl();
        handler.startRDF();
        handler.handleStatement(factory.createStatement(factory.createURI("http://sx"),
                factory.createURI("http://p1"), factory.createURI("http://sx")));
        handler.handleStatement(factory.createStatement(factory.createURI("http://sF"),
                factory.createURI("http://pF"), factory.createURI("http://sF")));
        handler.handleStatement(factory.createStatement(factory.createURI("http://sy"),
                factory.createURI("http://p2"), factory.createURI("http://sy")));
        handler.endRDF();

        Assert.assertEquals(1, flags[0]);
        Assert.assertEquals(1, flags[1]);
        Assert.assertEquals(2, statements.size());
        Assert.assertEquals("(http://sx, http://p1, http://sx)", statements.get(0).toString());
        Assert.assertEquals("(http://sy, http://p2, http://sy)", statements.get(1).toString());
    }
}
