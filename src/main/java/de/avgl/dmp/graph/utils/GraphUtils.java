package de.avgl.dmp.graph.utils;

import de.avgl.dmp.graph.model.GraphStatics;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.avgl.dmp.graph.DMPGraphException;
import de.avgl.dmp.graph.NodeType;

/**
 * @author tgaengler
 */
public class GraphUtils {

	private static final Logger	LOG	= LoggerFactory.getLogger(GraphUtils.class);

	public static NodeType determineNodeType(final Node node) throws DMPGraphException {

		final String nodeTypeString = (String) node.getProperty(GraphStatics.NODETYPE_PROPERTY, null);

		if (nodeTypeString == null) {

			final String message = "node type string can't never be null (node id = '" + node.getId() + "')";

			GraphUtils.LOG.error(message);

			throw new DMPGraphException(message);
		}

		final NodeType nodeType = NodeType.getByName(nodeTypeString);

		if (nodeType == null) {

			final String message = "node type can't never be null (node id = '" + node.getId() + "')";

			GraphUtils.LOG.error(message);

			throw new DMPGraphException(message);
		}

		return nodeType;
	}
}