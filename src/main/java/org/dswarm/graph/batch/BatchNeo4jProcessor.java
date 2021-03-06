/**
 * This file is part of d:swarm graph extension.
 *
 * d:swarm graph extension is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * d:swarm graph extension is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with d:swarm graph extension.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dswarm.graph.batch;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.carrotsearch.hppc.LongLongOpenHashMap;
import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import com.google.common.io.Resources;
import org.mapdb.DB;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dswarm.common.types.Tuple;
import org.dswarm.graph.DMPGraphException;
import org.dswarm.graph.GraphIndexStatics;
import org.dswarm.graph.Neo4jProcessor;
import org.dswarm.graph.NodeType;
import org.dswarm.graph.hash.HashUtils;
import org.dswarm.graph.index.MapDBUtils;
import org.dswarm.graph.model.GraphStatics;
import org.dswarm.graph.model.Statement;

/**
 * @author tgaengler
 */
public abstract class BatchNeo4jProcessor implements Neo4jProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(BatchNeo4jProcessor.class);

	protected int addedLabels = 0;

	protected final BatchInserter inserter;
	private BatchInserterIndex resources;
	private BatchInserterIndex resourcesWDataModel;
	private BatchInserterIndex resourceTypes;
	private BatchInserterIndex statementUUIDs;

	private BatchInserterIndexProvider resourcesProvider;
	private BatchInserterIndexProvider resourcesWDataModelProvider;
	private BatchInserterIndexProvider resourceTypesProvider;
	private BatchInserterIndexProvider statementUUIDsProvider;

	protected final ObjectLongOpenHashMap<String> tempResourcesIndex;
	protected final ObjectLongOpenHashMap<String> tempResourcesWDataModelIndex;
	protected final ObjectLongOpenHashMap<String> tempResourceTypes;

	private BatchInserterIndex values;

	private BatchInserterIndexProvider valuesProvider;

	protected final ObjectLongOpenHashMap<String> bnodes;
	// private BatchInserterIndex statementHashes;
	private Set<Long> statementHashes;
	private DB statementHashesDB;

	protected final Set<Long> tempStatementHashes;
	protected final DB tempStatementHashesDB;
	//protected final LongLongOpenHashMap tempStatementHashes;

	protected final LongLongOpenHashMap nodeResourceMap;

	public BatchNeo4jProcessor(final BatchInserter inserter) throws DMPGraphException {

		this.inserter = inserter;

		BatchNeo4jProcessor.LOG.debug("start writing");

		bnodes = new ObjectLongOpenHashMap<>();
		nodeResourceMap = new LongLongOpenHashMap();

		tempResourcesIndex = new ObjectLongOpenHashMap<>();
		tempResourcesWDataModelIndex = new ObjectLongOpenHashMap<>();
		tempResourceTypes = new ObjectLongOpenHashMap<>();
		//tempStatementHashes = new LongLongOpenHashMap();
		final Tuple<Set<Long>, DB> mapDBTuple = MapDBUtils
				.createOrGetInMemoryLongIndexTreeSetNonTransactional(GraphIndexStatics.TEMP_STATEMENT_HASHES_INDEX_NAME);
		tempStatementHashes = mapDBTuple.v1();
		tempStatementHashesDB = mapDBTuple.v2();

		// TODO: init all indices, when batch inserter should work on a pre-filled database (otherwise, the existing index would
		// utilised in the first run)
		// initIndices();
		initValueIndex();
		initStatementIndex();
	}

	protected void pumpNFlushNClearIndices() {

		BatchNeo4jProcessor.LOG.debug("start pumping indices");

		copyNFlushNClearIndex(tempResourcesIndex, resources, GraphStatics.URI, GraphIndexStatics.RESOURCES_INDEX_NAME);
		copyNFlushNClearIndex(tempResourcesWDataModelIndex, resourcesWDataModel, GraphStatics.URI_W_DATA_MODEL,
				GraphIndexStatics.RESOURCES_W_DATA_MODEL_INDEX_NAME);
		copyNFlushNClearIndex(tempResourceTypes, resourceTypes, GraphStatics.URI, GraphIndexStatics.RESOURCE_TYPES_INDEX_NAME);
		copyNFlushNClearLongIndex(tempStatementHashes, tempStatementHashesDB, statementHashes, GraphIndexStatics.STATEMENT_HASHES_INDEX_NAME,
				statementHashesDB);

		BatchNeo4jProcessor.LOG.debug("finished pumping indices");

		BatchNeo4jProcessor.LOG.debug("start shutting down indices");

		resourcesProvider.shutdown();
		resourcesWDataModelProvider.shutdown();
		resourceTypesProvider.shutdown();
		valuesProvider.shutdown();

		BatchNeo4jProcessor.LOG.debug("finished shutting down indices");
	}

	private void copyNFlushNClearIndex(final ObjectLongOpenHashMap<String> tempIndex, final BatchInserterIndex neo4jIndex,
	                                   final String indexProperty, final String indexName) {

		BatchNeo4jProcessor.LOG.debug("start pumping '{}' index of size '{}'", indexName, tempIndex.size());

		final Object[] keys = tempIndex.keys;
		final long[] values = tempIndex.values;
		final boolean[] states = tempIndex.allocated;

		BatchNeo4jProcessor.LOG.debug("keys size = '{}' :: values size = '{}' :: states size = '{}'", keys.length, values.length, states.length);

		int j = 0;
		long tick = System.currentTimeMillis();
		int sinceLast = 0;

		for (int i = 0; i < states.length; i++) {

			if (states[i]) {

				neo4jIndex.add(values[i], MapUtil.map(indexProperty, keys[i].toString()));

				j++;

				final int entryDelta = j - sinceLast;
				final long timeDelta = (System.currentTimeMillis() - tick) / 1000;

				if (entryDelta >= 1000000 || timeDelta >= 60) {

					sinceLast = j;
					final double duration = (double) entryDelta / timeDelta;

					BatchNeo4jProcessor.LOG.debug("wrote '{}' entries @ ~{} entries/second.", j, duration);

					tick = System.currentTimeMillis();
				}
			}
		}

		BatchNeo4jProcessor.LOG.debug("finished pumping '{}' index; wrote '{}' entries", indexName, j);

		BatchNeo4jProcessor.LOG.debug("start flushing and clearing index");

		neo4jIndex.flush();
		tempIndex.clear();

		BatchNeo4jProcessor.LOG.debug("finished flushing and clearing index");
	}

	private void copyNFlushNClearLongIndex(final Set<Long> tempIndex, final DB inMemoryDB, final Set<Long> persistentIndex, final String indexName,
	                                       final DB persistentDB) {

		BatchNeo4jProcessor.LOG.debug("start pumping '{}' index of size '{}'", indexName, tempIndex.size());

		int j = 0;
		long tick = System.currentTimeMillis();
		int sinceLast = 0;

		for (final Long key : tempIndex) {

			persistentIndex.add(key);

			j++;

			final int entryDelta = j - sinceLast;
			final long timeDelta = (System.currentTimeMillis() - tick) / 1000;

			if (entryDelta >= 1000000 || timeDelta >= 60) {

				sinceLast = j;
				final double duration = (double) entryDelta / timeDelta;

				BatchNeo4jProcessor.LOG.debug("wrote '{}' entries @ ~{} entries/second.", j, duration);

				tick = System.currentTimeMillis();
			}
		}

		BatchNeo4jProcessor.LOG.debug("finished pumping index '{}' index; wrote '{}' entries", indexName, tempIndex.size());

		BatchNeo4jProcessor.LOG.debug("start flushing and clearing index");

		//persistentIndex.flush();
		tempIndex.clear();
		inMemoryDB.commit();
		inMemoryDB.close();
		persistentDB.commit();
		persistentDB.close();

		BatchNeo4jProcessor.LOG.debug("finished flushing and clearing index");
	}

	protected void initValueIndex() throws DMPGraphException {

		try {

			final Tuple<BatchInserterIndex, BatchInserterIndexProvider> valuesIndexTuple = getOrCreateIndex(GraphIndexStatics.VALUES_INDEX_NAME,
					GraphStatics.VALUE, true, 1);
			values = valuesIndexTuple.v1();
			valuesProvider = valuesIndexTuple.v2();
		} catch (final Exception e) {

			final String message = "couldn't load indices successfully";

			BatchNeo4jProcessor.LOG.error(message, e);
			BatchNeo4jProcessor.LOG.debug("couldn't finish writing successfully");

			throw new DMPGraphException(message);
		}
	}

	private void initStatementIndex() throws DMPGraphException {

		try {

			final Tuple<BatchInserterIndex, BatchInserterIndexProvider> statementUUIDsIndexTuple = getOrCreateIndex(GraphIndexStatics.STATEMENT_UUIDS_INDEX_NAME, GraphStatics.UUID, false, 1);
			statementUUIDs = statementUUIDsIndexTuple.v1();
			statementUUIDsProvider = statementUUIDsIndexTuple.v2();
		} catch (final Exception e) {

			final String message = "couldn't load indices successfully";

			BatchNeo4jProcessor.LOG.error(message, e);
			BatchNeo4jProcessor.LOG.debug("couldn't finish write TX successfully");

			throw new DMPGraphException(message);
		}
	}

	protected void initIndices() throws DMPGraphException {

		try {

			final Tuple<BatchInserterIndex, BatchInserterIndexProvider> resourcesIndexTuple = getOrCreateIndex(GraphIndexStatics.RESOURCES_INDEX_NAME,
					GraphStatics.URI, true, 1);
			resources = resourcesIndexTuple.v1();
			resourcesProvider = resourcesIndexTuple.v2();

			final Tuple<BatchInserterIndex, BatchInserterIndexProvider> resourcesWDataModelIndexTuple = getOrCreateIndex(
					GraphIndexStatics.RESOURCES_W_DATA_MODEL_INDEX_NAME, GraphStatics.URI_W_DATA_MODEL, true, 1);
			resourcesWDataModel = resourcesWDataModelIndexTuple.v1();
			resourcesWDataModelProvider = resourcesWDataModelIndexTuple.v2();

			final Tuple<BatchInserterIndex, BatchInserterIndexProvider> resourceTypesIndexTuple = getOrCreateIndex(
					GraphIndexStatics.RESOURCE_TYPES_INDEX_NAME, GraphStatics.URI, true, 1);
			resourceTypes = resourceTypesIndexTuple.v1();
			resourceTypesProvider = resourceTypesIndexTuple.v2();
			// statementHashes = getOrCreateIndex(GraphIndexStatics.STATEMENT_HASHES_INDEX_NAME, GraphStatics.HASH, false,
			// 1000000);
		} catch (final Exception e) {

			final String message = "couldn't load indices successfully";

			BatchNeo4jProcessor.LOG.error(message, e);
			BatchNeo4jProcessor.LOG.debug("couldn't load indices successfully");

			throw new DMPGraphException(message);
		}

		try {

			final Tuple<Set<Long>, DB> mapDBTuple = getOrCreateLongIndex(GraphIndexStatics.STATEMENT_HASHES_INDEX_NAME);
			statementHashes = mapDBTuple.v1();
			statementHashesDB = mapDBTuple.v2();
		} catch (final IOException e) {

			throw new DMPGraphException("couldn't create or get statement hashes index");
		}
	}

	public BatchInserter getBatchInserter() {

		return inserter;
	}

	public void addToResourcesIndex(final String key, final long nodeId) {

		tempResourcesIndex.put(key, nodeId);
	}

	public Optional<Long> getNodeIdFromResourcesIndex(final String key) {

		return getIdFromIndex(key, tempResourcesIndex, resources, GraphStatics.URI);
	}

	public void addToResourcesWDataModelIndex(final String key, final long nodeId) {

		tempResourcesWDataModelIndex.put(key, nodeId);
	}

	public Optional<Long> getNodeIdFromResourcesWDataModelIndex(final String key) {

		return getIdFromIndex(key, tempResourcesWDataModelIndex, resourcesWDataModel, GraphStatics.URI_W_DATA_MODEL);
	}

	public void addToBNodesIndex(final String key, final long nodeId) {

		bnodes.put(key, nodeId);
	}

	public void addStatementToIndex(final long relId, final String statementUUID) {

		statementUUIDs.add(relId, MapUtil.map(GraphStatics.UUID, statementUUID));
	}

	public Optional<Long> getNodeIdFromBNodesIndex(final String key) {

		if (key == null) {

			return Optional.empty();
		}

		if (bnodes.containsKey(key)) {

			return Optional.of(bnodes.lget());
		}

		return Optional.empty();
	}

	public void addToResourceTypesIndex(final String key, final long nodeId) {

		tempResourceTypes.put(key, nodeId);
	}

	public Optional<Long> getNodeIdFromResourceTypesIndex(final String key) {

		return getIdFromIndex(key, tempResourceTypes, resourceTypes, GraphStatics.URI);
	}

	public void addToValueIndex(final String key, final long nodeId) {

		values.add(nodeId, MapUtil.map(GraphStatics.VALUE, key));
	}

	public void addToStatementIndex(final long key) {

		tempStatementHashes.add(key);
	}

	public void flushIndices() throws DMPGraphException {

		BatchNeo4jProcessor.LOG.debug("start flushing indices");

		if (resources == null) {

			initIndices();
		}

		pumpNFlushNClearIndices();
		flushStatementIndices();
		clearTempIndices();

		BatchNeo4jProcessor.LOG.debug("finished flushing indices");
	}

	public void flushStatementIndices() {

		statementUUIDs.flush();
		statementUUIDsProvider.shutdown();
	}

	protected void clearTempIndices() {

		clearTempStatementIndices();
	}

	protected void clearTempStatementIndices() {

		if (!tempStatementHashesDB.isClosed()) {

			tempStatementHashes.clear();
		}
	}

	public void clearMaps() {

		nodeResourceMap.clear();
		bnodes.clear();
	}

	public Optional<Long> determineNode(final Optional<NodeType> optionalResourceNodeType, final Optional<String> optionalResourceId,
	                                    final Optional<String> optionalResourceURI, final Optional<String> optionalDataModelURI) {

		if (!optionalResourceNodeType.isPresent()) {

			return Optional.empty();
		}

		if (NodeType.Resource == optionalResourceNodeType.get() || NodeType.TypeResource == optionalResourceNodeType.get()) {

			// resource node

			final Optional<Long> optionalNodeId;

			if (NodeType.TypeResource != optionalResourceNodeType.get()) {

				if (!optionalDataModelURI.isPresent()) {

					optionalNodeId = getResourceNodeHits(optionalResourceURI.get());
				} else {

					optionalNodeId = getNodeIdFromResourcesWDataModelIndex(optionalResourceURI.get() + optionalDataModelURI.get());
				}
			} else {

				optionalNodeId = getNodeIdFromResourceTypesIndex(optionalResourceURI.get());
			}

			return optionalNodeId;
		}

		if (NodeType.Literal == optionalResourceNodeType.get()) {

			// literal node - should never be the case

			return Optional.empty();
		}

		// resource must be a blank node

		return getNodeIdFromBNodesIndex(optionalResourceId.get());
	}

	public Optional<Long> determineResourceHash(final long subjectNodeId, final Optional<NodeType> optionalSubjectNodeType,
	                                            final Optional<Long> optionalSubjectHash, final Optional<Long> optionalResourceHash) {

		final Optional<Long> finalOptionalResourceHash;

		if (nodeResourceMap.containsKey(subjectNodeId)) {

			finalOptionalResourceHash = Optional.of(nodeResourceMap.lget());
		} else {

			finalOptionalResourceHash = determineResourceHash(optionalSubjectNodeType, optionalSubjectHash, optionalResourceHash);

			if (finalOptionalResourceHash.isPresent()) {

				nodeResourceMap.put(subjectNodeId, finalOptionalResourceHash.get());
			}
		}

		return finalOptionalResourceHash;
	}

	public Optional<Long> determineResourceHash(final Optional<NodeType> optionalSubjectNodeType, final Optional<Long> optionalSubjectHash,
	                                            final Optional<Long> optionalResourceHash) {

		final Optional<Long> finalOptionalResourceHash;

		if (optionalSubjectNodeType.isPresent()
				&& (NodeType.Resource == optionalSubjectNodeType.get() || NodeType.TypeResource == optionalSubjectNodeType.get())) {

			finalOptionalResourceHash = optionalSubjectHash;
		} else if (optionalResourceHash.isPresent()) {

			finalOptionalResourceHash = optionalResourceHash;
		} else {

			// shouldn't never be the case

			return Optional.empty();
		}

		return finalOptionalResourceHash;
	}

	public void addLabel(final long nodeId, final String labelString) {

		final Label label = DynamicLabel.label(labelString);

		inserter.setNodeLabels(nodeId, label);
	}

	public boolean checkStatementExists(final long hash) throws DMPGraphException {

		return checkLongIndex(hash, tempStatementHashes) || checkLongIndex(hash, statementHashes);
	}

	public Map<String, Object> prepareRelationship(final String statementUUID, final Optional<Map<String, Object>> optionalQualifiedAttributes) {

		final Map<String, Object> relProperties = new HashMap<>();

		relProperties.put(GraphStatics.UUID_PROPERTY, statementUUID);

		if (optionalQualifiedAttributes.isPresent()) {

			final Map<String, Object> qualifiedAttributes = optionalQualifiedAttributes.get();

			if (qualifiedAttributes.containsKey(GraphStatics.ORDER_PROPERTY)) {

				relProperties.put(GraphStatics.ORDER_PROPERTY, qualifiedAttributes.get(GraphStatics.ORDER_PROPERTY));
			}

			if (qualifiedAttributes.containsKey(GraphStatics.INDEX_PROPERTY)) {

				relProperties.put(GraphStatics.INDEX_PROPERTY, qualifiedAttributes.get(GraphStatics.INDEX_PROPERTY));
			}

			// TODO: versioning handling only implemented for data models right now

			if (qualifiedAttributes.containsKey(GraphStatics.EVIDENCE_PROPERTY)) {

				relProperties.put(GraphStatics.EVIDENCE_PROPERTY, qualifiedAttributes.get(GraphStatics.EVIDENCE_PROPERTY));
			}

			if (qualifiedAttributes.containsKey(GraphStatics.CONFIDENCE_PROPERTY)) {

				relProperties.put(GraphStatics.CONFIDENCE_PROPERTY, qualifiedAttributes.get(GraphStatics.CONFIDENCE_PROPERTY));
			}
		}

		return relProperties;
	}

	public long generateStatementHash(final long subjectNodeId, final String predicateName, final long objectNodeId, final NodeType subjectNodeType,
	                                  final NodeType objectNodeType) throws DMPGraphException {

		final Optional<NodeType> optionalSubjectNodeType = Optional.ofNullable(subjectNodeType);
		final Optional<NodeType> optionalObjectNodeType = Optional.ofNullable(objectNodeType);
		final Optional<String> optionalSubjectIdentifier = getIdentifier(subjectNodeId, optionalSubjectNodeType);
		final Optional<String> optionalObjectIdentifier = getIdentifier(objectNodeId, optionalObjectNodeType);

		return generateStatementHash(predicateName, optionalSubjectNodeType, optionalObjectNodeType, optionalSubjectIdentifier,
				optionalObjectIdentifier);
	}

	public long generateStatementHash(final long subjectNodeId, final Statement statement) throws DMPGraphException {

		final Optional<NodeType> optionalSubjectNodeType = statement.getOptionalSubjectNodeType();
		final Optional<NodeType> optionalObjectNodeType = statement.getOptionalObjectNodeType();
		final Optional<String> optionalSubjectIdentifier = getIdentifier(subjectNodeId, optionalSubjectNodeType);
		final Optional<String> optionalObjectIdentifier = statement.getOptionalObjectValue();
		final String predicateName = statement.getOptionalPredicateURI().get();

		return generateStatementHash(predicateName, optionalSubjectNodeType, optionalObjectNodeType, optionalSubjectIdentifier,
				optionalObjectIdentifier);
	}

	public long generateStatementHash(final String predicateName, final Optional<NodeType> optionalSubjectNodeType,
	                                  final Optional<NodeType> optionalObjectNodeType, final Optional<String> optionalSubjectIdentifier,
	                                  final Optional<String> optionalObjectIdentifier) throws DMPGraphException {

		if (!optionalSubjectNodeType.isPresent() || !optionalObjectNodeType.isPresent() || !optionalSubjectIdentifier.isPresent()
				|| !optionalObjectIdentifier.isPresent()) {

			final String message = "cannot generate statement hash, because the subject node type or object node type or subject identifier or object identifier is not present";

			BatchNeo4jProcessor.LOG.error(message);

			throw new DMPGraphException(message);
		}

		final String simpleHashString = optionalSubjectNodeType.get() + ":" + optionalSubjectIdentifier.get() + " " + predicateName + " "
				+ optionalObjectNodeType.get() + ":" + optionalObjectIdentifier.get();

		final String hashString = putSaltToStatementHash(simpleHashString);

		return HashUtils.generateHash(hashString);
	}

	public Optional<String> getIdentifier(final long nodeId, final Optional<NodeType> optionalNodeType) {

		if (!optionalNodeType.isPresent()) {

			return Optional.empty();
		}

		final String identifier;

		switch (optionalNodeType.get()) {

			case Resource:
			case TypeResource:

				final String uri = (String) getProperty(GraphStatics.URI_PROPERTY, inserter.getNodeProperties(nodeId));
				final String dataModel = (String) getProperty(GraphStatics.DATA_MODEL_PROPERTY, inserter.getNodeProperties(nodeId));

				if (dataModel == null) {

					identifier = uri;
				} else {

					identifier = uri + dataModel;
				}

				break;
			case BNode:
			case TypeBNode:

				identifier = String.valueOf(nodeId);

				break;
			case Literal:

				identifier = (String) getProperty(GraphStatics.VALUE_PROPERTY, inserter.getNodeProperties(nodeId));

				break;
			default:

				identifier = null;

				break;
		}

		return Optional.ofNullable(identifier);
	}

	public abstract void addObjectToResourceWDataModelIndex(final long nodeId, final String URI, final Optional<String> optionalDataModelURI);

	public abstract void handleObjectDataModel(final Map<String, Object> objectNodeProperties, final Optional<String> optionalDataModelURI);

	public abstract void handleSubjectDataModel(final Map<String, Object> subjectNodeProperties, String URI,
	                                            final Optional<String> optionalDataModelURI);

	public abstract Optional<Long> getResourceNodeHits(final String resourceURI);

	@Override
	public String createPrefixedURI(String fullURI) throws DMPGraphException {

		// TODO:

		return null;
	}

	@Override
	public abstract long generateResourceHash(final String resourceURI, final Optional<String> dataModelURI);

	protected abstract String putSaltToStatementHash(final String hash);

	protected Tuple<BatchInserterIndex, BatchInserterIndexProvider> getOrCreateIndex(final String name, final String property,
	                                                                                 final boolean nodeIndex, final int cachSize) {

		final BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(inserter);
		final BatchInserterIndex index;

		if (nodeIndex) {

			index = indexProvider.nodeIndex(name, MapUtil.stringMap("type", "exact"));
		} else {

			index = indexProvider.relationshipIndex(name, MapUtil.stringMap("type", "exact"));
		}

		index.setCacheCapacity(property, cachSize);

		return Tuple.tuple(index, indexProvider);
	}

	protected Tuple<Set<Long>, DB> getOrCreateLongIndex(final String name) throws IOException {

		final URL resource = Resources.getResource("dmpgraph.properties");
		final Properties properties = new Properties();

		try {

			properties.load(resource.openStream());
		} catch (final IOException e) {

			LOG.error("Could not load dmpgraph.properties", e);
		}

		final String indexStoreDir = properties.getProperty("index_store_dir");

		final String storeDir;

		if (indexStoreDir != null && !indexStoreDir.trim().isEmpty()) {

			storeDir = indexStoreDir;
		} else {

			// fallback default
			storeDir = System.getProperty("user.dir") + File.separator + inserter.getStoreDir();
		}

		// + File.separator + ChronicleMapUtils.INDEX_DIR
		return MapDBUtils.createOrGetPersistentLongIndexTreeSetGlobalTransactional(storeDir + File.separator + name, name);
	}

	private static Object getProperty(final String key, final Map<String, Object> properties) {

		if (properties == null) {

			return null;
		}

		return properties.get(key);
	}

	private Optional<Long> getIdFromIndex(final String key, final ObjectLongOpenHashMap<String> tempIndex, final BatchInserterIndex index,
	                                      final String indexProperty) {

		if (key == null) {

			return Optional.empty();
		}

		if (tempIndex.containsKey(key)) {

			return Optional.of(tempIndex.lget());
		}

		if (index == null) {

			return Optional.empty();
		}

		final IndexHits<Long> hits = index.get(indexProperty, key);

		if (hits != null && hits.hasNext()) {

			final Long hit = hits.next();

			hits.close();

			final Optional<Long> optionalHit = Optional.ofNullable(hit);

			if (optionalHit.isPresent()) {

				// temp cache index hits again
				tempIndex.put(key, optionalHit.get());
			}

			return optionalHit;
		}

		if (hits != null) {

			hits.close();
		}

		return Optional.empty();
	}

	private static boolean checkLongIndex(final long key, final Set<Long> index) {

		return index != null && index.contains(key);
	}
}
