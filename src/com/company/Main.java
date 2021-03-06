package com.company;

import com.arib.categoricalhashtable.CategoricalHashTable;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

public class Main {

    static int RPM = 0;
    static double TPS = 0;
    static double FOT = 0;
    static double ignAngle = 0;

    static double bar = 0;
    static double MAP = 0;
    static double lambda = 0;

    static double analog1 = 0;
    static double analog2 = 0;
    static double analog3 = 0;
    static double analog4 = 0;

    static double volts = 0;
    static double airTemp = 0;
    static double coolant = 0;

    static long currTime = 0;

    static CategoricalHashTable<LogObject> dataMap;

    public static void main(String[] args) throws IOException, NullPointerException {
        for (final File fileEntry : new File("logs").listFiles()) {
            dataMap = new CategoricalHashTable<>();

            RPM = 0;
            TPS = 0;
            FOT = 0;
            ignAngle = 0;

            bar = 0;
            MAP = 0;
            lambda = 0;

            analog1 = 0;
            analog2 = 0;
            analog3 = 0;
            analog4 = 0;

            volts = 0;
            airTemp = 0;
            coolant = 0;

            currTime = 0;

            Scanner scanner = null;
            try {
                scanner = new Scanner(fileEntry);
            } catch (Exception e) {
                continue;
            }
            boolean first = true;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(line.length() != 47)
                    continue;
                if (!first) {
                    try {
                        String time = line.substring(line.indexOf('T') + 1, line.indexOf('#') - 1);
                    } catch (StringIndexOutOfBoundsException e) {
                        continue;
                    }
                    String hexData = line.substring(line.indexOf('#'));
                    parse(hexData);
                    if (hexData.substring(0, 4).equals("#001")) {
                        writeData();
                        currTime += 50;
                    }
                } else {
                    try {
                        String time = line.substring(line.indexOf('T') + 1, line.indexOf('#') - 1);
                    } catch (StringIndexOutOfBoundsException e) {
                        continue;
                    }
                    String hexData = line.substring(line.indexOf('#'), line.length());
                    parse(hexData);
                    first = false;
                }
            }

            FileOutputStream fos = new FileOutputStream("logs/CSVs/" + fileEntry.toString().substring(fileEntry.toString().indexOf('/') + 1, fileEntry.toString().indexOf('.')) + ".csv");
            try {
                fos.write(hashMapToCSV().getBytes());
            } catch (IOException e) {
                System.out.println("error writing!");
            }
        }
    }

    private static void parse(String data) {
        //if the length of the line is not the right size skip the value
        if(data.length() != 20) {
            System.out.println("Invalid CAN String!");
            return;
        }
        //get the identifier of the data value
        String identifier = data.substring(1,4);
        //switch on the identifier
        switch (identifier) {
            //group one is RPM, TPS, fuelopentime, ignition angle
            case "001":
                parseGroupOne(data.substring(4));
                break;
            //group two is barometer, MAP, and lambda
            case "002":
                parseGroupTwo(data.substring(4));
                break;
            //group three contains the analog inputs
            case "003":
                parseGroupThree(data.substring(4));
                break;
            case "004":
                break;
            case "005":
                break;
            //group six contains battery voltage, ambient temperature, and coolant temperature
            case "006":
                parseGroupSix(data.substring(4));
                break;
            case "007":
                break;
            case "008":
                parseGroupEight(data.substring(4));
                break;
            case "009":
                break;
            case "010":
                break;
            case "011":
                break;
            case "012":
                break;
            case "013":
                break;
            case "014":
                parseGroupFourteen(data.substring(4));
                break;
            case "015":
                parseGroupFifteen(data.substring(4));
                break;
            case "016":
                parseGroupSixteen(data.substring(4));
                break;
            case "017":
                break;
            default:
                System.out.println("Parse fail");
                break;
        }
    }

    private static void writeData() {
        dataMap.put(new SimpleLogObject("Time,RPM", RPM, currTime));
        dataMap.put(new SimpleLogObject("Time,TPS", TPS, currTime));
        dataMap.put(new SimpleLogObject("Time,FuelOpenTime", FOT, currTime));
        dataMap.put(new SimpleLogObject("Time,IgnitionAngle", ignAngle, currTime));
        dataMap.put(new SimpleLogObject("Time,Barometer", bar, currTime));
        dataMap.put(new SimpleLogObject("Time,MAP", MAP, currTime));
        dataMap.put(new SimpleLogObject("Time,Lambda", lambda, currTime));
        dataMap.put(new SimpleLogObject("Time,Analog1", analog1, currTime));
        dataMap.put(new SimpleLogObject("Time,Analog2", analog2, currTime));
        dataMap.put(new SimpleLogObject("Time,Analog3", analog3, currTime));
        dataMap.put(new SimpleLogObject("Time,Analog4", analog4, currTime));
        dataMap.put(new SimpleLogObject("Time,Voltage", volts, currTime));
        dataMap.put(new SimpleLogObject("Time,AirTemp", airTemp, currTime));
        dataMap.put(new SimpleLogObject("Time,Coolant", coolant, currTime));
    }

    private static String hashMapToCSV()
    {
        ArrayList<String> tags = dataMap.getTags();
        //output String
        StringBuilder out = new StringBuilder();
        //for each tag
        for(String tag : tags) {
            //get the tags data
            LinkedList<LogObject> los = dataMap.getList(tag);
            //append the tag and a new line
            out.append(tag);
            out.append("\n");
            //append all of the data objects
            for(LogObject lo : los) {
                out.append(lo.toString());
                out.append("\n");
            }
            //append END with new line
            out.append("END");
            out.append("\n");
        }

        return out.toString();
    }

    /**
     * All parsing from below is done using the PE3 protocol available on BOX
     * There are supplemental documents that explain why the string are being split the way they are
     * In basic different parts of the hexstring contain different data elements we are not to look at the element as a whole
     * Multiplications are done because of offsets.
     * There are two variables for each element because one part of the byte contains the high bits and the other contains the low order bits because of protocol
     * @param line
     */
    private static void parseGroupOne(String line) {

        int rpm1, rpm2;
        rpm1 = Integer.parseUnsignedInt(line.substring(0, 2), 16);
        rpm2 = Integer.parseUnsignedInt(line.substring(2,4), 16) * 256;
        RPM = rpm1 + rpm2;

        int tps1, tps2;
        tps1 = Integer.parseInt(line.substring(4, 6), 16);
        tps2 = Integer.parseInt(line.substring(6, 8), 16) * 256;
        TPS = tps1 + tps2;
        TPS *= .1;

        int fot1, fot2;
        fot1 = Integer.parseInt((line.substring(8,10)), 16);
        fot2 = Integer.parseInt((line.substring(10,12)), 16) * 256;
        FOT = fot1 + fot2;
        FOT *= .01;

        int ignAngle1, ignAngle2;
        ignAngle1 = Integer.parseInt((line.substring(12, 14)), 16);
        ignAngle2 = Integer.parseInt((line.substring(14, 16)), 16) * 256;
        ignAngle = ignAngle1 + ignAngle2;
        ignAngle *= .1;

    }

    private static void parseGroupTwo(String line)
    {

        double barometer1, barometer2;
        barometer1 = Integer.parseInt(line.substring(0,2), 16);
        barometer2 = Integer.parseInt(line.substring(2,4), 16) * 256;
        bar = barometer1 + barometer2;
        bar *= 0.01;

        double map1, map2;
        map1 = Integer.parseInt(line.substring(4,6), 16);
        map2 = Integer.parseInt(line.substring(6,8), 16) * 256;
        MAP = map1 + map2;
        MAP *= 0.01;

        double lambda1, lambda2;
        lambda1 = Integer.parseInt(line.substring(8,10), 16);
        lambda2 = Integer.parseInt(line.substring(10,12), 16) * 256;
        lambda = lambda1 + lambda2;
        lambda *= 0.001;

    }

    private static void parseGroupThree(String line) throws NumberFormatException
    {
        double in1, in2;

        in1 = Integer.parseInt(line.substring(0,2), 16);
        in2 = Integer.parseInt(line.substring(2,4), 16) * 256;
        analog1 = in1 + in2;
        analog1 *= 0.001;

        in1 = Integer.parseInt(line.substring(4,6), 16);
        in2 = Integer.parseInt(line.substring(6,8), 16) * 256;
        analog2 = in1 + in2;
        analog2 *= 0.001;

        in1 = Integer.parseInt(line.substring(8,10), 16);
        in2 = Integer.parseInt(line.substring(10,12), 16) * 256;
        analog3 = in1 + in2;
        analog3 *= 0.001;

        in1 = Integer.parseInt(line.substring(12,14), 16);
        in2 = Integer.parseInt(line.substring(14,16), 16) * 256;
        analog4 = in1 + in2;
        analog4 *= 0.001;

    }

    public static void parseGroupSix(String line)
    {

        double batVol1, batVol2;
        batVol1 = Integer.parseInt(line.substring(0,2), 16);
        batVol2 = Integer.parseInt(line.substring(2,4), 16) * 256;
        volts = batVol1 + batVol2;
        volts *= 0.01;

        double airTemp1, airTemp2;
        airTemp1 = Integer.parseInt(line.substring(4,6), 16);
        airTemp2 = Integer.parseInt(line.substring(6,8), 16) * 256;
        airTemp = airTemp1 + airTemp2;
        airTemp *= 0.1;

        double coolantTemp1, coolantTemp2;
        coolantTemp1 = Integer.parseInt(line.substring(8,10), 16);
        coolantTemp2 = Integer.parseInt(line.substring(10,12), 16) * 256;
        coolant = coolantTemp1 + coolantTemp2;
        coolant *= 0.1;

    }

    private static void parseGroupEight(String line)
    {

    }

    private static void parseGroupFourteen(String line)
    {

    }

    private static void parseGroupFifteen(String line)
    {

    }

    private static void parseGroupSixteen(String line)
    {

    }
}
