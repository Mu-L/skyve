package org.skyve.toolchain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.skyve.impl.generate.DialectOptions;
import org.skyve.impl.generate.DomainGenerator;
import org.skyve.impl.generate.ViewGenerator;
import org.skyve.impl.metadata.repository.LocalDesignRepository;
import org.skyve.impl.metadata.repository.ProvidedRepositoryFactory;
import org.skyve.metadata.model.document.Bizlet;
import org.skyve.persistence.DocumentQuery;
import org.skyve.persistence.Persistence;
import org.skyve.toolchain.config.GenerateDomainConfig;
import org.skyve.toolchain.config.GenerateEditViewConfig;
import org.skyve.util.DataBuilder;
import org.skyve.util.test.SkyveFactory;
import org.skyve.util.test.SkyveFixture;
import org.skyve.util.test.SkyveFixture.FixtureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;

@Mojo(name = "newScaffoldedDocument")
public class NewScaffoldedDocumentMojo extends NewDocumentMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(NewScaffoldedDocumentMojo.class);

	/**
	 * Relative source directory.
	 */
	@Parameter(required = true, defaultValue = "src/main/java/")
	private String srcDir;

	/**
	 * Relative generated directory.
	 */
	@Parameter(required = true, defaultValue = "src/generated/java/")
	private String generatedDir;

	/**
	 * Relative test directory.
	 */
	@Parameter(required = true, defaultValue = "src/test/java/")
	private String testDir;

	/**
	 * Relative generated test directory.
	 */
	@Parameter(required = true, defaultValue = "src/generatedTest/java/")
	private String generatedTestDir;

	@Parameter
	private String customer;

	@Parameter
	private GenerateDomainConfig generateDomainConfig;

	@Parameter
	private GenerateEditViewConfig generateEditViewConfig;

	/**
	 * Executes the mojo to create a new scaffolded document.
	 * This will create all necessary classes and generate the domain and edit view.
	 * 
	 * @throws MojoExecutionException if there is an error during execution
	 */
	@Override
	public void execute() throws MojoExecutionException {
		super.execute();

		createExtensionClass();
		createBizletClass();
		createFactoryClass();
		createServiceClass();
		generateDomain();
		generateEditView();
	}

	/**
	 * Creates the document extension class.
	 * This class extends the base document class and allows for custom behavior.
	 */
	void createExtensionClass() {
		final TypeSpec documentExtension = TypeSpec.classBuilder(getExtensionName())
														.addModifiers(Modifier.PUBLIC)
														.superclass(ClassName.get("modules." + moduleName + ".domain", documentName))
														.build();

		final JavaFile javaFile = JavaFile.builder("modules." + moduleName + "." + documentName, documentExtension).indent("\t").build();

		try {
			javaFile.writeTo(Paths.get(srcDir));
		}
		catch (IOException e) {
			LOGGER.warn("Failed to scaffold document extension.", e);
		}
	}

	/**
	 * Creates the document Bizlet class.
	 * This class extends Bizlet and provides lifecycle hooks for the document.
	 */
	void createBizletClass() {
		final String bizletName = documentName + "Bizlet";
		final TypeSpec documentBizlet = TypeSpec.classBuilder(bizletName)
													.addModifiers(Modifier.PUBLIC)
													.superclass(ParameterizedTypeName.get(ClassName.get(Bizlet.class),
																							ClassName.get("modules." + moduleName + "." + documentName, getExtensionName())))
													.build();

		final JavaFile javaFile = JavaFile.builder("modules." + moduleName + "." + documentName, documentBizlet).indent("\t").build();

		try {
			javaFile.writeTo(Paths.get(srcDir));
		}
		catch (IOException e) {
			LOGGER.warn("Failed to scaffold document Bizlet.", e);
		}
	}

	/**
	 * Creates the document factory class.
	 * This class provides factory methods for creating test instances of the document.
	 */
	void createFactoryClass() {
		final String factoryName = documentName + "Factory";

		final ClassName extensionClassName = ClassName.get("modules." + moduleName + "." + documentName, getExtensionName());
		final MethodSpec crudInstance = MethodSpec.methodBuilder("crudInstance")
				.addModifiers(Modifier.PUBLIC)
				.returns(extensionClassName)
				.addAnnotation(AnnotationSpec.builder(ClassName.get(SkyveFixture.class))
						.addMember("types", "$T.$L", FixtureType.class, FixtureType.crud)
						.build())
				.addStatement("return new $T().fixture($T.$L).factoryBuild($T.MODULE_NAME, $T.DOCUMENT_NAME)",
						DataBuilder.class,
						FixtureType.class,
						FixtureType.crud,
						extensionClassName, extensionClassName)
				.build();

		final TypeSpec documentFactory = TypeSpec.classBuilder(factoryName)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(AnnotationSpec.builder(ClassName.get(SkyveFactory.class)).build())
				.addMethod(crudInstance)
				.build();

		final JavaFile javaFile = JavaFile.builder("modules." + moduleName + "." + documentName, documentFactory).indent("\t").build();

		try {
			javaFile.writeTo(Paths.get(srcDir));
		}
		catch (IOException e) {
			LOGGER.warn("Failed to scaffold document factory.", e);
		}
	}

	/**
	 * Creates the document service class.
	 * This class provides service layer methods for working with the document.
	 * It includes methods for retrieving single and multiple instances.
	 */
	void createServiceClass() {
		final String serviceName = documentName + "Service";

		final ClassName extensionClassName = ClassName.get("modules." + moduleName + "." + documentName,getExtensionName());
		final MethodSpec get = MethodSpec.methodBuilder("get")
				.addJavadoc(CodeBlock.builder()
						.add("Return the $L with the specified bizId.\n\n", documentName)
						.add("@param bizId The bizId of the $L to retrieve\n", documentName)
						.add("@return The $L, or null if one does not exist with the specified bizId", documentName)
						.build())
											.addModifiers(Modifier.PUBLIC)
											.returns(extensionClassName)
											.addParameter(String.class, "bizId")
											.addStatement("final $T query = persistence.newDocumentQuery($T.MODULE_NAME, $T.DOCUMENT_NAME)",
															DocumentQuery.class,
															extensionClassName,
															extensionClassName)
											.addStatement("query.getFilter().addEquals($T.DOCUMENT_ID, bizId)", extensionClassName)
											.addStatement("return query.beanResult()")
											.build();

		final MethodSpec getAll = MethodSpec.methodBuilder("getAll")
				.addJavadoc(CodeBlock.builder()
						.add("Retrieves all $Ls in the datastore.\n\n", documentName)
						.add("@return All $Ls", documentName)
						.build())
				.addModifiers(Modifier.PUBLIC)
												.returns(ParameterizedTypeName.get(ClassName.get(List.class), extensionClassName))
												.addStatement("final $T query = persistence.newDocumentQuery($T.MODULE_NAME, $T.DOCUMENT_NAME)",
																DocumentQuery.class,
																extensionClassName,
																extensionClassName)
												.addStatement("return query.beanResults()")
												.build();

		final TypeSpec serviceClass = TypeSpec.classBuilder(serviceName)
												.addJavadoc(CodeBlock.builder().add("This class acts as a service layer to encapsulate domain logic.\n\n")
																				.add("Add this line to classes that wish to use it: @Inject private transient " + serviceName + " service;")
																				.build())
												.addModifiers(Modifier.PUBLIC)
												.addAnnotation(AnnotationSpec.builder(ClassName.get(Default.class)).build())
												.addField(FieldSpec.builder(Persistence.class, "persistence")
																		.addAnnotation(Inject.class)
																		.addModifiers(Modifier.PRIVATE)
																		.build())
												.addMethod(get).addMethod(getAll).build();

		final JavaFile javaFile = JavaFile.builder("modules." + moduleName + "." + documentName, serviceClass).indent("\t").build();

		try {
			javaFile.writeTo(Paths.get(srcDir));
		} catch (IOException e) {
			LOGGER.warn("Failed to scaffold document service.", e);
		}
	}

	/**
	 * Gets the name of the extension class for the document.
	 * 
	 * @return The extension class name (document name + "Extension")
	 */
	private String getExtensionName() {
		return documentName + "Extension";
	}

	/**
	 * Generates the domain classes for the document.
	 * This includes generating the base document class and any related classes.
	 * 
	 * @throws MojoExecutionException if there is an error during generation
	 */
	void generateDomain() throws MojoExecutionException {
		if (generateDomainConfig == null) {
			throw new MojoExecutionException("Generate domain configuration not specified.");
		}

		try {
			configureClasspath(srcDir);
			ProvidedRepositoryFactory.set(new LocalDesignRepository(srcDir, false));
			DomainGenerator.newDomainGenerator(true,
												generateDomainConfig.isDebug(),
												generateDomainConfig.isMultiTenant(),
												DialectOptions.valueOf(generateDomainConfig.getDialect()),
												srcDir,
												generatedDir,
												testDir,
												generatedTestDir,
												generateDomainConfig.getExcludedModules().split(",")).generate();
		}
		catch (Exception e) {
			LOGGER.error("Failed to generated domain.", e);
			throw new MojoExecutionException("Failed to generate domain.", e);
		}
	}

	/**
	 * Generates the edit view for the document.
	 * This creates the XML view definition for editing the document.
	 * 
	 * @throws MojoExecutionException if there is an error during generation
	 */
	private void generateEditView() throws MojoExecutionException {
		try {
			final String configCustomerName = (generateEditViewConfig != null) ? generateEditViewConfig.getCustomer() : customer;
			final String customerName = getDefaultOrPromptCustomer(configCustomerName);

			final boolean isCustomerOverriden = (generateEditViewConfig != null) ? generateEditViewConfig.isCustomerOverriden() : false;

			final String overriddenViewName = (generateEditViewConfig != null) ? generateEditViewConfig.getOverridenViewName() : null;

			configureClasspath(srcDir);
			ViewGenerator.main(new String[] {srcDir,
												customerName,
												moduleName,
												documentName,
												Boolean.toString(isCustomerOverriden),
												overriddenViewName});

			final Path viewsDirectory = getModulesDirectory().resolve(moduleName).resolve(documentName).resolve("views");
			final Path source = viewsDirectory.resolve("generatedEdit.xml");
			final Path destination = viewsDirectory.resolve("edit.xml");
			Files.move(source, destination);
		}
		catch (Exception e) {
			LOGGER.error("Failed to generate edit view.", e);
			throw new MojoExecutionException("Failed to generate edit view.", e);
		}
	}
}
