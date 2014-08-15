package org.dswarm.graph.rdf.export.test;

import java.util.List;

import com.sun.jersey.api.client.ClientResponse;
import junit.framework.Assert;

public class ExportUtils {

	private static final String	ATTACHMENT_STRING	= "attachment; filename*=UTF-8''rdf_export";
	private static final String	CONTENT_DISPOSITION	= "Content-Disposition";

	/**
	 * Assert a {@link ClientResponse} contains exactly one "Content-Disposition" header field and its value contains the correct
	 * file ending.
	 * 
	 * @param response the response to be checked
	 * @param expectedFileEnding the expected file ending
	 */
	public static void checkContentDispositionHeader(final ClientResponse response, final String expectedFileEnding) {

		Assert.assertTrue("Header should contain field \"" + CONTENT_DISPOSITION + "\"", response.getHeaders().containsKey(CONTENT_DISPOSITION));
		final List<String> contentDispositionHeaders = response.getHeaders().get(CONTENT_DISPOSITION);
		Assert.assertEquals("there should be exactly one header filed \"" + CONTENT_DISPOSITION + "\"", 1, contentDispositionHeaders.size());

		final String contentDispositionValue = contentDispositionHeaders.get(0);
		Assert.assertEquals(CONTENT_DISPOSITION + " header value mismatch.", ExportUtils.ATTACHMENT_STRING + expectedFileEnding, contentDispositionValue);
	}

}
