/*
 *  Copyright 2010 Radim Hatlapatka.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package cz.muni.pdfjbim;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * class representing list of images compressed according to JBIG2 standard
 * in format suitable for pdf
 * and byte array of global Data
 *
 * @author Radim Hatlapatka (hata.radim@gmail.com)
 */
public class Jbig2ForPdf {

    private byte[] globalData;
    private SortedMap<Integer, PdfImage> jbig2Images;
    private List<File> jbFileNames = new ArrayList<File>();

    /**
     * constructor that reads jbig2 images and global data and saves them in array of bytes
     * @param pathToDir represents path to directory containing images data and global data
     * @throws pdfrecompression.PdfRecompressionException
     */
    public Jbig2ForPdf(String pathToDir, String basename) throws PdfRecompressionException {
        jbig2Images = new TreeMap<Integer, PdfImage>();
        File directory = new File(pathToDir);
        if (!directory.isDirectory()) {
            throw new PdfRecompressionException("argument pathToDir doesn`t contain path to directory");
        }

        File[] fileNames = directory.listFiles();
        for (int i = 0; i < fileNames.length; i++) {
            File checkFile = fileNames[i];
            String fileName = checkFile.getName();
            
            if (checkFile.isDirectory()) {
                continue;
            }

            if ((fileName.lastIndexOf(".") + 1) == (fileName.length() - 4)) {
                if (fileName.startsWith(basename)) {
                    String suffix = fileName.substring(fileName.length() - 4);
                    try {
                        int suffixInt = Integer.parseInt(suffix);
                        jbFileNames.add(checkFile);
                        jbig2Images.put(suffixInt, new PdfImage(checkFile));
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                }
            }
            if (fileName.equals(basename + ".sym")) {
                Long sizeOfFile = checkFile.length();
                int imageSize = 0;
                FileInputStream jbImageInput = null;

                try {
                    jbImageInput = new FileInputStream(checkFile);                    
                    jbFileNames.add(checkFile);
                    if (sizeOfFile > Integer.MAX_VALUE) {
                        throw new PdfRecompressionException("cannot process image greater than " + Integer.MAX_VALUE);
                    }

                    DataInput inputData = new DataInputStream(jbImageInput);
                    imageSize = sizeOfFile.intValue();
                    byte[] imageBytes = new byte[imageSize];
                    inputData.readFully(imageBytes);
                    globalData = imageBytes;
                } catch (FileNotFoundException ex) {
                    throw new PdfRecompressionException(ex);
                } catch (IOException ioEx) {
                    throw new PdfRecompressionException("io error", ioEx);
                }
            }
        }
    }

    /**
     * add pdf image
     * @param jbImage represents image encoding according to JBIG2 standard
     */
    public void addJbig2Image(int key, PdfImage jbImage) {
        jbig2Images.put(key, jbImage);
    }

    /**
     * sets information to concrete image in the list
     * @param i represents position of the image in the list
     * @param pdfImageInformation represents information about that image
     */
    public void setJbig2ImageInfo(int i, PdfImageInformation pdfImageInformation) {
        jbig2Images.get(i).setPdfImageInformation(pdfImageInformation);
    }

    /**
     * Sets informations about pdf image given in List.
     * This list of information has to have the same order as images in the list of pdf images and the same count
     * @param pdfImageInformations represents list of informations about pdf images
     * @throws pdfrecompression.PdfRecompressionException if there is different number of informations and images
     */
    public void setJbig2ImagesInfo(List<PdfImageInformation> pdfImageInformations) throws PdfRecompressionException {
        if (pdfImageInformations == null) {
            throw new NullPointerException("pdfImageInformations");
        }

        if (pdfImageInformations.size() != jbig2Images.size()) {
            throw new PdfRecompressionException("there can't be difference in count of images and their informations");
        }

        for (int i = 0; i < jbig2Images.size(); i++) {
            setJbig2ImageInfo(i, pdfImageInformations.get(i));
        }
    }

    /**
     * sets global data of image
     * @param globalData represents global data
     */
    public void setGlobalData(byte[] globalData) {
        this.globalData = globalData;
    }

    /**
     * sets atribut jbig2Images
     * @param jbig2Images represents list of pdf images
     */
    public void setJbig2Images(SortedMap<Integer, PdfImage> jbig2Images) {
        this.jbig2Images = jbig2Images;
    }

    /**
     * @return return global data
     */
    public byte[] getGlobalData() {
        return globalData;
    }

    /**
     * @param index represents position of image in the list
     * @return image from the list from position given by parameter index
     */
    public PdfImage getJbig2Image(int index) {
        return jbig2Images.get(index);
    }

    /**
     * @return list of pdf images encoded according to JBIG2 standard
     */
    public SortedMap<Integer, PdfImage> getSortedMapOfJbig2Images() {
        return jbig2Images;
    }

    /**
     *
     * @return map of images identified by PDF object ID
     */
    public Map<PdfObjId, PdfImage> getMapOfJbig2Images() {
        Map<PdfObjId, PdfImage> pdfImages = new HashMap<PdfObjId, PdfImage>();
        if (!jbig2Images.isEmpty()) {
            for (int i = 0; i <= jbig2Images.lastKey(); i++) {
                PdfImage jb2Im = jbig2Images.get(i);
                PdfImageInformation jb2ImInfo = jb2Im.getPdfImageInformation();
                PdfObjId objId = new PdfObjId(jb2ImInfo.getObjectNum(), jb2ImInfo.getObjectGenNum());
                pdfImages.put(objId, jb2Im);
            }
        }
        return pdfImages;
    }

    /**
     *
     * @return files that contains data of images and global data
     *         (output of jbig2enc with parameters -s and -p)
     */
    public List<File> getJbFiles() {
        return jbFileNames;
    }
}
