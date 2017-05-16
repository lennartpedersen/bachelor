package net.anders.autounlock.MachineLearning;

/**
 * Created by tom-fire on 16/05/2017.
 */

import org.apache.commons.math3.util.MultidimensionalCounter;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.collection.CollectionSequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.CollectionInputSplit;
import org.datavec.api.split.InputStreamInputSplit;
import org.datavec.api.split.NumberedFileInputSplit;
import org.datavec.api.split.StringSplit;
import org.deeplearning4j.berkeley.Iterators;
import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.DoublesDataSetIterator;
import org.deeplearning4j.datasets.iterator.INDArrayDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.InputSplit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import static net.anders.autounlock.MachineLearning.DatabaseRetriever.getTupleDict;
import static org.reflections.Reflections.log;

public class RecurrentNN {
    private final static int NUM_SAMPLES = 49;
    private final static int N_INPUT = 2;
    private final static int N_OUTPUT = 1;

    private static MultiLayerNetwork myNetwork;

    public static void main(String[] args) throws Exception {
        constructNetwork();
        getProbability();
    }

    private static void constructNetwork() throws Exception {
        /*
        DenseLayer inputLayer = new DenseLayer.Builder()
                .nIn(N_INPUT)
                .nOut(3)
                .name("Input")
                .build();
                */

        GravesLSTM inputLayer = new GravesLSTM.Builder()
                .activation(Activation.TANH).
                nIn(N_INPUT)
                .nOut(10)
                .build();

        RnnOutputLayer outputLayer = new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                .activation(Activation.SOFTMAX)
                .nIn(10)
                .nOut(N_OUTPUT)
                .build();

        NeuralNetConfiguration.Builder nncBuilder = new NeuralNetConfiguration.Builder()
                .seed(123)    //Random number generator seed for improved repeatability. Optional.
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).iterations(1)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .iterations(10000)
                .learningRate(0.01);

        NeuralNetConfiguration.ListBuilder listBuilder = nncBuilder.list();
        listBuilder.layer(0, inputLayer);
        //listBuilder.layer(1, hiddenLayer);
        listBuilder.layer(1, outputLayer);

        //listBuilder.backprop(true);
        listBuilder.backpropType(BackpropType.TruncatedBPTT)
                .tBPTTForwardLength(50)
                .tBPTTBackwardLength(50);

        myNetwork = new MultiLayerNetwork(listBuilder.build());
        myNetwork.init();

        trainNetwork();
    }

    private static void constructNetwork2() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)    //Random number generator seed for improved repeatability. Optional.
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).iterations(1)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .learningRate(0.005)
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)  //Not always required, but helps with this data set
                .gradientNormalizationThreshold(0.5)
                .list()
                .layer(0, new GravesLSTM.Builder().activation(Activation.TANH).nIn(N_INPUT).nOut(10).build())
                .layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX).nIn(10).nOut(N_OUTPUT).build())
                .pretrain(false).backprop(true).build();

        myNetwork = new MultiLayerNetwork(conf);
        myNetwork.init();
    }

    private static void trainNetwork() throws Exception {
        Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();

        for (int i = 1; i < trainData.keySet().size(); i++) {
            System.out.println("Fitting " + (i + 1) + " of " + trainData.keySet().size());
            //System.out.println("Orientation = " + array[0][i] + ", velocity = " + array[1][i]);

            Double[][] trainArray = trainData.get(i);
            Double[][] testArray  = trainData.get(i-1);
            INDArray trainingInputs = Nd4j.zeros(trainArray[0].length, NUM_SAMPLES);
            INDArray trainingOutputs = Nd4j.zeros(trainArray[0].length, 2);

            for (int j = 0; j < trainArray[0].length; j++) {
                trainingInputs.putScalar(new int[]{j,0}, trainArray[0][j]); //Orientation
                trainingInputs.putScalar(new int[]{j,1}, trainArray[1][j]); //Velocity
            }

            for (int j = 0; j < testArray[0].length; j++) {
                trainingOutputs.putScalar(new int[]{j,0}, testArray[0][i]);
                trainingOutputs.putScalar(new int[]{j,1}, testArray[1][i]);
            }

            System.out.println("Shape = [" + trainingInputs.shape()[0] + "," + trainingInputs.shape()[1] + "]");

            DataSet myData = new DataSet(trainingInputs, trainingOutputs);

            myNetwork.fit(myData);
        }
    }

    /**
     * Gets probability of the last timestep, which would be the whether or not to unlock
     *
     * See: https://deeplearning4j.org/usingrnns
     */
    private static void getProbability() throws Exception {
        Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();
        Double[][] array = trainData.get(5);

        INDArray newShit = Nd4j.zeros( array[0].length, N_INPUT);

        for (int i = 0; i < array[0].length; i++) {
            newShit.putScalar(new int[]{i,0}, array[0][i]); //Orientation
            newShit.putScalar(new int[]{i,1}, array[1][i]); //Velocity
        }

        INDArray arr = myNetwork.output(newShit);
        System.out.println(arr);
        int length = arr.size(2);
        INDArray probs = arr.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(length-1));
        System.out.println("Probability at last timestep: " + probs);
    }
}
