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
package org.apache.commons.imaging.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.apache.commons.imaging.ImageReadException;

public class BinaryInputStream extends InputStream {
    protected boolean debug = false;

    public final void setDebug(boolean b) {
        debug = b;
    }

    public final boolean getDebug() {
        return debug;
    }

    private final InputStream is;

    public BinaryInputStream(byte bytes[], ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
        this.is = new ByteArrayInputStream(bytes);
    }

    public BinaryInputStream(InputStream is, ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
        this.is = is;
    }

    public BinaryInputStream(InputStream is) {
        this.is = is;
    }

    // default byte order for Java, many file formats.
    private ByteOrder byteOrder = ByteOrder.NETWORK;

    protected void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    protected ByteOrder getByteOrder() {
        return byteOrder;
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    protected final int convertByteArrayToInt(String name, byte bytes[]) {
        return convertByteArrayToInt(name, bytes, byteOrder);
    }

    public final int convertByteArrayToShort(String name, byte bytes[]) {
        return convertByteArrayToShort(name, bytes, byteOrder);
    }

    public final int convertByteArrayToShort(String name, int start,
            byte bytes[]) {
        return convertByteArrayToShort(name, start, bytes, byteOrder);
    }

    public final int read4Bytes(String name, String exception)
            throws IOException {
        return read4Bytes(name, exception, byteOrder);
    }

    public final int read3Bytes(String name, String exception)
            throws IOException {
        return read3Bytes(name, exception, byteOrder);
    }

    public final int read2Bytes(String name, String exception)
            throws IOException {
        return read2Bytes(name, exception, byteOrder);
    }

    protected final void readRandomBytes() throws IOException {

        for (int counter = 0; counter < 100; counter++) {
            readByte("" + counter, "Random Data");
        }
    }

    public final void debugNumber(String msg, int data) {
        debugNumber(msg, data, 1);
    }

    public final void debugNumber(String msg, int data, int bytes) {
        System.out.print(msg + ": " + data + " (");
        int byteData = data;
        for (int i = 0; i < bytes; i++) {
            if (i > 0) {
                System.out.print(",");
            }
            int singleByte = 0xff & byteData;
            System.out.print((char) singleByte + " [" + singleByte + "]");
            byteData >>= 8;
        }
        System.out.println(") [0x" + Integer.toHexString(data) + ", "
                + Integer.toBinaryString(data) + "]");
    }

    public final void readAndVerifyBytes(byte expected[], String exception)
            throws ImageReadException, IOException {
        for (int i = 0; i < expected.length; i++) {
            int data = is.read();
            byte b = (byte) (0xff & data);

            if ((data < 0) || (b != expected[i])) {
                System.out.println("i" + ": " + i);

                this.debugByteArray("expected", expected);
                debugNumber("data[" + i + "]", b);
                // debugNumber("expected[" + i + "]", expected[i]);

                throw new ImageReadException(exception);
            }
        }
    }

    protected final void readAndVerifyBytes(String name, byte expected[],
            String exception) throws ImageReadException, IOException {
        byte bytes[] = readByteArray(name, expected.length, exception);

        for (int i = 0; i < expected.length; i++) {
            if (bytes[i] != expected[i]) {
                System.out.println("i" + ": " + i);
                debugNumber("bytes[" + i + "]", bytes[i]);
                debugNumber("expected[" + i + "]", expected[i]);

                throw new ImageReadException(exception);
            }
        }
    }

    public final void skipBytes(int length, String exception)
            throws IOException {
        long total = 0;
        while (length != total) {
            long skipped = is.skip(length - total);
            if (skipped < 1) {
                throw new IOException(exception + " (" + skipped + ")");
            }
            total += skipped;
        }
    }

    protected final void scanForByte(byte value) throws IOException {
        int count = 0;
        for (int i = 0; count < 3; i++) {
            int b = is.read();
            if (b < 0) {
                return;
            }
            if ((0xff & b) == value) {
                System.out.println("\t" + i + ": match.");
                count++;
            }
        }
    }

    public final byte readByte(String name, String exception)
            throws IOException {
        int result = is.read();

        if ((result < 0)) {
            System.out.println(name + ": " + result);
            throw new IOException(exception);
        }

        if (debug) {
            debugNumber(name, result);
        }

        return (byte) (0xff & result);
    }

    protected final RationalNumber[] convertByteArrayToRationalArray(
            String name, byte bytes[], int start, int length, ByteOrder byteOrder) {
        int expectedLength = start + length * 8;

        if (bytes.length < expectedLength) {
            System.out.println(name + ": expected length: " + expectedLength
                    + ", actual length: " + bytes.length);
            return null;
        }

        RationalNumber result[] = new RationalNumber[length];

        for (int i = 0; i < length; i++) {
            result[i] = convertByteArrayToRational(name, bytes, start + i * 8,
                    byteOrder);
        }

        return result;
    }

    protected final RationalNumber convertByteArrayToRational(String name,
            byte bytes[], ByteOrder byteOrder) {
        return convertByteArrayToRational(name, bytes, 0, byteOrder);
    }

    protected final RationalNumber convertByteArrayToRational(String name,
            byte bytes[], int start, ByteOrder byteOrder) {
        int numerator = convertByteArrayToInt(name, bytes, start + 0, 4,
                byteOrder);
        int divisor = convertByteArrayToInt(name, bytes, start + 4, 4,
                byteOrder);

        return new RationalNumber(numerator, divisor);
    }

    protected final int convertByteArrayToInt(String name, byte bytes[],
            ByteOrder byteOrder) {
        return convertByteArrayToInt(name, bytes, 0, 4, byteOrder);
    }

    protected final int convertByteArrayToInt(String name, byte bytes[],
            int start, int length, ByteOrder byteOrder) {
        byte byte0 = bytes[start + 0];
        byte byte1 = bytes[start + 1];
        byte byte2 = bytes[start + 2];
        byte byte3 = 0;
        if (length == 4) {
            byte3 = bytes[start + 3];
        }

        int result;

        if (byteOrder == ByteOrder.MOTOROLA) {
            result = ((0xff & byte0) << 24) + ((0xff & byte1) << 16)
                    + ((0xff & byte2) << 8) + ((0xff & byte3) << 0);
        } else {
            result = ((0xff & byte3) << 24) + ((0xff & byte2) << 16)
                    + ((0xff & byte1) << 8) + ((0xff & byte0) << 0);
        }

        if (debug) {
            debugNumber(name, result, 4);
        }

        return result;
    }

    protected final int[] convertByteArrayToIntArray(String name, byte bytes[],
            int start, int length, ByteOrder byteOrder) {
        int expectedLength = start + length * 4;

        if (bytes.length < expectedLength) {
            System.out.println(name + ": expected length: " + expectedLength
                    + ", actual length: " + bytes.length);
            return null;
        }

        int result[] = new int[length];

        for (int i = 0; i < length; i++) {
            result[i] = convertByteArrayToInt(name, bytes, start + i * 4, 4,
                    byteOrder);
        }

        return result;
    }

    protected final int convertByteArrayToShort(String name, byte bytes[],
            ByteOrder byteOrder) {
        return convertByteArrayToShort(name, 0, bytes, byteOrder);
    }

    protected final int convertByteArrayToShort(String name, int start,
            byte bytes[], ByteOrder byteOrder) {
        byte byte0 = bytes[start + 0];
        byte byte1 = bytes[start + 1];

        int result;

        if (byteOrder == ByteOrder.MOTOROLA) {
            result = ((0xff & byte0) << 8) + ((0xff & byte1) << 0);
        } else {
            result = ((0xff & byte1) << 8) + ((0xff & byte0) << 0);
        }

        if (debug) {
            debugNumber(name, result, 2);
        }

        return result;
    }

    protected final int[] convertByteArrayToShortArray(String name,
            byte bytes[], int start, int length, ByteOrder byteOrder) {
        int expectedLength = start + length * 2;

        if (bytes.length < expectedLength) {
            System.out.println(name + ": expected length: " + expectedLength
                    + ", actual length: " + bytes.length);
            return null;
        }

        int result[] = new int[length];

        for (int i = 0; i < length; i++) {
            result[i] = convertByteArrayToShort(name, start + i * 2, bytes,
                    byteOrder);

            // byte byte0 = bytes[start + i * 2];
            // byte byte1 = bytes[start + i * 2 + 1];
            // result[i] = convertBytesToShort(name, byte0, byte1, byteOrder);
        }

        return result;
    }

    public final byte[] readByteArray(String name, int length, String exception)
            throws IOException {
        byte result[] = new byte[length];

        int read = 0;
        while (read < length) {
            int count = is.read(result, read, length - read);
            if (count < 1) {
                throw new IOException(exception);
            }

            read += count;
        }

        if (debug) {
            for (int i = 0; ((i < length) && (i < 150)); i++) {
                debugNumber(name + " (" + i + ")", 0xff & result[i]);
            }
        }
        return result;
    }

    protected final void debugByteArray(String name, byte bytes[]) {
        System.out.println(name + ": " + bytes.length);

        for (int i = 0; ((i < bytes.length) && (i < 50)); i++) {
            debugNumber(name + " (" + i + ")", bytes[i]);
        }
    }

    protected final void debugNumberArray(String name, int numbers[], int length) {
        System.out.println(name + ": " + numbers.length);

        for (int i = 0; ((i < numbers.length) && (i < 50)); i++) {
            debugNumber(name + " (" + i + ")", numbers[i], length);
        }
    }

    public final byte[] readBytearray(String name, byte bytes[], int start,
            int count) {
        if (bytes.length < (start + count)) {
            return null;
        }

        byte result[] = new byte[count];
        System.arraycopy(bytes, start, result, 0, count);

        if (debug) {
            debugByteArray(name, result);
        }

        return result;
    }

    public final byte[] readByteArray(int length, String error)
            throws ImageReadException, IOException {
        boolean verbose = false;
        boolean strict = true;
        return readByteArray(length, error, verbose, strict);
    }

    public final byte[] readByteArray(int length, String error,
            boolean verbose, boolean strict) throws ImageReadException,
            IOException {
        byte bytes[] = new byte[length];
        int total = 0;
        int read;
        while ((read = read(bytes, total, length - total)) > 0) {
            total += read;
        }
        if (total < length) {
            if (strict) {
                throw new ImageReadException(error);
            } else if (verbose) {
                System.out.println(error);
            }
            return null;
        }
        return bytes;
    }

    protected final byte[] getBytearrayTail(String name, byte bytes[], int count) {
        return readBytearray(name, bytes, count, bytes.length - count);
    }

    protected final byte[] getBytearrayHead(String name, byte bytes[], int count) {
        return readBytearray(name, bytes, 0, bytes.length - count);
    }

    public final boolean compareByteArrays(byte a[], int aStart, byte b[],
            int bStart, int length) {
        if (a.length < (aStart + length)) {
            return false;
        }
        if (b.length < (bStart + length)) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (a[aStart + i] != b[bStart + i]) {
                debugNumber("a[" + (aStart + i) + "]", a[aStart + i]);
                debugNumber("b[" + (bStart + i) + "]", b[bStart + i]);

                return false;
            }
        }

        return true;
    }

