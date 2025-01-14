/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.pilato.elasticsearch.tools.updaters;

import fr.pilato.elasticsearch.tools.util.SettingsFinder;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static fr.pilato.elasticsearch.tools.util.SettingsReader.getJsonContent;

/**
 * Manage elasticsearch index settings
 * @author David Pilato
 */
public class ElasticsearchIndexUpdater {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexUpdater.class);

	/**
	 * Create a new index in Elasticsearch. Read also _settings.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 * @param force Remove index if exists (Warning: remove all data)
	 * @throws Exception if the elasticsearch API call is failing
	 */
	@Deprecated
	public static void createIndex(Client client, String root, String index, boolean force) throws Exception {
		String json = getJsonContent(root, index, SettingsFinder.Defaults.IndexSettingsFileName);
		createIndexWithSettings(client, index, json, force);
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no specific settings
	 * @param force Remove index if exists (Warning: remove all data)
	 * @throws Exception if the elasticsearch API call is failing
	 */
	@Deprecated
	public static void createIndexWithSettings(Client client, String index, String settings, boolean force) throws Exception {
		if (force && isIndexExist(client, index)) {
			logger.debug("Index [{}] already exists but force set to true. Removing all data!", index);
			removeIndexInElasticsearch(client, index);
		}
		if (force || !isIndexExist(client, index)) {
			logger.debug("Index [{}] doesn't exist. Creating it.", index);
			createIndexWithSettingsInElasticsearch(client, index, settings);
		} else {
			logger.debug("Index [{}] already exists.", index);
		}
	}

	/**
	 * Remove a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @throws Exception if the elasticsearch API call is failing
	 */
	@Deprecated
	public static void removeIndexInElasticsearch(Client client, String index) throws Exception {
		logger.trace("removeIndex([{}])", index);

		assert client != null;
		assert index != null;

		try {
			AcknowledgedResponse response = client.admin().indices().prepareDelete(index).get();
			if (!response.isAcknowledged()) {
				logger.warn("Could not delete index [{}]", index);
				throw new Exception("Could not delete index ["+index+"].");
			}
		} catch (IndexNotFoundException e) {
			// This is expected
		}

		logger.trace("/removeIndex([{}])", index);
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no specific settings
	 * @throws Exception if the elasticsearch API call is failing
	 */
	@Deprecated
	private static void createIndexWithSettingsInElasticsearch(Client client, String index, String settings) throws Exception {
		logger.trace("createIndex([{}])", index);

		assert client != null;
		assert index != null;

		CreateIndexRequestBuilder cirb = client.admin().indices().prepareCreate(index);

		// If there are settings for this index, we use it. If not, using Elasticsearch defaults.
		if (settings != null) {
			logger.trace("Found settings for index [{}]: [{}]", index, settings);
			cirb.setSource(settings, XContentType.JSON);
		}

		CreateIndexResponse createIndexResponse = cirb.execute().actionGet();
		if (!createIndexResponse.isAcknowledged()) {
			logger.warn("Could not create index [{}]", index);
			throw new Exception("Could not create index ["+index+"].");
		}

		logger.trace("/createIndex([{}])", index);
	}

	/**
	 * Update settings in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no update settings
	 */
	@Deprecated
	private static void updateIndexWithSettingsInElasticsearch(Client client, String index, String settings) {
		logger.trace("updateIndex([{}])", index);

		assert client != null;
		assert index != null;

		if (settings != null) {
			logger.trace("Found update settings for index [{}]: [{}]", index, settings);
			logger.debug("updating settings for index [{}]", index);
			client.admin().indices().prepareUpdateSettings(index).setSettings(settings, XContentType.JSON).get();
		}

		logger.trace("/updateIndex([{}])", index);
	}

	/**
	 * Update mapping in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param mapping Mapping if any, null if no update mapping
	 */
	@Deprecated
	private static void updateMappingInElasticsearch(Client client, String index, String mapping) {
		logger.trace("updateMapping([{}])", index);

		assert client != null;
		assert index != null;

		if (mapping != null) {
			logger.trace("Found update mapping for index [{}]: [{}]", index, mapping);
			logger.debug("updating mapping for index [{}]", index);
			client.admin().indices().preparePutMapping(index).setType("_doc").setSource(mapping, XContentType.JSON).get();
		}

		logger.trace("/updateMapping([{}])", index);
	}

	/**
	 * Check if an index already exists
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @return true if index already exists
	 */
	@Deprecated
	public static boolean isIndexExist(Client client, String index) {
		return client.admin().indices().prepareExists(index).get().isExists();
	}

	/**
	 * Update index settings in Elasticsearch. Read also _update_settings.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 */
	@Deprecated
	public static void updateSettings(Client client, String root, String index) throws IOException {
		String json = getJsonContent(root, index, SettingsFinder.Defaults.UpdateIndexSettingsFileName);
		updateIndexWithSettingsInElasticsearch(client, index, json);
	}

	/**
	 * Update index mapping in Elasticsearch. Read also _update_mapping.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 */
	@Deprecated
	public static void updateMapping(Client client, String root, String index) throws IOException {
		String json = getJsonContent(root, index, SettingsFinder.Defaults.UpdateIndexMappingFileName);
		updateMappingInElasticsearch(client, index, json);
	}

	/**
	 * Create a new index in Elasticsearch. Read also _settings.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 * @param force Remove index if exists (Warning: remove all data)
	 * @throws Exception if the elasticsearch API call is failing
	 */
	public static void createIndex(RestClient client, String root, String index, boolean force) throws Exception {
		String json = getJsonContent(root, index, SettingsFinder.Defaults.IndexSettingsFileName);
		createIndexWithSettings(client, index, json, force);
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no specific settings
	 * @param force Remove index if exists (Warning: remove all data)
	 * @throws Exception if the elasticsearch API call is failing
	 */
	public static void createIndexWithSettings(RestClient client, String index, String settings, boolean force) throws Exception {
		if (force && isIndexExist(client, index)) {
			logger.debug("Index [{}] already exists but force set to true. Removing all data!", index);
			removeIndexInElasticsearch(client, index);
		}
		if (force || !isIndexExist(client, index)) {
			logger.debug("Index [{}] doesn't exist. Creating it.", index);
			createIndexWithSettingsInElasticsearch(client, index, settings);
		} else {
			logger.debug("Index [{}] already exists.", index);
		}
	}

	/**
	 * Remove a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @throws Exception if the elasticsearch API call is failing
	 */
	public static void removeIndexInElasticsearch(RestClient client, String index) throws Exception {
		logger.trace("removeIndex([{}])", index);

		assert client != null;
		assert index != null;

		int statusCode;

		try {
			Response response = client.performRequest(new Request("DELETE", "/" + index));
			statusCode = response.getStatusLine().getStatusCode();
		} catch (ResponseException e) {
			statusCode = e.getResponse().getStatusLine().getStatusCode();
		}
		if (statusCode != 200 && statusCode != 404) {
			logger.warn("Could not delete index [{}]", index);
			throw new Exception("Could not delete index ["+index+"].");
		}

		logger.trace("/removeIndex([{}])", index);
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no specific settings
	 * @throws Exception if the elasticsearch API call is failing
	 */
	private static void createIndexWithSettingsInElasticsearch(RestClient client, String index, String settings) throws Exception {
		logger.trace("createIndex([{}])", index);

		assert client != null;
		assert index != null;

        Request request = new Request("PUT", "/" + index);

		// If there are settings for this index, we use it. If not, using Elasticsearch defaults.
		if (settings != null) {
			logger.trace("Found settings for index [{}]: [{}]", index, settings);
			request.setJsonEntity(settings);
		}

        Response response = client.performRequest(request);
		if (response.getStatusLine().getStatusCode() != 200) {
			logger.warn("Could not create index [{}]", index);
			throw new Exception("Could not create index ["+index+"].");
		}

		logger.trace("/createIndex([{}])", index);
	}

	/**
	 * Update settings in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no update settings
	 * @throws Exception if the elasticsearch API call is failing
	 */
	private static void updateIndexWithSettingsInElasticsearch(RestClient client, String index, String settings) throws Exception {
		logger.trace("updateIndex([{}])", index);

		assert client != null;
		assert index != null;


		if (settings != null) {
			logger.trace("Found update settings for index [{}]: [{}]", index, settings);
			logger.debug("updating settings for index [{}]", index);
            Request request = new Request("PUT", "/" + index + "/_settings");
            request.setJsonEntity(settings);
			client.performRequest(request);
		}

		logger.trace("/updateIndex([{}])", index);
	}

	/**
	 * Update mapping in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param mapping Mapping if any, null if no update mapping
	 * @throws Exception if the elasticsearch API call is failing
	 */
	private static void updateMappingInElasticsearch(RestClient client, String index, String mapping) throws Exception {
		logger.trace("updateMapping([{}])", index);

		assert client != null;
		assert index != null;


		if (mapping != null) {
			logger.trace("Found update mapping for index [{}]: [{}]", index, mapping);
			logger.debug("updating mapping for index [{}]", index);
            Request request = new Request("PUT", "/" + index + "/_mapping");
            request.setJsonEntity(mapping);
			client.performRequest(request);
		}

		logger.trace("/updateMapping([{}])", index);
	}

	/**
	 * Check if an index already exists
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @return true if index already exists
	 * @throws Exception if the elasticsearch API call is failing
	 */
	public static boolean isIndexExist(RestClient client, String index) throws Exception {
		Response response = client.performRequest(new Request("HEAD", "/" + index));
		return response.getStatusLine().getStatusCode() == 200;
	}

	/**
	 * Update index settings in Elasticsearch. Read also _update_settings.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 * @throws Exception if the elasticsearch API call is failing
	 */
	public static void updateSettings(RestClient client, String root, String index) throws Exception {
		String json = getJsonContent(root, index, SettingsFinder.Defaults.UpdateIndexSettingsFileName);
		updateIndexWithSettingsInElasticsearch(client, index, json);
	}

	/**
	 * Update index mapping in Elasticsearch. Read also _update_mapping.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 * @throws Exception if the elasticsearch API call is failing
	 */
	public static void updateMapping(RestClient client, String root, String index) throws Exception {
		String json = getJsonContent(root, index, SettingsFinder.Defaults.UpdateIndexMappingFileName);
		updateMappingInElasticsearch(client, index, json);
	}
}
