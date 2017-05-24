package net.anders.autounlock.MachineLearning;

/**
 * Created by tom-fire on 16/05/2017.
 */

import net.anders.autounlock.CoreService;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Map;

public class RecurrentNN {
    private final static int NUM_SAMPLES = 50;
    private final static int N_INPUT = 2;
    private final static int N_OUTPUT = 1;

    private static MultiLayerNetwork myNetwork;

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

        GravesLSTM inputLayer = new GravesLSTM.Builder()
                .activation(Activation.TANH).
                nIn(NUM_SAMPLES)
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
        Map<Integer, Double[][]> trainData = CoreService.RNN;

        for (int i = 0; i < trainData.keySet().size(); i++) {
            System.out.println("Fitting " + (i + 1) + " of " + trainData.keySet().size());
            //System.out.println("Orientation = " + array[0][i] + ", velocity = " + array[1][i]);

            Double[][] trainArray = trainData.get(i);
            //System.out.println("Size: " + trainArray.length + ", " + trainArray[0].length);

            INDArray trainingInputs = Nd4j.zeros(N_INPUT, NUM_SAMPLES);
            INDArray trainingOutputs = Nd4j.zeros(1, 1);

            for (int j = 0; j < trainArray[0].length; j++) {
                trainingInputs.putScalar(new int[]{0,j}, trainArray[0][j]); //Orientation
                trainingInputs.putScalar(new int[]{1,j}, trainArray[1][j]); //Velocity
            }

            trainingOutputs.putScalar(new int[]{0,0}, 1);


            //System.out.println("Shape = [" + trainingInputs.shape()[0] + "," + trainingInputs.shape()[1] + "]");

            DataSet myData = new DataSet(trainingInputs, trainingOutputs);

            myNetwork.fit(myData);
        }
    }

    /**
     * Gets probability of the last timestep, which would be the whether or not to unlock
     *
     * See: https://deeplearning4j.org/usingrnns
     */
    public static double getProbability(Double[][] sequence) throws Exception {
        //Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();


        //Double[][] array = trainData.get(7);

        //Double[][] array = DatabaseRetriever.readTestData();
        if (sequence == null) {
            System.out.println("TestData is null");
            return 0;
        }

        INDArray newShit = Nd4j.zeros(N_INPUT, NUM_SAMPLES);

        for (int i = 0; i < sequence[0].length; i++) {
            newShit.putScalar(new int[]{0, i}, sequence[0][i]); //Orientation
            newShit.putScalar(new int[]{1, i}, sequence[1][i]); //Velocity
        }

        INDArray arr = myNetwork.output(newShit);
        //System.out.println(arr);
        int length = arr.size(2);
        INDArray probs = arr.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(length - 1));
        double prob = probs.getDouble(0);


        return  prob;
    }
}
