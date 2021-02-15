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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

private val COMPOSABLE_ANNOTATION = ClassName("androidx.compose.runtime", "Composable")
private val STABLE_ANNOTATION = ClassName("androidx.compose.runtime", "Stable")
private val REMEMBER_FUN = MemberName("androidx.compose.runtime", "remember")
private val REMEMBER_SAVEABLE_FUN =
  MemberName("androidx.compose.runtime.saveable", "rememberSaveable")
private val MAP_OF_FUN = MemberName("kotlin.collections", "mapOf")
private val MUTABLE_STATE_OF_FUN = MemberName("androidx.compose.runtime", "mutableStateOf")
private val UNIT_NAME = Unit::class.asClassName()
private val SAVER_INTERFACE = ClassName("androidx.compose.runtime.saveable", "Saver")

fun generateRememberFunction(
  model: ModelInterface,
  builderInterfaceSpec: BuilderInterfaceSpec,
  implSpec: ModelImplSpec,
  converter: TypeConverter
): FunSpec {
  with(converter) {
    val interfaceName = ClassName(model.packageName, model.simpleName)

    val updateLambdaType = LambdaTypeName.get(
      receiver = ClassName(model.packageName, builderInterfaceSpec.spec.name!!),
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
      // This must be the last parameter trailing lambda syntax.
      .addParameter(updateParam)
      .returns(interfaceName)
      .addCode(
        CodeBlock.builder()
          .add("return %L",
            rememberMaybeSaveable(implSpec.saverName) {
              add(
                generateImplConstructorCall(
                  implSpec.name,
                  requiredParams.associate { it.name to CodeBlock.of("%N", it.name) })
              )
              // add("%N(", implSpec.spec)
              // requiredParams.forEach { add("%N, ", it.name) }
              // add(")\n")
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
  implClassName: ClassName,
  config: Config,
  converter: TypeConverter
): ModelImplSpec {
  with(converter) {
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

    val saverSpec = if (config.saveable) {
      generateSaver(implClassName, constructorParamsByName.values.toList(), properties)
        .also(implBuilder::addType)
    } else null

    return ModelImplSpec(
      implBuilder.build(),
      name = implClassName,
      saverSpec = saverSpec,
      saverName = saverSpec?.name?.let(implClassName::nestedClass),
      imports = listOf(
        MemberName("androidx.compose.runtime", "getValue"),
        MemberName("androidx.compose.runtime", "setValue"),
      )
    )
  }
}

private fun generateSaver(
  implType: TypeName,
  implConstructorParams: List<ParameterSpec>,
  properties: List<PropertySpec>
): TypeSpec {
  val savedType = Map::class.parameterizedBy(String::class, Any::class)

  val saveFunction = FunSpec.builder("save")
    .addModifiers(OVERRIDE)
    .receiver(ClassName("androidx.compose.runtime.saveable", "SaverScope"))
    .addParameter(ParameterSpec("value", implType))
    .returns(savedType.copy(nullable = true))
    .addCode(
      "return %L", stringMapOf(
        properties.map { it.name to CodeBlock.of("value.%N", it.name) }
      )
    )
    .build()

  val constructorParamNames = implConstructorParams.mapTo(mutableSetOf()) { it.name }
  val nonConstructorProperties = properties.filterNot { it.name in constructorParamNames }
  val restoreConstructor = generateImplConstructorCall(implType,
    implConstructorParams.associate {
      it.name to CodeBlock.of("value.getValue(%S)·as·%T", it.name, it.type)
    })
  val restoreInitializers = nonConstructorProperties.map {
    CodeBlock.of("%1N·=·value.getValue(%1S)·as·%2T", it.name, it.type)
  }

  val restoreFunction = FunSpec.builder("restore")
    .addModifiers(OVERRIDE)
    .addParameter(ParameterSpec("value", savedType))
    .returns(implType.copy(nullable = true))
    .addCode(
      CodeBlock.builder()
        .add("return %L", restoreConstructor)
        .apply {
          if (restoreInitializers.isNotEmpty()) {
            beginControlFlow(".apply")
            restoreInitializers.forEach {
              addStatement("%L", it)
            }
            endControlFlow()
          }
        }
        .build()
    )
    .build()

  return TypeSpec.objectBuilder("Saver")
    .addSuperinterface(SAVER_INTERFACE.parameterizedBy(implType, savedType))
    // .primaryConstructor(
    //   FunSpec.constructorBuilder()
    //     .addParameter("propertySaver", propertySaverType)
    //     .build()
    // )
    // .addProperty(
    //   PropertySpec.builder("propertySaver", propertySaverType)
    //     .initializer("propertySaver")
    //     .build()
    // )
    .addFunction(saveFunction)
    .addFunction(restoreFunction)
    .build()
}

private fun mutableStateOf(initializer: CodeBlock) =
  CodeBlock.of("%M(%L)", MUTABLE_STATE_OF_FUN, initializer)

private fun rememberMaybeSaveable(
  saverName: TypeName?,
  initializer: CodeBlock.Builder.() -> Unit
): CodeBlock {
  val rememberCall =
    saverName?.let { CodeBlock.of("%M(saver = %T)", REMEMBER_SAVEABLE_FUN, it) }
      ?: CodeBlock.of("%M", REMEMBER_FUN)
  return CodeBlock.builder()
    .beginControlFlow("%L", rememberCall)
    .apply(initializer)
    .endControlFlow()
    .build()
}

private fun stringMapOf(entries: Iterable<Pair<String, CodeBlock>>) =
  CodeBlock.builder()
    .addStatement("%M(", MAP_OF_FUN)
    .apply {
      entries.forEach { (name, value) ->
        addStatement("%S·to·%L,", name, value)
      }
    }
    .add(")")
    .build()

private fun generateImplConstructorCall(
  implName: TypeName,
  propertyValues: Map<String, CodeBlock>
): CodeBlock = CodeBlock.builder()
  .addStatement("%T(", implName)
  .apply {
    propertyValues.forEach { (name, value) ->
      addStatement("%N·=·%L,", name, value)
    }
  }
  .addStatement(")")
  .build()

private fun KSPropertyDeclaration.defaultOr(initialValue: CodeBlock): CodeBlock =
  getter?.let { CodeBlock.of("super.%N", simpleName.getShortName()) }
    ?: initialValue

/*

    object Saver : androidx.compose.runtime.saveable.Saver<ViewModelImpl, Map<String,Any>>{
      override fun SaverScope.save(value: ViewModelImpl): Map<String, Any>? {
        return mapOf(
          "name" to value.name,
          "address" to value.address,
          "edits" to value.edits,
        )
      }

      override fun restore(value: Map<String, Any>): ViewModelImpl? {
        return ViewModelImpl(
          name = value.getValue("name") as String,
          address = value.getValue("address") as Address
        ).apply {
          edits = value.getValue("edits") as Int
        }
      }
    }
 */