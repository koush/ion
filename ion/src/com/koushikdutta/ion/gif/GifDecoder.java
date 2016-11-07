package com.koushikdutta.ion.gif;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class GifDecoder implements Cloneable {

    public static final int STATUS_PARSING = 0;
    public static final int STATUS_FORMAT_ERROR = 1;
    public static final int STATUS_OPEN_ERROR = 2;
    public static final int STATUS_FINISH = -1;

    private int status;

    public int width; // full image width
    public int height; // full image height
    private boolean gctFlag; // global color table used
    private int gctSize; // size of global color table
    private int loopCount = 1; // iterations; 0 = repeat forever

    private int[] gct; // global color table
    private int[] lct; // local color table
    private int[] act; // active color table

    private int bgIndex; // background color index
    private int bgColor; // background color
    private int lastBgColor; // previous bg color
    private int pixelAspect; // pixel aspect ratio

    private boolean lctFlag; // local color table flag
    private boolean interlace; // interlace flag
    private int lctSize; // local color table size

    private int ix, iy, iw, ih; // current image rectangle
    private int lrx, lry, lrw, lrh;


    private byte[] block = new byte[256]; // current data block
    private int blockSize = 0; // block size
    private int dispose = 0;
    private int lastDispose = 0;
    private boolean transparency = false; // use transparent color
    private int delay = 0; // delay in milliseconds
    private int transIndex; // transparent color index

    private static final int MaxStackSize = 4096;
    // max decoder pixel stack size

    // LZW decoder working arrays
    private short[] prefix;
    private byte[] suffix;
    private byte[] pixelStack;
    private byte[] pixels;

    private byte[] gifData = null;
    private int gifDataOffset;
    private int gifDataLength;

    private int readBytes;
    private int currentFrame;

    GifFrame lastFrame;
    GifFrame restoreFrame;
    int[] dest;

    // Any pixels at this index or beyond will reuse their values from the previous frame.
    private int pixelsTrimIndex = Integer.MAX_VALUE;

    public GifDecoder mutate() {
        try {
            GifDecoder ret = (GifDecoder) clone();
            block = new byte[256];
            prefix = null;
            suffix = null;
            pixelStack = null;
            pixels = null;
            dest = null;
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(gifData, gifDataOffset, gifDataLength);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getGifDataLength() {
        return gifDataLength;
    }

    public GifFrame getLastFrame() {
        return lastFrame;
    }

    public GifDecoder(ByteBuffer bb) {
        this(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
    }

    public GifDecoder(byte[] data) {
        this(data, 0, data.length);
    }

    public GifDecoder(byte[] data, int offset, int length) {
        gifData = data;
        gifDataOffset = offset;
        gifDataLength = length;

        restart();
    }

    public void restart() {
        readBytes = 0;
        status = STATUS_PARSING;
        gct = null;
        lct = null;
        readHeader();
    }

    public synchronized GifFrame nextFrame() {
        // read GIF file content blocks
        while (!err() && status == STATUS_PARSING) {
            int code = read();
            switch (code) {
                case 0x2C: // image separator
                    return lastFrame = readImage();
                case 0x21: // extension
                    code = read();
                    switch (code) {
                        case 0xf9: // graphics control extension
                            readGraphicControlExt();
                            break;
                        case 0xff: // application extension
                            readBlock();
                            String app = "";
                            for (int i = 0; i < 11; i++) {
                                app += (char) block[i];
                            }
                            if (app.equals("NETSCAPE2.0")) {
                                readNetscapeExt();
                            } else {
                                skip(); // don't care
                            }
                            break;
                        default: // uninteresting extension
                            skip();
                    }
                    break;
                case 0x3b: // terminator
                    status = STATUS_FINISH;
                    return null;
                case 0x00: // bad byte, but keep going and see what happens
                    break;
                default:
                    status = STATUS_FORMAT_ERROR;
            }
        }

        status = STATUS_FORMAT_ERROR;
        return null;
    }

    public int getStatus() {
        return status;
    }

    public boolean parseOk() {
        return status == STATUS_FINISH;
    }

    public int getLoopCount() {
        return loopCount;
    }

    private Bitmap setPixels() {
        if (lastDispose == 2) {
            if (dest == null)
                dest = new int[width * height];
            int c = 0;
            if (!transparency) {
                c = lastBgColor;
            }
            Arrays.fill(dest, c);
        } else {
            if (dest == null) {
                dest = new int[width * height];
                if (restoreFrame != null)
                    restoreFrame.image.getPixels(dest, 0, width, 0, 0, width, height);
                else
                    Arrays.fill(dest, 0);
            } else if (lastDispose == 3) {
                // force restore on dispose method restore previous
                if (restoreFrame != null)
                    restoreFrame.image.getPixels(dest, 0, width, 0, 0, width, height);
                else
                    Arrays.fill(dest, 0);
            }
        }

        // copy each source line to the appropriate place in the destination
        int pass = 1;
        int inc = 8;
        int iline = 0;
        for (int i = 0; i < ih; i++) {
            int line = i;
            if (interlace) {
                if (iline >= ih) {
                    pass++;
                    switch (pass) {
                        case 2:
                            iline = 4;
                            break;
                        case 3:
                            iline = 2;
                            inc = 4;
                            break;
                        case 4:
                            iline = 1;
                            inc = 2;
                    }
                }
                line = iline;
                iline += inc;
            }
            line += iy;
            if (line < height) {
                int k = line * width;
                int dx = k + ix; // start of line in dest
                int dlim = dx + iw; // end of dest line
                if ((k + width) < dlim) {
                    dlim = k + width; // past dest edge
                }
                int sx = i * iw; // start of line in source
                while (dx < dlim) {
                    if (sx >= pixelsTrimIndex) break; // There's no more "new" pixels in this frame.

                    // map color and insert in destination
                    int index = ((int) pixels[sx++]) & 0xff;
                    if (!transparency || index != transIndex) {
                        dest[dx] = act[index];
                    }
                    dx++;
                }
            }
        }
        return Bitmap.createBitmap(dest, width, height, Config.ARGB_4444);
    }

    private void decodeImageData() {
        int NullCode = -1;
        int npix = iw * ih;
        int available, clear, code_mask, code_size, end_of_information, in_code, old_code, bits, code, count, i, datum, data_size, first, top, bi, pi;

        if ((pixels == null) || (pixels.length < npix)) {
            pixels = new byte[npix]; // allocate new pixel array
        }
        if (prefix == null) {
            prefix = new short[MaxStackSize];
        }
        if (suffix == null) {
            suffix = new byte[MaxStackSize];
        }
        if (pixelStack == null) {
            pixelStack = new byte[MaxStackSize + 1];
        }
        // Initialize GIF data stream decoder.
        data_size = read();
        clear = 1 << data_size;
        end_of_information = clear + 1;
        available = clear + 2;
        old_code = NullCode;
        code_size = data_size + 1;
        code_mask = (1 << code_size) - 1;
        for (code = 0; code < clear; code++) {
            prefix[code] = 0;
            suffix[code] = (byte) code;
        }

        // Decode GIF pixel stream.
        datum = bits = count = first = top = pi = bi = 0;
        for (i = 0; i < npix; ) {
            if (top == 0) {
                if (bits < code_size) {
                    // Load bytes until there are enough bits for a code.
                    if (count == 0) {
                        // Read a new data block.
                        count = readBlock();
                        if (count <= 0) {
                            break;
                        }
                        bi = 0;
                    }
                    datum += (((int) block[bi]) & 0xff) << bits;
                    bits += 8;
                    bi++;
                    count--;
                    continue;
                }
                // Get the next code.
                code = datum & code_mask;
                datum >>= code_size;
                bits -= code_size;

                // Interpret the code
                if ((code > available) || (code == end_of_information)) {
                    break;
                }
                if (code == clear) {
                    // Reset decoder.
                    code_size = data_size + 1;
                    code_mask = (1 << code_size) - 1;
                    available = clear + 2;
                    old_code = NullCode;
                    continue;
                }
                if (old_code == NullCode) {
                    pixelStack[top++] = suffix[code];
                    old_code = code;
                    first = code;
                    continue;
                }
                in_code = code;
                if (code == available) {
                    pixelStack[top++] = (byte) first;
                    code = old_code;
                }
                while (code > clear) {
                    pixelStack[top++] = suffix[code];
                    code = prefix[code];
                }
                first = ((int) suffix[code]) & 0xff;
                // Add a new string to the string table,
                if (available >= MaxStackSize) {
                    break;
                }
                pixelStack[top++] = (byte) first;
                prefix[available] = (short) old_code;
                suffix[available] = (byte) first;
                available++;
                if (((available & code_mask) == 0)
                        && (available < MaxStackSize)) {
                    code_size++;
                    code_mask += available;
                }
                old_code = in_code;
            }

            // Pop a pixel off the pixel stack.
            top--;
            pixels[pi++] = pixelStack[top];
            i++;
        }

        pixelsTrimIndex = pi;
    }

    private boolean err() {
        return status != STATUS_PARSING;
    }

    private int read() {
        if (readBytes >= gifDataLength)
            return 0;
        return gifData[gifDataOffset + readBytes++] & 0xFF;
    }

    private int read(byte[] bytes, int offset, int length) throws IOException {
        if (readBytes >= gifDataLength)
            return -1;
        int toCopy = Math.min(gifDataLength - readBytes, length);
        System.arraycopy(gifData, gifDataOffset + readBytes, bytes, offset, toCopy);
        readBytes += toCopy;
        return toCopy;
    }

    private int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    private int readBlock() {
        blockSize = read();
        int n = 0;
        if (blockSize > 0) {
            try {
                int count = 0;
                while (n < blockSize) {
                    count = read(block, n, blockSize - n);
                    if (count == -1) {
                        break;
                    }
                    n += count;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (n < blockSize) {
                status = STATUS_FORMAT_ERROR;
            }
        }
        return n;
    }

    private int[] readColorTable(int ncolors) {
        int nbytes = 3 * ncolors;
        int[] tab = null;
        byte[] c = new byte[nbytes];
        int n = 0;
        try {
            n = read(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (n < nbytes) {
            status = STATUS_FORMAT_ERROR;
        } else {
            tab = new int[256]; // max size to avoid bounds checks
            int i = 0;
            int j = 0;
            while (i < ncolors) {
                int r = ((int) c[j++]) & 0xff;
                int g = ((int) c[j++]) & 0xff;
                int b = ((int) c[j++]) & 0xff;
                tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
        return tab;
    }

    private void readGraphicControlExt() {
        read(); // block size
        int packed = read(); // packed fields
        dispose = (packed & 0x1c) >> 2; // disposal method
        if (dispose == 0) {
            dispose = 1; // elect to keep old image if discretionary
        }
        transparency = (packed & 1) != 0;
        delay = readShort() * 10; // delay in milliseconds
        transIndex = read(); // transparent color index
        read(); // block terminator
    }

    private void readHeader() {
        String id = "";
        for (int i = 0; i < 6; i++) {
            id += (char) read();
        }
        if (!id.startsWith("GIF")) {
            status = STATUS_FORMAT_ERROR;
            return;
        }
        readLSD();
        if (gctFlag && !err()) {
            gct = readColorTable(gctSize);
            bgColor = gct[bgIndex];
        }
    }

    private GifFrame readImage() {
        ix = readShort(); // (sub)image position & size
        iy = readShort();
        iw = readShort();
        ih = readShort();
        int packed = read();
        lctFlag = (packed & 0x80) != 0; // 1 - local color table flag
        interlace = (packed & 0x40) != 0; // 2 - interlace flag
        // 3 - sort flag
        // 4-5 - reserved
        lctSize = 2 << (packed & 7); // 6-8 - local color table size
        if (lctFlag) {
            lct = readColorTable(lctSize); // read table
            act = lct; // make local table active
        } else {
            act = gct; // make global table active
            if (bgIndex == transIndex) {
                bgColor = 0;
            }
        }
        if (act == null) {
            status = STATUS_FORMAT_ERROR; // no color table defined
        }
        if (err()) {
            return null;
        }
        decodeImageData(); // decode pixel data
        skip();
        if (err()) {
            return null;
        }
        currentFrame++;
        // create new image to receive frame data
        // createImage(width, height);
        Bitmap image = setPixels(); // transfer pixel data to image
        GifFrame gifFrame = new GifFrame(image, delay);
        // frames.addElement(new GifFrame(image, delay)); // add image to frame
        // list
        resetFrame(gifFrame);
        return gifFrame;
    }

    private void readLSD() {
        // logical screen size
        width = readShort();
        height = readShort();
        // packed fields
        int packed = read();
        gctFlag = (packed & 0x80) != 0; // 1 : global color table flag
        // 2-4 : color resolution
        // 5 : gct sort flag
        gctSize = 2 << (packed & 7); // 6-8 : gct size
        bgIndex = read(); // background color index
        pixelAspect = read(); // pixel aspect ratio
    }

    private void readNetscapeExt() {
        do {
            readBlock();
            if (block[0] == 1) {
                // loop count sub-block
                int b1 = ((int) block[1]) & 0xff;
                int b2 = ((int) block[2]) & 0xff;
                loopCount = (b2 << 8) | b1;
            }
        } while ((blockSize > 0) && !err());
    }

    private int readShort() {
        // read 16-bit value, LSB first
        return read() | (read() << 8);
    }

    private void resetFrame(GifFrame newFrame) {
        // fill in starting image contents based on last image's dispose code
        switch (dispose) {
            case 0:
                // no dispose, leave restore frame as whatever this is
                restoreFrame = newFrame;
                break;
            case 1:
                // use last bitmap pixels
                restoreFrame = newFrame;
                break;
            case 2:
                // fill last image rect area with background color,
                // handle that in setPixels
                restoreFrame = null;
                break;
            case 3:
                // revert canvas to previous, so just leave the original restore frame
                break;
            default:
                // wtf?
                Log.w("Ion", "Unknown gif dispose code: " + lastDispose);
                break;
        }

        lastDispose = dispose;
        lrx = ix;
        lry = iy;
        lrw = iw;
        lrh = ih;
        lastBgColor = bgColor;
        dispose = 0;
        transparency = false;
        delay = 0;
        lct = null;
        pixelsTrimIndex = Integer.MAX_VALUE;
    }

    /**
     * Skips variable length blocks up to and including next zero length block.
     */
    private void skip() {
        do {
            readBlock();
        } while ((blockSize > 0) && !err());
    }
}
