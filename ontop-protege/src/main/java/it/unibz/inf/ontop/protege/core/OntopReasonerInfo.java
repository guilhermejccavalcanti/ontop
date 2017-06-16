package it.unibz.inf.ontop.protege.core;

/*
 * #%L
 * ontop-protege
 * %%
 * Copyright (C) 2009 - 2013 KRDB Research Centre. Free University of Bozen Bolzano.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLConfiguration;
import org.protege.editor.owl.model.inference.AbstractProtegeOWLReasonerInfo;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

public class OntopReasonerInfo extends AbstractProtegeOWLReasonerInfo {

	/**
	 * Mutable
	 */
	private static class FlexibleConfigurationBuilder {
		private Optional<Properties> optionalProperties = Optional.empty();
		private Optional<OBDAModelWrapper> optionalObdaModelWrapper = Optional.empty();
		private Optional<File> optionalImplicitDBConstraintFile = Optional.empty();

		public OntopSQLOWLAPIConfiguration buildOntopSQLOWLAPIConfiguration() {
			OntopSQLOWLAPIConfiguration.Builder builder = OntopSQLOWLAPIConfiguration.defaultBuilder();
			optionalProperties
					.ifPresent(builder::properties);
			optionalObdaModelWrapper
					.ifPresent(w -> builder.ppMapping(w.getCurrentImmutableOBDAModel()));
			optionalImplicitDBConstraintFile
					.ifPresent(builder::basicImplicitConstraintFile);

			return builder.build();
		}

		public void setProperties(Properties properties) {
			this.optionalProperties = Optional.of(properties);
		}

		public void setOBDAModelWrapper(OBDAModelWrapper modelWrapper) {
			this.optionalObdaModelWrapper = Optional.of(modelWrapper);
		}

		public void setImplicitDBConstraintFile(File implicitDBConstraintFile) {
			this.optionalImplicitDBConstraintFile = Optional.ofNullable(implicitDBConstraintFile);
		}
	}

    private OntopOWLFactory factory = new OntopOWLFactory();

    private final FlexibleConfigurationBuilder configBuilder = new FlexibleConfigurationBuilder();

    @Override
	public BufferingMode getRecommendedBuffering() {
		return BufferingMode.BUFFERING;
	}

	@Override
	public OWLReasonerFactory getReasonerFactory() {
		return factory;
	}

	public void setPreferences(Properties preferences) {
        configBuilder.setProperties(preferences);
	}

	public void setOBDAModelWrapper(OBDAModelWrapper modelWrapper) {
        configBuilder.setOBDAModelWrapper(modelWrapper);
	}

	/**
	 * Allows the user to supply database keys that are not in the database metadata
	 *
	 * @param implicitDBConstraintFile The user-supplied path to the file to database constraints
	 */
	public void setImplicitDBConstraintFile(File implicitDBConstraintFile) {
		configBuilder.setImplicitDBConstraintFile(implicitDBConstraintFile);
	}

    @Override
    public OWLReasonerConfiguration getConfiguration(ReasonerProgressMonitor monitor) {
		return new QuestOWLConfiguration(configBuilder.buildOntopSQLOWLAPIConfiguration(), monitor);
    }
}
