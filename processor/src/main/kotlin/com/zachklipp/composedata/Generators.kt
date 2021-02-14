package com.zachklipp.composedata

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

private val COMPOSABLE_ANNOTATION = ClassName("androidx.compose.runtime", "Composable")
private val STABLE_ANNOTATION = ClassName("androidx.compose.runtime", "Stable")
private val REMEMBER_FUN = MemberName("androidx.compose.runtime", "remember")
private val MUTABLE_STATE_OF_FUN = MemberName("androidx.compose.runtime", "mutableStateOf")
private val UNIT_NAME = Unit::class.asClassName()

fun generateRememberFunction(
  model: ModelInterface,
  builderInterfaceSpec: BuilderInterfaceSpec,
  implSpec: ModelImplSpec,
  converter: TypeConverter
): FunSpec {
  with(converter) {
    val interfaceName = ClassName(model.packageName, model.simpleName)

    val updateLambdaType = LambdaTypeName.get(
      receiver = null,
      ClassName(model.packageName, builderInterfaceSpec.spec.name!!),
      returnType = UNIT_NAME
    ).copy(annotations = listOf(AnnotationSpec.builder(COMPOSABLE_ANNOTATION).build()))

    val updateParam = ParameterSpec("update", updateLambdaType)

    val requiredParams = model.properties.filter { !it.hasDefault }
      .map { property ->
        ParameterSpec(property.name, property.declaration.type.toTypeName())
      }

    return FunSpec.builder("remember${model.simpleName}")
      // Builder function is always internal – the code should wrap the builder with
      // its own actual function that includes the implementation.
      .addModifiers(INTERNAL)
      .addAnnotation(COMPOSABLE_ANNOTATION)
      .addParameters(requiredParams)
      // This must be at the end for trailing lambda syntax.
      .addParameter(updateParam)
      .returns(interfaceName)
      .addCode(
        CodeBlock.builder()
          .add("return %L", remember {
            add("%N(", implSpec.spec)
            requiredParams.forEach { add("%N, ", it.name) }
            add(")\n")
          })
          .add(".also { %N(it) }", updateParam)
          .build()
      )
      .build()
  }
}

fun generateBuilderInterface(
  model: ModelInterface,
  converter: TypeConverter
): BuilderInterfaceSpec {
  with(converter) {
    val builderInterfaceName = ClassName(model.packageName, "${model.simpleName}Builder")

    val propertySpecs = model.properties.map {
      it to
        PropertySpec.builder(it.declaration.simpleName.asString(), it.declaration.type.toTypeName())
          .mutable(true)
          .build()
    }

    val eventHandlerSpecs = model.eventHandlers.map { eventHandler ->
      val declaration = eventHandler.declaration
      val eventHandlerLambda = LambdaTypeName.get(
        parameters = declaration.parameters.map { eventParam ->
          ParameterSpec(eventParam.name!!.asString(), eventParam.type.toTypeName())
        },
        returnType = UNIT_NAME
      )

      EventHandlerSpec(
        declaration.simpleName.asString(),
        eventHandler,
        eventHandlerLambda,
        setter = FunSpec.builder(declaration.simpleName.asString())
          .addModifiers(ABSTRACT)
          .addParameter("handler", eventHandlerLambda)
          .build()
      )
    }

    return BuilderInterfaceSpec(
      model,
      spec = TypeSpec.interfaceBuilder(builderInterfaceName)
        // Internal for the same reason that the remember function is.
        .addModifiers(INTERNAL)
        .addAnnotation(STABLE_ANNOTATION)
        .addProperties(propertySpecs.map { it.second })
        .addFunctions(eventHandlerSpecs.map { it.setter })
        .build(),
      propertySpecs,
      eventHandlerSpecs
    )
  }
}

fun generateImplClass(
  model: ModelInterface,
  builder: BuilderInterfaceSpec,
  converter: TypeConverter
): ModelImplSpec {
  with(converter) {
    val implClassName = ClassName(model.packageName, "${model.simpleName}Impl")

    val constructorParamsByName = model.properties.filter { !it.hasDefault }
      .associate {
        val name = it.declaration.simpleName.asString()
        name to ParameterSpec(name, it.declaration.type.toTypeName())
      }

    val properties = model.properties.map { property ->
      PropertySpec.builder(property.name, property.declaration.type.toTypeName())
        .addModifiers(OVERRIDE)
        .mutable(true)
        .delegate(mutableStateOf(property.declaration.defaultOr(CodeBlock.of("%N", property.name))))
        .build()
    }

    val eventHandlerProperties: Map<String, PropertySpec> = builder.eventHandlers.associate {
      val property = PropertySpec
        .builder("${it.name}\$handler", it.lambdaTypeName.copy(nullable = true))
        .addModifiers(PRIVATE)
        .mutable(true)
        .initializer("null")
        .build()
      Pair(it.name, property)
    }

    val eventHandlerSetters = builder.eventHandlers.map {
      it.setter.toBuilder()
        .apply { modifiers -= ABSTRACT }
        .addModifiers(OVERRIDE)
        .addCode("%N = handler", eventHandlerProperties.getValue(it.name))
        .build()
    }

    val eventHandlers = model.eventHandlers.map { eventHandler ->
      val backingProperty = eventHandlerProperties.getValue(eventHandler.name)
      val parameters = eventHandler.declaration.parameters.map {
        ParameterSpec(it.name!!.asString(), it.type.toTypeName())
      }
      FunSpec.builder(eventHandler.name)
        .addModifiers(OVERRIDE)
        .addParameters(parameters)
        .addCode("%N?.invoke(", backingProperty)
        .apply {
          parameters.forEach {
            addCode("%N, ", it)
          }
        }
        .addCode(")")
        .build()
    }

    val implBuilder = TypeSpec.classBuilder(implClassName)
      .addModifiers(PRIVATE)
      .addAnnotation(STABLE_ANNOTATION)
      .addSuperinterface(ClassName(model.packageName, model.simpleName))
      .addSuperinterface(ClassName(model.packageName, builder.spec.name!!))
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameters(constructorParamsByName.values)
          .build()
      )
      .addProperties(properties)
      .addProperties(eventHandlerProperties.values)
      .addFunctions(eventHandlerSetters)
      .addFunctions(eventHandlers)

    return ModelImplSpec(
      implBuilder.build(),
      imports = listOf(
        MemberName("androidx.compose.runtime", "getValue"),
        MemberName("androidx.compose.runtime", "setValue"),
      )
    )
  }
}

private fun mutableStateOf(initializer: CodeBlock) =
  CodeBlock.of("%M(%L)", MUTABLE_STATE_OF_FUN, initializer)

private fun remember(initializer: CodeBlock.Builder.() -> Unit) =
  CodeBlock.builder()
    .beginControlFlow("%M", REMEMBER_FUN)
    .apply(initializer)
    .endControlFlow()
    .build()

private fun KSPropertyDeclaration.defaultOr(initialValue: CodeBlock): CodeBlock =
  getter?.let { CodeBlock.of("super.%N", simpleName.getShortName()) }
    ?: initialValue