    protected final int read4Bytes(String name, String exception, ByteOrder byteOrder)
            throws IOException {
        int size = 4;
        byte bytes[] = new byte[size];

        int read = 0;
        while (read < size) {
            int count = is.read(bytes, read, size - read);
            if (count < 1) {
                throw new IOException(exception);
            }

            read += count;
        }

        return convertByteArrayToInt(name, bytes, byteOrder);
    }

    protected final int read3Bytes(String name, String exception, ByteOrder byteOrder)
            throws IOException {
        int size = 3;
        byte bytes[] = new byte[size];

        int read = 0;
        while (read < size) {
            int count = is.read(bytes, read, size - read);
            if (count < 1) {
                throw new IOException(exception);
            }

            read += count;
        }

        return convertByteArrayToInt(name, bytes, 0, 3, byteOrder);

    }

    protected final int read2Bytes(String name, String exception, ByteOrder byteOrder)
            throws IOException {
        int size = 2;
        byte bytes[] = new byte[size];

        int read = 0;
        while (read < size) {
            int count = is.read(bytes, read, size - read);
            if (count < 1) {
                throw new IOException(exception);
            }

            read += count;
        }

        return convertByteArrayToShort(name, bytes, byteOrder);
    }

    public final int read1ByteInteger(String exception)
            throws ImageReadException, IOException {
        int byte0 = is.read();
        if (byte0 < 0) {
            throw new ImageReadException(exception);
        }

        return 0xff & byte0;
    }

