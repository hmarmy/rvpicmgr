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
package org.apache.commons.imaging.formats.tiff.fieldtypes;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.common.BinaryConversions;
import org.apache.commons.imaging.common.ByteOrder;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.util.Debug;

public class FieldTypeShort extends FieldType {
    public FieldTypeShort(int type, String name) {
        super(type, 2, name);
    }

    @Override
    public Object getSimpleValue(TiffField entry) throws ImageReadException {
        if (entry.length == 1) {
            return BinaryConversions.toShort(entry.valueOffsetBytes,
                    entry.byteOrder);
        }

        return BinaryConversions.toShorts(getRawBytes(entry),
                entry.byteOrder);
    }

    @Override
    public byte[] writeData(Object o, ByteOrder byteOrder) throws ImageWriteException {
        if (o instanceof Short) {
            return BinaryConversions.toBytes(
                    ((Short)o).shortValue(), byteOrder);
        } else if (o instanceof short[]) {
            short numbers[] = (short[]) o;
            return BinaryConversions.toBytes(numbers, byteOrder);
        } else if (o instanceof Short[]) {
            Short numbers[] = (Short[]) o;
            short values[] = new short[numbers.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = numbers[i].shortValue();
            }
            return BinaryConversions.toBytes(values, byteOrder);
        } else {
            throw new ImageWriteException("Invalid data: " + o + " ("
                    + Debug.getType(o) + ")");
        }
    }

}
