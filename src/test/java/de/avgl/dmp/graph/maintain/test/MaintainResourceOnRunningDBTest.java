package de.avgl.dmp.graph.maintain.test;

import de.avgl.dmp.graph.test.Neo4jRunningDBWrapper;

/**
 * @author tgaengler
 */
public class MaintainResourceOnRunningDBTest extends MaintainResourceTest {

	public MaintainResourceOnRunningDBTest() {

		super(new Neo4jRunningDBWrapper(), "running");
	}
}
