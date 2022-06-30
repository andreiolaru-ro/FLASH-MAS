/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.mlModels;

import java.io.File;

import java.util.*;
import org.json.*;
import org.eclipse.rdf4j.*;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * The {@link MLDescriptionShard} class manages the SPARQL queries and transfering of ML models.
 *
 *  @author Daniel Liurca
 */
public class MLDescriptionShard extends AgentShardGeneral {
	/**
	 * The UID.
	 */
	private static final long	serialVersionUID	= 5214882018809437402L;
	/**
	 * Endpoint element for this shard.
	 */
	public static final String	SHARD_ENDPOINT		= "ml_description";

	/**
	 * The functionality of the shard
	 */
	public static final String	FUNCTIONALITY	    = "ML_DESCRIPTION";

	/**
	 * Cache for the name of this agent.
	 */
	String						thisAgent		    = null;

	private String storage = "D:\\Facultate\\models";
	private File localStorage = new File(storage, "agent"); // get the name of the agent
	private List<LinkedHashModel> model_descriptions  = new ArrayList<LinkedHashModel>();

	{
		setUnitName("ml_description-shard");
		setLoggerType(PlatformUtils.platformLogType());
	}

	/**
	 * Default constructor
	 */
	public MLDescriptionShard()
	{
		super(AgentShardDesignation.customShard(FUNCTIONALITY));
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event)
	{
		super.signalAgentEvent(event);
		switch(event.getType())
		{
			case AGENT_WAVE:
				if(!(((AgentWave) event).getFirstDestinationElement()).equals(SHARD_ENDPOINT))
					break;

				String content = ((AgentWave) event).getContent();
				JSONObject json = new JSONObject(content);
				String message_type = json.getString("type");
				String task = json.getString("task");

				// Ask for model
				if (type == "ml_ask") {
					for (LinkedHashModel graph : model_descriptions) {
						Prefix data = SparqlBuilder.prefix("data", iri("http://example.org#"));
						Prefix mls  = SparqlBuilder.prefix("mls", iri("http://www.w3.org/ns/mls#"));
						Variable task = SparqlBuilder.var("Task");
						TriplePattern triple = GraphPatterns.tp(task, Rdf.type, data.iri(task));
						
						if (triple.getQueryString() != null) {
							sendMessage("YES", SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
						}
					}

					sendMessage("NOT FOUND", SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
				}

				// Receive task + load model
				if (type == "ml_task") {
					for (LinkedHashModel graph : model_descriptions) {
						Prefix data = SparqlBuilder.prefix("data", iri("http://example.org#"));
						Prefix mls  = SparqlBuilder.prefix("mls", iri("http://www.w3.org/ns/mls#"));
						Variable task = SparqlBuilder.var("Task");
						TriplePattern task_name = GraphPatterns.tp(task, Rdf.type, data.iri(task));
						
						if (task_name.getQueryString() != null) {
							Variable name = SparqlBuilder.var("Name");
							TriplePattern triple = GraphPatterns.tp(data.iri("Name"), mls.iri("HasValue"), name);

							String model = triple.getObject();

							Map<String, String> parameters = new HashMap<>();
							parameters.put("model", model);

							String location = "http://localhost:5000/load";

							if (!parameters.isEmpty()) {
								location += "?";

								for (Map.Entry<String, String> mapElement : parameters.entrySet()) {
									String key   = mapElement.getKey();
									String value = mapElement.getValue();

									location += key + "=" + value + "&";
								}
							}

							location = location.substring(0, location.length() - 1);

							URL url = new URL(location);
							HttpURLConnection connection = (HttpURLConnection) url.openConnection();
							connection.setRequestMethod("GET");

							try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream())))
							{
								String line;
								while ((line = in.readLine()) != null) {
									System.out.println(line);
								}
								System.out.println(connection.getResponseCode());
							}
						}
					}

					sendMessage("NOT FOUND", SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
				}

				// Transfer model
				if (type == "ml_transfer") {
					Path path = Paths.get(localStorage.getAbsolutePath() + "\\model.h5");
					byte[] data = Files.readAllBytes(path);
					sendMessage(data, SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
				}


				String replyContent = content + " reply";
				sendMessage(replyContent, SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());

				break;
			case AGENT_START:
				if (!localStorage.exists()) {
					localStorage.mkdir();
				}

				RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);

				for( File f : localStorage.listFiles()){
					java.net.URL documentUrl = new URL(f);
					InputStream inputStream = documentUrl.openStream();
					RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);

					String name = f.getName();
					int i = name.lastIndexOf('.');
					String extension = name.substring(i+1);

					if (extension == "rdf")
						model_descriptions.add(Rio.parse(inputStream, documentUrl.toString(), RDFFormat.TURTLE));
				}

				break;
			case AGENT_STOP:
				break;
			case SIMULATION_START:
				break;
			case SIMULATION_PAUSE:
				break;
			default:
				break;
		}
	}

	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		if(getAgent() != null)
			thisAgent = getAgent().getEntityName();
	}

	/**
	 * Method for excuting a speciefic request
	 */
	private void executeRequest() {}
}
