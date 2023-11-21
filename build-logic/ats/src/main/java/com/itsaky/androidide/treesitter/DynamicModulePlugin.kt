/*
 *  This file is part of android-tree-sitter.
 *
 *  android-tree-sitter library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  android-tree-sitter library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *  along with android-tree-sitter.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.treesitter

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import java.io.File
import java.io.FileNotFoundException

/**
 * Dynamically creates and adds the grammar project modules to the build.
 *
 * @author Akash Yadav
 */
@Suppress("unused")
class DynamicModulePlugin : Plugin<Settings> {

  override fun apply(target: Settings) {
    target.run {
      val rootDir = target.rootDir
      val grammars = target.readGrammars()

      for (grammar in grammars) {
        val moduleDir = rootDir.resolve("grammar-modules/${grammar.name}")
          .also { it.mkdirs() }
        generateGrammarModule(grammar, moduleDir, rootDir)

        val moduleName = "tree-sitter-${grammar.name}"
        include(moduleName)
        project(":${moduleName}").projectDir = moduleDir
      }
    }
  }

  private fun generateGrammarModule(grammar: TsGrammar, moduleDir: File,
                                    rootDir: File
  ) {
    println("Generating module for grammar '${grammar.name}'")
    writeBuildGradle(moduleDir)
    writeLangBinding(grammar, moduleDir)
    writeNative(grammar, moduleDir, rootDir)
  }

  private fun writeNative(grammar: TsGrammar, moduleDir: File, rootDir: File
  ) {
    val cppDir = File(moduleDir, "src/main/cpp/").also { it.mkdirs() }
    writeCmakeLists(grammar, cppDir, rootDir)
    writeJniImpl(grammar, cppDir)

    File(cppDir, ".gitignore").writeText("/host-build")
  }

  private fun writeJniImpl(grammar: TsGrammar, cppDir: File) {
    val file = File(cppDir, "tree-sitter-${grammar.name}.cpp")
    file.writeText(grammar.jniImplSrc().autoGenerated())
  }

  private fun writeCmakeLists(grammar: TsGrammar, cppDir: File, rootDir: File
  ) {
    val file = File(cppDir, "CMakeLists.txt")
    file.writeText(grammar.cmakeListsSrc(rootDir).autoGenerated(comment = "##"))
  }

  private fun writeLangBinding(grammar: TsGrammar, moduleDir: File) {
    val file = File(moduleDir,
      "src/main/java/com/itsaky/androidide/treesitter/${grammar.name}/TSLanguage${grammar.capitalizedName()}.java")
    file.parentFile.mkdirs()
    file.writeText(grammar.langBindingSrc().autoGenerated())
  }

  private fun writeBuildGradle(moduleDir: File) {
    val file = File(moduleDir, "build.gradle")
    file.writeText(buildGradleSrc().autoGenerated())
  }
}

private fun TsGrammar.jniImplSrc(): String = """
$LICENSE

#include <jni.h>
#include <tree_sitter/api.h>

#include "ts__onload.h"
#include "ts_${name}.h"

#ifdef __ANDROID__
static jint JNI_VERSION = JNI_VERSION_1_6;
#else
static jint JNI_VERSION = JNI_VERSION_10;
#endif

extern "C" TSLanguage *tree_sitter_${name}();

static jlong TSLanguage${capitalizedName()}_getInstance(JNIEnv *env, jclass clazz) {
    return (jlong) tree_sitter_${name}();
}

void TSLanguage${capitalizedName()}_Native__SetJniMethods(JNINativeMethod *methods, int count) {
  SET_JNI_METHOD(methods, TSLanguage${capitalizedName()}_Native_getInstance, TSLanguage${capitalizedName()}_getInstance);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {

  JNIEnv *env;
  if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION) != JNI_OK) {
    LOGE("AndroidTreeSitter", "Failed to get JNIEnv* from JavaVM: %p", vm);
    return JNI_ERR;
  }

  TS_JNI_ONLOAD__DEFINE_METHODS_ARR
  TS_JNI_ONLOAD__AUTO_REGISTER(env)

  return JNI_VERSION;
}

""".trimIndent()

