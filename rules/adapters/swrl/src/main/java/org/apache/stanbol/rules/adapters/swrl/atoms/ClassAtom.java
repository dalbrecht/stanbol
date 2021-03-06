/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.rules.adapters.swrl.atoms;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.swrl.ArgumentSWRLAtom;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.IObjectAtom;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLRule;

/**
 * It adapts any IndividualPropertyAtom to a SWRL class atom.
 * 
 * @author anuzzolese
 * 
 */

public class ClassAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.ClassAtom tmp = (org.apache.stanbol.rules.manager.atoms.ClassAtom) ruleAtom;

        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        IObjectAtom classResource = tmp.getClassResource();
        IObjectAtom argument = tmp.getArgument1();

        SWRLAtom classResourceAtom = (SWRLAtom) adapter.adaptTo(classResource, SWRLRule.class);
        SWRLAtom argumentAtom = (SWRLAtom) adapter.adaptTo(argument, SWRLRule.class);

        OWLClass classPredicate;
        SWRLIArgument argumentResource;

        if (classResourceAtom instanceof ArgumentSWRLAtom) {

            classPredicate = factory.getOWLClass(IRI.create(((ArgumentSWRLAtom) classResourceAtom).getId()));
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        if (argumentAtom instanceof ArgumentSWRLAtom) {
            argumentResource = (SWRLIArgument) ((ArgumentSWRLAtom) argumentAtom).getSwrlArgument();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        return (T) factory.getSWRLClassAtom(classPredicate, argumentResource);

    }

}
