package cn.lliiooll.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class FunctionHookEntryItemProcessor(
        private val codeGenerator: CodeGenerator,
        private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("io.github.qauxv.base.annotation.FunctionHookEntry")
            .filterIsInstance<KSClassDeclaration>()
            .toList()
        if (symbols.isEmpty()) {
            return emptyList()
        }

        logger.info("FunctionHookEntryProcessor start.")
        val simpleNameMap = HashMap<String, String>(symbols.size)
        val array = ClassName("kotlin", "Array")
        val hook = ClassName("io.github.qauxv.base", "IDynamicHook")
        val arrayOfHook = array.parameterizedBy(hook)
        val mGetApi = FunSpec.builder("getAnnotatedFunctionHookEntryList").run {
            returns(arrayOfHook)
            addCode(CodeBlock.Builder().run {
                add("return arrayOf(«")
                symbols.forEachIndexed { index, ksClassDeclaration ->
                    if (simpleNameMap.contains(ksClassDeclaration.simpleName.asString())) {
                        logger.error("Duplicate name in FunctionHookEntry's simpleName: " +
                            "${ksClassDeclaration.qualifiedName?.asString() ?: "null"}," +
                            " ${simpleNameMap[ksClassDeclaration.simpleName.asString()]}")
                    } else {
                        simpleNameMap[ksClassDeclaration.simpleName.asString()] =
                                ksClassDeclaration.qualifiedName?.asString() ?: "null"
                    }
                    val isJava = ksClassDeclaration.containingFile?.filePath?.endsWith(".java") == true
                    // logger.warn("Processing >>> $ksClassDeclaration,isJava = $isJava")
                    val typeName = ksClassDeclaration.toClassName()
                    val format = StringBuilder("\n%T").run {
                        if (isJava) append(".INSTANCE")
                        if (index == symbols.lastIndex) append("\n") else append(",")
                        toString()
                    }
                    add(format, typeName)
                }
                add("»)")
                build()
            })
            build()
        }
        logger.info("FunctionHookEntryProcessor count = " + symbols.size + ".")
        // @file:JvmName("AnnotatedFunctionItemList")
        val annotationSpec = AnnotationSpec.builder(JvmName::class).run {
            addMember("%S", "AnnotatedFunctionHookEntryList")
            build()
        }
        val dependencies = Dependencies(true, *Array(symbols.size) {
            symbols[it].containingFile!!
        })
        FileSpec.builder("io.github.qauxv.gen", "AnnotatedFunctionHookEntryList")
                .addAnnotation(annotationSpec)
                .addFunction(mGetApi)
                .build()
                .writeTo(codeGenerator, dependencies)
        return emptyList()
    }
}

class FunctionItemProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FunctionHookEntryItemProcessor(environment.codeGenerator, environment.logger)
    }
}
