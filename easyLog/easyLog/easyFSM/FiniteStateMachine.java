package easyLog.easyFSM;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSourceDOT;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;

import java.io.IOException;
import java.util.regex.Pattern;

public class FiniteStateMachine {
    private Node currentState;
    private Node finalState;
    private final Graph graph;

    public FiniteStateMachine() {
        this.graph = new SingleGraph("FSM");
        this.graph.setStrict(false);
        this.graph.setAutoCreate(true);
        // Set attributes for the graph
        graph.setAttribute("ui.quality");
        // This line is used to set the quality of the graph rendering
        graph.setAttribute("ui.antialias");
        // Set the stylesheet for the graph and make it look nice and readable edges
        graph.setAttribute("ui.stylesheet", "node { fill-color: red; size: 20px; text-size: 18px; text-color: black; text-style: bold-italic; }" +
                "edge { fill-color: black; size: 2px; text-size: 16px; text-color: black; text-alignment: above; shape: cubic-curve; }");
    }

    public void setFinalState(String stateName) {
        Node node = graph.getNode(stateName);
        if (node == null) {
            throw new IllegalArgumentException("State does not exist: " + stateName);
        }
        this.finalState = node;
    }

    public void setInitialState(String stateName) {
        Node node = graph.getNode(stateName);
        if (node == null) {
            throw new IllegalArgumentException("State does not exist: " + stateName);
        }
        this.currentState = node;
    }

    public Node getCurrentState() {
        return currentState;
    }

    public void triggerEvent(String event){
        if (currentState == null) {
            throw new IllegalStateException("Initial state not set.");
        }

        boolean transitionFound = false;

        for(int i = 0 ; i < currentState.leavingEdges().count(); i++)
        {
            Edge edge = currentState.getLeavingEdge(i);
            edge.setAttribute("ui.label", edge.getAttribute("label"));
            edge.setAttribute("ui.style", "text-alignment: above;");
            String edgeEvent = (String) edge.getAttribute("label");
            String regexEdgeEvent = Pattern.quote(edgeEvent);
            if (event.matches(regexEdgeEvent))
            {
                currentState.setAttribute("ui.style", "fill-color: blue;");

                // Move to next state
                currentState = edge.getTargetNode();

                // Set new state's color
                currentState.setAttribute("ui.style", "fill-color: green;");
                transitionFound = true;
                break;
            }

        }

        if (!transitionFound) {
            throw new IllegalArgumentException(
                    "No transition for event '" + event + "' from state '" + currentState.getId() + "'");
        }
    }

    public void printTransitions() {

    }

    public void loadFromDotFile(String filePath) throws IOException {
        FileSourceDOT fileSource = new FileSourceDOT();
        fileSource.addSink(graph);
        fileSource.readAll(filePath);
        fileSource.removeSink(graph);
//        SpriteManager sprites = new SpriteManager(graph);
        for(Node node: graph)
        {
            String label = node.getId();
            if(label!= null)
            {
                node.setAttribute("ui.label",label);
            }
            node.edges().forEach(edge -> {
                Node source = edge.getSourceNode();
                Node target = edge.getTargetNode();

                // Check if bidirectional edge exists
                if (source.hasEdgeToward(target) && target.hasEdgeToward(source)) {
                    // Style first edge
                    if (edge.getSourceNode().getId().compareTo(edge.getTargetNode().getId()) < 0) {
                        edge.setAttribute("ui.style",
                                "text-alignment: at-left; text-offset: 0,-5; shape: cubic-curve;");
                    }
                    // Style second edge
                    else {
                        edge.setAttribute("ui.style",
                                "text-alignment: at-right; text-offset: 10,30; shape: cubic-curve;");
                    }
                }
            });
        }


    }

    public void displayGraph() {
        System.setProperty("org.graphstream.ui", "swing" );
        Viewer viewer = graph.display();
        viewer.getDefaultView().enableMouseOptions();
    }

}