private fun TsGrammar.cmakeListsSrc(rootDir: File,
                                    grammarDir: File = File(rootDir,
                                      "grammars/$name")
): String {
  if (!grammarDir.exists()) {
    throw FileNotFoundException(
      "Source directory for grammar '$name' not found")
  }

  val sources = srcExtra.joinToString(
    separator = "\n        ") { "${grammarDir.absolutePath}/${it}" }
  return """
$CMAKE_LICENSE

cmake_minimum_required(VERSION 3.22.1)

project("tree-sitter-$name")

# Set the root project directory
set(PROJECT_DIR ${rootDir.absolutePath})

# Include common configuration
include(${rootDir.absolutePath}/cmake/common-config.cmake)

# This includes the header file for the parser
include_directories(${grammarDir.absolutePath}/src)

# add tree-sitter-java library
add_library(${'$'}{CMAKE_PROJECT_NAME} SHARED
        ${grammarDir.absolutePath}/src/parser.c
        $sources
        tree-sitter-${name}.cpp)
        
if (${'$'}{CMAKE_SYSTEM_NAME} STREQUAL Android)
    # Find the log library and link it to tree-sitter-$name
    find_library(log log)
    target_link_libraries(tree-sitter-$name ${'$'}{log})
endif()
""".trimIndent()
}

private fun TsGrammar.langBindingSrc(): String = """
$LICENSE

package com.itsaky.androidide.treesitter.$name;

import com.itsaky.androidide.treesitter.annotations.GenerateNativeHeaders;

import com.itsaky.androidide.treesitter.TSLanguage;
import com.itsaky.androidide.treesitter.TSLanguageCache;

/**
 * Tree Sitter for ${capitalizedName()}.
 *
 * @author Akash Yadav
 */
public final class TSLanguage${capitalizedName()} {

  static {
    System.loadLibrary("tree-sitter-$name");
  }

  private TSLanguage${capitalizedName()}() {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Tree Sitter language instances are <code>static const</code> and hence, they do not change. The
   * name of this method is misleading, use {@link TSLanguage${capitalizedName()}#getInstance()} instead.
   */
  @Deprecated
  public static TSLanguage newInstance() {
    return getInstance();
  }
  
  /**
   * Get the instance of the ${capitalizedName()} language.
   *
   * @return The instance of the ${capitalizedName()} language.
   */
  public static TSLanguage getInstance() {
    var language = TSLanguageCache.get("$name");
    if (language != null) {
      return language;
    }

    language = TSLanguage.create("$name", Native.getInstance());
    TSLanguageCache.cache("$name", language);
    return language;
  }

  @GenerateNativeHeaders(fileName = "$name")
  public static class Native {
    public static native long getInstance();
  }
}
""".trimIndent()

private fun buildGradleSrc(): String = """
$LICENSE

plugins {
  id("com.android.library")
  id("com.vanniktech.maven.publish.base")
  id("android-tree-sitter.ts")
  id("android-tree-sitter.ts-grammar")
}

dependencies {
  implementation(projects.annotations)
  annotationProcessor(projects.annotationProcessors)
}
""".trimIndent()

private fun String.autoGenerated(comment: String = "//"): String {
  return "$comment This file is automatically generated. DO NOT EDIT!\n\n$this"
}

private const val LICENSE = """
/*
 *  This file is part of android-tree-sitter.
 *
 *  android-tree-sitter library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  android-tree-sitter library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *  along with android-tree-sitter.  If not, see <https://www.gnu.org/licenses/>.
 */
"""

private const val CMAKE_LICENSE = """#
#  This file is part of android-tree-sitter.
#
#  android-tree-sitter library is free software; you can redistribute it and/or
#  modify it under the terms of the GNU Lesser General Public
#  License as published by the Free Software Foundation; either
#  version 2.1 of the License, or (at your option) any later version.
#
#  android-tree-sitter library is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
#  Lesser General Public License for more details.
#
#   You should have received a copy of the GNU General Public License
#  along with android-tree-sitter.  If not, see <https://www.gnu.org/licenses/>.
#"""

internal fun Project.readGrammars() = readGrammars(rootProject.rootDir)

internal fun Settings.readGrammars() = readGrammars(rootDir)

internal fun readGrammars(rootDir: File): List<TsGrammar> {
  val grammarsFile = rootDir.resolve("grammars/grammars.json")
  if (!grammarsFile.exists()) {
    throw FileNotFoundException("grammars.json file does not exist")
  }
  val type =
    TypeToken.getParameterized(List::class.java, TsGrammar::class.java)

  @Suppress("UnnecessaryVariable")
  val grammars: List<TsGrammar> =
    Gson().fromJson(grammarsFile.bufferedReader(), type.type)
  return grammars
}