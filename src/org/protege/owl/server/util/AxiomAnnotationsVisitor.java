package org.protege.owl.server.util;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

public abstract class AxiomAnnotationsVisitor implements OWLAxiomVisitor {
    private OWLDataFactory factory;
    private OWLAxiom result;

    protected AxiomAnnotationsVisitor(OWLDataFactory factory) {
        this.factory = factory;
    }

    protected abstract Set<OWLAnnotation> processAnnotations(Set<OWLAnnotation> axiomAnnotations);

    public OWLAxiom getOWLAxiom() {
        return result;
    }

    @Override
    public void visit(OWLDeclarationAxiom axiom) {
        result = factory.getOWLDeclarationAxiom(axiom.getEntity(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLSubClassOfAxiom axiom) {
        result = factory.getOWLSubClassOfAxiom(axiom.getSubClass(), axiom.getSuperClass(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        result = factory.getOWLNegativeObjectPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        result = factory.getOWLAsymmetricObjectPropertyAxiom(axiom.getProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
        result = factory.getOWLReflexiveObjectPropertyAxiom(axiom.getProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLDisjointClassesAxiom axiom) {
        result = factory.getOWLDisjointClassesAxiom(axiom.getClassExpressions(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLDataPropertyDomainAxiom axiom) {
        result = factory.getOWLDataPropertyDomainAxiom(axiom.getProperty(), axiom.getDomain(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLObjectPropertyDomainAxiom axiom) {
        result = factory.getOWLObjectPropertyDomainAxiom(axiom.getProperty(), axiom.getDomain(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        result = factory.getOWLEquivalentObjectPropertiesAxiom(axiom.getProperties(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        result = factory.getOWLNegativeDataPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLDifferentIndividualsAxiom axiom) {
        result = factory.getOWLDifferentIndividualsAxiom(axiom.getIndividuals(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLDisjointDataPropertiesAxiom axiom) {
        result = factory.getOWLDisjointDataPropertiesAxiom(axiom.getProperties(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
        result = factory.getOWLDisjointObjectPropertiesAxiom(axiom.getProperties(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLObjectPropertyRangeAxiom axiom) {
        result = factory.getOWLObjectPropertyRangeAxiom(axiom.getProperty(), axiom.getRange(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLObjectPropertyAssertionAxiom axiom) {
        result = factory.getOWLObjectPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
        result = factory.getOWLFunctionalObjectPropertyAxiom(axiom.getProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLSubObjectPropertyOfAxiom axiom) {
        result = factory.getOWLSubObjectPropertyOfAxiom(axiom.getSubProperty(), axiom.getSuperProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLDisjointUnionAxiom axiom) {
        result = factory.getOWLDisjointUnionAxiom(axiom.getOWLClass(), axiom.getClassExpressions(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
        result = factory.getOWLSymmetricObjectPropertyAxiom(axiom.getProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLDataPropertyRangeAxiom axiom) {
        result = factory.getOWLDataPropertyRangeAxiom(axiom.getProperty(), axiom.getRange(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLFunctionalDataPropertyAxiom axiom) {
        result = factory.getOWLFunctionalDataPropertyAxiom(axiom.getProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
        result = factory.getOWLEquivalentDataPropertiesAxiom(axiom.getProperties(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLClassAssertionAxiom axiom) {
        result = factory.getOWLClassAssertionAxiom(axiom.getClassExpression(), axiom.getIndividual(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        result = factory.getOWLEquivalentClassesAxiom(axiom.getClassExpressions(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLDataPropertyAssertionAxiom axiom) {
        result = factory.getOWLDataPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
        result = factory.getOWLTransitiveObjectPropertyAxiom(axiom.getProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        result = factory.getOWLIrreflexiveObjectPropertyAxiom(axiom.getProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLSubDataPropertyOfAxiom axiom) {
        result = factory.getOWLSubDataPropertyOfAxiom(axiom.getSubProperty(), axiom.getSuperProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        result = factory.getOWLInverseFunctionalObjectPropertyAxiom(axiom.getProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLSameIndividualAxiom axiom) {
        result = factory.getOWLSameIndividualAxiom(axiom.getIndividuals(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLSubPropertyChainOfAxiom axiom) {
        result = factory.getOWLSubPropertyChainOfAxiom(axiom.getPropertyChain(), axiom.getSuperProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLInverseObjectPropertiesAxiom axiom) {
        result = factory.getOWLInverseObjectPropertiesAxiom(axiom.getFirstProperty(), axiom.getSecondProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLHasKeyAxiom axiom) {
        result = factory.getOWLHasKeyAxiom(axiom.getClassExpression(), axiom.getPropertyExpressions(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLDatatypeDefinitionAxiom axiom) {
        result = factory.getOWLDatatypeDefinitionAxiom(axiom.getDatatype(), axiom.getDataRange(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(SWRLRule rule) {
        result = factory.getSWRLRule(rule.getBody(), rule.getHead(), processAnnotations(rule.getAnnotations()));
    }

    @Override
    public void visit(OWLAnnotationAssertionAxiom axiom) {
        result = factory.getOWLAnnotationAssertionAxiom(axiom.getSubject(), axiom.getAnnotation(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
        result = factory.getOWLSubAnnotationPropertyOfAxiom(axiom.getSubProperty(), axiom.getSuperProperty(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
        result = factory.getOWLAnnotationPropertyDomainAxiom(axiom.getProperty(), axiom.getDomain(), processAnnotations(axiom.getAnnotations()));
    }

    @Override
    public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
        result = factory.getOWLAnnotationPropertyRangeAxiom(axiom.getProperty(), axiom.getRange(), processAnnotations(axiom.getAnnotations()));
    }

}