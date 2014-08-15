package org.dswarm.graph.gdm.read;

import java.util.HashMap;
import java.util.Map;

import org.dswarm.graph.DMPGraphException;
import org.dswarm.graph.NodeType;
import org.dswarm.graph.json.LiteralNode;
import org.dswarm.graph.json.ResourceNode;
import org.dswarm.graph.model.GraphStatics;
import org.dswarm.graph.utils.GraphUtils;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tgaengler
 */
public class PropertyGraphGDMReader {

	private static final Logger					LOG				= LoggerFactory.getLogger(PropertyGraphGDMReader.class);

	final Map<Long, org.dswarm.graph.json.Node>	bnodes			= new HashMap<>();
	final Map<String, ResourceNode>                resourceNodes     = new HashMap<>();

	public org.dswarm.graph.json.Node readSubject(final Node subjectNode) throws DMPGraphException {

		final long subjectId = subjectNode.getId();
		final NodeType subjectNodeType = GraphUtils.determineNodeType(subjectNode);

		final org.dswarm.graph.json.Node subjectGDMNode;

		switch (subjectNodeType) {

			case Resource:
			case TypeResource:

				subjectGDMNode = readResource(subjectNode);

				break;
			case BNode:
			case TypeBNode:

				subjectGDMNode = createResourceFromBNode(subjectId);

				break;
			default:

				final String message = "subject node type can only be a resource (or type resource) or bnode (or type bnode)";

				PropertyGraphGDMReader.LOG.error(message);

				throw new DMPGraphException(message);
		}

		return subjectGDMNode;
	}

	public org.dswarm.graph.json.Node readObject(final Node objectNode) throws DMPGraphException {

		final long objectId = objectNode.getId();
		final NodeType objectNodeType = GraphUtils.determineNodeType(objectNode);

		final org.dswarm.graph.json.Node objectGDMNode;

		switch (objectNodeType) {

			case Resource:
			case TypeResource:

				objectGDMNode = readResource(objectNode);

				break;
			case BNode:
			case TypeBNode:

				objectGDMNode = createResourceFromBNode(objectId);

				break;
			case Literal:

				final Node endNode = objectNode;
				final String object = (String) endNode.getProperty(GraphStatics.VALUE_PROPERTY, null);

				if (object == null) {

					final String message = "object value can't be null";

					PropertyGraphGDMReader.LOG.error(message);

					throw new DMPGraphException(message);
				}

				objectGDMNode = new LiteralNode(objectId, object);

				break;
			default:

				final String message = "unknown node type " + objectNodeType.getName() + " for object node";

				PropertyGraphGDMReader.LOG.error(message);

				throw new DMPGraphException(message);
		}

		return objectGDMNode;
	}

	private ResourceNode readResource(final Node node) throws DMPGraphException {

		final String resourceURI = (String) node.getProperty(GraphStatics.URI_PROPERTY, null);

		if (resourceURI == null) {

			final String message = "resource URI can't be null";

			PropertyGraphGDMReader.LOG.error(message);

			throw new DMPGraphException(message);
		}

		final String provenanceURI = (String) node.getProperty(GraphStatics.PROVENANCE_PROPERTY, null);

		final ResourceNode resourceNode;

		if (provenanceURI == null) {

			resourceNode = createResourceFromURI(node.getId(), resourceURI);
		} else {

			resourceNode = createResourceFromURIAndProvenance(node.getId(), resourceURI, provenanceURI);
		}

		return resourceNode;
	}

	private org.dswarm.graph.json.Node createResourceFromBNode(final long bnodeId) {

		if (!bnodes.containsKey(Long.valueOf(bnodeId))) {

			bnodes.put(Long.valueOf(bnodeId), new org.dswarm.graph.json.Node(bnodeId));
		}

		return bnodes.get(Long.valueOf(bnodeId));
	}

	private ResourceNode createResourceFromURI(final long id, final String uri) {

		if (!resourceNodes.containsKey(uri)) {

			resourceNodes.put(uri, new ResourceNode(id, uri));
		}

		return resourceNodes.get(uri);
	}

	private ResourceNode createResourceFromURIAndProvenance(final long id, final String uri, final String provenance) {

		if (!resourceNodes.containsKey(uri + provenance)) {

			resourceNodes.put(uri + provenance, new ResourceNode(id, uri, provenance));
		}

		return resourceNodes.get(uri + provenance);
	}



}
