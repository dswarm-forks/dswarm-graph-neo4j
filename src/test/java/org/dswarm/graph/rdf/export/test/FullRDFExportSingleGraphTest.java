package org.dswarm.graph.rdf.export.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.http.HttpStatus;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.sun.jersey.api.client.ClientResponse;
import junit.framework.Assert;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dswarm.common.MediaTypeUtil;
import org.dswarm.graph.rdf.utils.RDFUtils;
import org.dswarm.graph.test.Neo4jDBWrapper;

/**
 * @author polowins
 * @author tgaengler
 * @author reichert
 */
public abstract class FullRDFExportSingleGraphTest extends RDFExportTest {

	private static final Logger	LOG			= LoggerFactory.getLogger(FullRDFExportSingleGraphTest.class);

	private static final String	RDF_N3_FILE	= "dmpf_bsp1.n3";

	public FullRDFExportSingleGraphTest(final Neo4jDBWrapper neo4jDBWrapper, final String dbTypeArg) {

		super(neo4jDBWrapper, dbTypeArg);
	}

	/**
	 * request to export all data in n-quads format
	 * 
	 * @throws IOException
	 */
	@Test
	public void readAllRDFFromDBAcceptNquads() throws IOException {

		readAllRDFFromDBinternal(MediaTypeUtil.N_QUADS, HttpStatus.SC_OK, Lang.NQUADS, ".nq");
	}

	/**
	 * request to export all data in trig format
	 * 
	 * @throws IOException
	 */
	@Test
	public void readAllRDFFromDBAcceptTriG() throws IOException {

		readAllRDFFromDBinternal(MediaTypeUtil.TRIG, HttpStatus.SC_OK, Lang.TRIG, ".trig");
	}

	/**
	 * Test the fallback to default format n-quads in case the accept header is empty
	 * 
	 * @throws IOException
	 */
	@Test
	public void readAllRDFFromDBEmptyAcceptHeader() throws IOException {

		// we need to send an empty accept header. In case we omit this header field at all, the current jersey implementation
		// adds a standard header "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
		readAllRDFFromDBinternal("", HttpStatus.SC_OK, Lang.NQUADS, ".nq");
	}

	/**
	 * request to export all data in rdf+xml format. This format is not supported, a HTTP 406 (not acceptable) response is
	 * expected.
	 * 
	 * @throws IOException
	 */
	@Test
	public void readAllRDFFromDBUnsupportedFormat() throws IOException {

		readAllRDFFromDBinternal(MediaTypeUtil.RDF_XML, HttpStatus.SC_NOT_ACCEPTABLE, null, null);
	}

	/**
	 * request to export all data in a not existing format by sending some "random" accept header value. A HTTP 406 (not
	 * acceptable) response is expected.
	 * 
	 * @throws IOException
	 */
	@Test
	public void readAllRDFFromDBRandomFormat() throws IOException {

		readAllRDFFromDBinternal("khlav/kalash", HttpStatus.SC_NOT_ACCEPTABLE, null, null);
	}

	/**
	 * @param requestedExportLanguage the serialization format neo4j should export the data to. (this value is used as accept
	 *            header arg to query neo4j)
	 * @param expectedHTTPResponseCode the expected HTTP status code of the response, e.g. {@link HttpStatus.SC_OK} or
	 *            {@link HttpStatus.SC_NOT_ACCEPTABLE}
	 * @param expectedExportLanguage the language the exported data is expected to be serialized in. hint: language may differ
	 *            from {@code requestedExportLanguage} to test for default values. (ignored if expectedHTTPResponseCode !=
	 *            {@link HttpStatus.SC_OK})
	 * @param expectedFileEnding the expected file ending to be received from neo4j (ignored if expectedHTTPResponseCode !=
	 *            {@link HttpStatus.SC_OK})
	 * @throws IOException
	 */
	private void readAllRDFFromDBinternal(final String requestedExportLanguage, final int expectedHTTPResponseCode, final Lang expectedExportLanguage,
			final String expectedFileEnding) throws IOException {

		FullRDFExportSingleGraphTest.LOG.debug("start export all RDF statements test for RDF resource at " + dbType + " DB using a single rdf file");

		final String provenanceURI = "http://data.slub-dresden.de/resources/2";

		// prepare: write data to graph
		writeRDFToDBInternal(provenanceURI, FullRDFExportSingleGraphTest.RDF_N3_FILE);

		// request export from end point
		final ClientResponse response = service().path("/rdf/getall").accept(requestedExportLanguage).get(ClientResponse.class);

		Assert.assertEquals("expected " + expectedHTTPResponseCode, expectedHTTPResponseCode, response.getStatus());

		// in case we requested an unsupported format, stop processing here since there is no exported data to verify
		if (expectedHTTPResponseCode == HttpStatus.SC_NOT_ACCEPTABLE) {
			return;
		}

		// check Content-Disposition header for correct file ending
		ExportUtils.checkContentDispositionHeader(response, expectedFileEnding);

		// verify exported data
		final String body = response.getEntity(String.class);

		Assert.assertNotNull("response body shouldn't be null", body);

		// FullRDFExportSingleGraphTest.LOG.trace("Response body : " + body);

		final InputStream stream = new ByteArrayInputStream(body.getBytes("UTF-8"));

		Assert.assertNotNull("input stream (from body) shouldn't be null", stream);

		// read actual data set from response body
		final Dataset dataset = DatasetFactory.createMem();
		RDFDataMgr.read(dataset, stream, expectedExportLanguage);

		Assert.assertNotNull("dataset shouldn't be null", dataset);

		final long statementsInExportedRDFModel = RDFUtils.determineDatasetSize(dataset);

		FullRDFExportSingleGraphTest.LOG.debug("exported '" + statementsInExportedRDFModel + "' statements");

		final URL fileURL = Resources.getResource(FullRDFExportSingleGraphTest.RDF_N3_FILE);
		final InputSupplier<InputStream> inputSupplier = Resources.newInputStreamSupplier(fileURL);

		final Model modelFromOriginalRDFile = ModelFactory.createDefaultModel();
		modelFromOriginalRDFile.read(inputSupplier.getInput(), null, "TURTLE");

		final long statementsInOriginalRDFFile = modelFromOriginalRDFile.size();

		Assert.assertEquals("the number of statements should be " + statementsInOriginalRDFFile, statementsInOriginalRDFFile,
				statementsInExportedRDFModel);
		Assert.assertTrue("the received dataset should contain a graph with the provenance uri '" + provenanceURI + "'",
				dataset.containsNamedModel(provenanceURI));

		final Model actualModel = dataset.getNamedModel(provenanceURI);

		Assert.assertNotNull("the graph (model) for provenance uri '" + provenanceURI + "' shouldn't be null", actualModel);

		// check if statements are the "same" (isomorphic, i.e. blank nodes may have different IDs)
		Assert.assertTrue("the RDF from the property graph is not isomorphic to the RDF in the original file ",
				actualModel.isIsomorphicWith(modelFromOriginalRDFile));

		FullRDFExportSingleGraphTest.LOG.debug("finished export all RDF statements test for RDF resource at " + dbType
				+ " DB using a single rdf file");
	}

}
