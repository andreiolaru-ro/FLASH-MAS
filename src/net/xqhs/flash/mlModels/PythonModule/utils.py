import rdflib
from rdflib.namespace import RDF, XSD
import os

# rdflib.plugin.register('sparql', rdflib.query.Processor, 'rdfextras.sparql.processor', 'Processor')
# rdflib.plugin.register('sparql', rdflib.query.Result, 'rdfextras.sparql.query', 'SPARQLQueryResult')

DATA = rdflib.Namespace('http://example.org#')
MLS  = rdflib.Namespace('http://www.w3.org/ns/mls#')

def save_rdf(model_name, algorithm, dataset_link, accuracy, implementation, software, task, classes):
    g = rdflib.Graph()

    g.add((DATA[algorithm], RDF.type, MLS['Algorithm']))
    g.add((rdflib.URIRef(dataset_link), RDF.type, MLS['Dataset']))
    g.add((DATA['Accuracy'], RDF.type, MLS['EvaluationMeasure']))
    g.add((DATA['ModelEvaluation'], RDF.type, MLS['ModelEvaluation']))
    g.add((DATA['ModelEvaluation'], MLS['specifiedBy'], DATA['Accuracy']))
    g.add((DATA['ModelEvaluation'], MLS['hasValue'], rdflib.Literal(accuracy, datatype=XSD.float)))
    g.add((DATA[implementation], RDF.type, MLS['Implementation']))
    g.add((DATA[implementation], MLS['implements'], MLS[implementation]))
    g.add((DATA[software], RDF.type, MLS['Software']))
    g.add((DATA[task], RDF.type, MLS['Task']))
    if classes:
        g.add((DATA['Class'], RDF.type, MLS['ModelCharacteristic']))

        for class_ in classes:
            g.add((DATA['Class'], MLS['hasPart'], rdflib.Literal(class_, datatype=XSD.string)))

    g.bind('', DATA)
    g.bind('mls', MLS)

    # g.serialize(destination=f'{os.getcwd()}\\models\\{model_name}\\model_desctiption.rdf', format='ttl')

    return g

def query_subject(subject):
    return "SELECT ?pred ?obj WHERE { <" + DATA[subject] + "> ?pred ?obj }"

def query_object(obj):
    return "SELECT ?pred ?obj WHERE { ?sub ?pred <" + DATA[obj] + "> }"

def check_parameters(params, dictionary):
    for param in params:
        if param not in dictionary:
            return False

    return True