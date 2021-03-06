/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.imaging.formats.tiff.taginfos;

import org.apache.commons.imaging.common.BinaryConversions;
import org.apache.commons.imaging.common.ByteOrder;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;


public class TagInfoLong extends TagInfo {
    public TagInfoLong(String name, int tag, int length, TiffDirectoryType directoryType) {
        super(name, tag, FIELD_TYPE_LONG, length, directoryType);
    }
    
    public TagInfoLong(String name, int tag, int length, TiffDirectoryType directoryType, boolean isOffset) {
        super(name, tag, FIELD_TYPE_LONG, length, directoryType, isOffset);
    }
    
    public int[] getValue(ByteOrder byteOrder, byte[] bytes) {
        return BinaryConversions.toInts(bytes, byteOrder);
    }
    
    public byte[] encodeValue(ByteOrder byteOrder, int... values) {
        return BinaryConversions.toBytes(values, byteOrder);
    }
}
