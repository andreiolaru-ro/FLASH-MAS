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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
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
				JSONObject json = (JSONObject) JSONValue.parse(content);
				String type = (String) json.get("type");
				String taskString = (String) json.get("task");

				// Ask for model
				if (type == "ml_ask") {
					for (LinkedHashModel graph : model_descriptions) {
						Prefix data = SparqlBuilder.prefix("data", Rdf.iri("http://example.org#"));
						Prefix mls = SparqlBuilder.prefix("mls", Rdf.iri("http://www.w3.org/ns/mls#"));
						Variable task = SparqlBuilder.var("Task");
						TriplePattern triple = GraphPatterns.tp(task, RDF.TYPE, data.iri(task.getVarName()));
						
						if (triple.getQueryString() != null) {
							sendMessage("YES", SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
						}
					}

					sendMessage("NOT FOUND", SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
				}

				// Receive task + load model
				if (type == "ml_task") {
					for (LinkedHashModel graph : model_descriptions) {
						Prefix data = SparqlBuilder.prefix("data", Rdf.iri("http://example.org#"));
						Prefix mls = SparqlBuilder.prefix("mls", Rdf.iri("http://www.w3.org/ns/mls#"));
						Variable task = SparqlBuilder.var("Task");
						TriplePattern task_name = GraphPatterns.tp(task, RDF.TYPE, data.iri(task.getVarName()));
						
						if (task_name.getQueryString() != null) {
							Variable name = SparqlBuilder.var("Name");
							TriplePattern triple = GraphPatterns.tp(data.iri("Name"), mls.iri("HasValue"), name);

							String model = triple.toString(); // FIXME?

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

							try {
								URL url = new URL(location);
								HttpURLConnection connection = (HttpURLConnection) url.openConnection();
								connection.setRequestMethod("GET");

								try (BufferedReader in = new BufferedReader(
										new InputStreamReader(connection.getInputStream()))) {
									String line;
									while((line = in.readLine()) != null) {
										System.out.println(line);
									}
									System.out.println(connection.getResponseCode());
								}
							} catch(IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}

					sendMessage("NOT FOUND", SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
				}

				// Transfer model
				if (type == "ml_transfer") {
					Path path = Paths.get(localStorage.getAbsolutePath() + "\\model.h5");
					
					try {
						StringBuffer fileData = new StringBuffer();
						BufferedReader reader = new BufferedReader(new FileReader(path.toString()));
						char[] buf = new char[1024];
						int numRead = 0;
						while((numRead = reader.read(buf)) != -1) {
							String readData = String.valueOf(buf, 0, numRead);
							fileData.append(readData);
						}
						reader.close();
						sendMessage(fileData.toString(), SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
					} catch(IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}


				String replyContent = content + " reply";
				sendMessage(replyContent, SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());

				break;
			case AGENT_START:
				if (!localStorage.exists()) {
					localStorage.mkdir();
				}

				if(localStorage.listFiles() == null)
					break;
				for( File f : localStorage.listFiles()){
					URL documentUrl;
					try {
						documentUrl = f.toURI().toURL();
						InputStream inputStream;
						try {
							inputStream = documentUrl.openStream();
							RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
							
							String name = f.getName();
							int i = name.lastIndexOf('.');
							String extension = name.substring(i + 1);
							
							if(extension == "rdf")
								try {
									model_descriptions.add((LinkedHashModel) Rio.parse(inputStream,
											documentUrl.toString(), RDFFormat.TURTLE));
								} catch(Exception e) {
									// TODO: handle exception
								}
						} catch(IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} catch(MalformedURLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
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
	 * Method for executing a specific request
	 */
	private void executeRequest() {}
}
