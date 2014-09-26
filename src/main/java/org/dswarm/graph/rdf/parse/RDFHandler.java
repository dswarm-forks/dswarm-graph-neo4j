package org.dswarm.graph.rdf.parse;

import org.dswarm.graph.DMPGraphException;
import org.dswarm.graph.parse.Handler;

import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author tgaengler
 */
public interface RDFHandler extends Handler {

	public void handleStatement(final Statement st) throws DMPGraphException;
}
