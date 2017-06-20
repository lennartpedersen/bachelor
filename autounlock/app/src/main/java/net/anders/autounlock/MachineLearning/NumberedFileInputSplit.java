package net.anders.autounlock.MachineLearning;

/**
 * Created by Lennart Pedersen on 16-06-2017.
 */

/*
 *  * Copyright 2016 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */

import android.net.Uri;

import org.datavec.api.split.InputSplit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;


/**InputSplit for sequences of numbered files.
 * Example usages:<br>
 * Suppose files are sequenced according to "myFile_100.txt", "myFile_101.txt", ..., "myFile_200.txt"
 * then use new NumberedFileInputSplit("myFile_%d.txt",100,200)
 * NumberedFileInputSplit utilizes String.format(), hence the requirement for "%d" to represent
 * the integer index.
 */
public class NumberedFileInputSplit implements InputSplit {
    private final String baseString;
    private final int minIdx;
    private final int maxIdx;

    /**
     * @param baseString String that defines file format. Must contain "%d", which will be replaced with
     *                   the index of the file.
     * @param minIdxInclusive Minimum index/number (starting number in sequence of files, inclusive)
     * @param maxIdxInclusive Maximum index/number (last number in sequence of files, inclusive)
     */
    public NumberedFileInputSplit(String baseString, int minIdxInclusive, int maxIdxInclusive){
        if(baseString == null || !baseString.contains("%d")){
            throw new IllegalArgumentException("Base String must contain  character sequence %d");
        }
        this.baseString = baseString;
        this.minIdx = minIdxInclusive;
        this.maxIdx = maxIdxInclusive;
    }

    @Override
    public long length() {
        return maxIdx-minIdx+1;
    }

    @Override
    public URI[] locations() {
        URI[] uris = new URI[(int)length()];
        String test = "";
        int x=0;
        for( int i=minIdx; i<=maxIdx; i++ ){
            test = Uri.parse(String.format(baseString, i)).toString();
            uris[x++] = URI.create(test);

        }
        return uris;
    }

    @Override
    public void write(DataOutput out) throws IOException {

    }

    @Override
    public void readFields(DataInput in) throws IOException {

    }

    @Override
    public double toDouble(){
        throw new UnsupportedOperationException();
    }

    @Override
    public float toFloat(){
        throw new UnsupportedOperationException();
    }

    @Override
    public int toInt(){
        throw new UnsupportedOperationException();
    }

    @Override
    public long toLong(){
        throw new UnsupportedOperationException();
    }
}