    public final int read2ByteInteger(String exception)
            throws ImageReadException, IOException {
        int byte0 = is.read();
        int byte1 = is.read();
        if (byte0 < 0 || byte1 < 0) {
            throw new ImageReadException(exception);
        }

        if (byteOrder == ByteOrder.MOTOROLA) {
            return ((0xff & byte0) << 8) + ((0xff & byte1) << 0);
        } else {
            return ((0xff & byte1) << 8) + ((0xff & byte0) << 0);
        }
    }

    public final int read4ByteInteger(String exception)
            throws ImageReadException, IOException {
        int byte0 = is.read();
        int byte1 = is.read();
        int byte2 = is.read();
        int byte3 = is.read();
        if (byte0 < 0 || byte1 < 0 || byte2 < 0 || byte3 < 0) {
            throw new ImageReadException(exception);
        }

        if (byteOrder == ByteOrder.MOTOROLA) {
            return ((0xff & byte0) << 24) + ((0xff & byte1) << 16)
                    + ((0xff & byte2) << 8) + ((0xff & byte3) << 0);
        } else {
            return ((0xff & byte3) << 24) + ((0xff & byte2) << 16)
                    + ((0xff & byte1) << 8) + ((0xff & byte0) << 0);
        }
    }

    protected final void printCharQuad(String msg, int i) {
        System.out.println(msg + ": '" + (char) (0xff & (i >> 24))
                + (char) (0xff & (i >> 16)) + (char) (0xff & (i >> 8))
                + (char) (0xff & (i >> 0)) + "'");
    }

    protected final void printByteBits(String msg, byte i) {
        System.out.println(msg + ": '" + Integer.toBinaryString(0xff & i));
    }

    protected final static int charsToQuad(char c1, char c2, char c3, char c4) {
        return (((0xff & c1) << 24) | ((0xff & c2) << 16) | ((0xff & c3) << 8) | ((0xff & c4) << 0));
    }

    public final int findNull(byte src[]) {
        return findNull(src, 0);
    }

    public final int findNull(byte src[], int start) {
        for (int i = start; i < src.length; i++) {
            if (src[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    protected final byte[] getRAFBytes(RandomAccessFile raf, long pos,
            int length, String exception) throws IOException {
        byte result[] = new byte[length];

        if (debug) {
            System.out.println("getRAFBytes pos" + ": " + pos);
            System.out.println("getRAFBytes length" + ": " + length);
        }

        raf.seek(pos);

        int read = 0;
        while (read < length) {
            int count = raf.read(result, read, length - read);
            if (count < 1) {
                throw new IOException(exception);
            }

            read += count;
        }

        return result;

    }

    protected void skipBytes(int length) throws IOException {
        skipBytes(length, "Couldn't skip bytes");
    }

}
