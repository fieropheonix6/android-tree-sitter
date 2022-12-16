/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/\>.
 */

#ifndef ANDROIDTREESITTER_UTF16STRING_H
#define ANDROIDTREESITTER_UTF16STRING_H

#include <jni.h>
#include <vector>

using namespace std;

/**
 * Provides access to <code>std::string</code> to Java classes.
 */
class UTF16String {

private:
    vector<jbyte> _string;

public:
    UTF16String();

    /**
     * Get the Java 'char' at the given index.
     * @param index The index of the character to retrive.
     * @return The Java character.
     */
    jchar char_at(int index);

    /**
     * Insert the Java 'char' at the given index.
     * @param c The Java character to insert.
     * @param index The index to insert at.
     * @return Returns this instance.
     */
    UTF16String *insert(jchar c, int index);

    /**
     * Appends the given Java character.
     * @param c The character to append.
     */
    void append(jchar c);

    /**
     * Appends the given source jstring to this UTF16String.
     * @param src The jstring to append.
     * @return Returns this instance.
     */
    UTF16String *append(JNIEnv *env, jstring src);

    /**
     * Appends the given part of the source jstring to this UTF16String.
     * @param src The jstring to append.
     * @return Returns this instance.
     */
    UTF16String *append(JNIEnv *env, jstring src, jint from, jint len);

    /**
     * Insert the given string at the given index.
     * @param env The JNI environment.
     * @param src The source string to insert.
     * @param index The index to insert at.
     * @return Returns this instance.
     */
    UTF16String *insert(JNIEnv *env, jstring src, jint index);

    /**
     * Deletes the range of characters from this string. The indices must be Java char-based indices.
     *
     * @param env The JNI environment.
     * @param start The start index to delete from.
     * @param end The end index to delete to.
     * @return Returns this instance.
     */
    UTF16String *delete_chars(JNIEnv *env, jint start, jint end);

    /**
     * Deletes the range of characters from this string. The indices must be byte-based indices.
     *
     * @param env The JNI environment.
     * @param start The start index to delete from.
     * @param end The end index to delete to.
     * @return Returns this instance.
     */
    UTF16String *delete_bytes(JNIEnv *env, jint start, jint end);

    /**
     * @return The length (char-based) of this string.
     */
    jint length();

    /**
     * @return The byte-based length of this string.
     */
    jint byte_length();

    /**
     * Returns this string as a C-style string.
     *
     * @return This string as C-style string. It is the responsibility of the caller to call
     *         <code>delete [] chars</code> on the returned pointer.
     */
    const char *to_cstring();

    /**
     * @return This string as jstring.
     */
    jstring to_jstring(JNIEnv *env);

    bool operator==(const UTF16String &rhs) const;

    bool operator!=(const UTF16String &rhs) const;
};

UTF16String *as_str(jlong pointer);

int vsize(const vector<jbyte> &vc);

#endif //ANDROIDTREESITTER_UTF16STRING_H