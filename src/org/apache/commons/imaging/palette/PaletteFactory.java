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
package org.apache.commons.imaging.palette;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.imaging.ImageWriteException;

/**
 * Factory for creating palettes.
 */
public class PaletteFactory {
    private static final boolean debug = false;

    /**
     * Builds an exact complete opaque palette containing all the colors in {@code src},
     * using an algorithm that is faster than {@linkplain #makeExactRgbPaletteSimple} for large images
     * but uses 2 mebibytes of working memory. Treats all the colors as opaque.
     * @param src the image whose palette to build
     * @return the palette
     */
    public Palette makeExactRgbPaletteFancy(BufferedImage src) {
        // map what rgb values have been used

        byte rgbmap[] = new byte[256 * 256 * 32];

        int width = src.getWidth();
        int height = src.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = src.getRGB(x, y);
                int rggbb = 0x1fffff & argb;
                int highred = 0x7 & (argb >> 21);
                int mask = 1 << highred;
                rgbmap[rggbb] |= mask;
            }
        }

        int count = 0;
        for (int i = 0; i < rgbmap.length; i++) {
            int eight = 0xff & rgbmap[i];
            count += Integer.bitCount(eight);
        }

        if (debug) {
            System.out.println("Used colors: " + count);
        }

        int colormap[] = new int[count];
        int mapsize = 0;
        for (int i = 0; i < rgbmap.length; i++) {
            int eight = 0xff & rgbmap[i];
            int mask = 0x80;
            for (int j = 0; j < 8; j++) {
                int bit = eight & mask;
                mask >>>= 1;

                if (bit > 0) {
                    int rgb = i | ((7 - j) << 21);

                    colormap[mapsize++] = rgb;
                }
            }
        }

        Arrays.sort(colormap);
        return new SimplePalette(colormap);
    }

    private int pixelToQuantizationTableIndex(int argb, int precision) {
        int result = 0;
        int precision_mask = (1 << precision) - 1;

        for (int i = 0; i < components; i++) {
            int sample = argb & 0xff;
            argb >>= 8;

            sample >>= (8 - precision);
            result = (result << precision) | (sample & precision_mask);
        }

        return result;
    }

    private int getFrequencyTotal(int table[], int mins[], int maxs[],
            int precision) {
        int sum = 0;

        for (int blue = mins[2]; blue <= maxs[2]; blue++) {
            int b = (blue << (2 * precision));
            for (int green = mins[1]; green <= maxs[1]; green++) {
                int g = (green << (1 * precision));
                for (int red = mins[0]; red <= maxs[0]; red++) {
                    int index = b | g | red;

                    sum += table[index];
                }
            }
        }

        return sum;
    }

    private DivisionCandidate finishDivision(int table[],
            ColorSpaceSubset subset, int component, int precision, int sum,
            int slice) {
        if (debug) {
            subset.dump("trying (" + component + "): ");
        }

        int total = subset.total;

        if ((slice < subset.mins[component])
                || (slice >= subset.maxs[component])) {
            return null;
        }

        if ((sum < 1) || (sum >= total)) {
            return null;
        }

        int remainder = total - sum;
        if ((remainder < 1) || (remainder >= total)) {
            return null;
        }

        int slice_mins[] = new int[subset.mins.length];
        System.arraycopy(subset.mins, 0, slice_mins, 0, subset.mins.length);
        int slice_maxs[] = new int[subset.maxs.length];
        System.arraycopy(subset.maxs, 0, slice_maxs, 0, subset.maxs.length);

        slice_maxs[component] = slice;
        slice_mins[component] = slice + 1;

        if (debug) {
            System.out.println("total: " + total);
            System.out.println("first total: " + sum);
            System.out.println("second total: " + (total - sum));
            // System.out.println("start: " + start);
            // System.out.println("end: " + end);
            System.out.println("slice: " + slice);

        }

        ColorSpaceSubset first = new ColorSpaceSubset(sum, precision,
                subset.mins, slice_maxs, table);
        ColorSpaceSubset second = new ColorSpaceSubset(total - sum, precision,
                slice_mins, subset.maxs, table);

        return new DivisionCandidate(subset, first, second);

    }

    private List<DivisionCandidate> divideSubset2(int table[],
            ColorSpaceSubset subset, int component, int precision) {
        if (debug) {
            subset.dump("trying (" + component + "): ");
        }

        int total = subset.total;

        int slice_mins[] = new int[subset.mins.length];
        System.arraycopy(subset.mins, 0, slice_mins, 0, subset.mins.length);
        int slice_maxs[] = new int[subset.maxs.length];
        System.arraycopy(subset.maxs, 0, slice_maxs, 0, subset.maxs.length);

        int sum1 = 0, sum2;
        int slice1, slice2;
        int last = 0;

        for (slice1 = subset.mins[component]; slice1 != subset.maxs[component] + 1; slice1++) {

            slice_mins[component] = slice1;
            slice_maxs[component] = slice1;

            last = getFrequencyTotal(table, slice_mins, slice_maxs,
                    precision);

            sum1 += last;

            if (sum1 >= (total / 2)) {
                break;
            }
        }

        sum2 = sum1 - last;
        slice2 = slice1 - 1;

        DivisionCandidate dc1 = finishDivision(table, subset, component,
                precision, sum1, slice1);
        DivisionCandidate dc2 = finishDivision(table, subset, component,
                precision, sum2, slice2);

        List<DivisionCandidate> result = new ArrayList<DivisionCandidate>();

        if (dc1 != null) {
            result.add(dc1);
        }
        if (dc2 != null) {
            result.add(dc2);
        }

        return result;
    }

    private DivisionCandidate divideSubset2(int table[],
            ColorSpaceSubset subset, int precision) {
        List<DivisionCandidate> dcs = new ArrayList<DivisionCandidate>();

        dcs.addAll(divideSubset2(table, subset, 0, precision));
        dcs.addAll(divideSubset2(table, subset, 1, precision));
        dcs.addAll(divideSubset2(table, subset, 2, precision));

        DivisionCandidate best_v = null;
        double best_score = Double.MAX_VALUE;

        for (int i = 0; i < dcs.size(); i++) {
            DivisionCandidate dc = dcs.get(i);

            ColorSpaceSubset first = dc.dst_a;
            ColorSpaceSubset second = dc.dst_b;
            int area1 = first.total;
            int area2 = second.total;

            int diff = Math.abs(area1 - area2);
            double score = ((double) diff) / ((double) Math.max(area1, area2));

            if (best_v == null) {
                best_v = dc;
                best_score = score;
            } else if (score < best_score) {
                best_v = dc;
                best_score = score;
            }

        }

        return best_v;
    }

    public static final int components = 3; // in bits

    private static class DivisionCandidate {
        // private final ColorSpaceSubset src;
        private final ColorSpaceSubset dst_a, dst_b;

        public DivisionCandidate(ColorSpaceSubset src, ColorSpaceSubset dst_a,
                ColorSpaceSubset dst_b) {
            // this.src = src;
            this.dst_a = dst_a;
            this.dst_b = dst_b;
        }
    }

    private List<ColorSpaceSubset> divide(List<ColorSpaceSubset> v,
            int desired_count, int table[], int precision) {
        List<ColorSpaceSubset> ignore = new ArrayList<ColorSpaceSubset>();

        int count = 0;
        while (true) {
            count++;

            if (debug) {
                System.out.println("cycle(" + count + "): " + v.size()
                        + " done");
            }

            int max_area = -1;
            ColorSpaceSubset max_subset = null;

            for (int i = 0; i < v.size(); i++) {
                ColorSpaceSubset subset = v.get(i);
                if (ignore.contains(subset)) {
                    continue;
                }
                int area = subset.total;

                if (max_subset == null) {
                    max_subset = subset;
                    max_area = area;
                } else if (area > max_area) {
                    max_subset = subset;
                    max_area = area;
                }
            }

            if (max_subset == null) {
                return v;
            }
            if (debug) {
                System.out.println("\t" + "area: " + max_area);
            }

            DivisionCandidate dc = divideSubset2(table, max_subset,
                    precision);
            if (dc != null) {
                v.remove(max_subset);
                v.add(dc.dst_a);
                v.add(dc.dst_b);
            } else {
                ignore.add(max_subset);
            }

            if (v.size() == desired_count) {
                return v;
            }
        }
    }

    /**
     * Builds an inexact opaque palette of at most {@code max} colors in {@code src}
     * using a variation of the Median Cut algorithm. Accurate to 6 bits per component,
     * and works by splitting the color bounding box most heavily populated by colors
     * along the component which splits the colors most evenly.
     * @param src the image whose palette to build
     * @param max the maximum number of colors the palette can contain
     * @return the palette of at most {@code max} colors
     */
    public Palette makeQuantizedRgbPalette(BufferedImage src, int max) {
        int precision = 6; // in bits

        int table_scale = precision * components;
        int table_size = 1 << table_scale;
        int table[] = new int[table_size];

        int width = src.getWidth();
        int height = src.getHeight();

        List<ColorSpaceSubset> subsets = new ArrayList<ColorSpaceSubset>();
        ColorSpaceSubset all = new ColorSpaceSubset(width * height, precision);
        subsets.add(all);

        if (debug) {
            int pre_total = getFrequencyTotal(table, all.mins, all.maxs, precision);
            System.out.println("pre total: " + pre_total);
        }

        // step 1: count frequency of colors
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = src.getRGB(x, y);

                int index = pixelToQuantizationTableIndex(argb, precision);

                table[index]++;
            }
        }

        if (debug) {
            int all_total = getFrequencyTotal(table, all.mins, all.maxs, precision);
            System.out.println("all total: " + all_total);
            System.out.println("width * height: " + (width * height));
        }

        subsets = divide(subsets, max, table, precision);

        if (debug) {
            System.out.println("subsets: " + subsets.size());
            System.out.println("width*height: " + width * height);
        }

        for (int i = 0; i < subsets.size(); i++) {
            ColorSpaceSubset subset = subsets.get(i);

            subset.setAverageRGB(table);

            if (debug) {
                subset.dump(i + ": ");
            }
        }

        Collections.sort(subsets, ColorSpaceSubset.rgbComparator);

        return new QuantizedPalette(subsets, precision);
    }
    
    /**
     * Builds an inexact possibly translucent palette of at most {@code max} colors in {@code src}
     * using the traditional Median Cut algorithm. Color bounding boxes are split along the
     * longest axis, with each step splitting the box. All bits in each component are used.
     * The Algorithm is slower and seems exact than {@linkplain #makeQuantizedRgbPalette(BufferedImage, int)}.
     * @param src the image whose palette to build
     * @param transparent whether to consider the alpha values
     * @param max the maximum number of colors the palette can contain
     * @return the palette of at most {@code max} colors
     */
    public Palette makeQuantizedRgbaPalette(BufferedImage src, boolean transparent, int max) throws ImageWriteException {
        return new MedianCutQuantizer(!transparent).process(src, max, false);
    }

    /**
     * Builds an exact complete opaque palette containing all the colors in {@code src},
     * and fails by returning {@code null} if there are more than {@code max} colors necessary to do this.
     * @param src the image whose palette to build
     * @param max the maximum number of colors the palette can contain
     * @return the complete palette of {@code max} or less colors, or {@code null} if more than {@code max} colors are necessary
     */
    public SimplePalette makeExactRgbPaletteSimple(BufferedImage src, int max) {
        // This is not efficient for large values of max, say, max > 256;
        Set<Integer> rgbs = new HashSet<Integer>();

        int width = src.getWidth();
        int height = src.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = src.getRGB(x, y);
                int rgb = 0xffffff & argb;

                if (rgbs.add(rgb) && rgbs.size() > max) {
                    return null;
                }
            }
        }
        
        int result[] = new int[rgbs.size()];
        int next = 0;
        for (int rgb : rgbs) {
            result[next++] = rgb;
        }
        Arrays.sort(result);

        return new SimplePalette(result);
    }

    public boolean isGrayscale(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (ColorSpace.TYPE_GRAY == src.getColorModel().getColorSpace()
                .getType()) {
            return true;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = src.getRGB(x, y);

                int red = 0xff & (argb >> 16);
                int green = 0xff & (argb >> 8);
                int blue = 0xff & (argb >> 0);

                if (red != green || red != blue) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasTransparency(BufferedImage src) {
        return hasTransparency(src, 255);
    }

    public boolean hasTransparency(BufferedImage src, int threshold) {
        int width = src.getWidth();
        int height = src.getHeight();

        if (!src.getColorModel().hasAlpha()) {
            return false;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = src.getRGB(x, y);
                int alpha = 0xff & (argb >> 24);
                if (alpha < threshold) {
                    return true;
                }
            }
        }
        return false;
    }

    public int countTrasparentColors(int rgbs[]) {
        int first = -1;

        for (int i = 0; i < rgbs.length; i++) {
            int rgb = rgbs[i];
            int alpha = 0xff & (rgb >> 24);
            if (alpha < 0xff) {
                if (first < 0) {
                    first = rgb;
                } else if (rgb != first) {
                    return 2; // more than one transparent color;
                }
            }
        }

        if (first < 0) {
            return 0;
        }
        return 1;
    }

    public int countTransparentColors(BufferedImage src) {
        ColorModel cm = src.getColorModel();
        if (!cm.hasAlpha()) {
            return 0;
        }

        int width = src.getWidth();
        int height = src.getHeight();

        int first = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = src.getRGB(x, y);
                int alpha = 0xff & (rgb >> 24);
                if (alpha < 0xff) {
                    if (first < 0) {
                        first = rgb;
                    } else if (rgb != first) {
                        return 2; // more than one transparent color;
                    }
                }
            }
        }

        if (first < 0) {
            return 0;
        }
        return 1;
    }

}
