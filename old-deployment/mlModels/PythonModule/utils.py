import rdflib
from rdflib.namespace import RDF, XSD
import os

# rdflib.plugin.register('sparql', rdflib.query.Processor, 'rdfextras.sparql.processor', 'Processor')
# rdflib.plugin.register('sparql', rdflib.query.Result, 'rdfextras.sparql.query', 'SPARQLQueryResult')

DATA = rdflib.Namespace('http://example.org#')
MLS  = rdflib.Namespace('http://www.w3.org/ns/mls#')

def save_rdf(model_name, algorithm, dataset_link, metrics, implementation, software, task, classes, loss, optimizer):
    g = rdflib.Graph()

    g.add((DATA['Name'], RDF.type, MLS['ModelCharacteristic']))
    g.add((DATA['Name'], MLS['hasValue'], rdflib.Literal(model_name, datatype=XSD.string)))
    g.add((DATA[algorithm], RDF.type, MLS['Algorithm']))
    g.add((rdflib.URIRef(dataset_link), RDF.type, MLS['Dataset']))
    g.add((DATA['Accuracy'], RDF.type, MLS['EvaluationMeasure']))
    g.add((DATA['ModelEvaluation'], RDF.type, MLS['ModelEvaluation']))
    g.add((DATA['ModelEvaluation'], MLS['specifiedBy'], DATA['Accuracy']))
    g.add((DATA['ModelEvaluation'], MLS['hasValue'], rdflib.Literal(metrics['Accuracy'], datatype=XSD.float)))
    g.add((DATA[implementation], RDF.type, MLS['Implementation']))
    g.add((DATA[implementation], MLS['implements'], MLS[implementation]))
    g.add((DATA[software], RDF.type, MLS['Software']))
    g.add((DATA[task], RDF.type, MLS['Task']))
    g.add((DATA['Loss'], RDF.type, MLS['ModelCharacteristic']))
    g.add((DATA['Loss'], MLS['hasValue'], rdflib.Literal(loss, datatype=XSD.string)))
    g.add((DATA['Optimizer'], RDF.type, MLS['ModelCharacteristic']))
    g.add((DATA['Optimizer'], MLS['hasValue'], rdflib.Literal(optimizer, datatype=XSD.string)))
    if classes:
        g.add((DATA['Class'], RDF.type, MLS['ModelCharacteristic']))

        for class_ in classes:
            g.add((DATA['Class'], MLS['hasPart'], rdflib.Literal(class_, datatype=XSD.string)))

    g.bind('', DATA)
    g.bind('mls', MLS)

    # g.serialize(destination=f'{os.getcwd()}\\models\\{model_name}\\model_desctiption.rdf', format='ttl')

    return g

def query_subject(subject):
    return "SELECT ?pred ?obj WHERE { <" + subject + "> ?pred ?obj }"

def query_object(obj):
    return "SELECT ?sub ?pred WHERE { ?sub ?pred <" + obj + "> }"

def check_parameters(params, dictionary):
    for param in params:
        if param not in dictionary:
            return False

    return True