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

#ifndef ATS_JNI_MACROS_H
#define ATS_JNI_MACROS_H

#include <jni.h>

#define MAKE_JNI_METHOD(_name, _sig, _func) (JNINativeMethod) { \
                                                .name = _name,  \
                                                .signature = _sig, \
                                                .fnPtr = reinterpret_cast<void *>(&_func) \
                                            }

#endif //ATS_JNI_MACROS_H