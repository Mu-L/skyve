package org.skyve.impl.content.jdbc;

import org.skyve.impl.content.elastic.ElasticContentManager;

/**
 * See {@link JDBCRemoteContentManagerServer}.
 * Change the following line...
 * <pre>
 * // Skyve content manager class
 * contentManagerClass: "org.skyve.impl.content.jdbc.JDBCRemoteElasticContentManagerServer"},
 * </pre>
 * @author mike
 */
public class JDBCRemoteElasticContentManagerServer extends ElasticContentManager {
	@Override
	public void startup() {
		JDBCRemoteContentManagerServer.startup();
		super.startup();
	}
	
	@Override
	public void shutdown() {
		try {
			JDBCRemoteContentManagerServer.shutdown();
		}
		finally {
			super.shutdown();
		}
	}
}
