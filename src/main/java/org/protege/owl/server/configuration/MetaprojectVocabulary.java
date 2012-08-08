package org.protege.owl.server.configuration;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

public class MetaprojectVocabulary {
	public static final String NS = "http://protege.stanford.edu/ontologies/metaproject.owl";
	
	public static final OWLClass SERVER;
	public static final OWLClass STANDARD_SERVER;
	public static final OWLClass RMI_TRANSPORT;
	
	public static final OWLObjectProperty HAS_SERVER_FILTER;
	public static final OWLObjectProperty HAS_TRANSPORT;
	
	public static final OWLDataProperty HAS_ROOT_PATH;
	public static final OWLDataProperty HAS_HOST_NAME;
	public static final OWLDataProperty HAS_HOST_PORT;
	
	
	static {
		OWLDataFactory factory = OWLManager.getOWLDataFactory();
		SERVER            = factory.getOWLClass(IRI.create(NS + "#Server"));
		STANDARD_SERVER   = factory.getOWLClass(IRI.create(NS + "#StandardOWL2Server"));
		RMI_TRANSPORT     = factory.getOWLClass(IRI.create(NS + "#RMI"));
		
		HAS_SERVER_FILTER = factory.getOWLObjectProperty(IRI.create(NS + "#hasServerFilter"));
		HAS_TRANSPORT     = factory.getOWLObjectProperty(IRI.create(NS + "#hasTransport"));
		
		HAS_ROOT_PATH     = factory.getOWLDataProperty(IRI.create(NS + "#hasServerRootPath"));
		HAS_HOST_NAME     = factory.getOWLDataProperty(IRI.create(NS + "#hasHostName"));
		HAS_HOST_PORT     = factory.getOWLDataProperty(IRI.create(NS + "#hasHostPort"));
	}

	public static void addIRIMapper(OWLOntologyManager manager) {
		URL metaprojectURL = MetaprojectVocabulary.class.getClassLoader().getResource("/metaproject.owl");
		IRI metaprojectIRI;
		try {
			metaprojectIRI = IRI.create(metaprojectURL.toURI());
		}
		catch (URISyntaxException use) {
			throw new RuntimeException("Unexpected class loader exception", use);
		}
		manager.addIRIMapper(new SimpleIRIMapper(IRI.create(NS), metaprojectIRI));		
	}
	
	public static Set<OWLIndividual> getIndividuals(OWLOntology ontology, OWLClass cls) {
		Set<OWLIndividual> individuals = new TreeSet<OWLIndividual>();
		Set<OWLClass> includedClasses = new TreeSet<OWLClass>();
		addIndividuals(ontology, cls, individuals, includedClasses);
		return individuals;
	}
	
	private static void addIndividuals(OWLOntology ontology, OWLClass cls, Set<OWLIndividual> individuals, Set<OWLClass> includedClasses) {
		if (!includedClasses.contains(cls)) {
			includedClasses.add(cls);
			individuals.addAll(cls.getIndividuals(ontology.getImportsClosure()));
			for (OWLClassExpression subCls : cls.getSubClasses(ontology.getImportsClosure())) {
				if (!subCls.isAnonymous()) {
					addIndividuals(ontology, subCls.asOWLClass(), individuals, includedClasses);
				}
			}
		}
	}
	
	private MetaprojectVocabulary() {
		;
	}
}