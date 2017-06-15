package net.anders.autounlock.MachineLearning;

/**
 * Created by tom-fire on 16/05/2017.
 */

import android.util.Log;
import android.widget.Toast;

import net.anders.autounlock.CoreService;

import org.deeplearning4j.api.storage.StatsStorage;
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
//import org.deeplearning4j.ui.api.UIServer;
//import org.deeplearning4j.ui.stats.StatsListener;
//import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;



import java.util.Map;

public class RecurrentNN {
    private final static int NUM_SAMPLES = 25;
    private final static int N_INPUT = 3;
    private final static int N_OUTPUT = 2;

    private static MultiLayerNetwork myNetwork;

    /*public static void main(String args[]) throws Exception {
        startTraining();
        getProbability();
    }*/

    public static void startTraining() throws Exception {
        constructNetwork();
    }

    private static void constructNetwork() throws Exception {
        /*
        DenseLayer inputLayer = new DenseLayer.Builder()
                .nIn(N_INPUT)
                .nOut(3)
                .name("Input")
                .build();
                */

        GravesLSTM layer1 = new GravesLSTM.Builder()
                .activation(Activation.TANH)
                .nIn(NUM_SAMPLES)
                .nOut(100)
                .build();

        RnnOutputLayer outputLayer = new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                .activation(Activation.SOFTMAX)
                .nIn(100)
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
        listBuilder.layer(0, layer1);
        listBuilder.layer(1, outputLayer);

        //listBuilder.backprop(true);
        listBuilder.backpropType(BackpropType.TruncatedBPTT) //Not necessary if timeseries are short.
                .tBPTTForwardLength(20)  //Maximum length = length of time series
                .tBPTTBackwardLength(20);

        myNetwork = new MultiLayerNetwork(listBuilder.build());
        myNetwork.init();

        trainNetworkOnStart();
    }


    public static void trainNetworkOnStart() throws Exception {
        Map<Integer, Double[][]> trainData = CoreService.RNN;
        //Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();

        for (int i = 0; i < trainData.keySet().size(); i++) {
            System.out.println("Fitting " + (i + 1) + " of " + trainData.keySet().size());

            Double[][] trainArray = trainData.get(i);

            INDArray trainingInputs = Nd4j.zeros(N_INPUT, NUM_SAMPLES);
            INDArray trainingOutputs = Nd4j.zeros(1, NUM_SAMPLES);

            for (int j = 0; j < NUM_SAMPLES; j++) {
                trainingInputs.putScalar(new int[]{0,j}, trainArray[0][j]); //Orientation
                trainingInputs.putScalar(new int[]{1,j}, trainArray[1][j]); //Acceleration X
                trainingInputs.putScalar(new int[]{2,j}, trainArray[2][j]); //Acceleration Y
            }

            for (int j = 0; j < NUM_SAMPLES-1; j++) {
                trainingOutputs.putScalar(new int[]{0,j}, 0);
            }

            trainingOutputs.putScalar(new int[]{0,NUM_SAMPLES-1}, DatabaseRetriever.getUnlockValue(i+1));


            DataSet myData = new DataSet(trainingInputs, trainingOutputs);

            myData.normalize();

            myNetwork.fit(myData);
        }
    }

    public static Double[][] createSequenceData(WindowData[] snapshot){
        Double[][] sequence = new Double[2][];
        Double[] ori = new Double[snapshot.length];
        Double[] accX = new Double[snapshot.length];
        Double[] accY = new Double[snapshot.length];
        for (int i = 0; i < snapshot.length; i++) {
            WindowData window = snapshot[i];
            ori[i] = window.getOrientation();
            accX[i] = window.getAccelerationX();
            accY[i] = window.getAccelerationY();
        }
        sequence[0] = ori;
        sequence[1] = accX;
        sequence[2] = accY;

        return sequence;
    }

    public static void trainNetwork(WindowData[] snapshot) {

        Double[][] trainArray = createSequenceData(snapshot);
        //System.out.println("Size: " + trainArray.length + ", " + trainArray[0].length);

        INDArray trainingInputs = Nd4j.zeros(N_INPUT, NUM_SAMPLES);
        INDArray trainingOutputs = Nd4j.zeros(1, NUM_SAMPLES);

        for (int j = 0; j < NUM_SAMPLES; j++) {
            trainingInputs.putScalar(new int[]{0,j}, trainArray[0][j]); //Orientation
            trainingInputs.putScalar(new int[]{1,j}, trainArray[1][j]); //Acceleration X
            trainingInputs.putScalar(new int[]{2,j}, trainArray[2][j]); //Acceleration Y
        }

        for (int i = 0; i < NUM_SAMPLES-1; i++) {
            trainingOutputs.putScalar(new int[]{0,i}, 0);
        }

        trainingOutputs.putScalar(new int[]{0,NUM_SAMPLES-1}, 1);

        //System.out.println("Shape = [" + trainingInputs.shape()[0] + "," + trainingInputs.shape()[1] + "]");

        DataSet myData = new DataSet(trainingInputs, trainingOutputs);

        myNetwork.fit(myData);
    }

    /**
     * Gets probability of the last timestep, which would be the whether or not to unlock
     *
     * See: https://deeplearning4j.org/usingrnns
     */
    public static double getProbability(Double[][] sequence) throws Exception {
        //Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();

        /*for (Double[] dd: sequence
                ) {
            String row = "";
            for (Double ddd: dd
                    ) {
                row += ddd + " ";
            }
            Log.v("sequenceinformation", row);
        }*/



        //Double[][] array = trainData.get(7);

        //Double[][] sequence = DatabaseRetriever.readTestData();
        if (sequence == null || sequence[0].length == 0) {
            System.out.println("TestData is null or zero");
            return 0;
        }

        INDArray newData = Nd4j.zeros(N_INPUT, NUM_SAMPLES);

        for (int i = 0; i < NUM_SAMPLES; i++) {
            newData.putScalar(new int[]{0, i}, sequence[0][i]); //Orientation
            newData.putScalar(new int[]{1, i}, sequence[1][i]); //Acceleration X
            newData.putScalar(new int[]{2, i}, sequence[2][i]); //Acceleration Y
        }

        INDArray arr = myNetwork.output(newData);
        //System.out.println(arr);
        int length = arr.size(2);
        INDArray probs = arr.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(length - 1));
        double prob = probs.getDouble(1);
        //Log.v("sequenceinformation", "Prob = " + prob);
        System.out.println("Probability: " + prob);
        return  prob;
    }
}
