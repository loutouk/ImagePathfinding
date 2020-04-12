/**
 * Created by Louis Boursier
 */

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

public class Main {

    // divides the maze into chunk whose values are average of their cells
    // when set to 1, there is no compression and the maze is treated like the original input
    public static int DIVIDING_FACTOR = 1;
    public static BufferedImage imageBandW;
    public static BufferedImage imageOriginal;
    public static int cellsOnWidth;
    public static int cellsOnHeight;
    public static boolean[][] map;
    public static long VISUALIZER_UPDATE_RATE = 1; // speeds up the visualizing process by printing big chunks
    public static String[] HEURISTICS = {"NONE", "MANHATTAN", "EUCLIDEAN"};
    public static String HEURISTIC = HEURISTICS[2]; // euclidean is an admissible heuristic here (gives us A*)

    public static void main(String[] args){

        int X_START = 1049;
        int Y_START = 235;
        int X_END = 81;
        int Y_END = 601;

        try {

            File file = new File("bigField.png");
            imageBandW = ImageIO.read(file);
            File fileBis = new File("originalField.png");
            imageOriginal = ImageIO.read(fileBis);
            DIVIDING_FACTOR = 1;

            init();

            Point start = new Point(X_START, Y_START);
            Point end = new Point(X_END, Y_END);

            //quickDraw(pathfinding(map, start, end, false,null,null));

            visualDraw(start, end);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void init() {

        cellsOnWidth = imageBandW.getWidth()/DIVIDING_FACTOR;
        cellsOnHeight = imageBandW.getHeight()/DIVIDING_FACTOR;

        int[][] cells = new int[cellsOnHeight][cellsOnWidth];
        map = new boolean[cellsOnHeight][cellsOnWidth];

        // compute the sum of cells being an obstacle and cells being free for each chunk (DIVIDING_FACTOR)
        for(int line=0 ; line<imageBandW.getHeight() ; line++){
            for(int col=0 ; col<imageBandW.getWidth() ; col++){
                int clr =  imageBandW.getRGB(col,line);
                int obstacle = 0;
                if(clr == Color.WHITE.getRGB()) obstacle++;
                else if(clr == Color.BLACK.getRGB()) obstacle--;
                // TODO Should be black and white only...
                // TODO but prefer to add an obstacle for now
                else obstacle++; //System.err.println("Color detected that is neither black nor white!");
                cells[Math.max(0,(line/DIVIDING_FACTOR)-1)][Math.max(0,(col/DIVIDING_FACTOR)-1)] += obstacle;
            }

        }

        for(int i=0 ; i<cellsOnHeight ; i++){
            for(int j=0 ; j<cellsOnWidth ; j++){
                // set the map's point to either be free or an obstacle according to the average of the its cell
                if(cells[i][j] > 0) map[i][j] = true;
                else map[i][j] = false;
                // prints cells in the console
                /*if(i==start.y && j==start.x)System.out.print("S");
                else if(i==end.y && j==end.x) System.out.print("E");
                else System.out.print(map[i][j] ? "@" : " ");*/
            }
            System.out.println();
        }
    }

    private static void quickDraw(ArrayList<Point> bestPath) {
        // draw the shortest path
        Graphics2D g = (Graphics2D) imageOriginal.getGraphics();
        for(Point p : bestPath){
            g.setColor(Color.RED);
            Ellipse2D.Double circle = new Ellipse2D.Double(p.x*DIVIDING_FACTOR, p.y*DIVIDING_FACTOR, 4, 4);
            g.fill(circle);
        }

        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(imageOriginal)));
        frame.pack();
        frame.setVisible(true);


        Graphics2D gBis = (Graphics2D) imageBandW.getGraphics();
        for(Point p : bestPath){
            gBis.setColor(Color.RED);
            Ellipse2D.Double circle = new Ellipse2D.Double(p.x*DIVIDING_FACTOR, p.y*DIVIDING_FACTOR, 4, 4);
            gBis.fill(circle);
        }
        frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(imageBandW)));
        frame.pack();
        frame.setVisible(true);
    }

    private static void visualDraw(Point start, Point end) {

        Graphics2D graphics2D = (Graphics2D) imageOriginal.getGraphics();

        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(imageOriginal)));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Already there
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);
        frame.pack();


        for(int i=0 ; i<cellsOnHeight-1 ; i++) {
            for (int j = 0; j < cellsOnWidth; j++) {
                if(map[i][j]) graphics2D.setColor(new Color(43,49,61));
                else graphics2D.setColor(new Color(2,131,188));
                graphics2D.fillRect(j,i,DIVIDING_FACTOR,DIVIDING_FACTOR);
            }
        }

        graphics2D.setColor(Color.GREEN);
        Ellipse2D.Double circle = new Ellipse2D.Double(start.x, start.y, 16, 16);
        graphics2D.fill(circle);
        circle = new Ellipse2D.Double(end.x, end.y, 16, 16);
        graphics2D.fill(circle);


        frame.setVisible(true);


        pathfinding(map, start, end, true, frame, graphics2D);
    }

    private static ArrayList<Point> pathfinding(boolean[][] map, Point start, Point end, boolean visualizeMode, JFrame frame, Graphics2D graphics2D) {

        TreeSet<PointToCompute> pointsToCompute = null;

        // Purposely elude the equals case
        // Because we can have different vertices with same distance
        // And we still want to see them as different for the TreeSet to accept them
        // TODO find a more appropriate data structure

        if(HEURISTIC.equals("MANHATTAN")){
            pointsToCompute = new TreeSet<>(new Comparator<PointToCompute>() {
                @Override
                public int compare(PointToCompute o1, PointToCompute o2) {
                    return (o1.distance+(Math.abs(o1.point.x-end.x)+Math.abs(o1.point.y-end.y)) >
                            o2.distance+(Math.abs(o2.point.x-end.x)+Math.abs(o2.point.y-end.y))) ? 1 :  -1;
                }
            });
        }else if(HEURISTIC.equals("EUCLIDEAN")){
            pointsToCompute = new TreeSet<>(new Comparator<PointToCompute>() {
                @Override
                public int compare(PointToCompute o1, PointToCompute o2) {
                    return (o1.distance+Math.sqrt(Math.pow(o1.point.x-end.x, 2.0) + Math.pow(o1.point.y-end.y, 2.0))) >
                            o2.distance+Math.sqrt(Math.pow(o2.point.x-end.x, 2.0) + Math.pow(o2.point.y-end.y, 2.0)) ? 1 :  -1;
                }
            });
        }else if(HEURISTIC.equals("NONE")){
            pointsToCompute = new TreeSet<>(new Comparator<PointToCompute>() {
                @Override
                public int compare(PointToCompute o1, PointToCompute o2) { return o1.distance > o2.distance ? 1 :  -1; }
            });
        }else{
            System.err.println("Heuristic " + HEURISTIC + " is not defined!");
        }




        // starts with the... start :)
        pointsToCompute.add(new PointToCompute(0, start));

        int[][] distances = new int[map.length][map[0].length];
        Point[][] bestPoint = new Point[map.length][map[0].length];
        boolean[][] alreadyComputed = new boolean[map.length][map[0].length];

        setInitialDistances(distances, start);

        boolean done = false;

        long visualizerCount=0;


        /*try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        while(!done){

            if(pointsToCompute.size()==0) System.err.println("No path can be found!");

            Point current = pointsToCompute.pollFirst().point;

            if(visualizeMode && visualizerCount++%VISUALIZER_UPDATE_RATE==0){
                graphics2D.setColor(new Color(255,111,30));
                graphics2D.fillRect(current.x,current.y,(int)VISUALIZER_UPDATE_RATE,(int)VISUALIZER_UPDATE_RATE);
                SwingUtilities.updateComponentTreeUI(frame);
            }


            int currentDistance = distances[current.y][current.x];

            for(int i=0 ; i<8 ; i++){

                int posXneighbour=current.x;
                int posYneighbour=current.y;

                boolean outOfBouds = false;

                switch (i){
                    case 0:
                        if(current.x==0 || current.y==0) outOfBouds=true;
                        posXneighbour--;
                        posYneighbour--;
                        break;
                    case 1:
                        if(current.y==0) outOfBouds=true;
                        posYneighbour--;
                        break;
                    case 2:
                        if(current.y==0 || current.x==map[0].length-1) outOfBouds=true;
                        posYneighbour--;
                        posXneighbour++;
                        break;
                    case 3:
                        if(current.x==0) outOfBouds=true;
                        posXneighbour--;
                        break;
                    case 4:
                        if(current.x==map[0].length-1) outOfBouds=true;
                        posXneighbour++;
                        break;
                    case 5:
                        if(current.x==0 || current.y==map.length-1) outOfBouds=true;
                        posYneighbour++;
                        posXneighbour--;
                        break;
                    case 6:
                        if(current.y==map.length-1) outOfBouds=true;
                        posYneighbour++;
                        break;
                    case 7:
                        if(current.y==map.length-1 || current.x==map[0].length-1) outOfBouds=true;
                        posYneighbour++;
                        posXneighbour++;
                        break;
                }

                if(!outOfBouds){

                    if(map[posYneighbour][posXneighbour]) continue;

                    Point neighbour = new Point(posXneighbour, posYneighbour);
                    int tmpDistance = currentDistance + 1;

                    if(distances[posYneighbour][posXneighbour]>tmpDistance){
                        distances[posYneighbour][posXneighbour] = tmpDistance;
                        bestPoint[posYneighbour][posXneighbour] = current;
                        if(!alreadyComputed[posYneighbour][posXneighbour]){
                            pointsToCompute.add(new PointToCompute(tmpDistance, neighbour));
                        }
                    }
                }

            }

            alreadyComputed[current.y][current.x] = true;
            if(current.x == end.x && current.y == end.y){done = true;}
        }

        // trace back and build the shortest path from end point to start point
        ArrayList<Point> bestPath = new ArrayList<>();
        Point index = bestPoint[end.y][end.x];
        while(!(index.x == start.x && index.y == start.y)){
            bestPath.add(new Point(index.x, index.y));
            index = bestPoint[index.y][index.x];
        }

        return bestPath;
    }

    static class PointToCompute {

        int distance;
        Point point;

        public PointToCompute(int distance, Point point) {
            this.distance = distance;
            this.point = point;
        }
    }

    public static void setInitialDistances(int[][] array, Point start){
        for(int i=0 ; i<array.length ; i++){
            for(int j=0 ; j<array[0].length ; j++){
                array[i][j] = Integer.MAX_VALUE/2; // divide by two to avoid overflow (dirty trick)
            }
        }
        array[start.y][start.x] = 0; // starting point should not have a weight
    }
}
